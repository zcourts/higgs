package com.fillta.higgs.http.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpMessageConverter {
    public static final AttributeKey<String> attTopic = new AttributeKey<>("http-message-converter-topic");
    public static final AttributeKey<HTTPResponse> attResponse = new AttributeKey<>("http-message-converter-response");
    public static final AttributeKey<Boolean> attSeen = new AttributeKey<>("http-message-converter-seen-request");
    public static final AttributeKey<Long> attLength = new AttributeKey<>("http-message-converter-response-length");
    //protected Logger log = LoggerFactory.getLogger(getClass());

    public Object serialize(Channel channel, HttpRequest msg) {
        channel.attr(attTopic).set(msg.getId());
        return msg;
    }

    public HTTPResponse deserialize(ChannelHandlerContext ctx, Object msg) {
        Attribute<HTTPResponse> resAttr = ctx.channel().attr(attResponse);
        resAttr.compareAndSet(null, new HTTPResponse());
        HTTPResponse response = resAttr.get();
        Attribute<String> topicAtt = ctx.channel().attr(attTopic);
        topicAtt.compareAndSet(null, "");
        String requestID = topicAtt.get();
        response.setRequestID(requestID);
        Attribute<Boolean> resSeen = ctx.channel().attr(attSeen);
        resSeen.compareAndSet(null, false);
        boolean seen = resSeen.get();
        resSeen.set(true);
        Attribute<Long> resLength = ctx.channel().attr(attLength);
        resLength.compareAndSet(null, -1L);
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            response.setStatus(res.status());
            response.setProtocolVersion(res.protocolVersion());
            response.setChunkedTransferEncoding(HttpHeaders.isTransferEncodingChunked(res));
            if (!res.headers().isEmpty()) {
                for (String name : res.headers().names()) {
                    if (HttpHeaders.Names.CONTENT_LENGTH.equalsIgnoreCase(name)) {
                        String length = res.headers().get(name);
                        if (length != null) {
                            resLength.set(Long.parseLong(length));
                        }
                    }
                    for (String value : res.headers().getAll(name)) {
                        response.putHeader(name, value);
                    }
                }
            }
        }
        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;
            response.write(content.data());
            if (content instanceof LastHttpContent
                    //bug???, Netty doesn't always send LastHttpContent try to infer end of content
                    || (response.getBuffer().writerIndex() >= resLength.get() && resLength.get() > -1)) {
                //mark the stream as ended
                response.streamEnded();
                cleanUp(ctx);
            }
            //we would have sent this response back already we don't want event processor to
            //invoke callbacks multiple times for the same response so when a HttpContent is received
            //return null and the stream would have been updated without triggering callback again
            return null;
        }
        if (seen) {
            //don't trigger callback again if we've seen this response before
            return null;
        }
        return response;
    }

    private void cleanUp(final ChannelHandlerContext ctx) {
        ctx.channel().attr(attTopic).remove();
        ctx.channel().attr(attSeen).remove();
    }
}

