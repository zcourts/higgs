package io.higgs.http.server;

import io.higgs.core.ResourcePath;
import io.higgs.core.reflect.ReflectionUtil;
import io.higgs.http.server.params.FormFiles;
import io.higgs.http.server.params.FormParams;
import io.higgs.http.server.params.HttpCookie;
import io.higgs.http.server.params.HttpCookies;
import io.higgs.http.server.params.HttpFile;
import io.higgs.http.server.params.QueryParams;
import io.higgs.http.server.params.RequiredParam;
import io.higgs.http.server.params.ValidationResult;
import io.higgs.http.server.protocol.HttpMethod;
import io.netty.channel.ChannelHandlerContext;
import org.apache.shiro.session.Session;

import java.nio.channels.Channel;
import java.util.List;

/**
 * Inspect the provided method parameters and substitute supported types as parameters where necessary
 * The following can be injected:
 * {@link HttpRequest},{@link FormFiles},{@link HttpFile},{@link FormParams},
 * {@link HttpCookie},{@link QueryParams},{@link io.higgs.http.server.auth.HiggsSession},{@link ResourcePath}
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DefaultParamInjector implements ParamInjector {
    @Override
    public Object[] injectParams(HttpMethod method, HttpRequest request, HttpResponse res, ChannelHandlerContext ctx,
                                 Object[] args) {
        MethodParam[] params = method.getParams();

        ResourcePath path = request.getPath();
        ResourcePath.Component[] components = new ResourcePath.Component[0];
        //@template annotation is optional
        if (path != null) {
            components = path.getComponents();
        }
        ValidationResult result = new ValidationResult();
        method.setValidationResult(result);
        for (int i = 0; i < params.length; i++) {
            if (args[i] != null) {
                continue; //this param has already been injected, move on
            }
            MethodParam param = params[i];
            if (ValidationResult.class.isAssignableFrom(param.getParameterType())) {
                args[i] = result;
                continue;
            }
            Object o;
            if (param.isNamed()) {
                //process annotations, i.e. the named parameters
                o = processAnnotations(method, request, param, params, path, components, ctx);
            } else {
                //process the non-named parameters
                o = processClasses(method, request, res, param, params, path, components, ctx);
            }
            if (param.isValidationRequired()) {
                boolean valid = param.getValidator().isValid(o);
                args[i] = RequiredParam.class.isAssignableFrom(param.getParameterType()) ?
                        new RequiredParam<>(o, valid) : o;
                result.put(param.getName() + "_valid", valid);
                if (!valid) {
                    result.invalid();
                    result.put(param.getName(), param.getValidator().getValidationMessage(param));
                }
            } else {
                //if validation isn't required _valid is always trueString
                result.put(param.getName() + "_valid", true);
                args[i] = o;
            }
        }
        return args;
    }

    /**
     * The following can be injected:
     * {@link HttpRequest},{@link FormFiles},{@link FormParams},
     * {@link HttpCookies},{@link QueryParams},{@link io.higgs.http.server.auth.HiggsSession},{@link ResourcePath},
     * {@link ChannelHandlerContext} ,{@link Channel}
     */
    private Object processClasses(HttpMethod method, HttpRequest request, HttpResponse res, MethodParam param,
                                  MethodParam[] params, ResourcePath path, ResourcePath.Component[] components,
                                  ChannelHandlerContext ctx) {
        if (io.netty.handler.codec.http.HttpRequest.class.isAssignableFrom(param.getParameterType())) {
            return request;
        } else if (FormFiles.class.isAssignableFrom(param.getParameterType())) {
            return request.getFormFiles();
        } else if (HttpResponse.class.isAssignableFrom(param.getParameterType())) {
            return res;
        } else if (FormParams.class.isAssignableFrom(param.getParameterType())) {
            return request.getFormParam();
        } else if (HttpCookies.class.isAssignableFrom(param.getParameterType())) {
            return request.getCookies();
        } else if (QueryParams.class.isAssignableFrom(param.getParameterType())) {
            return request.getQueryParams();
        } else if (Session.class.isAssignableFrom(param.getParameterType())) {
            return request.getSession();
        } else if (ResourcePath.class.isAssignableFrom(param.getParameterType())) {
            return path;
        } else if (ChannelHandlerContext.class.isAssignableFrom(param.getParameterType())) {
            return ctx;
        } else if (Channel.class.isAssignableFrom(param.getParameterType())) {
            return ctx.channel();
        } else {
//todo add support for custom parameter provider (i.e. allow anything to be injected if registered)
            return null;
        }
    }

    private Object processAnnotations(HttpMethod method, HttpRequest request, MethodParam param, MethodParam[] params,
                                      ResourcePath path, ResourcePath.Component[] components,
                                      ChannelHandlerContext ctx) {
        if (param.isCookieParam()) {
            return extractCookieParam(param, request);
        } else if (param.isHeaderParam()) {
            return extractHeaderParam(param, request);
        } else if (param.isFormParam()) {
            return extractFormParam(param, request);
        } else if (param.isQueryParam()) {
            return extractQueryParam(param, request);
        } else if (param.isPathParam()) {
            return extractPathParam(param, path);
        } else if (param.isSessionParam()) {
            return request.getSession() == null ? null : request.getSession().getAttribute(param.getName());
        }
        return null;
    }

    protected Object extractPathParam(MethodParam param, ResourcePath path) {
        ResourcePath.Component component = path.getComponent(param.getName());
        if (String.class.isAssignableFrom(param.getParameterType())) {
            if (component != null) {
                return component.getRuntimeValue();
            }
        } else {
            if (ReflectionUtil.isNumeric(param.getParameterType())) {
                //if param is a number then try to handle with NumberType.parseType
                return extractNumberParam(param, component == null ? null : component.getRuntimeValue());
            }
        }
        return null;
    }

    protected Object extractQueryParam(MethodParam param, HttpRequest request) {
        //query string param can be a list or string, if neither set to null
        if (List.class.isAssignableFrom(param.getParameterType())) {
            return request.getQueryParams().get(param.getName());
        } else if (String.class.isAssignableFrom(param.getParameterType())) {
            return request.getQueryParams().getFirst(param.getName());
        } else {
            if (ReflectionUtil.isNumeric(param.getParameterType())) {
                //if param is a number then try to handle with NumberType.parseType
                return extractNumberParam(param, request.getQueryParams().getFirst(param.getName()));
            } else {
                return null;
            }
        }
    }

    protected Object extractFormParam(MethodParam param, HttpRequest request) {
        Object obj = request.getFormParam().get(param.getName());
        if (obj != null && param.getParameterType().isAssignableFrom(obj.getClass())) {
            return obj;
        } else {
            if (ReflectionUtil.isNumeric(param.getParameterType())) {
                //if param is a number then try to handle with NumberType.parseType
                return extractNumberParam(param, (String) request.getFormParam().get(param.getName()));
            } else {
                return null;
            }
        }
    }

    protected Object extractHeaderParam(MethodParam param, HttpRequest request) {
        //header param can be a list or string, if neither set to null
        if (List.class.isAssignableFrom(param.getParameterType())) {
            return request.headers().getAll(param.getName());
        } else {
            if (String.class.isAssignableFrom(param.getParameterType())) {
                return request.headers().get(param.getName());
            } else {
                if (ReflectionUtil.isNumeric(param.getParameterType())) {
                    //if param is a number then try to handle with NumberType.parseType
                    return extractNumberParam(param, request.headers().get(param.getName()));
                } else {
                    return null;
                }
            }
        }
    }

    protected Object extractCookieParam(MethodParam param, HttpRequest request) {
        HttpCookie cookie = request.getCookie(param.getName());
        if (cookie == null) {
            return null;
        }
        if (String.class.isAssignableFrom(param.getParameterType())) {
            return cookie.getValue();
        } else if (HttpCookie.class.isAssignableFrom(param.getParameterType())) {
            return cookie;
        } else if (ReflectionUtil.isNumeric(param.getParameterType())) {
            //if param is a number then try to handle with NumberType.parseType
            return extractNumberParam(param, cookie.getValue());
        } else {
            return null;
        }
    }

    /**
     * Given the parameters try to convert the string value to a numeric value of the
     * method class given in the MethodParam. If a number format exception occurs then
     * the param at the given index is set to null. Resource classes should use the boxed class
     * types of numbers if they want to differentiate between 0 and an invalid format. i.e.
     * use Integer,Double,Float,Long,Byte and short instead of int,double etc...
     * if the primitive types are used the values will be 0 if the conversion fails
     *
     * @param param the parameter to be injected
     * @param value the value to be converted
     */
    protected Object extractNumberParam(MethodParam param, String value) {
        try {
            if (Integer.class.isAssignableFrom(param.getParameterType())) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException nfe) {
                    //return null for class numeric types
                    return null;
                }
            } else if (Long.class.isAssignableFrom(param.getParameterType())) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException nfe) {
                    //return null for class numeric types
                    return null;
                }
            } else if (Float.class.isAssignableFrom(param.getParameterType())) {
                try {
                    return Float.parseFloat(value);
                } catch (NumberFormatException nfe) {
                    //return null for class numeric types
                    return null;
                }
            } else if (Double.class.isAssignableFrom(param.getParameterType())) {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException nfe) {
                    //return null for class numeric types
                    return null;
                }
            } else if (Short.class.isAssignableFrom(param.getParameterType())) {
                try {
                    return Short.parseShort(value);
                } catch (NumberFormatException nfe) {
                    //return null for class numeric types
                    return null;
                }
            } else if (Byte.class.isAssignableFrom(param.getParameterType())) {
                try {
                    return Byte.parseByte(value);
                } catch (NumberFormatException nfe) {
                    //return null for class numeric types
                    return null;
                }
            } else if (int.class.isAssignableFrom(param.getParameterType())) {
                return Integer.parseInt(value);
            } else if (long.class.isAssignableFrom(param.getParameterType())) {
                return Long.parseLong(value);
            } else if (float.class.isAssignableFrom(param.getParameterType())) {
                return Float.parseFloat(value);
            } else if (double.class.isAssignableFrom(param.getParameterType())) {
                return Double.parseDouble(value);
            } else if (short.class.isAssignableFrom(param.getParameterType())) {
                return Short.parseShort(value);
            } else if (byte.class.isAssignableFrom(param.getParameterType())) {
                return Byte.parseByte(value);
            } else {
                return 0;
            }
        } catch (NumberFormatException nfe) {
            //return 0 for primitive numeric types
            return 0;
        }
    }

}
