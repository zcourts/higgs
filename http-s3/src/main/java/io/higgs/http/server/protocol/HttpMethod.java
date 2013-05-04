package io.higgs.http.server.protocol;

import io.higgs.core.InvokableMethod;
import io.higgs.core.ObjectFactory;
import io.higgs.core.ResourcePath;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.MethodParam;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.resource.MediaType;
import io.higgs.http.server.resource.Produces;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class HttpMethod extends InvokableMethod {
    private MethodParam[] params = new MethodParam[0];
    private LinkedList<MediaType> mediaTypes = new LinkedList<>();
    /**
     * Path to an HTML template file used to format responses
     */
    private String template;

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
            mediaTypes.addAll(mediaTypeList);
        }
    }

    @Override
    public boolean matches(String path, ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof HttpRequest)) {
            //only HttpRequests can match
            return false;
        }
        HttpRequest request = (HttpRequest) msg;
        ResourcePath p = path();
        if (p.matches(path)) {
            request.setPath(p);
            //does the method or it's class have the @Produces annotation?
            if (mediaTypes.size() > 0) {
                //if so does this method produce a media type which matches what the client accepts
                for (MediaType producesMediaType : mediaTypes) {
                    for (MediaType acceptedMediaType : request.getMediaTypes()) {
                        if (producesMediaType.isCompatible(acceptedMediaType)) {
                            //set the matched media type to the type the class produces
                            request.setMatchedMediaType(producesMediaType);
                            return true;
                        }
                    }
                }
                //path matched but media type didn't
                log.debug(String.format("template %s matched %s but no compatible media types found",
                        path, p.getUri()));
                throw new WebApplicationException(HttpResponseStatus.NOT_ACCEPTABLE, request);
            } else {
                return true;
            }
        }
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
        return template != null;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
}
