package com.fillta.higgs.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fillta.higgs.HiggsInterceptor;
import com.fillta.higgs.MessageConverter;
import com.fillta.higgs.MessageTopicFactory;
import com.fillta.higgs.RPCServer;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.http.server.HttpServer;
import com.fillta.higgs.http.server.config.ServerConfig;
import com.fillta.higgs.sniffing.ProtocolSniffer;
import com.fillta.higgs.ws.flash.*;
import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A WebSocket server piggy backs on an HTTP server. As such, this implementation makes user of Higgs'
 * {@link HiggsInterceptor} and {@link ProtocolSniffer}.
 * <p/>
 * If one of the registered protocol detectors
 * detects a WebSocket request it intercepts the request and passes it to this server.
 * <p/>
 * The structure of a message is fairly simple. It is a JSON object with two fields, topic and message.
 * The topic is a Higgs topic which identifies a method or callback registered on the WebSocket server.
 * The message is any arbitrary JSON object. Using {@link JsonEvent#as(Class)} the message can be converted
 * to a compatible type...
 * <p/>
 * <p/>
 * <h2>Flash Policy Pipeline</h2>
 * Since not all browsers support WebSockets we provide support for flash sockets. A requirement for
 * modern flash versions is that there is a "Flash Policy File" available before communication can start.
 * <p/>
 * This slightly complicates things and unless you're familiar with both Netty and Higgs it may not be easy
 * to connect just how it all works. This is an attempt to explain the process.
 * <p/>
 * When a binary socket's connect method is invoked in flash, flash automatically sends a request to the
 * server. The request contains the string '{@code<policy-file-request />}' or in some cases
 * '{@code<policy-file-request/>}' i.e. no space. When this request is sent, flash expects a policy file
 * to be returned.
 * <p/>
 * See <a href="http://www.adobe.com/cn/devnet/flashplayer/articles/socket_policy_files.html">Adobe's Docs</a>
 * and the sample flas-policy.xml file in the resources folder.
 * <p/>
 * Note that there are two types of policy files, one for Flash's XMLSocket and another for its Socket class.
 * Higgs only supports the latter. By default, if no policy file is provided the {@link FlashPolicyFile}
 * creates a very open policy file which allows all hosts and ports. You may want to provide one that
 * limits the host and port.
 * <p/>
 * Higgs will automatically respond to flash's request and provide it with whatever policy file it has
 * available. In order to do this, the request pipeline must be dynamic so as to support two sets of
 * encoders,decoders and handlers.
 * <p/>
 * Because the flash policy request comes first, the {@link FlashSocketPolicyDetector} is used first.
 * This basically checks for the policy request string mentioned before and if it's found sends the
 * policy file back to flash using the {@link FlashPolicyDecoder} and {@link FlashPolicyEncoder}.
 * Flash expects that the policy file returned is followed by a null byte terminator (basically 0x00 or just 0).
 * If the null byte isn't sent the flash socket will hang until it times out and the request fails.
 * <p/>
 * Once finished, consider this the handshake for a flash binary socket complete.
 * <p/>
 * <h2>Higgs' Flash Socket protocol</h2>
 * <p/>
 * In the next phase, the user attempts to send a JSON string to the server. In order to detect
 * this and provide simple protocol the flash client must also prefix any string with a 7 byte header. The
 * value of the first three bytes are 72,83,70 this basically forms the start of the protocol's header and is effectively the
 * 3 characters HFS ('H'=72,'F'=83,'S'=70), short for Higgs Flash Socket.
 * <p/>
 * The value of the next 4 bytes is a 32 bit signed integer which tells the server how many bytes are in
 * the message to come. These 7 bytes together for the protocol header.
 * <p/>
 * Once the header is sent (in the order mentioned) what follows next is a series of bytes that make up the
 * message's content.
 * <p/>
 * <h2>Higgs' Flash Protocol Pipeline</h2>
 * <p/>
 * Given the above protocol the server's pipeline then comes into play. Using the {@link FlashSocketProtocolDetector}
 * the first 3 bytes are used to detect the protocol. If the protocol matches then the {@link Decoder}
 * and {@link Encoder} are added to the pipeline.
 * <p/>
 * The decoder reads the JSON string received from the flash socket and constructs a {@link TextWebSocketFrame}
 * which can be handled by this server.
 * <p/>
 * Its important to note that the pipeline ordering makes a difference. For e.g. it takes 23 bytes to
 * detect a flash policy request where as it only takes 3 bytes to detect the data protocol.
 * <p/>
 * If the wrong decoder is put in the pipeline first it'll lead to situations where, the server interprets
 * the values wrong (because reading 4 random bytes of a string can lead to a massive number) which
 * results in the server waiting forever expecting more data to make up a single message.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketServer extends RPCServer<JsonEvent, JsonEvent, TextWebSocketFrame> {
	public final static ObjectMapper mapper = new ObjectMapper();
	/**
	 * This server is never bound to a port BY DEFAULT. An already bound instance of an
	 * {@link HttpServer} can be provided via {@link #WebSocketServer(HttpServer, FlashPolicyFile, String, int)}
	 * By default it shares the same port as this {@link WebSocketServer} and is used to
	 * handle HTTP requests on the WebSocket port.
	 */
	public final HttpServer HTTP;
	protected final WebSocketInterceptor interceptor;
	private final FlashPolicyFile policy;

	/**
	 * Creates a web socket server whose only path is set to /
	 *
	 * @param port
	 */
	public WebSocketServer(int port) {
		this(new HttpServer(new ServerConfig()), new FlashPolicyFile(), "/", port);
	}

	public WebSocketServer(HttpServer http, FlashPolicyFile policy, String path, int port) {
		super(port);
		HTTP = http;
		this.policy = policy;
		interceptor = new WebSocketInterceptor(this);
		addPath(path);
		setSniffProtocol(true);
		enableHTTPDetection(HTTP);
		//detects flash policy requests with 23 bytes
		addProtocolDetector(new FlashSocketPolicyDetector(this, this.policy));
		//detects the Higgs HFS protocol header with 3 bytes
		addProtocolDetector(new FlashSocketProtocolDetector(this, this.policy));
		//must add interceptor to HTTP requests
		HTTP.addInterceptor(interceptor);
		addErrorListener();
	}

	private void addErrorListener() {
		final WebSocketServer me = this;
		ChannelEventListener errorHandler = new ChannelEventListener() {
			public void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex) {
				Object request = me.getRequest(ctx.channel());
				if (request instanceof TextWebSocketFrame) {
					Map<String, String> returns = new HashMap<>();
					returns.put("error", ex.get().getMessage());
					returns.put("cause", ex.get().getCause() == null ? null : ex.get().getCause().getMessage());
					me.respond(ctx.channel(), new JsonEvent("error", mapper.createObjectNode().POJONode(returns)));
					log.warn("An error occurred handling a WebSocket/JSON request", ex.get());
				}
			}
		};
		//add the error handler to both event processors
		on(HiggsEvent.EXCEPTION_CAUGHT, errorHandler);
		HTTP.on(HiggsEvent.EXCEPTION_CAUGHT, errorHandler);
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
			else {
				//if the parameter is not a supported type, try to convert the message to that type
				Object obj = request.message.as(argTypes[i]);
				if (obj != null) {
					args[i] = obj;
				} else {
					log.warn(String.format("Unsupported parameter type found %s and the message could " +
							"not be converted to the type", argTypes[i].getName()));
				}
			}
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
				return new JsonEvent(methodName, mapper.createObjectNode().POJONode(returns));
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
					throw new WebSocketException("Unable to serialize data to be sent", e);
				}
			}

			public JsonEvent deserialize(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
				try {
					return mapper.readValue(msg.getText(), JsonEvent.class);
				} catch (IOException e) {
					//throw error so that it propagates and the error handler notifies the client
					throw new WebSocketException("Unable to de-serialize message", e);
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
