package com.fillta.higgs.http.server.transformers;

import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.http.server.*;
import com.fillta.higgs.http.server.resource.MediaType;
import com.fillta.higgs.http.server.transformers.thymeleaf.WebContext;
import com.google.common.base.Optional;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpErrorTransformer extends BaseTransformer {
	private final HttpServer server;
	protected final Map<Integer, String> templates = new HashMap<>();
	private final ThymeleafTransformer thymeleaf;
	private final JsonTransformer json;

	public HttpErrorTransformer(final HttpServer server, JsonTransformer jsonTransformer,
	                            ThymeleafTransformer thymeleafTransformer) {
		this.server = server;
		json = jsonTransformer;
		thymeleaf = thymeleafTransformer;
		this.server.on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
			public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
				HttpResponse response = buildErrorResponse(ex.get(), null);
				server.respond(ctx.channel(), response);
			}
		});
	}

	public boolean canTransform(Object response, HttpRequest request) {
		if (response instanceof Throwable) {
			return true;
		}
		return false;
	}

	public HttpResponse transform(HttpServer ignore, Object response, HttpRequest request,
	                              Queue<ResponseTransformer> registeredTransformers) {
		if (response instanceof Throwable) {
			return buildErrorResponse((Throwable) response, request);
		}
		return null;
	}

	private HttpResponse buildErrorResponse(Throwable throwable, HttpRequest request) {
		WebContext ctx = new WebContext();
		ctx.setVariable("status", 500);
		ctx.setVariable("name", "Internal Server Error");
		//error 500 by default
		HttpResponse response = new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR);
		//get template for 500 status, if null use default
		String template = templates.get(500);
		if (template == null) {
			template = server.getConfig().default_error_template;
		}
		//try to infer a better error type
		if (throwable instanceof WebApplicationException) {
			WebApplicationException e = (WebApplicationException) throwable;
			response.setStatus(e.getStatus());
			ctx.setVariable("status", e.getStatus().getCode());
			ctx.setVariable("name", e.getStatus().getReasonPhrase());
			if (e.hasRequest()) {
				return handleWAE(ctx, template, e);
			} else {
				//todo handle WAE without request object
			}
		} else {
			//not a web application exception...is request null?
			if (request != null) {
				return handleAnyThrowableWithRequest(ctx, template, request);

			} else {
				//todo handle arbitrary exception with no access to request object
			}
		}
		return response;
	}

	protected HttpResponse handleAnyThrowableWithRequest(final WebContext ctx, String template, HttpRequest request) {
		//if has request then we can use a transformer
		boolean thymeleafMediaType = true;
		for (MediaType type : request.getMediaTypes()) {
			if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE) ||
					type.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
				thymeleafMediaType = false;
				break;
			}
		}
		if (thymeleafMediaType) {
			return thymeleaf.transform(ctx, server, "", request,
					new LinkedBlockingQueue<ResponseTransformer>(), template);
		} else {
			return json.transform(server, null, request,
					new LinkedBlockingQueue<ResponseTransformer>());
		}
	}

	protected HttpResponse handleWAE(final WebContext ctx, String template, final WebApplicationException e) {
		//get template for status, if null use default
		template = templates.get(e.getStatus().getCode());
		if (template == null) {
			template = server.getConfig().default_error_template;
		}
		//if has request then we can use a transformer
		boolean thymeleafMediaType = true;
		for (MediaType type : e.getRequest().getMediaTypes()) {
			if (!type.isWildcardType() && (
					type.isCompatible(MediaType.TEXT_PLAIN_TYPE) ||
							type.isCompatible(MediaType.APPLICATION_JSON_TYPE)
			)) {
				thymeleafMediaType = false;
				break;
			}
		}
		if (thymeleafMediaType) {
			//pass an empty string for response if null. don't want thymeleaf transformer to throw
			//an exception, we're probably already in the exceptionCaught method for Netty
			return thymeleaf.transform(ctx, server, e.getResponse() == null ? "" : e.getResponse(), e.getRequest(),
					new LinkedBlockingQueue<ResponseTransformer>(), template);
		} else {
			return json.transform(server, e.getResponse(), e.getRequest(),
					new LinkedBlockingQueue<ResponseTransformer>());
		}
	}

	public void setErrorTemplate(HttpResponseStatus status, String template) {
		setErrorTemplate(status.getCode(), template);
	}

	public void setErrorTemplate(int status, String template) {
		if (template != null)
			templates.put(status, template);
	}
}
