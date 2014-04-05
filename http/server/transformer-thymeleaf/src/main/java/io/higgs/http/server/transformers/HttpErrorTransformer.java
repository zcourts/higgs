package io.higgs.http.server.transformers;

import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.http.server.protocol.HttpProtocolConfiguration;
import io.higgs.http.server.resource.MediaType;
import io.higgs.http.server.transformers.thymeleaf.WebContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
//@ProviderFor(ResponseTransformer.class)
public class HttpErrorTransformer extends BaseTransformer {
    private final HttpProtocolConfiguration protocolConfiguration;
    private final JsonTransformer json;
    private final ThymeleafTransformer thymeleaf;
    protected Map<Integer, String> templates = new HashMap<>();

    public HttpErrorTransformer(HttpProtocolConfiguration protocolConfiguration, JsonTransformer json,
                                ThymeleafTransformer thymeleaf) {
        this.protocolConfiguration = protocolConfiguration;
        this.json = json;
        this.thymeleaf = thymeleaf;
    }

    @Override
    public boolean canTransform(Object response, HttpRequest request, MediaType mediaType, HttpMethod method,
                                ChannelHandlerContext ctx) {
        return response instanceof Throwable;
    }

    @Override
    public void transform(Object response, HttpRequest request, HttpResponse httpResponse, MediaType mediaType,
                          HttpMethod method,
                          ChannelHandlerContext ctx) {
        if (response instanceof Throwable) {
            buildErrorResponse((Throwable) response, request, httpResponse, mediaType, method, ctx);
        }
    }

    private void buildErrorResponse(Throwable throwable, HttpRequest request, HttpResponse httpResponse,
                                    MediaType mediaType, HttpMethod method, ChannelHandlerContext ctx) {
        WebContext webContext = new WebContext();
        webContext.setVariable("status", 500);
        webContext.setVariable("name", "Internal Server Error");
        //get template for 500 status, if null use default
        String templateName = templates.get(500);
        if (templateName == null) {
            templateName = protocolConfiguration.getServer().getConfig().default_error_template;
        }
        //try to infer a better error type
        if (throwable instanceof WebApplicationException) {
            WebApplicationException e = (WebApplicationException) throwable;
            webContext.setVariable("status", e.getStatus().code());
            webContext.setVariable("name", e.getStatus().reasonPhrase());
            if (e.hasRequest()) {
                handleWAE(e, webContext, request, httpResponse, mediaType, method, ctx);
            } else {
                thymeleaf.instance().transform(webContext, templateName, throwable, request, httpResponse,
                        mediaType, method,
                        ctx, e.getStatus() != null ? e.getStatus() : null);
            }
        } else {
            //not a web application exception...is request null?
            if (request != null) {
                handleAnyThrowableWithRequest(webContext, templateName, throwable, request, httpResponse, mediaType,
                        method, ctx);
            } else {
                thymeleaf.instance().transform(webContext, templateName, throwable, request, httpResponse, mediaType,
                        method,
                        ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    private void handleAnyThrowableWithRequest(WebContext webContext, String templateName, Throwable throwable,
                                               HttpRequest request, HttpResponse httpResponse,
                                               MediaType mediaType, HttpMethod method,
                                               ChannelHandlerContext ctx) {
        //if has request then we can use a transformer
        boolean thymeleafMediaType = true;
        for (MediaType type : request.getAcceptedMediaTypes()) {
            if (type.isCompatible(MediaType.TEXT_PLAIN_TYPE) ||
                    type.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                thymeleafMediaType = false;
                break;
            }
        }
        if (thymeleafMediaType) {
            thymeleaf.transform(webContext, templateName, throwable, request, httpResponse, mediaType, method,
                    ctx, null);
        } else {
            json.transform(null, request, httpResponse, mediaType, method, ctx);
        }
    }

    protected void handleWAE(WebApplicationException e, WebContext webContext, HttpRequest request,
                             HttpResponse httpResponse, MediaType mediaType, HttpMethod method,
                             ChannelHandlerContext ctx) {
        HttpResponseStatus status = e.getStatus();
        //get template for status, if null use default
        String templateName = templates.get(status.code());
        if (templateName == null) {
            templateName = protocolConfiguration.getServer().getConfig().default_error_template;
        }
        //if has request then we can use a transformer
        boolean thymeleafMediaType = true;
        for (MediaType type : e.getRequest().getAcceptedMediaTypes()) {
            if (!type.isWildcardType() && (
                    type.isCompatible(MediaType.TEXT_PLAIN_TYPE) ||
                            type.isCompatible(MediaType.APPLICATION_JSON_TYPE)
            )) {
                thymeleafMediaType = false;
                break;
            }
        }
        if (thymeleafMediaType) {
            thymeleaf.transform(webContext, templateName, e, request, httpResponse, mediaType, method,
                    ctx, status);
        } else {
            json.transform(null, request, httpResponse, mediaType, method, ctx, status);
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

    @Override
    public ResponseTransformer instance() {
        return new HttpErrorTransformer(protocolConfiguration, json, thymeleaf);
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE; //should be the last transformer applied
    }
}
