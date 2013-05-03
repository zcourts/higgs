package io.higgs.http.server.protocol;

import io.higgs.http.server.params.HttpCookie;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.ServerCookieEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpResponse extends DefaultFullHttpResponse {
    private Map<String, HttpCookie> cookies = new HashMap<>();
    private StaticFilePostWriteOperation postWriteOp;
    private final ByteBuf content;
    private HttpResponseStatus status;
    private HttpVersion version = HttpVersion.HTTP_1_1;
    private HttpHeaders headers = new DefaultHttpHeaders();
    private DecoderResult result;

    public HttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content) {
        super(version, status);
        this.content = content;
    }

    /**
     * Creates a new instance.
     *
     * @param version the HTTP version of this response
     * @param status  the status of this response
     */
    public HttpResponse(HttpVersion version, HttpResponseStatus status) {
        this(version, status, Unpooled.buffer());
    }

    public HttpResponse(HttpResponseStatus status) {
        this(HttpVersion.HTTP_1_1, status);
    }

    /**
     * Initializes a response with 200 status and sets the connection header to whatever the client
     * requested. If no connection header is found in the client request then it is set to CLOSE
     *
     * @param message
     */
    public HttpResponse(final HttpRequest message) {
        this(message == null ? HttpVersion.HTTP_1_1 : message.getProtocolVersion(), HttpStatus.OK);
        if (message != null) {
            String conn = message.headers().get(HttpHeaders.Names.CONNECTION);
            if (conn == null) {
                conn = HttpHeaders.Values.CLOSE;
            }
            headers().set(HttpHeaders.Names.CONNECTION, conn);
        }
    }

    /**
     * creates a 200 ok response
     */
    public HttpResponse() {
        this(HttpResponseStatus.OK);
    }

    public HttpResponse(final ByteBuf buffer) {
        this(HttpVersion.HTTP_1_1, HttpStatus.OK, buffer);
    }

    public HttpResponse(HttpStatus status, ByteBuf buffer) {
        this(HttpVersion.HTTP_1_1, status, buffer);
    }

    public ByteBuf content() {
        return content;
    }

    public void setCookies(final Map<String, HttpCookie> cookies) {
        this.cookies.putAll(cookies);
    }

    /**
     * Sets a cookie with path as "/"
     *
     * @param name
     * @param value
     */
    public void setCookie(final String name, final String value) {
        HttpCookie cookie = new HttpCookie(name, value);
        cookie.setPath("/");
        cookies.put(name, cookie);
    }

    public void setCookie(final HttpCookie cookie) {
        cookies.put(cookie.getName(), cookie);
    }

    public void clearHeaders() {
        cookies.clear();
        headers().clear();
    }

    /**
     * sets any overridden headers
     */
    protected void finalizeCustomHeaders() {
        headers().set(HttpHeaders.Names.SET_COOKIE,
                ServerCookieEncoder.encode(new ArrayList<Cookie>(cookies.values())));
    }

    public void postWrite(ChannelFuture future) {
        if (postWriteOp != null && !postWriteOp.isDone()) {
            postWriteOp.apply();
        }
    }

    public void setPostWriteOp(StaticFilePostWriteOperation postWriteOp) {
        this.postWriteOp = postWriteOp;
    }

    public StaticFilePostWriteOperation getPostWriteOp() {
        return postWriteOp;
    }

    @Override
    public HttpResponseStatus getStatus() {
        return status;
    }

    @Override
    public FullHttpResponse setStatus(HttpResponseStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public HttpVersion getProtocolVersion() {
        return version;
    }

    @Override
    public FullHttpResponse setProtocolVersion(HttpVersion version) {
        this.version = version;
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public DecoderResult getDecoderResult() {
        return result;
    }

    @Override
    public void setDecoderResult(DecoderResult result) {
        this.result = result;
    }
}
