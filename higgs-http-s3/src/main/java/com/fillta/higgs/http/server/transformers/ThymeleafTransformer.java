package com.fillta.higgs.http.server.transformers;

import com.fillta.higgs.http.server.*;
import com.fillta.higgs.http.server.config.TemplateConfig;
import com.fillta.higgs.http.server.resource.MediaType;
import com.fillta.higgs.http.server.resource.Path;
import com.fillta.higgs.http.server.transformers.thymeleaf.Thymeleaf;
import com.fillta.higgs.http.server.transformers.thymeleaf.WebContext;
import com.fillta.higgs.reflect.ReflectionUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;

/**
 * See {@link Path#template()} for a list of types that will be injected by default
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ThymeleafTransformer extends BaseTransformer {
	private Logger log = LoggerFactory.getLogger(getClass());
	private final ReflectionUtil reflection = new ReflectionUtil();
	protected TemplateConfig config;
	protected Thymeleaf tl;

	public ThymeleafTransformer(TemplateConfig config) {
		this.config = config;
		tl = new Thymeleaf(this.config);
	}

	@Override
	public boolean canTransform(Object response, HttpRequest request) {
		//first and foremost an endpoint must have a template annotation to even be considered
		if (!request.getEndpoint().hasTemplate()) {
			return false;
		}
		if (request.getMediaTypes().isEmpty()) {
			return true;//assume */*
		}
		for (MediaType type : request.getMediaTypes()) {
			if (type.isCompatible(MediaType.WILDCARD_TYPE) ||
					type.isCompatible(MediaType.TEXT_HTML_TYPE) ||
					type.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE) ||
					type.isCompatible(MediaType.APPLICATION_XHTML_XML_TYPE))
				return true;
		}
		return false;
	}

	@Override
	public HttpResponse transform(final HttpServer server, Object returns, final HttpRequest request,
	                              final Queue<ResponseTransformer> registeredTransformers) {
		WebContext ctx = new WebContext();
		return transform(ctx, server, returns, request, registeredTransformers,
				request.getEndpoint().getTemplate());
	}

	public HttpResponse transform(WebContext ctx, HttpServer server, Object returns, HttpRequest request,
	                              final Queue<ResponseTransformer> registeredTransformers,
	                              String template) {
		return transform(ctx, server, returns, request, registeredTransformers, template, null);
	}

	public HttpResponse transform(WebContext ctx, HttpServer server, Object returns, HttpRequest request,
	                              final Queue<ResponseTransformer> registeredTransformers,
	                              String template, HttpResponseStatus status) {
		if (returns == null) {
			//if returns==null then the resource method returned void so return No Content
			return new HttpResponse(HttpStatus.NO_CONTENT);
		} else {
			byte[] data = null;
			try {
				if (request != null) {
					if (config.determine_language_from_accept_header) {
						try {
							ctx.setLocale(Locale.forLanguageTag(
									request.headers().get(HttpHeaders.Names.ACCEPT_LANGUAGE)));
						} catch (Throwable t) {
							log.warn("Unable to set locale from accept header");
						}
					}
					populateContext(ctx, server, returns, request);
				}
				String content = tl.getTemplateEngine().process(template, ctx);
				data = content.getBytes(Charset.forName(config.character_encoding));
			} catch (Throwable e) {
				log.warn("Unable to transform response to HTML using Thymeleaf transformer", e);
				//todo use template to generate 500
				return new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR);
			}
			if (data != null) {
				HttpResponse response = new HttpResponse(request.protocolVersion(),
						status == null ? HttpStatus.OK : status,
						Unpooled.wrappedBuffer(data));
				HttpHeaders.setContentLength(response, data.length);
				return response;
			} else {
				return tryNextTransformer(server, returns, request, registeredTransformers);
			}
		}
	}

	private void populateContext(final WebContext ctx, final HttpServer server, final Object response,
	                             final HttpRequest request) {
		//set defaults first so that users can override
		//${_query} ,${_form},${_files},${_session},${_cookies},${_request},${_response},${_server}
		ctx.setVariable("_query", request.getQueryParams());
		ctx.setVariable("_form", request.getFormParam());
		ctx.setVariable("_files", request.getFormFiles());
		//TODO session should never be null!
		ctx.setVariable("_session", server.getSession(request.getSessionId()));
		ctx.setVariable("_cookies", request.getCookies());
		ctx.setVariable("_request", request);
		ctx.setVariable("_response", response);
		ctx.setVariable("_server", server);
		//response already available under ${_response} so only include if is POJO or Map, then we can
		//do a field to value setup
		if (response != null) {
			if (!reflection.isNumeric(response.getClass())
					|| List.class.isAssignableFrom(response.getClass())
					|| Set.class.isAssignableFrom(response.getClass())) {
				if (response instanceof Map && config.convert_map_responses_to_key_value_pairs) {
					ctx.setVariables((Map<String, ?>) response);
				} else {
					//it must be a POJO otherwise (since its not a primitive or a Map,List or Set...)
					if (config.convert_pojo_responses_to_key_value_pairs) {
						//get fields going a max of 10 parent classes up in the chain
						List<Field> fields = reflection.getAllFields(new ArrayList<Field>(), response.getClass(), 10);
						for (Field field : fields) {
							try {
								field.setAccessible(true);
								ctx.setVariable(field.getName(), field.get(response));
							} catch (IllegalAccessException e) {
								log.warn(String.format("Unable to set template variable %s", field.getName()), e);
							}
						}
					}
				}
			}
		}
	}

	public TemplateConfig getConfig() {
		return config;
	}

	/**
	 * Get the Thymeleaf template engine which can be used configured further.
	 *
	 * @return
	 */
	public TemplateEngine getTemplateEngine() {
		return tl.getTemplateEngine();
	}
}
