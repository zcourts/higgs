package com.fillta.higgs.http.server;

import com.fillta.functional.Function1;
import com.fillta.higgs.HiggsServer;
import com.fillta.higgs.MessageConverter;
import com.fillta.higgs.MessageTopicFactory;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.http.server.config.ServerConfig;
import com.fillta.higgs.http.server.files.StaticResourceFilter;
import com.fillta.higgs.http.server.params.HttpSession;
import com.fillta.higgs.http.server.resource.*;
import com.fillta.higgs.http.server.transformers.HttpErrorTransformer;
import com.fillta.higgs.http.server.transformers.JsonTransformer;
import com.fillta.higgs.http.server.transformers.ThymeleafTransformer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Consider http://asm.ow2.org/ later for ByteCode transformations  as opposed to the current reflection
 * based approach.
 * Considered making it JAX-RS compliant (http://jsr311.java.net/nonav/releases/1.1/spec/spec.html)
 * http://jersey.java.net/nonav/documentation/latest/user-guide.html
 * but that's more effort than required for now (Future versions maybe...). Its not meant to be a servlet container
 * after all...but many of the ideas used are stolen from JAX-RS (http://jsr311.java.net/nonav/releases/1.1/javax/ws/rs/package-summary.html)
 * Class names are the same or similar so that in future it's easier to make it JAX-RS compliant
 * + as a bonus if you're familiar with JAX-RS you can hit the ground running.
 * JAX-RS is solid stuff so for the most part this HttpServer and its associated classes and operations
 * function in a similar way. Where seriously huge variations occur it is noted.  The first and obvious
 * one is that this is NOT  servlet container...don't expect it to act like one.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpServer extends HiggsServer<String, HttpResponse, HttpRequest, Object> {
	private Map<String, HttpSession> sessions = new ConcurrentHashMap<String, HttpSession>();
	private Set<Endpoint> get = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
	private Set<Endpoint> put = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
	private Set<Endpoint> post = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
	private Set<Endpoint> delete = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
	private Set<Endpoint> head = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
	private Set<Endpoint> options = Collections.newSetFromMap(new ConcurrentHashMap<Endpoint, Boolean>());
	private final AtomicReference<ParamInjector> injector = new AtomicReference<>();
	private final LinkedBlockingDeque<ResponseTransformer> transformers = new LinkedBlockingDeque<>();
	private final LinkedBlockingDeque<ResourceFilter> filters = new LinkedBlockingDeque<>();
	//default settings
	private ServerConfig config;
	public static final String SID = "HS3SESSIONID";

	public HttpServer() {
		this(new ServerConfig());
	}

	public HttpServer(final ServerConfig config) {
		super(8080);
		if (config == null)
			throw new NullPointerException("You cannot create a server instance with a null getConfig");
		this.config = config;
		setPort(config.port);
		parseConfig();
	}

	public HttpServer(String configFile) {
		super(8080);
		if (configFile == null || configFile.isEmpty()) {
			throw new RuntimeException(String.format("usage: %s path/to/config.yml", getClass().getName()));
		}
		Yaml yaml = new Yaml();
		try {
			config = yaml.loadAs(Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(configFile)
					, ServerConfig.class);
		} catch (Exception e) {
			log.error(String.format("The service cannot be started, its config file was not found (%s)", configFile), e);
			//start up error. should not continue
			System.exit(-1);
		}
		setPort(config.port);
		parseConfig();
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
		if (endpoint == null)
			throw new NullPointerException("Endpoint is null. Cannot register a null endpoint");
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

	public MessageTopicFactory<String, HttpRequest> topicFactory() {
		return new MessageTopicFactory<String, HttpRequest>() {
			//get URL from request and find method name
			public String extract(final HttpRequest msg) {
				Endpoint endpoint = null;
				//note the order of the iterator is guaranteed to be LIFO as expected
				Iterator<ResourceFilter> it = filters.descendingIterator();
				while (it.hasNext()) {
					ResourceFilter filter = it.next();
					endpoint = filter.getEndpoint(msg);
					if (endpoint != null)
						break;
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
		};
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
		Iterator<ResponseTransformer> it = transformers.descendingIterator();
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

	public ServerConfig getConfig() {
		return config;
	}

	public ChannelInitializer<SocketChannel> initializer() {
		return new HttpServerInitializer(this, true, false);
	}

	public ResourceFilter getFilter() {
		return filters.peek();
	}

	public void addFilter(final ResourceFilter filter) {
		if (filter == null)
			throw new NullPointerException("You cannot set the resource filter to null");
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
		Iterator<ResponseTransformer> it = transformers.descendingIterator();
		while (it.hasNext()) {
			ResponseTransformer transformer = it.next();
			if (transformer.canTransform(returns, a.message)) {
				response = transformer.transform(this, returns, a.message,
						transformers);
				if (response != null)
					break;
			}
		}
		if (response == null) {
			//status not acceptable by default unless a more suitable error is found from the error resolvers
			throw new WebApplicationException(HttpStatus.NOT_ACCEPTABLE, a.message);
		}
		//always send session cookie ID
		if (a.message.isNewSession()) {
			response.setCookie(a.message.getCookie(SID));
		}
		response.finalizeCustomHeaders();
		return response;
	}

	public MessageConverter<HttpRequest, HttpResponse, Object> serializer() {
		//todo on post requests files can be split across multiple calls to de-serialize
		//in such cases this can be optimized to return the same HttpConverter instance
		return new HttpConverter(this);
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
}
