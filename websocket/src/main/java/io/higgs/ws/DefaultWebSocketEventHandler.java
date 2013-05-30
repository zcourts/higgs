package io.higgs.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.higgs.core.InvokableMethod;
import io.higgs.http.server.MessagePusher;
import io.higgs.http.server.MethodParam;
import io.higgs.http.server.WrappedResponse;
import io.higgs.http.server.protocol.HttpMethod;
import io.higgs.ws.protocol.WebSocketConfiguration;
import io.higgs.ws.protocol.WebSocketHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * An event handler which converts JSON events into method invocations
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DefaultWebSocketEventHandler implements WebSocketEventHandler {
    public static final ObjectMapper mapper = new ObjectMapper();
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void onMessage(TextWebSocketFrame frame, WebSocketHandler webSocketHandler, ChannelHandlerContext ctx,
                          Queue<InvokableMethod> methods, WebSocketConfiguration config) {
        String str = frame.text();
        try {
            JsonRequest request = mapper.readValue(str, JsonRequest.class);
            HttpMethod method = webSocketHandler.findMethod(request.getPath(), ctx, request, HttpMethod.class);
            if (method == null) {
                log.warn(String.format("No method found matching websocket event path, message:\n%s", str));
            } else {
                invoke(method, request, frame, webSocketHandler, ctx, config, methods);
            }
        } catch (IOException e) {
            log.warn(String.format("Unable to extract JsonRequest from web socket event msg:\n%s", str));
        }
    }

    protected void invoke(final HttpMethod method, final JsonRequest request, TextWebSocketFrame frame, WebSocketHandler
            handler, final ChannelHandlerContext ctx, WebSocketConfiguration config, Queue<InvokableMethod> methods) {
        Object[] params = new Object[method.getParams().length];
        MessagePusher pusher = new MessagePusher() {
            @Override
            public ChannelFuture push(Object message) {
                if (message != null) {
                    Object res;
                    if (message instanceof TextWebSocketFrame) {
                        res = ((TextWebSocketFrame) message).text();
                    } else {
                        res = message;
                    }
                    Map<String, Object> map = new HashMap<>();
                    String c = request.getCallback();
                    String cbk = res instanceof WrappedResponse ? ((WrappedResponse) res).callback() : null;
                    Object wrappedRes = res instanceof WrappedResponse ? ((WrappedResponse) res).data() : null;
                    if (cbk != null) {
                        c = cbk;
                    }
                    if (wrappedRes != null) {
                        res = wrappedRes;
                    }
                    map.put("callback", c != null && !c.isEmpty() ? c : method.rawPath());
                    map.put("data", res);
                    //
                    try {
                        return ctx.channel().write(new TextWebSocketFrame(mapper.writeValueAsString(map)));
                    } catch (JsonProcessingException e) {
                        log.warn(String.format("Message received but failed to convert object to JSON string"));
                        return ctx.newFailedFuture(e);
                    }
                }
                return ctx.newFailedFuture(new IllegalArgumentException("Tried to push a null message"));
            }

            @Override
            public ChannelHandlerContext ctx() {
                return ctx;
            }
        };
        injectParams(params, method, request, frame, handler, ctx, config, method, pusher);
        try {
            Object returns = method.invoke(ctx, request.getPath(), request, params);
            pusher.push(returns);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            log.warn(String.format("Crap! Unable to invoke method %s", method), e);
        }
    }

    /**
     * Try to inject {@link Channel}, {@link ChannelHandlerContext},{@link JsonRequest}, {@link WebSocketConfiguration}
     * and if the parameter type does not match any of these then an attempt is made to conver the {@link JsonRequest}
     * to the desired type using Jackson's {@link ObjectMapper}. If this fails then the parameter is set to null.
     */
    private void injectParams(Object[] params, HttpMethod method, JsonRequest request, TextWebSocketFrame frame,
                              WebSocketHandler handler, ChannelHandlerContext ctx, WebSocketConfiguration config,
                              HttpMethod method1, MessagePusher pusher) {
        //TODO migrate to the new Injector API
        MethodParam[] p = method.getParams();
        for (int i = 0; i < p.length; i++) {
            MethodParam param = p[i];
            Class<?> paramType = param.getParameterType();
            if (paramType.isAssignableFrom(request.getClass())) {
                params[i] = request;
            } else if (paramType.isAssignableFrom(ChannelHandlerContext.class)) {
                params[i] = ctx;
            } else if (paramType.isAssignableFrom(Channel.class)) {
                params[i] = ctx.channel();
            } else if (paramType.isAssignableFrom(WebSocketConfiguration.class)) {
                params[i] = config;
            } else if (paramType.isAssignableFrom(MessagePusher.class)) {
                params[i] = pusher;
            } else {
                try {
                    params[i] = request.as(paramType);
                } catch (Throwable t) {
                    params[i] = null;
                    log.warn(String.format("Unable to inject parameter, expecting \n %s from JSON \n %s",
                            paramType.getName(), request.getMessage()));
                }
            }
        }
    }
}
