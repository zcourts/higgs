package com.fillta.higgs.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fillta.higgs.MessageConverter;
import com.fillta.higgs.MessageTopicFactory;
import com.fillta.higgs.RPCServer;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.http.server.HttpServer;
import com.fillta.higgs.http.server.config.ServerConfig;
import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.IOException;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketServer extends RPCServer<JsonEvent, JsonEvent, TextWebSocketFrame> {
	public final static ObjectMapper mapper = new ObjectMapper();
	/**
	 * This server is never bound to a port BY DEFAULT. An already bound instance of an
	 * {@link HttpServer} can be provided via {@link #WebSocketServer(HttpServer, String, int)}
	 * By default it shares the same port as this {@link WebSocketServer} and is used to
	 * handle HTTP requests on the WebSocket port.
	 */
	public final HttpServer HTTP;
	protected final WebSocketInterceptor interceptor;

	/**
	 * Creates a web socket server whose only path is set to /
	 *
	 * @param port
	 */
	public WebSocketServer(int port) {
		this(new HttpServer(new ServerConfig()), "/", port);
	}

	public WebSocketServer(HttpServer http, String path, int port) {
		super(port);
		HTTP = http;
		interceptor = new WebSocketInterceptor(this);
		addPath(path);
		setSniffProtocol(true);
		enableHTTPDetection(HTTP);
		addProtocolDetector(new FlashSocketProtocolDetector(this));
		//must add interceptor to HTTP requests
		HTTP.addInterceptor(interceptor);
	}

	public void addPath(String path) {
		interceptor.addPath(path);
	}

	public Object[] getArguments(Class<?>[] argTypes, ChannelMessage<JsonEvent> request) {
		if (argTypes.length == 0)
			return new Object[0];
		Object[] args = new Object[argTypes.length];
		for (int i = 0; i < argTypes.length; i++) {
			if (argTypes[i].isAssignableFrom(JsonEvent.class))
				args[i] = request.message;
			else if (argTypes[i].isAssignableFrom(ChannelMessage.class))
				args[i] = request;
			else if (argTypes[i].isAssignableFrom(Channel.class))
				args[i] = request.channel;
			else if (argTypes[i].isAssignableFrom(ChannelHandlerContext.class))
				args[i] = request.context;
		}
		return args;
	}

	protected JsonEvent newResponse(String methodName, ChannelMessage<JsonEvent> request,
	                                Optional<Object> returns, Optional<Throwable> error) {
		if (returns.isPresent()) {
			if (returns.get() instanceof JsonEvent) {
				JsonEvent event = (JsonEvent) returns.get();
				event.setTopic(methodName);
				return event;
			} else {
				return new JsonEvent(methodName, returns);
			}
		}
		return null;
	}

	public MessageConverter<JsonEvent, JsonEvent, TextWebSocketFrame> serializer() {
		return new MessageConverter<JsonEvent, JsonEvent, TextWebSocketFrame>() {
			public TextWebSocketFrame serialize(Channel ctx, JsonEvent msg) {
				try {
					return new TextWebSocketFrame(mapper.writeValueAsString(msg));
				} catch (JsonProcessingException e) {
					log.warn("Unable to serialize JsonEvent", e);
					return new TextWebSocketFrame();
				}
			}

			public JsonEvent deserialize(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
				try {
					return mapper.readValue(msg.getText(), JsonEvent.class);
				} catch (IOException e) {
					log.warn(String.format("Unable to de-serialize JSON message"), e);
					return new JsonEvent();
				}
			}
		};
	}

	public MessageTopicFactory<String, JsonEvent> topicFactory() {
		return new MessageTopicFactory<String, JsonEvent>() {
			public String extract(JsonEvent msg) {
				return msg.getTopic();
			}
		};
	}
}
