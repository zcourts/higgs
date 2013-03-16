package io.higgs.http.server;

import io.higgs.HiggsServer;
import io.higgs.events.ChannelMessage;
import io.higgs.functional.Function1;
import io.higgs.http.server.config.ServerConfig;
import io.higgs.http.server.files.StaticResourceFilter;
import io.higgs.http.server.params.HttpSession;
import io.higgs.http.server.resource.DELETE;
import io.higgs.http.server.resource.GET;
import io.higgs.http.server.resource.HEAD;
import io.higgs.http.server.resource.OPTIONS;
import io.higgs.http.server.resource.POST;
import io.higgs.http.server.resource.PUT;
import io.higgs.http.server.transformers.HttpErrorTransformer;
import io.higgs.http.server.transformers.JsonTransformer;
import io.higgs.http.server.transformers.ThymeleafTransformer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static io.netty.handler.codec.http.HttpHeaders.getHeader;

/**
 * Consider http://asm.ow2.org/ later for ByteCode transformations  as opposed to the current reflection
 * based approach.
 * Considered making it JAX-RS compliant (http://jsr311.java.net/nonav/releases/1.1/spec/spec.html)
 * http://jersey.java.net/nonav/documentation/latest/user-guide.html
 * but that's more effort than required for now (Future versions maybe...). Its not meant to be a servlet container
 * after all...but many of the ideas used are stolen from
 * JAX-RS (http://jsr311.java.net/nonav/releases/1.1/javax/ws/rs/package-summary.html)
 * Class names are the same or similar so that in future it's easier to make it JAX-RS compliant
 * + as a bonus if you're familiar with JAX-RS you can hit the ground running.
 * JAX-RS is solid stuff so for the most part this HttpServer and its associated classes and operations
 * function in a similar way. Where seriously huge variations occur it is noted.  The first and obvious
 * one is that this is NOT  servlet container...don't expect it to act like one.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpServer<C extends ServerConfig> extends HiggsServer<String, HttpResponse, HttpRequest, Object> {
    protected Map<String, HttpSession> sessions = new ConcurrentHashMap<String, HttpSession>();
    protected Set<Endpoint> get = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
    protected Set<Endpoint> put = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
    protected Set<Endpoint> post = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
    protected Set<Endpoint> delete = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
    protected Set<Endpoint> head = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
    protected Set<Endpoint> options = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
    protected final AtomicReference<ParamInjector> injector = new AtomicReference<>();
    protected final PriorityBlockingQueue<ResponseTransformer> transformers =
            new PriorityBlockingQueue<>(10, new Comparator<ResponseTransformer>() {
                @Override
                public int compare(ResponseTransformer o1, ResponseTransformer o2) {
                    return o2.priority() < o1.priority() ? -1 : (o2.priority() == o1.priority() ? 0 : 1);
                }
            });
    protected final PriorityBlockingQueue<ResourceFilter> filters =
            new PriorityBlockingQueue<>(10, new Comparator<ResourceFilter>() {
                @Override
                public int compare(ResourceFilter o1, ResourceFilter o2) {
                    return o2.priority() < o1.priority() ? -1 : (o2.priority() == o1.priority() ? 0 : 1);
                }
            });
    protected C config;
    public static final String SID = "HS3-SESSION-ID";
    protected HttpConverter converter = new HttpConverter(this);

    public HttpServer(final C config) {
        super(8080);
        if (config == null) {
            throw new NullPointerException("You cannot create a server instance with a null getConfig");
        }
        this.config = config;
        setPort(config.port);
        parseConfig();
    }

    public HttpServer(Class<C> klass, String configFile) {
        super(8080);
        if (configFile == null || configFile.isEmpty()) {
            throw new RuntimeException(String.format("usage: %s path/to/config.yml", getClass().getName()));
        }
        Yaml yaml = new Yaml();
        try {
            config = yaml.loadAs(new FileInputStream(Paths.get(configFile).toFile()), klass);
        } catch (Throwable e) {
            log.error(String.format("The server cannot be started, unable to load config (%s)", configFile), e);
            //start up error. should not continue
            System.exit(-1);
        }
        setPort(config.port);
        parseConfig();
        setEnableProtocolSniffing(true);
        addProtocolDetector(new HttpDetector(this));
        setEnableGZip(false);
//        new ProtocolSniffer()
    }

    public void register(Class<?> klass) {
        Constructor[] ctors = klass.getConstructors();
        //only no arg ctor for now
        Constructor ctor = null;
        for (Constructor c : ctors) {
            if (c.getParameterTypes().length == 0) {
                ctor = c;
            }
        }
        if (ctor == null) {
            log.warn(String.format("No no arg constructor available in registered class '%s'", klass.getName()));
        } else {
            //register with no arg constructor
            register(klass, ctor);
        }
    }

    public void register(final Class<?> klass, final Constructor ctor, Object... constructorArgs) {
        Method[] methods = klass.getMethods();
        //for each method with an HTTP method annotation create an endpoint
        for (Method method : methods) {
            if (isEndPoint(method)) {
                Endpoint e = new Endpoint(this, klass, method, ctor, constructorArgs);
                register(e);
            }
        }
    }

    /**
     * Register an endpoint
     *
     * @param endpoint
     */
    public void register(final Endpoint endpoint) {
        if (endpoint == null) {
            throw new NullPointerException("Endpoint is null. Cannot register a null endpoint");
        }
        log.info(String.format("REGISTERED > %1$-20s", endpoint));
        boolean doListen = false;
        //allow one method to handle more than 1 request type
        if (endpoint.isGet()) {
            if (get.contains(endpoint)) {
                log.warn(String.format("Endpoint (%s) already exists", endpoint));
            } else {
                get.add(endpoint);
                doListen = true;
            }
        }
        if (endpoint.isDelete()) {
            if (delete.contains(endpoint)) {
                log.warn(String.format("Endpoint (%s) already exists", endpoint));
            } else {
                delete.add(endpoint);
                doListen = true;
            }
        }
        if (endpoint.isPost()) {
            if (post.contains(endpoint)) {
                log.warn(String.format("Endpoint (%s) already exists", endpoint));
            } else {
                post.add(endpoint);
                doListen = true;
            }
        }
        if (endpoint.isPut()) {
            if (put.contains(endpoint)) {
                log.warn(String.format("Endpoint (%s) already exists", endpoint));
            } else {
                put.add(endpoint);
                doListen = true;
            }
        }
        if (endpoint.isHead()) {
            if (head.contains(endpoint)) {
                log.warn(String.format("Endpoint (%s) already exists", endpoint));
            } else {
                head.add(endpoint);
                doListen = true;
            }
        }
        if (endpoint.isOptions()) {
            if (options.contains(endpoint)) {
                log.warn(String.format("Endpoint (%s) already exists", endpoint));
            } else {
                options.add(endpoint);
                doListen = true;
            }
        }
        if (doListen) {
            doListen(endpoint.getPath());
        }
    }

    private void doListen(final String path) {
        //subscribe to requests using the un-parsed string path
        listen(path, new Function1<ChannelMessage<HttpRequest>>() {
            public void apply(final ChannelMessage<HttpRequest> a) {
                HttpRequest request = a.message;
                Endpoint endpoint = request.getEndpoint();
                endpoint.invoke(a);
            }
        });
    }

    private boolean isEndPoint(final Method method) {
        if (method.isAnnotationPresent(GET.class) ||
                method.isAnnotationPresent(DELETE.class) ||
                method.isAnnotationPresent(POST.class) ||
                method.isAnnotationPresent(PUT.class) ||
                method.isAnnotationPresent(HEAD.class) ||
                method.isAnnotationPresent(OPTIONS.class)
                ) {
            return true;
        }
        return false;
    }

    public Object serialize(final Channel ctx, final HttpResponse msg) {
        return converter.serialize(ctx, msg);
    }

    public HttpRequest deserialize(final ChannelHandlerContext ctx, final Object msg) {
        return converter.deserialize(ctx, msg);
    }

    public String getTopic(final HttpRequest msg) {
        Endpoint endpoint = null;
        ArrayList<ResourceFilter> arr = new ArrayList<>(filters);
        Collections.sort(arr, filters.comparator());
        for (ResourceFilter filter : arr) {
            endpoint = filter.getEndpoint(msg);
            if (endpoint != null) {
                break;
            }
        }
        if (endpoint != null) {
            //always set the endpoint the request matches
            msg.setEndpoint(endpoint);
            //if endpoint is unregistered and using default unregistered listener
            if (endpoint.isUnregistered() && endpoint.useDefaultUnregisteredListener()) {
                doListen(endpoint.getPath());
            }
            return endpoint.getPath();
        }
        if (!msg.isSupportedMethod()) {
            throw new WebApplicationException(HttpStatus.METHOD_NOT_ALLOWED, msg);
        }
        //if endpoint is null then no resource is found.
        throw new WebApplicationException(HttpStatus.NOT_FOUND, msg);
    }

    protected boolean setupPipeline(final ChannelPipeline pipeline) {
        if (!enableProtocolSniffing) {
            //these are added dynamically and automatically if sniffing is enabled
            pipeline.addLast("decoder", new HttpRequestDecoder());
            pipeline.addLast("encoder", new HttpResponseEncoder());
            pipeline.addLast("deflater", new HttpContentCompressor());
            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
            return true; //add handler
        }
        return false;
    }

    public void addResponseTransformer(ResponseTransformer rt) {
        transformers.add(rt);
    }

    public void removeResponseTransformer(ResponseTransformer rt) {
        transformers.remove(rt);
    }

    /**
     * Retrieves the head of the LIFO queue of transformers.
     * This transformer is not removed from the queue, it simply returns a reference to the transformer that
     * is to be used first to handle response transformations.
     *
     * @return
     */
    public ResponseTransformer getResponseTransformer() {
        return transformers.peek();
    }

    /**
     * Finds an instance of the response transformer represented by the given class.
     * If no such instance is registered then it returns null
     * NOTE: Any subclass of the given class will be considered an instance.
     *
     * @param rtc The response transformer class to search for
     * @return
     */
    public <T extends ResponseTransformer> T getResponseTransformer(Class<T> rtc) {
        Iterator<ResponseTransformer> it = transformers.iterator();
        while (it.hasNext()) {
            ResponseTransformer rt = it.next();
            if (rtc.isAssignableFrom(rt.getClass())) {
                return (T) rt;
            }
        }
        return null;
    }

    /**
     * Removes the given endpoint from all HTTP methods
     *
     * @param endpoint
     */
    public void removeEndpoint(final Endpoint endpoint) {
        get.remove(endpoint);
        post.remove(endpoint);
        put.remove(endpoint);
        delete.remove(endpoint);
        head.remove(endpoint);
        options.remove(endpoint);
    }

    public C getConfig() {
        return config;
    }

    public ResourceFilter getFilter() {
        return filters.peek();
    }

    public void addFilter(final ResourceFilter filter) {
        if (filter == null) {
            throw new NullPointerException("You cannot set the resource filter to null");
        }
        this.filters.add(filter);
    }

    public Set<Endpoint> getGetEndpoints() {
        return get;
    }

    public Set<Endpoint> getPostEndpoints() {
        return post;
    }

    public Set<Endpoint> getPutEndpoints() {
        return put;
    }

    public Set<Endpoint> getDeleteEndpoints() {
        return delete;
    }

    public Set<Endpoint> getHeadEndpoints() {
        return head;
    }

    public Set<Endpoint> getOptionsEndpoints() {
        return options;
    }

    public ParamInjector getInjector() {
        return injector.get();
    }

    public void setInjector(ParamInjector i) {
        injector.lazySet(i);
    }

    public HttpResponse processResponse(final Object returns, final ChannelMessage<HttpRequest> a,
                                        Endpoint endpoint) {
        HttpResponse response = null;
        //note the order of the iterator is guaranteed to be LIFO as expected
        ArrayList<ResponseTransformer> arr = new ArrayList<>(transformers);
        Collections.sort(arr, transformers.comparator());
        for (ResponseTransformer transformer : arr) {
            if (transformer.canTransform(returns, a.message)) {
                response = transformer.transform(this, returns, a.message, transformers);
                if (response != null) {
                    break;
                }
            }
        }
        if (response == null) {
            //status not acceptable by default unless a more suitable error is found from the error resolvers
            throw new WebApplicationException(HttpStatus.NOT_ACCEPTABLE, a.message);
        }
        return response;
    }

    private void parseConfig() {
        DiskFileUpload.deleteOnExitTemporaryFile = config.files.delete_temp_on_exit; // should delete file
        // on exit (in normal
        // exit)
        DiskFileUpload.baseDirectory = config.files.temp_directory; // system temp directory
        DiskAttribute.deleteOnExitTemporaryFile = config.files.delete_temp_on_exit; // should delete file on
        // exit (in normal exit)
        DiskAttribute.baseDirectory = config.files.temp_directory; // system temp directory
        //add static file filter before so that it becomes the fallback after the default filter
        if (config.add_static_resource_filter) {
            addFilter(new StaticResourceFilter(this));
        }
        if (config.add_default_resource_filter) {
            addFilter(new DefaultResourceFilter(this));
        }
        if (config.add_default_injector) {
            injector.set(new DefaultParamInjector());
        }
        if (config.add_json_transformer) {
            addResponseTransformer(new JsonTransformer());
        }
        if (config.add_thymeleaf_transformer) {
            addResponseTransformer(new ThymeleafTransformer(config.template_config));
        }
        if (config.add_default_error_transformer) {
            addResponseTransformer(new HttpErrorTransformer(this,
                    new JsonTransformer(),
                    new ThymeleafTransformer(config.template_config))
            );
        }
    }

    public Map<String, HttpSession> getSessions() {
        return sessions;
    }

    public HttpSession getSession(final String sessionID) {
        return sessions.get(sessionID);
    }

    public ChannelFuture respond(Channel channel, HttpResponse response) {
        Object obj = getRequest(channel);
        if (obj instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) obj;
            //always send session cookie ID
            if (request.isNewSession()) {
                response.setCookie(request.getSession());
            }
            response.finalizeCustomHeaders();
            if (config.log_requests) {
                SocketAddress address = channel.remoteAddress();
                //going with the Apache format
                //194.116.215.20 - [14/Nov/2005:22:28:57 +0000] “GET / HTTP/1.0″ 200 16440
                log.info(String.format("%s - [%s] \"%s %s %s\" %s %s",
                        address,
                        HttpHeaders.getDate(request, request.getCreatedAt().toDate()),
                        request.getMethod().name(),
                        request.getUri(),
                        request.getProtocolVersion(),
                        response.getStatus().code(),
                        getHeader(response, HttpHeaders.Names.CONTENT_LENGTH) == null ?
                                response.data().writerIndex() : HttpHeaders.getContentLength(response)
                ));
            }
        } else {
            response.finalizeCustomHeaders();
        }
        return super.respond(channel, response);
    }
}
