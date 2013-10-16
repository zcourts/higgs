package io.higgs.ws.client;

import io.higgs.events.Event;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketEvent implements Event {
    public static final String MESSAGE_STR = "ws-message", ERROR_STR = "ws-error", DISCONNECT_STR = "ws-disconnect",
            CONNECT_STR = "ws-connect", PONG_STR = "ws-pong", PING_STR = "ws-ping";
    public static final WebSocketEvent
            PING = new WebSocketEvent(PING_STR),
            PONG = new WebSocketEvent(PONG_STR),
            CONNECT = new WebSocketEvent(CONNECT_STR),
            DISCONNECT = new WebSocketEvent(DISCONNECT_STR),
            ERROR = new WebSocketEvent(ERROR_STR),
            MESSAGE = new WebSocketEvent(MESSAGE_STR);
    protected String name;

    public WebSocketEvent(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}
