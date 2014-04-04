package io.higgs.ws.protocol;

import com.google.common.net.HttpHeaders;
import io.higgs.core.StaticUtil;
import io.higgs.http.server.protocol.HttpHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * A stateful {@link io.higgs.core.MessageHandler} which processes HttpRequests.
 * There will be 1 instance of this class per Http request.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketHandler extends HttpHandler {
    protected WebSocketConfiguration protocolConfig;
    private final String WEBSOCKET_PATH;
    private WebSocketServerHandshaker handshaker;

    public WebSocketHandler(WebSocketConfiguration config) {
        super(config);
        protocolConfig = config;
        WEBSOCKET_PATH = config.getWebsocketPath();
    }

    /**
     * Only HTTP GET requests come through the WebSocketHandler.
     * The {@link WebSocketDetector} uses the {@link io.netty.handler.codec.http.HttpObjectAggregator}
     * to ensure that only {@link FullHttpRequest}s are passed in.
     * If it is a full http request then if the path matches the configured web socket path
     * the request is handled as a WebSocket upgrade request (if the upgrade header is present)
     * <p/>
     * Otherwise the request is passed to the {@link HttpHandler} which will handle the request
     * as a normal HTTP request.
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        } else {
            super.channelRead0(ctx, msg);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        if (!WEBSOCKET_PATH.equalsIgnoreCase(req.getUri()) ||
                !req.headers().contains(HttpHeaders.UPGRADE) ||
                req.getMethod() != GET) {
            //if the web socket path doesn't match then it's a normal GET request
            super.channelRead0(ctx, req);
            return;
        }
        // Handle a bad request.
        if (!req.getDecoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        try {
            // Handshake
            WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                    getWebSocketLocation(req), null, false);
            handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), req);
            }
        } catch (WebSocketHandshakeException wshe) {
            super.channelRead0(ctx, req);
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            frame.retain();
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame);
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            frame.content().retain();
            StaticUtil.write(ctx.channel(), new PongWebSocketFrame(frame.content()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }
        protocolConfig.getWebSocketEventHandler().onMessage((TextWebSocketFrame) frame, this,
                ctx, methods, protocolConfig);
    }

    private void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        if (res.getStatus().code() != 200) {
            res.content().writeBytes(Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
            setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = StaticUtil.write(ctx.channel(),res);
        if (!isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private String getWebSocketLocation(FullHttpRequest req) {
        return "ws://" + req.headers().get(HOST) + WEBSOCKET_PATH;
    }
}
