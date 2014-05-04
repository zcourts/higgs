package io.higgs.http.server.protocol;

import io.higgs.core.FixedSortedList;
import io.higgs.core.InvokableMethod;
import io.higgs.core.MessageHandler;
import io.higgs.core.ResolvedFile;
import io.higgs.core.reflect.dependency.DependencyProvider;
import io.higgs.core.reflect.dependency.Injector;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.MessagePusher;
import io.higgs.http.server.ParamInjector;
import io.higgs.http.server.StaticFileMethod;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.WrappedResponse;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.http.server.protocol.mediaTypeDecoders.FormUrlEncodedDecoder;
import io.higgs.http.server.protocol.mediaTypeDecoders.JsonDecoder;
import io.higgs.http.server.transformers.ResponseTransformer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.getHeader;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;

/**
 * A stateful {@link MessageHandler} which processes HttpRequests.
 * There will be 1 instance of this class per Http request.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpHandler extends MessageHandler<HttpConfig, Object> {
    protected static final Class<HttpMethod> methodClass = HttpMethod.class;
    protected final Queue<MediaTypeDecoder> mediaTypeDecoders = new ConcurrentLinkedDeque<>();
    protected final HttpConfig httpConfig;
    /**
     * The current HTTP request
     */
    protected HttpRequest request;
    protected HttpResponse res;
    /**
     * The current HTTP method which matches the current {@link #request}.
     * If no method matches this will be null
     */
    protected HttpMethod method;
    protected ParamInjector injector;
    protected HttpProtocolConfiguration protocolConfig;
    protected boolean replied;
    protected MediaTypeDecoder decoder;
    private Logger requestLogger = LoggerFactory.getLogger("request_logger");

    public HttpHandler(HttpProtocolConfiguration config) {
        super(config.getServer().<HttpConfig>getConfig());
        httpConfig = config.getServer().getConfig();
        protocolConfig = config;
        injector = config.getInjector();
        mediaTypeDecoders.addAll(config.getMediaTypeDecoders());
    }

    public <M extends InvokableMethod> M findMethod(String path, ChannelHandlerContext ctx,
                                                    Object msg, Class<M> methodClass) {
        M m = super.findMethod(path, ctx, msg, methodClass);
        if (m == null && config.add_static_resource_filter) {
            StaticFileMethod fileMethod = new StaticFileMethod(protocolConfig.getServer().getFactories(),
                    protocolConfig);
            if (fileMethod.matches(path, ctx, msg)) {
                return (M) fileMethod;
            }
        }
        return m;
    }

    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof LastHttpContent && !(msg instanceof FullHttpRequest) && replied) {
            return;  //can happen if exception was thrown before last http content received
        }
        replied = false;
        if (msg instanceof HttpRequest || msg instanceof FullHttpRequest) {
            if (msg instanceof HttpRequest) {
                request = (HttpRequest) msg;
            } else {
                request = new HttpRequest((FullHttpRequest) msg);
            }
            res = new HttpResponse(Unpooled.buffer());
            //apply transcriptions
            protocolConfig.getTranscriber().transcribe(request);
            //must always set protocol config before anything uses the request
            request.setConfig(protocolConfig);
            //initialise request, setting cookies, media types etc
            request.init(ctx);
            method = findMethod(request.getUri(), ctx, request, methodClass);
            if (method == null) {
                //404
                throw new WebApplicationException(HttpStatus.NOT_FOUND, request);
            }
            if (isEntityRequest()) {
                if (httpConfig.add_form_url_decoder) {
                    mediaTypeDecoders.add(new FormUrlEncodedDecoder(request));
                }
                if (httpConfig.add_json_decoder) {
                    mediaTypeDecoders.add(new JsonDecoder(request));
                }
            }
        }
        if (request == null || method == null) {
            log.warn(String.format("Method or request is null \n method \n%s \n request \n%s",
                    method, request));
            throw new WebApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, request);
        }
        //we have a request and it matches a registered method
        if (!isEntityRequest()) {
            if (msg instanceof LastHttpContent) {
                //only post and put requests  are allowed to send form data so everything else just returns
                invoke(ctx);
            }
        } else {
            if (decoder == null) {
                for (MediaTypeDecoder d : mediaTypeDecoders) {
                    if (d.canDecode(request.getContentType())) {
                        decoder = d;
                        break;
                    }
                }
                if (decoder == null) {
                    throw new WebApplicationException(HttpResponseStatus.NOT_ACCEPTABLE);
                }
            }
            //decoder is created if it doesn't exist, can decode all if entire message received
            request.setChunked(HttpHeaders.isTransferEncodingChunked(request));
            if (msg instanceof HttpContent) {
                // New chunk is received
                HttpContent chunk = (HttpContent) msg;
                decoder.offer(chunk);
                if (chunk instanceof LastHttpContent) {
                    decoder.finished(ctx);
                    invoke(ctx);
                }
            }
        }
    }

    /**
     * @return true if post or put request, i.e. requests that have a body/entity
     */
    private boolean isEntityRequest() {
        return io.netty.handler.codec.http.HttpMethod.POST.name().equalsIgnoreCase(request.getMethod().name()) ||
                io.netty.handler.codec.http.HttpMethod.PUT.name().equalsIgnoreCase(request.getMethod().name());
    }

    protected void invoke(final ChannelHandlerContext ctx) {
        MessagePusher pusher = new MessagePusher() {
            @Override
            public ChannelFuture push(Object message) {
                //http methods can return null or void and still have the response injected and modified
                //so null messages are allowed here
                Object wrappedRes = message != null && message instanceof WrappedResponse ?
                        ((WrappedResponse) message).data() : null;
                if (wrappedRes != null) {
                    message = wrappedRes;
                }

                Queue<ResponseTransformer> transformers = protocolConfig.getTransformers();
                return writeResponse(ctx, message, transformers);
            }

            @Override
            public ChannelHandlerContext ctx() {
                return ctx;
            }
        };
        //inject globally available provider
        DependencyProvider provider = decoder == null ? DependencyProvider.from() : decoder.provider();
        //take all objects in the global provider
        provider.take(DependencyProvider.global());

        provider.add(ctx, ctx.channel(), ctx.executor(), request, res,
                request.getFormFiles(), request.getFormParam(), request.getCookies(), request.getSubject(),
                request.getSubject().getSession(), request.getQueryParams(), pusher, request.getPath());

        Object[] params = Injector.inject(method.method().getParameterTypes(), new Object[0], provider);
        //inject request specific provider
        injector.injectParams(method, request, res, ctx, params);
        try {
            Object response = method.invoke(ctx, request.getUri(), method, params, provider);
            pusher.push(response);
        } catch (Throwable t) {
            if (t.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) t.getCause();
            } else {
                logDetailedFailMessage(true, params, t, method.method());
                throw new WebApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, request, t);
            }
        }
    }

    protected ChannelFuture writeResponse(ChannelHandlerContext ctx, Object response, Queue<ResponseTransformer> t) {
        if (res.isRedirect()) {
            return doWrite(ctx);
        }

        if (response instanceof HttpResponse) {
            res = (HttpResponse) response;
            return doWrite(ctx);
        }
        List<ResponseTransformer> ts = new FixedSortedList<>(t);
        boolean notAcceptable = false;
        for (ResponseTransformer transformer : ts) {
            if (transformer.canTransform(response, request, request.getMatchedMediaType(), method, ctx)) {
                transformer.transform(response, request, res, request.getMatchedMediaType(),
                        method, ctx);
                notAcceptable = false;
                break;
            }
            notAcceptable = true;
        }
        if (notAcceptable) {
            res.setStatus(HttpStatus.NOT_ACCEPTABLE);
        }
        return doWrite(ctx);
    }

    protected ChannelFuture doWrite(ChannelHandlerContext ctx) {
        long responseSize = getHeader(res, HttpHeaders.Names.CONTENT_LENGTH) == null ?
                res.content().writerIndex() : HttpHeaders.getContentLength(res);
        //apply request cookies to response, this includes the session id
        res.finalizeCustomHeaders(request);
        // Decide whether to close the connection or not.
        boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(request.headers().get(CONNECTION))
                || request.getProtocolVersion().equals(HttpVersion.HTTP_1_0)
                && !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(request.headers().get(CONNECTION));
        if (!close && res.getManagedWriter() == null) {
            setContentLength(res, res.content().readableBytes());
        }
        ChannelFuture future;
        if (res.getManagedWriter() == null) {
            //if no post write op is set then the handler flushes the response
            future = ctx.writeAndFlush(res);
        } else {
            //if there is write manager it'll do all the write/flush
            future = res.doManagedWrite();
            ResolvedFile f = res.getManagedWriter().getFile();
            if (f != null) {
                responseSize = f.size();
            }
        }
        // Close the connection after the write operation is done if necessary.
        if (close || !config.enable_keep_alive_requests) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
        if (config.log_requests) {
            SocketAddress address = ctx.channel().remoteAddress();
            //going with the Apache format
            //194.116.215.20 - [14/Nov/2005:22:28:57 +0000] “GET / HTTP/1.0″ 200 16440
            requestLogger.info(String.format("%s - [%s] \"%s %s %s\" %s %s",
                    address,
                    HttpHeaders.getDate(request, request.getCreatedAt().toDate()),
                    request.getMethod().name(),
                    request.getUri(),
                    request.getProtocolVersion(),
                    res.getStatus().code(),
                    responseSize
            ));
        }
        //clean up and prep for next request. if keep-alive browsers like chrome will
        //make multiple requests on the same channel
        request = null;
        res = null;
        decoder = null;
        replied = true;
        return future;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            if (cause instanceof WebApplicationException) {
                writeResponse(ctx, cause, protocolConfig.getTransformers());
            } else {
                log.warn(String.format("Error while processing request %s", request), cause);
                writeResponse(ctx, new WebApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, request, cause),
                        protocolConfig.getTransformers());
            }
        } catch (Throwable t) {
            //at this point if an exception occurs, just log and return internal server error
            //internal server error
            log.warn(String.format("Uncaught error while processing request %s", request), cause);
            res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            doWrite(ctx);
        }
    }
}
