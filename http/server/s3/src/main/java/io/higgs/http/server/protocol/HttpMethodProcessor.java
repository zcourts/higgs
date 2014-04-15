package io.higgs.http.server.protocol;

import io.higgs.core.MethodProcessor;
import io.higgs.core.ObjectFactory;
import io.higgs.http.server.MethodParam;
import io.higgs.http.server.params.CookieParam;
import io.higgs.http.server.params.DefaultValidator;
import io.higgs.http.server.params.FormParam;
import io.higgs.http.server.params.HeaderParam;
import io.higgs.http.server.params.IllegalValidatorException;
import io.higgs.http.server.params.PathParam;
import io.higgs.http.server.params.QueryParam;
import io.higgs.http.server.params.SessionParam;
import io.higgs.http.server.params.valid;
import io.higgs.http.server.resource.DELETE;
import io.higgs.http.server.resource.GET;
import io.higgs.http.server.resource.HEAD;
import io.higgs.http.server.resource.OPTIONS;
import io.higgs.http.server.resource.POST;
import io.higgs.http.server.resource.PUT;
import io.higgs.http.server.resource.template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpMethodProcessor implements MethodProcessor {
    private final HttpProtocolConfiguration config;
    private Logger log = LoggerFactory.getLogger(getClass());

    public HttpMethodProcessor(HttpProtocolConfiguration config) {
        this.config = config;
    }

    @Override
    public HttpMethod process(Method method, Class<?> klass, Queue<ObjectFactory> factories) {

        HttpMethod im = new HttpMethod(factories, klass, method);

        determineTemplate(method, klass, im);
        processVerbs(im, method);
        Class<?>[] parameters = method.getParameterTypes();
        //outter array is each parameter, inner array is list of annotations for each parameter
        Annotation[][] methodAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < methodAnnotations.length; i++) {
            Annotation[] paramterAnnotations = methodAnnotations[i];
            if (i == 0) {
                im.initialiseParams(methodAnnotations.length);
            }
            Class<?> parameterType = parameters[i];
            MethodParam methodParam = new MethodParam();
            //set the class type of the parameter
            methodParam.setParameterType(parameterType);
            methodParam.setPosition(i);
            im.setParam(methodParam);
            //inner array is the list of annotations on the current method parameter
            for (Annotation annotation : paramterAnnotations) {
                //a single parameter can have multiple of these annotations
                if (annotation.annotationType().isAssignableFrom(valid.class)) {
                    valid validationParam = (valid) annotation;
                    methodParam.setValidationRequired(true);
                    if (DefaultValidator.class.isAssignableFrom(validationParam.value())) {
                        methodParam.setValidator(new DefaultValidator());
                    } else {
                        try {
                            methodParam.setValidator(validationParam.value().newInstance());
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new IllegalValidatorException("All validators must have a publicly accessible " +
                                    "no-args constructor", e);
                        }
                    }
                    continue;
                }
                if (annotation.annotationType().isAssignableFrom(SessionParam.class)) {
                    SessionParam sessionParam = (SessionParam) annotation;
                    if (sessionParam.value() != null && !sessionParam.value().isEmpty()) {
                        methodParam.setSessionParam(true);
                        methodParam.setName(sessionParam.value());
                    }
                    continue;
                }
                if (annotation.annotationType().isAssignableFrom(PathParam.class)) {
                    PathParam pathParam = (PathParam) annotation;
                    methodParam.setPathParam(true);
                    if (pathParam.value() != null && !pathParam.value().isEmpty()) {
                        methodParam.setName(pathParam.value());
                    }
                    continue;
                }
                if (annotation.annotationType().isAssignableFrom(QueryParam.class)) {
                    QueryParam queryParam = (QueryParam) annotation;
                    methodParam.setQueryParam(true);
                    if (queryParam.value() != null && !queryParam.value().isEmpty()) {
                        methodParam.setName(queryParam.value());
                    }
                    continue;
                }
                if (annotation.annotationType().isAssignableFrom(FormParam.class)) {
                    FormParam formParam = (FormParam) annotation;
                    methodParam.setFormParam(true);
                    if (formParam.value() != null && !formParam.value().isEmpty()) {
                        methodParam.setName(formParam.value());
                    }
                    continue;
                }
                if (annotation.annotationType().isAssignableFrom(CookieParam.class)) {
                    CookieParam cookieParam = (CookieParam) annotation;
                    methodParam.setCookieParam(true);
                    if (cookieParam.value() != null && !cookieParam.value().isEmpty()) {
                        methodParam.setName(cookieParam.value());
                    }
                    continue;
                }
                if (annotation.annotationType().isAssignableFrom(HeaderParam.class)) {
                    HeaderParam headerParam = (HeaderParam) annotation;
                    methodParam.setHeaderParam(true);
                    if (headerParam.value() != null && !headerParam.value().isEmpty()) {
                        methodParam.setName(headerParam.value());
                    }
                    continue;
                }
                log.warn(String.format("Unknown param type annotation %s", annotation.annotationType().getName()));
            }
        }
        return im;
    }

    private void processVerbs(HttpMethod im, Method method) {
        Annotation[] annotations = method.getAnnotations(); //o length array if none so safe from null
        for (Annotation a : annotations) {
            if (GET.class.isAssignableFrom(a.annotationType())) {
                im.addVerb(HttpMethod.VERB.GET);
            }
            if (POST.class.isAssignableFrom(a.annotationType())) {
                im.addVerb(HttpMethod.VERB.POST);
            }
            if (PUT.class.isAssignableFrom(a.annotationType())) {
                im.addVerb(HttpMethod.VERB.PUT);
            }
            if (DELETE.class.isAssignableFrom(a.annotationType())) {
                im.addVerb(HttpMethod.VERB.DELETE);
            }
            if (HEAD.class.isAssignableFrom(a.annotationType())) {
                im.addVerb(HttpMethod.VERB.HEAD);
            }
            if (OPTIONS.class.isAssignableFrom(a.annotationType())) {
                im.addVerb(HttpMethod.VERB.OPTIONS);
            }
        }
    }

    private void determineTemplate(Method method, Class<?> klass, HttpMethod im) {
        boolean classHasTemplate = klass.isAnnotationPresent(template.class);
        String methodTemplate = null;
        boolean templateAnnotationFound = classHasTemplate;
        //does the method have a template?
        if (classHasTemplate) {
            template template = klass.getAnnotation(template.class);
            if (template.value() != null && !template.value().isEmpty()) {
                methodTemplate = template.value();
            }
            im.setTemplate(template.fragments());
        }
        //if the method has a template and the class has one, override the class' with the template found on the method
        if (method.isAnnotationPresent(template.class)) {
            templateAnnotationFound = true;
            template template = method.getAnnotation(template.class);
            if (template.value() != null && !template.value().isEmpty()) {
                methodTemplate = template.value();
            }
            im.setTemplate(template.fragments());
        }
        im.setTemplate(methodTemplate);
        if (templateAnnotationFound && (!im.hasFragments() && !im.hasTemplate())) {
            throw new IllegalStateException(String.format("%s in %s has template annotation but hasn't provided a " +
                    "template or any fragments", method.getName(), klass.getName()));
        }
    }
}
