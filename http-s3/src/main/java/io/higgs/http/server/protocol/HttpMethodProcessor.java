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
import io.higgs.http.server.params.valid;
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

        boolean classHasTemplate = klass.isAnnotationPresent(template.class);
        String methodTemplate = null;
        //does the method have a template?
        if (classHasTemplate) {
            template template = klass.getAnnotation(template.class);
            if (template.value() != null && !template.value().isEmpty()) {
                methodTemplate = template.value();
            }
        }
        //if the method has a template and the class has one, override the class' with the template found on the method
        if (method.isAnnotationPresent(template.class)) {
            template template = method.getAnnotation(template.class);
            if (template.value() != null && !template.value().isEmpty()) {
                methodTemplate = template.value();
            }
        }
        im.setTemplate(methodTemplate);
        Class<?>[] parameters = method.getParameterTypes();
        //outter array is list of annotations on the method
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
}
