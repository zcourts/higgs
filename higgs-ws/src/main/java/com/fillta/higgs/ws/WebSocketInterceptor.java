package com.fillta.higgs.ws;

import com.fillta.higgs.HiggsInterceptor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketInterceptor implements HiggsInterceptor {
	//one interceptor may be accessed from multiple threads
	private final Set<String> paths = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	public final static AttributeKey<WebSocketServerHandshaker> attHandshaker = new AttributeKey("handshaker");
	private final WebSocketServer server;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public WebSocketInterceptor(WebSocketServer server) {
		this.server = server;
	}

	public boolean matches(Object msg) {
		if (msg instanceof HttpRequest) {
			//if it's an HttpRequest then return true if one of the paths match
			String uri = ((HttpRequest) msg).getUri();
			for (String path : paths) {
				if (path.equalsIgnoreCase(uri)) {
					return true;
				}
			}
		} else if (msg instanceof WebSocketFrame) {
			return true;
		}
		return false;
	}


	public boolean intercept(final ChannelHandlerContext ctx, final Object msg) {
		if (msg instanceof HttpRequest) {
			//if it's an HttpRequest we need to do handshake, find the path that matched and do it
			return canPerformHandShake(ctx, (HttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			WebSocketFrame frame = (WebSocketFrame) msg;
			return canHandleWebSocketFrame(ctx, frame);
		}
		return false;
	}

	private boolean canHandleWebSocketFrame(final ChannelHandlerContext ctx, final WebSocketFrame frame) {
		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			ctx.channel().attr(attHandshaker).get()
					.close(ctx.channel(), (CloseWebSocketFrame) frame);
			return true;
		}
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.getBinaryData()));
			ctx.flush();
			return true;
		}
		if (!(frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
					.getName()));
		}
		server.emitMessage(ctx, (TextWebSocketFrame) frame);
		//as long as we emit the message return true
		return true;
	}

	private boolean canPerformHandShake(ChannelHandlerContext ctx, HttpRequest req) {
		for (String path : paths) {
			if (path.equalsIgnoreCase(req.getUri())) {
				String wsPath = getWebSocketLocation(req, path);
				try {
					// Handshake
					WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
							wsPath, null, false);
					WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
					if (handshaker == null) {
						WebSocketServerHandshakerFactory
								.sendUnsupportedWebSocketVersionResponse(ctx.channel());
					} else {
						handshaker.handshake(ctx.channel(), req);
					}
					//make sure we set the handshake object here...
					ctx.channel().attr(attHandshaker).set(handshaker);
					return true;
				} catch (WebSocketHandshakeException wshe) {
					log.debug(String.format("HTTP request received on WebSocket path, will not intercept."));
					return false;
				}
			}
		}
		//a path must match and the handshake must be performed for true to be returned
		return false;
	}

	/**
	 * Constructs a web socket URL from the request host and path
	 *
	 * @param req
	 * @return
	 */
	private String getWebSocketLocation(HttpRequest req, String path) {
		return "ws://" + req.getHeader(HOST) + path;
	}

	/**
	 * Registers a path to this interceptor
	 *
	 * @param path
	 */
	public void addPath(final String path) {
		if (path != null && !path.isEmpty()) {
			paths.add(path);
		} else {
			throw new IllegalArgumentException("Cannot add a null or empty interceptor path");
		}
	}
}
