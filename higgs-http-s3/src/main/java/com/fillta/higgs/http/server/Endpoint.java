package com.fillta.higgs.http.server;

import com.fillta.functional.Function;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.http.server.config.ServerConfig;
import com.fillta.higgs.http.server.params.CookieParam;
import com.fillta.higgs.http.server.params.FormParam;
import com.fillta.higgs.http.server.params.HeaderParam;
import com.fillta.higgs.http.server.params.PathParam;
import com.fillta.higgs.http.server.params.QueryParam;
import com.fillta.higgs.http.server.params.ResourcePath;
import com.fillta.higgs.http.server.resource.DELETE;
import com.fillta.higgs.http.server.resource.GET;
import com.fillta.higgs.http.server.resource.HEAD;
import com.fillta.higgs.http.server.resource.OPTIONS;
import com.fillta.higgs.http.server.resource.POST;
import com.fillta.higgs.http.server.resource.PUT;
import com.fillta.higgs.http.server.resource.Path;
import com.fillta.higgs.http.server.resource.Resource;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;

/**
 * An immutable object representing an HTTP end point
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Endpoint {
    private Class<?> klass;
    private Constructor ctor;
    private Method method;
    Object instance;
    private boolean get;
    private boolean delete;
    private boolean post;
    private boolean put;
    private boolean head;
    private boolean options;
    private String path;
    private Object[] constructorArgs;
    private final HttpServer server;
    private Logger log = LoggerFactory.getLogger(getClass());
    private MethodParam[] params;
    private String template;
    private String unregisteredID;
    private boolean useDefaultUnregisteredListener = true;

    /**
     * Creates an unregistered Endpoint with the given ID. Using this constructor causes
     * the endpoint's {@link #getPath()} to return the id parameter provided instead of the path
     * extracted from the method. This means a subscriber needs to be listening to this ID
     * on the server in order to receive requests to the endpoint
     *
     * @param id the ID which becomes the path
     */
    public Endpoint(String id, HttpServer server, Class<?> klass, Method method, Constructor ctor,
                    Object... constructorArgs) {
        this(server, klass, method, ctor, constructorArgs);
        unregisteredID = id;
    }

    /**
     * Create an HTTP endpoint
     *
     * @param klass           the class that declares the given method.
     * @param method          the method that is invoked for this endpoint
     * @param ctor            The constructor used to create new instances of the given class
     * @param constructorArgs arguments passed to the constructor when creating a new instance
     */
    public Endpoint(final HttpServer server, final Class<?> klass, final Method method, final Constructor ctor,
                    Object... constructorArgs) {
        this.klass = klass;
        this.ctor = ctor;
        this.method = method;
        this.constructorArgs = constructorArgs;
        this.server = server;
        parseAnnotations();
        parseMethodParameters();
    }

    public void parseAnnotations() {
        //parse server getConfig related to end points before parsing any resource annotations because resource
        //annotations override the global server getConfig
        parseServerConfig();
        //does it have a resource getConfig?
        if (klass.isAnnotationPresent(Resource.class)) {
            parseResourceOptions();
        }
        String classPath = null, methodPath = null;
        if (klass.isAnnotationPresent(Path.class)) {
            Path path = klass.getAnnotation(Path.class);
            classPath = path.value() != null && !path.value().isEmpty() ? path.value() : "/";
        }
        if (method.isAnnotationPresent(Path.class)) {
            Path path = method.getAnnotation(Path.class);
            methodPath = path.value() != null && !path.value().isEmpty() ? path.value() : "/";
            if (path.template() != null && !path.template().isEmpty()) {
                template = path.template();
            }
        }
        if (classPath == null) {
            classPath = "/";
        }
        if (methodPath == null) {
            methodPath = "/";
        }
        if (methodPath.startsWith("/") && classPath.endsWith("/")) {
            methodPath = methodPath.substring(1);
        }
        if (!methodPath.startsWith("/") && !classPath.endsWith("/")) {
            classPath += "/";
        }
        path = classPath + methodPath;
        if (method.isAnnotationPresent(GET.class)) {
            get = true;
        }
        if (method.isAnnotationPresent(PUT.class)) {
            put = true;
        }
        if (method.isAnnotationPresent(POST.class)) {
            post = true;
        }
        if (method.isAnnotationPresent(DELETE.class)) {
            delete = true;
        }
        if (method.isAnnotationPresent(HEAD.class)) {
            head = true;
        }
        if (method.isAnnotationPresent(OPTIONS.class)) {
            options = true;
        }
    }

    /**
     * Get the name of the template associated with this endpoint
     *
     * @return
     */
    public String getTemplate() {
        return template;
    }

    public boolean hasTemplate() {
        return template != null;
    }

    private void parseServerConfig() {
        ServerConfig config = server.getConfig();
        if (!config.instance_per_request) {
            instance = createInstance();
        }
    }

    private void parseResourceOptions() {
        Resource r = klass.getAnnotation(Resource.class);
        if (r.singleton()) {
            instance = createInstance();
        }
    }

    private Object createInstance() {
        try {
            return ctor.newInstance(constructorArgs);
        } catch (InstantiationException e) {
            log.warn(String.format("Unable to create instance of '%s'", klass.getName()), e);
        } catch (IllegalAccessException e) {
            log.warn(String.format("Unable to access constructor of '%s'", klass.getName()), e);
        } catch (InvocationTargetException e) {
            log.warn(String.format("Unable to create instance of '%s'", klass.getName()), e);
        }
        //remove endpoint from server, it is not valid
        server.removeEndpoint(this);
        return null;
    }

    public Object getInstance() {
        //its a singleton if instance is not null
        if (instance != null) {
            return instance;
        } else {
            //otherwise an instance per request
            return createInstance();
        }
    }

    /**
     * @return A new {@link ResourcePath} for this endpoint.
     *         A new instance must be used because the class is not thread safe.
     */
    public ResourcePath newPath() {
        return new ResourcePath(path);
    }

    private void parseMethodParameters() {
        Class<?>[] parameters = method.getParameterTypes();
        params = new MethodParam[parameters.length];
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            Annotation[] annotations1 = annotations[i];
            Class<?> param = parameters[i];
            MethodParam methodParam = new MethodParam();
            params[i] = methodParam;
            methodParam.setClass(param);
            for (Annotation annotation : annotations1) {
                if (annotation.annotationType().isAssignableFrom(PathParam.class)) {
                    PathParam pathParam = (PathParam) annotation;
                    if (pathParam.value() != null && !pathParam.value().isEmpty()) {
                        methodParam.setPathParam(true);
                        methodParam.setName(pathParam.value());
                    }
                } else {
                    if (annotation.annotationType().isAssignableFrom(QueryParam.class)) {
                        QueryParam queryParam = (QueryParam) annotation;
                        if (queryParam.value() != null && !queryParam.value().isEmpty()) {
                            methodParam.setQueryParam(true);
                            methodParam.setName(queryParam.value());
                        }
                    } else {
                        if (annotation.annotationType().isAssignableFrom(FormParam.class)) {
                            FormParam formParam = (FormParam) annotation;
                            if (formParam.value() != null && !formParam.value().isEmpty()) {
                                methodParam.setFormParam(true);
                                methodParam.setName(formParam.value());
                            }
                        } else {
                            if (annotation.annotationType().isAssignableFrom(CookieParam.class)) {
                                CookieParam cookieParam = (CookieParam) annotation;
                                if (cookieParam.value() != null && !cookieParam.value().isEmpty()) {
                                    methodParam.setCookieParam(true);
                                    methodParam.setName(cookieParam.value());
                                }
                            } else {
                                if (annotation.annotationType().isAssignableFrom(HeaderParam.class)) {
                                    HeaderParam headerParam = (HeaderParam) annotation;
                                    if (headerParam.value() != null && !headerParam.value().isEmpty()) {
                                        methodParam.setHeaderParam(true);
                                        methodParam.setName(headerParam.value());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void invoke(final ChannelMessage<HttpRequest> a) {
        Object[] args = new Object[params.length];
        server.getInjector().injectParams(server, params, a, args);
        try {
            Object returns = method.invoke(getInstance(), args);
            if (returns instanceof io.netty.handler.codec.http.HttpResponse) {
                //if endpoint returns an HttpResponse, don't touch it, just send it to the client
                ChannelFuture future = server.respond(a.channel, (HttpResponse) returns);
                if (!isKeepAlive(a.message)) {
                    future.addListener(ChannelFutureListener.CLOSE);
                }
                return;
            }
            //if a function is returned it is assumed that function will handle writing the response
            if (returns instanceof Function) {
                ((Function) returns).apply();
                return;
            }
            if (returns instanceof WebApplicationException) {
                ((WebApplicationException) returns).setRequest(a.message);
            }
            ChannelFuture future = server.respond(a.channel, server.processResponse(returns, a, this));
            if (!isKeepAlive(a.message)) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
            //make sure we return here
            return;
        } catch (Throwable t) {
            if (t.getCause() instanceof WebApplicationException) {
                WebApplicationException wae = new WebApplicationException((WebApplicationException) t.getCause());
                wae.setRequest(a.message);
                throw wae;
            }
            if (t instanceof InvocationTargetException) {
                log.warn(String.format("'%s' unable to invoke method", method.getName()), t);
            }
            if (t instanceof IllegalAccessException) {
                log.warn(String.format("'%s' is not accessible.", method.getName()), t);
            }
            if (t instanceof IllegalArgumentException)
            //todo print expected param types, the actual param types and their values
            {
                log.warn(String.format("Injector created an invalid set of arguments for the resource : '%s' .",
                        method.getName()), t);
            }
            //if an exception occurs and we get here then internal server error
            throw new WebApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, a.message);
        }
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getKlass() {
        return klass;
    }

    public boolean isGet() {
        return get;
    }

    public boolean isDelete() {
        return delete;
    }

    public boolean isPost() {
        return post;
    }

    public boolean isPut() {
        return put;
    }

    public boolean isHead() {
        return head;
    }

    public boolean isOptions() {
        return options;
    }

    public boolean isUnregistered() {
        return unregisteredID != null;
    }

    public String getPath() {
        if (isUnregistered()) {
            return unregisteredID;
        }
        return path;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "path='" + path + '\'' +
                ", method=" + method +
                ", get=" + get +
                ", delete=" + delete +
                ", post=" + post +
                ", put=" + put +
                ", head=" + head +
                ", options=" + options +
                ", class=" + klass.getName() +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Endpoint)) {
            return false;
        }

        final Endpoint endpoint = (Endpoint) o;

        if (delete != endpoint.delete) {
            return false;
        }
        if (get != endpoint.get) {
            return false;
        }
        if (head != endpoint.head) {
            return false;
        }
        if (options != endpoint.options) {
            return false;
        }
        if (post != endpoint.post) {
            return false;
        }
        if (put != endpoint.put) {
            return false;
        }
        if (!ctor.equals(endpoint.ctor)) {
            return false;
        }
        if (!instance.equals(endpoint.instance)) {
            return false;
        }
        if (!klass.equals(endpoint.klass)) {
            return false;
        }
        if (!log.equals(endpoint.log)) {
            return false;
        }
        if (!method.equals(endpoint.method)) {
            return false;
        }
        if (!path.equals(endpoint.path)) {
            return false;
        }
        if (!server.equals(endpoint.server)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = klass.hashCode();
        result = 31 * result + ctor.hashCode();
        result = 31 * result + method.hashCode();
        result = 31 * result + (get ? 1 : 0);
        result = 31 * result + (delete ? 1 : 0);
        result = 31 * result + (post ? 1 : 0);
        result = 31 * result + (put ? 1 : 0);
        result = 31 * result + (head ? 1 : 0);
        result = 31 * result + (options ? 1 : 0);
        result = 31 * result + path.hashCode();
        result = 31 * result + Arrays.hashCode(constructorArgs);
        return result;
    }

    /**
     * If true then a listener is subscribed to this endpoint's path if the endpoint is unregistered
     *
     * @return
     */
    public boolean useDefaultUnregisteredListener() {
        return useDefaultUnregisteredListener;
    }

    public void setDefaultUnregisteredListener(boolean b) {
        useDefaultUnregisteredListener = b;
    }

    public static class MethodParam {
        private Class<?> methodClass;
        private String name;
        private boolean queryParam;
        private boolean pathParam;
        private boolean formParam;
        private boolean headerParam;
        private boolean cookieParam;

        public Class<?> getMethodClass() {
            return methodClass;
        }

        public void setClass(final Class<?> methodClass) {
            this.methodClass = methodClass;
        }

        public String getName() {
            return name;
        }

        public boolean isNamed() {
            return name != null;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public boolean isQueryParam() {
            return queryParam;
        }

        public void setQueryParam(final boolean queryParam) {
            this.queryParam = queryParam;
        }

        public boolean isPathParam() {
            return pathParam;
        }

        public void setPathParam(final boolean pathParam) {
            this.pathParam = pathParam;
        }

        public boolean isFormParam() {
            return formParam;
        }

        public void setFormParam(final boolean formParam) {
            this.formParam = formParam;
        }

        public boolean isHeaderParam() {
            return headerParam;
        }

        public void setHeaderParam(final boolean headerParam) {
            this.headerParam = headerParam;
        }

        public boolean isCookieParam() {
            return cookieParam;
        }

        public void setCookieParam(final boolean cookieParam) {
            this.cookieParam = cookieParam;
        }

        public String toString() {
            return "MethodParam{" +
                    "methodClass=" + methodClass.getName() +
                    ", name='" + name + '\'' +
                    ", queryParam=" + queryParam +
                    ", pathParam=" + pathParam +
                    ", formParam=" + formParam +
                    ", headerParam=" + headerParam +
                    ", cookieParam=" + cookieParam +
                    '}';
        }
    }
}
