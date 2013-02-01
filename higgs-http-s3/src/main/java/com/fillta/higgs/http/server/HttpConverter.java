package com.fillta.higgs.http.server;

import com.fillta.higgs.http.server.params.HttpCookie;
import com.fillta.higgs.http.server.params.HttpFile;
import com.fillta.higgs.http.server.params.HttpSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpConverter {
    public static AttributeKey<Boolean> attChunks = new AttributeKey<>("http-server-reading-chunks");
    public static AttributeKey<HttpPostRequestDecoder> attDecoder = new AttributeKey<>("http-server-files-decoder");
    public static AttributeKey<HttpRequest> attRequest = new AttributeKey<>("http-channel-request");
    public static AttributeKey<Boolean> attSeen = new AttributeKey<>("http-server-converter-seen-request");
    private static HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE); //Disk
    private Logger log = LoggerFactory.getLogger(getClass());
    private HttpServer server;

    public HttpConverter(HttpServer server) {
        this.server = server;
    }

    public HttpResponse serialize(Channel ctx, HttpResponse msg) {
        return msg;
    }

    /**
     * Deserialize an http request. if its a post or put request the de serialization can happen
     * accross multiple instances of the http converter. to maintain state, data is associated with
     * the channel which initiated the request.  Once all files and form parameters are received
     * this method will return a fully de-serialized http request. Until then it will return null.
     *
     * @param ctx The Netty channel context
     * @param msg the serialized message
     * @return the de-serialized request
     */
    public HttpRequest deserialize(ChannelHandlerContext ctx, Object msg) {
        Attribute<HttpRequest> requestAttribute = ctx.channel().attr(attRequest);
        Attribute<Boolean> resSeen = ctx.channel().attr(attSeen);
        resSeen.compareAndSet(null, false);
        //boolean seen = resSeen.get();
        resSeen.set(true);
        //in large post requests only the first object will be of type HttpRequest, others may be chunks
        if (msg instanceof HttpRequest) {
            //since we have the request see if we had one on the channel already, if not init session
            HttpRequest request = requestAttribute.get();
            if (request != null) {
                //multiple requests can be made via the same channel. Chrome for e.g.
                //keeps a socket open and sends multiple requests on it, so always remove previously
                //associated request
                requestAttribute.set(null);
                request = null;
            }
            //associate a request with the channel if not already done
            request = initHiggsRequest((HttpRequest) msg);
            requestAttribute.set(request);
            if (!HttpMethod.POST.name().equalsIgnoreCase(request.getMethod().name()) &&
                    !HttpMethod.PUT.name().equalsIgnoreCase(request.getMethod().name())) {
                //only post and put requests  are allowed to send form data so everything else just returns
                return request;
            } else {
                //if its a post or put request and a decoder doesn't exist then create one.
                Attribute<HttpPostRequestDecoder> decoderAttribute = ctx.channel().attr(attDecoder);
                if (decoderAttribute.get() == null) {
                    try {
                        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, request);
                        decoderAttribute.set(decoder);
                    } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
                        log.warn("Unable to decode data", e1);
                        throw new WebApplicationException(HttpStatus.BAD_REQUEST, request);
                    } catch (HttpPostRequestDecoder.IncompatibleDataDecoderException e) {
                        log.warn("Incompatible request type", e);
                        throw new WebApplicationException(HttpStatus.BAD_REQUEST, request);
                    }
                }
                //decoder is created if it doesn't exist, can decode all if entire message received
                Attribute<Boolean> chunksAtt = ctx.channel().attr(attChunks);
                chunksAtt.compareAndSet(null, HttpHeaders.isTransferEncodingChunked(request));
                boolean readingChunks = chunksAtt.get();
                if (!readingChunks) {
                    if (decoderAttribute.get().isMultipart()) {
                        //reading chunks... won't come here again
                        ctx.channel().attr(attChunks).set(true);
                    } else {
                        // Not chunk version
                        return readAllHttpDataReceived(ctx.channel());
                    }
                }
            }
        }
        if (msg instanceof HttpContent) {
            if (requestAttribute.get() == null) {
                return null;
            }
            if (!HttpMethod.POST.name().equalsIgnoreCase(requestAttribute.get().getMethod().name()) &&
                    !HttpMethod.PUT.name().equalsIgnoreCase(requestAttribute.get().getMethod().name())) {
                //only post and put requests have content
                return null;
            } else {
                //reading chunks, channel would have had decoder created already
                HttpPostRequestDecoder decoder = ctx.channel().attr(attDecoder).get();
                // New chunk is received
                HttpContent chunk = (HttpContent) msg;
                try {
                    decoder.offer(chunk);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
                    log.warn("Unable to decode HTTP chunk", e1);
                    throw new WebApplicationException(HttpStatus.BAD_REQUEST, requestAttribute.get());
                }
                if (chunk instanceof LastHttpContent) {
                    ctx.channel().attr(attChunks).set(false);
                    return readAllHttpDataReceived(ctx.channel());
                }
            }
        }
        //not done receiving post or put request so return null
        return null;
    }

    private HttpRequest readAllHttpDataReceived(Channel channel) {
        //entire message/request received
        HttpPostRequestDecoder decoder = channel.attr(attDecoder).get();
        List<InterfaceHttpData> data;
        try {
            data = decoder.getBodyHttpDatas();
        } catch (HttpPostRequestDecoder.NotEnoughDataDecoderException e1) {
            log.warn("Not enough data to decode", e1);
            throw new WebApplicationException(HttpStatus.BAD_REQUEST, channel.attr(attRequest).get());
        }
        //called when all data is received, go over request data and separate form fields from files
        Attribute<HttpRequest> requestAttribute = channel.attr(attRequest);
        HttpRequest request = requestAttribute.get();
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
        requestAttribute.set(null);
        return request;
    }

    private HttpRequest initHiggsRequest(HttpRequest request) {
        //custom initialization that cannot be done in constructor because data is not known at the time
        request.init();
        //if the user has no session available then set one
        if (!request.hasSessionID()
                //if session cookie exists but the server was restarted or doesn't have the session for some reason
                || server.getSession(request.getSessionId()) == null) {
            SecureRandom random = new SecureRandom();
            String id = new BigInteger(130, random).toString(32);
            HttpCookie session = new HttpCookie(HttpServer.SID, id);
            session.setPath(server.getConfig().session_path);
            session.setMaxAge(server.getConfig().session_max_age);
            session.setHttpOnly(server.getConfig().session_http_only);
            if (server.getConfig().session_domain != null &&
                    !server.getConfig().session_domain.isEmpty()) {
                session.setDomain(server.getConfig().session_domain);
            }
            String sp = server.getConfig().session_ports;
            if (sp != null && !sp.isEmpty()) {
                String[] ps = sp.split(",");
                List<Integer> ports = new ArrayList<>(ps.length);
                for (int i = 0; i < ps.length; i++) {
                    String p = ps[i];
                    try {
                        ports.add(parseInt(p));
                    } catch (NumberFormatException nfe) {
                        log.warn(String.format("Session port config contained non-numeric value (%s)", p));
                    }
                }
                session.setPorts(ports);
            }
            request.setNewSession(session);
            server.getSessions().put(id, new HttpSession());
        }
        return request;
    }
}
