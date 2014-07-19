package io.higgs.http.server.protocol;

import io.higgs.core.InvokableMethod;
import io.higgs.core.ObjectFactory;
import io.higgs.core.ResourcePath;
import io.higgs.core.reflect.dependency.DependencyProvider;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.MethodParam;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.params.ValidationResult;
import io.higgs.http.server.resource.MediaType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

public class HttpMethod extends InvokableMethod {
    private MethodParam[] params = new MethodParam[0];
    private LinkedList<MediaType> producesMediaTypes = new LinkedList<>();
    private LinkedList<MediaType> consumesMediaTypes = new LinkedList<>();
    /**
     * Path to an HTML template file used to format responses
     */
    private String template;
    private ValidationResult validationResult;
    private String[] fragments = new String[0];
    private List<VERB> verbs = new ArrayList<>();

    public HttpMethod(Queue<ObjectFactory> factories, Class<?> klass, Method classMethod) {
        super(factories, klass, classMethod);
        parseMediaTypes();
    }

    public void parseMediaTypes() {
        String[] classProduces = new String[0], methodProduces = new String[0];
        if (klass.isAnnotationPresent(Produces.class)) {
            Produces produces = klass.getAnnotation(Produces.class);
            classProduces = produces.value() != null ?
                    produces.value() : new String[]{ MediaType.WILDCARD };
        }
        if (classMethod.isAnnotationPresent(Produces.class)) {
            Produces path = classMethod.getAnnotation(Produces.class);
            methodProduces = path.value() != null ? path.value() : new String[]{ MediaType.WILDCARD };
        }
        String[] mTypes = new String[classProduces.length + methodProduces.length];
        System.arraycopy(classProduces, 0, mTypes, 0, classProduces.length);
        System.arraycopy(methodProduces, 0, mTypes, classProduces.length, methodProduces.length);
        for (String mType : mTypes) {
            List<MediaType> mediaTypeList = MediaType.valueOf(mType);
            producesMediaTypes.addAll(mediaTypeList);
        }
        //TODO remove repeated code for consumes and produces
        String[] classConsumes = new String[0], methodConsumes = new String[0];
        if (klass.isAnnotationPresent(Consumes.class)) {
            Consumes consumes = klass.getAnnotation(Consumes.class);
            classConsumes = consumes.value() != null ?
                    consumes.value() : new String[]{ MediaType.WILDCARD };
        }
        if (classMethod.isAnnotationPresent(Consumes.class)) {
            Consumes path = classMethod.getAnnotation(Consumes.class);
            methodConsumes = path.value() != null ? path.value() : new String[]{ MediaType.WILDCARD };
        }
        String[] consumesTypes = new String[classConsumes.length + methodConsumes.length];
        System.arraycopy(classConsumes, 0, consumesTypes, 0, classConsumes.length);
        System.arraycopy(methodConsumes, 0, consumesTypes, classConsumes.length, methodConsumes.length);
        for (String mType : consumesTypes) {
            List<MediaType> mediaTypeList = MediaType.valueOf(mType);
            consumesMediaTypes.addAll(mediaTypeList);
        }
    }

    public LinkedList<MediaType> getConsumesMediaTypes() {
        return consumesMediaTypes;
    }

    public boolean produces(MediaType... mediaTypes) {
        for (MediaType mt : producesMediaTypes) {
            for (MediaType mt2 : mediaTypes) {
                if (mt.isCompatible(mt2)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasProduces() {
        return getProducesMediaTypes().size() > 0;
    }

    public LinkedList<MediaType> getProducesMediaTypes() {
        return producesMediaTypes;
    }

    @Override
    protected Object[] injectParameters(ChannelHandlerContext ctx, Object msg, Object[] params, Object instance,
                                        DependencyProvider deps) {
        //http handler has already injected everything it needs to
        return params;
    }

    @Override
    public boolean matches(String requestPath, ChannelHandlerContext ctx, Object msg) {
        ResourcePath resourcePath = path();
        if (resourcePath.matches(requestPath)) {
            if (!(msg instanceof HttpRequest)) {
                //if not an HttpRequest but the path matches then return true
                return true;
            } else {
                //if it is an http request the the media type must also match, if set
                HttpRequest request = (HttpRequest) msg;
                request.setPath(resourcePath);
                //firstly does the request's verb matches the method's
                if (!matchesVerb(request.getMethod().name())) {
                    return false; //if verb doesn't match nothing else matters
                }
                //does the method limit the content type it consumes?
                if (consumesMediaTypes.size() > 0) {
                    //is there a content type and does the method consume the content type supplied?
                    String strType = request.headers() == null ? null : request.headers().get(CONTENT_TYPE);
                    if (strType != null && !strType.isEmpty()) {
                        LinkedList<MediaType> contentType = MediaType.valueOf(strType);
                        boolean consumesType = false;
                        for (MediaType consumesMediaType : consumesMediaTypes) {
                            for (MediaType type : contentType) {
                                if (consumesMediaType.isCompatible(type)) {
                                    consumesType = true;
                                    break;
                                }
                            }
                        }
                        //if after all the consumed media types are enumerated and none match then return false
                        if (!consumesType) {
                            return false;
                        }
                    }
                }
                //does the method or it's class have the @Produces annotation?
                if (producesMediaTypes.size() > 0) {
                    //if so does this method produce a media type which matches what the client accepts
                    for (MediaType producesMediaType : producesMediaTypes) {
                        for (MediaType acceptedMediaType : request.getAcceptedMediaTypes()) {
                            if (producesMediaType.isCompatible(acceptedMediaType)) {
                                //set the matched media type to the type the class produces
                                request.setMatchedMediaType(producesMediaType);
                                return true;
                            }
                        }
                    }
                    //path matched but media type didn't
                    log.debug(String.format("template %s matched %s but no compatible media types found",
                            requestPath, resourcePath.getUri()));
                    throw new WebApplicationException(HttpResponseStatus.NOT_ACCEPTABLE, request);
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean matchesVerb(String verb) {
        if (verbs.isEmpty()) {
            //by default if no verb annotation is specified the method responds to everything
            return true;
        }
        //if any one of the verb annotations match return true
        for (VERB v : verbs) {
            if (v.matches(verb)) {
                return true;
            }
        }
        //all else fails return false
        return false;
    }

    /**
     * Sets the given parameter
     *
     * @param param the param to set
     */
    public void setParam(MethodParam param) {
        params[param.getPosition()] = param;
    }

    /**
     * @return All the {@link MethodParam}s accepted by this method
     */
    public MethodParam[] getParams() {
        return params;
    }

    /**
     * Initialises the set of parameters to the given length
     */
    public void initialiseParams(int length) {
        params = new MethodParam[length];
    }

    /**
     * @return true if this method is annotated with a template which can be used by Thymeleaf
     */
    public boolean hasTemplate() {
        return template != null || fragments.length > 0;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setTemplate(String[] fragments) {
        this.fragments = fragments;
    }

    public String[] getFragments() {
        return fragments;
    }

    public boolean hasFragments() {
        return fragments.length > 0;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    public void addVerb(VERB a) {
        if (a != null) {
            verbs.add(a);
        }
    }

    public static enum VERB {
        GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE"), HEAD("HEAD"), OPTIONS("OPTIONS");
        public final String value;

        VERB(String a) {
            value = a;
        }

        public boolean matches(String method) {
            return value.equalsIgnoreCase(method);
        }
    }
}
