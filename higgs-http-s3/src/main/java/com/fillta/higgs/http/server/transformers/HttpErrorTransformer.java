package com.fillta.higgs.http.server.transformers;

import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.http.server.HttpRequest;
import com.fillta.higgs.http.server.HttpResponse;
import com.fillta.higgs.http.server.HttpServer;
import com.fillta.higgs.http.server.HttpStatus;
import com.fillta.higgs.http.server.ResponseTransformer;
import com.fillta.higgs.http.server.WebApplicationException;
import com.fillta.higgs.http.server.resource.MediaType;
import com.fillta.higgs.http.server.transformers.thymeleaf.WebContext;
import com.google.common.base.Optional;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpErrorTransformer extends BaseTransformer {
    private HttpServer server;
    protected Map<Integer, String> templates = new HashMap<>();
    private ThymeleafTransformer thymeleaf;
    private JsonTransformer json;
    private Logger log = LoggerFactory.getLogger(getClass());

    public HttpErrorTransformer(final HttpServer server, JsonTransformer jsonTransformer,
                                ThymeleafTransformer thymeleafTransformer) {
        this.server = server;
        json = jsonTransformer;
        thymeleaf = thymeleafTransformer;
        this.server.on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
            public void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex) {
                try {
                    Object request = server.getRequest(ctx.channel());
                    //only if it's an HttpRequest should we attempt to respond to the client...
                    //could be an interceptor's type e.g. WebSocket intercepts HTTP requests
                    //in that case request would be an instanceof TextWebSocketFrame...
                    if (request instanceof HttpRequest) {
                        log.warn(String.format("Exception caught. \nMessage: %s ,\nCause: %s", ex.get().getMessage(),
                                ex.get().getCause() == null ? "null" : ex.get().getCause().getMessage()));
                        HttpResponse response = buildErrorResponse(ex.get(), null);
                        server.respond(ctx.channel(), response).addListener(ChannelFutureListener.CLOSE);
                    }
                } catch (Throwable t) {
                    //if any exception gets thrown in here we have no choice. Internal Server Error!
                    log.warn("Error occurred while generating error response", t);
                    HttpResponse response = new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR);
                    server.respond(ctx.channel(), response);
                }
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
        //get template for 500 status, if null use default
        String template = templates.get(500);
        if (template == null) {
            template = server.getConfig().default_error_template;
        }
        //try to infer a better error type
        if (throwable instanceof WebApplicationException) {
            WebApplicationException e = (WebApplicationException) throwable;
            ctx.setVariable("status", e.getStatus().code());
            ctx.setVariable("name", e.getStatus().reasonPhrase());
            if (e.hasRequest()) {
                return handleWAE(ctx, template, e);
            } else {
                return returnGenericError(ctx, template, e.getStatus());
            }
        } else {
            //not a web application exception...is request null?
            if (request != null) {
                return handleAnyThrowableWithRequest(ctx, template, request);
            } else {
                return returnGenericError(ctx, template, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    protected HttpResponse returnGenericError(WebContext ctx, String template, HttpResponseStatus status) {
        return thymeleaf.transform(ctx, server, "", null,
                new LinkedBlockingQueue<ResponseTransformer>(), template,
                status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status);
    }

    protected HttpResponse handleAnyThrowableWithRequest(WebContext ctx, String template, HttpRequest request) {
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

    protected HttpResponse handleWAE(WebContext ctx, String template, WebApplicationException e) {
        HttpResponseStatus status = e.getStatus();
        //get template for status, if null use default
        template = templates.get(status.code());
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
                    new LinkedBlockingQueue<ResponseTransformer>(), template, status);
        } else {
            return json.transform(server, e.getResponse(), e.getRequest(),
                    new LinkedBlockingQueue<ResponseTransformer>(), status);
        }
    }

    public void setErrorTemplate(HttpResponseStatus status, String template) {
        setErrorTemplate(status.code(), template);
    }

    public void setErrorTemplate(int status, String template) {
        if (template != null) {
            templates.put(status, template);
        }
    }
}
