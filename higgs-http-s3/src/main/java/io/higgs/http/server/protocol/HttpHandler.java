package io.higgs.http.server.protocol;

import io.higgs.core.FixedSortedList;
import io.higgs.core.InvokableMethod;
import io.higgs.core.MessageHandler;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.higgs.http.server.HttpStatus;
import io.higgs.http.server.ParamInjector;
import io.higgs.http.server.StaticFileMethod;
import io.higgs.http.server.WebApplicationException;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.http.server.params.HttpFile;
import io.higgs.http.server.transformers.ResponseTransformer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.Queue;

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
    protected static HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); //Disk
    /**
     * The current HTTP request
     */
    protected HttpRequest request;
    /**
     * The current HTTP method which matches the current {@link #request}.
     * If no method matches this will be null
     */
    protected HttpMethod method;
    protected ParamInjector injector;
    protected HttpProtocolConfiguration protocolConfig;
    protected HttpPostRequestDecoder decoder;

    public HttpHandler(HttpProtocolConfiguration config) {
        super(config.getServer().getConfig());
        protocolConfig = config;
        injector = config.getInjector();
        // should delete file
        DiskFileUpload.deleteOnExitTemporaryFile = config.getServer().getConfig().files.delete_temp_on_exit;
        // system temp directory
        DiskFileUpload.baseDirectory = config.getServer().getConfig().files.temp_directory;
        // should delete file on
        DiskAttribute.deleteOnExitTemporaryFile = config.getServer().getConfig().files.delete_temp_on_exit;
        // exit (in normal exit)
        DiskAttribute.baseDirectory = config.getServer().getConfig().files.temp_directory; // system temp directory
    }

    public <M extends InvokableMethod> M findMethod(String path, ChannelHandlerContext ctx,
                                                    Object msg, Class<M> methodClass) {
        M m = super.findMethod(path, ctx, msg, methodClass);
        if (m == null && config.add_static_resource_filter) {
            StaticFileMethod fileMethod = new StaticFileMethod(protocolConfig);
            if (fileMethod.matches(path, ctx, msg)) {
                return (M) fileMethod;
            }
        }
        return m;
    }

    public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            if (request != null) {
                //browsers like chrome keep the connection open and make additional requests
//                throw new IllegalStateException(String.format("HttpRequest instance received but request already
// set." +
//                        "Old request :\n%s \nNew request :\n%s", request, msg));
            }
            request = (HttpRequest) msg;
            //apply transcriptions
            protocolConfig.getTranscriber().transcribe(request);
            //must always set protocol config before anything uses the request
            request.setConfig(protocolConfig);
            //initialise request, setting cookies, media types etc
            request.init();
            method = findMethod(request.getUri(), ctx, msg, methodClass);
            if (method == null) {
                //404
                throw new WebApplicationException(HttpStatus.NOT_FOUND, request);
            }
        }
        if (request == null || method == null) {
            log.warn(String.format("Method or request is null \n method \n%s \n request \n%s",
                    method, request));
            throw new WebApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, request);
        }
        //we have a request and it matches a registered method
        if (!io.netty.handler.codec.http.HttpMethod.POST.name().equalsIgnoreCase(request.getMethod().name()) &&
                !io.netty.handler.codec.http.HttpMethod.PUT.name().equalsIgnoreCase(request.getMethod().name())) {
            if (msg instanceof LastHttpContent && !(msg instanceof HttpRequest)) {
                //only post and put requests  are allowed to send form data so everything else just returns
                invoke(ctx);
            }
        } else {
            if (msg instanceof HttpRequest) {
                //if its a post or put request and a decoder doesn't exist then create one.
                if (decoder == null) {
                    try {
                        decoder = new HttpPostRequestDecoder(factory, request);
                    } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
                        log.warn("Unable to decode data", e1);
                        throw new WebApplicationException(HttpStatus.BAD_REQUEST, request);
                    } catch (HttpPostRequestDecoder.IncompatibleDataDecoderException e) {
                        log.warn("Incompatible request type", e);
                        throw new WebApplicationException(HttpStatus.BAD_REQUEST, request);
                    }
                }
                //decoder is created if it doesn't exist, can decode all if entire message received
                request.setChunked(HttpHeaders.isTransferEncodingChunked(request));
                request.setMultipart(decoder.isMultipart());

                if (!request.isChunked()) {
                    allHttpDataReceived(ctx);
                }
            }
            if (msg instanceof HttpContent) {
                // New chunk is received
                HttpContent chunk = (HttpContent) msg;
                try {
                    decoder.offer(chunk);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
                    log.warn("Unable to decode HTTP chunk", e1);
                    throw new WebApplicationException(HttpStatus.BAD_REQUEST, request);
                }
                if (chunk instanceof LastHttpContent) {
                    allHttpDataReceived(ctx);
                }
            }
        }
    }

    private void allHttpDataReceived(ChannelHandlerContext ctx) {
        //entire message/request received
        List<InterfaceHttpData> data;
        try {
            data = decoder.getBodyHttpDatas();
        } catch (HttpPostRequestDecoder.NotEnoughDataDecoderException e1) {
            log.warn("Not enough data to decode", e1);
            throw new WebApplicationException(HttpStatus.BAD_REQUEST, request);
        }
        //called when all data is received, go over request data and separate form fields from files
        for (InterfaceHttpData httpData : data) {
            //check if is file upload or attribute, attributes go into form params and file uploads to file params
            if (httpData instanceof io.netty.handler.codec.http.multipart.Attribute) {
                io.netty.handler.codec.http.multipart.Attribute field =
                        (io.netty.handler.codec.http.multipart.Attribute) httpData;
                try {
                    //add form param
                    request.addFormField(field.getName(), field.getValue());
                } catch (IOException e) {
                    log.warn(String.format("unable to extract form field's value, field name = %s", field.getName()));
                }
            } else {
                if (httpData instanceof FileUpload) {
                    //add form file
                    request.addFormFile(new HttpFile((FileUpload) httpData));
                } else {
                    if (httpData != null) {
                        log.warn(String.format("Unknown form type encountered Class: %s,data type:%s,name:%s",
                                httpData.getClass().getName(), httpData.getHttpDataType().name(), httpData.getName()));
                    }
                }
            }
        }
        invoke(ctx);
    }

    private void invoke(ChannelHandlerContext ctx) {
        Object[] params = injector.injectParams(method, request, ctx);
        try {
            Object response = method.invoke(ctx, request.getUri(), method, params);
            Queue<ResponseTransformer> transformers = protocolConfig.getTransformers();
            writeResponse(ctx, response, transformers);
        } catch (Throwable t) {
            logDetailedFailMessage(true, params, t, method.method());
            throw new WebApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, request, t);
        }
    }

    private void writeResponse(ChannelHandlerContext ctx, Object response, Queue<ResponseTransformer> t) {
        List<ResponseTransformer> ts = new FixedSortedList<>(t);
        HttpResponse httpResponse = null;
        if (response == null) {
            //method returned nothing
            httpResponse = new HttpResponse(HttpStatus.NO_CONTENT);
        }
        for (ResponseTransformer transformer : ts) {
            if (transformer.canTransform(response, request, request.getMatchedMediaType(), method, ctx)) {
                httpResponse = transformer.transform(response, request, request.getMatchedMediaType(),
                        method, ctx);
                break;
            }
        }
        if (httpResponse == null) {
            //no transformer could create a response to match what the client accepts
            httpResponse = new HttpResponse(HttpStatus.NOT_ACCEPTABLE);
        }
        doWrite(ctx, httpResponse);
    }

    private void doWrite(ChannelHandlerContext ctx, HttpResponse response) {
        //apply request cookies to response, this includes the session id
        response.setCookies(request.getCookies());
        response.finalizeCustomHeaders();
        if (config.log_requests) {
            SocketAddress address = ctx.channel().remoteAddress();
            //going with the Apache format
            //194.116.215.20 - [14/Nov/2005:22:28:57 +0000] “GET / HTTP/1.0″ 200 16440
            log.info(String.format("%s - [%s] \"%s %s %s\" %s %s",
                    address,
                    HttpHeaders.getDate(request, request.getCreatedAt().toDate()),
                    request.getMethod().name(),
                    request.getUri(),
                    request.getProtocolVersion(),
                    response.getStatus().code(),
                    getHeader(response, HttpHeaders.Names.CONTENT_LENGTH) == null ?
                            response.content().writerIndex() : HttpHeaders.getContentLength(response)
            ));
        }
        // Decide whether to close the connection or not.
        boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(request.headers().get(CONNECTION))
                || request.getProtocolVersion().equals(HttpVersion.HTTP_1_0)
                && !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(request.headers().get(CONNECTION));
        if (!close && response.getPostWriteOp() == null) {
            setContentLength(response, response.content().readableBytes());
        }
        ChannelFuture future = ctx.write(response);
        response.postWrite(future);
        // Close the connection after the write operation is done if necessary.
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            if (cause instanceof WebApplicationException) {
                writeResponse(ctx, cause, protocolConfig.getErrorTransformers());
            } else {
                log.warn(String.format("Error while processing request %s", request), cause);
                writeResponse(ctx, new WebApplicationException(HttpStatus.INTERNAL_SERVER_ERROR, request, cause),
                        protocolConfig.getErrorTransformers());
            }
        } catch (Throwable t) {
            //at this point if an exception occurs, just log and return internal server error
            //internal server error
            log.warn(String.format("Uncaught error while processing request %s", request), cause);
            HttpResponse ise = new HttpResponse(HttpStatus.INTERNAL_SERVER_ERROR);
            doWrite(ctx, ise);
        }
    }
}
