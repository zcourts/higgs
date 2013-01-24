package com.fillta.higgs.http.server;

import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.http.server.params.*;
import com.fillta.higgs.reflect.ReflectionUtil;

import java.util.List;

/**
 * Inspect the provided method parameters and substitute supported types as parameters where necessary
 * The following can be injected:
 * {@link HttpRequest},{@link FormFiles},{@link HttpFile},{@link FormParams},
 * {@link HttpCookie},{@link QueryParams},{@link HttpSession},{@link ResourcePath}
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DefaultParamInjector implements ParamInjector {
	private final ReflectionUtil reflection = new ReflectionUtil();

	public void injectParams(final HttpServer server, final Endpoint.MethodParam[] params,
	                         final ChannelMessage<HttpRequest> event, final Object[] args) {
		ResourcePath path = event.message.getPath();
		ResourcePath.Component[] components = new ResourcePath.Component[0];
		//@Path annotation is optional
		if (path != null) {
			components = path.getComponents();
		}
		for (int i = 0; i < params.length; i++) {
			Endpoint.MethodParam param = params[i];
			if (param.isNamed()) {
				//process annotations, i.e. the named parameters
				args[i] = processAnnotations(server, param, params, event, path, components);
			} else {
				//process the non-named parameters
				args[i] = processClasses(server, param, params, event, path, components);
			}
		}
	}

	/**
	 * The following can be injected:
	 * {@link HttpRequest},{@link FormFiles},{@link FormParams},
	 * {@link HttpCookies},{@link QueryParams},{@link HttpSession},{@link ResourcePath},
	 * {@link ChannelMessage< HttpRequest >} ,{@link HttpServer}
	 */
	private Object processClasses(HttpServer server, Endpoint.MethodParam param,
	                              Endpoint.MethodParam[] params, ChannelMessage<HttpRequest> event,
	                              ResourcePath path, ResourcePath.Component[] components) {
		if (HttpRequest.class.isAssignableFrom(param.getMethodClass())) {
			return event.message;
		} else if (FormFiles.class.isAssignableFrom(param.getMethodClass())) {
			return event.message.getFormFiles();
		} else if (FormParams.class.isAssignableFrom(param.getMethodClass())) {
			return event.message.getFormParam();
		} else if (HttpCookies.class.isAssignableFrom(param.getMethodClass())) {
			return event.message.getCookies();
		} else if (QueryParams.class.isAssignableFrom(param.getMethodClass())) {
			return event.message.getQueryParams();
		} else if (HttpSession.class.isAssignableFrom(param.getMethodClass())) {
			return server.getSession(event.message.getSessionId());
		} else if (ResourcePath.class.isAssignableFrom(param.getMethodClass())) {
			return path;
		} else if (HttpServer.class.isAssignableFrom(param.getMethodClass())) {
			return server;
		} else if (ChannelMessage.class.isAssignableFrom(param.getMethodClass())) {
			return event;
		} else {
//todo add support for custom parameter provider (i.e. allow anything to be injected if registered)
			return null;
		}
	}

	protected Object processAnnotations(HttpServer server, Endpoint.MethodParam param,
	                                    Endpoint.MethodParam[] params, ChannelMessage<HttpRequest> event,
	                                    ResourcePath path, ResourcePath.Component[] components) {
		HttpRequest request = event.message;
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
		}
		return null;
	}

	protected Object extractPathParam(Endpoint.MethodParam param, ResourcePath path) {
		ResourcePath.Component component = path.getComponent(param.getName());
		if (String.class.isAssignableFrom(param.getMethodClass())) {
			if (component != null)
				return component.getRuntimeValue();
		} else if (reflection.isNumeric(param.getMethodClass())) {
			//if param is a number then try to handle with NumberType.parseType
			return extractNumberParam(param, component == null ? null : component.getRuntimeValue());
		}
		return null;
	}

	protected Object extractQueryParam(Endpoint.MethodParam param, HttpRequest request) {
		//query string param can be a list or string, if neither set to null
		if (List.class.isAssignableFrom(param.getMethodClass())) {
			return request.getQueryParams().get(param.getName());
		} else if (String.class.isAssignableFrom(param.getMethodClass())) {
			return request.getQueryParams().getFirst(param.getName());
		} else if (reflection.isNumeric(param.getMethodClass())) {
			//if param is a number then try to handle with NumberType.parseType
			return extractNumberParam(param, request.getQueryParams().getFirst(param.getName()));
		} else {
			return null;
		}
	}

	protected Object extractFormParam(Endpoint.MethodParam param, HttpRequest request) {
		if (String.class.isAssignableFrom(param.getMethodClass())) {
			return request.getFormParam().get(param.getName());
		} else if (reflection.isNumeric(param.getMethodClass())) {
			//if param is a number then try to handle with NumberType.parseType
			return extractNumberParam(param, request.getFormParam().get(param.getName()));
		} else {
			return null;
		}
	}

	protected Object extractHeaderParam(Endpoint.MethodParam param, HttpRequest request) {
		//header param can be a list or string, if neither set to null
		if (List.class.isAssignableFrom(param.getMethodClass())) {
			return request.headers().getAll(param.getName());
		} else if (String.class.isAssignableFrom(param.getMethodClass())) {
			return request.headers().get(param.getName());
		} else if (reflection.isNumeric(param.getMethodClass())) {
			//if param is a number then try to handle with NumberType.parseType
			return extractNumberParam(param, request.headers().get(param.getName()));
		} else {
			return null;
		}
	}

	protected Object extractCookieParam(Endpoint.MethodParam param, HttpRequest request) {
		HttpCookie cookie = request.getCookie(param.getName());
		if (cookie == null)
			return null;
		if (String.class.isAssignableFrom(param.getMethodClass())) {
			return cookie.getValue();
		} else if (HttpCookie.class.isAssignableFrom(param.getMethodClass())) {
			return cookie;
		} else if (reflection.isNumeric(param.getMethodClass())) {
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
	protected Object extractNumberParam(Endpoint.MethodParam param, String value) {
		try {
			if (Integer.class.isAssignableFrom(param.getMethodClass())) {
				try {
					return Integer.parseInt(value);
				} catch (NumberFormatException nfe) {
					//return null for class numeric types
					return null;
				}
			} else if (Long.class.isAssignableFrom(param.getMethodClass())) {
				try {
					return Long.parseLong(value);
				} catch (NumberFormatException nfe) {
					//return null for class numeric types
					return null;
				}
			} else if (Float.class.isAssignableFrom(param.getMethodClass())) {
				try {
					return Float.parseFloat(value);
				} catch (NumberFormatException nfe) {
					//return null for class numeric types
					return null;
				}
			} else if (Double.class.isAssignableFrom(param.getMethodClass())) {
				try {
					return Double.parseDouble(value);
				} catch (NumberFormatException nfe) {
					//return null for class numeric types
					return null;
				}
			} else if (Short.class.isAssignableFrom(param.getMethodClass())) {
				try {
					return Short.parseShort(value);
				} catch (NumberFormatException nfe) {
					//return null for class numeric types
					return null;
				}
			} else if (Byte.class.isAssignableFrom(param.getMethodClass())) {
				try {
					return Byte.parseByte(value);
				} catch (NumberFormatException nfe) {
					//return null for class numeric types
					return null;
				}
			}
			//now check for primitive types as opposed to the boxed, class types above
			else if (int.class.isAssignableFrom(param.getMethodClass())) {
				return Integer.parseInt(value);
			} else if (long.class.isAssignableFrom(param.getMethodClass())) {
				return Long.parseLong(value);
			} else if (float.class.isAssignableFrom(param.getMethodClass())) {
				return Float.parseFloat(value);
			} else if (double.class.isAssignableFrom(param.getMethodClass())) {
				return Double.parseDouble(value);
			} else if (short.class.isAssignableFrom(param.getMethodClass())) {
				return Short.parseShort(value);
			} else if (byte.class.isAssignableFrom(param.getMethodClass())) {
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
