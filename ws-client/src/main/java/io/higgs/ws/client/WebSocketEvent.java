package io.higgs.ws.client;

import io.higgs.events.Event;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketEvent implements Event {
    public static final WebSocketEvent
            PING = new WebSocketEvent("ws-ping"),
            PONG = new WebSocketEvent("ws-pong"),
            CONNECT = new WebSocketEvent("ws-connect"),
            DISCONNECT = new WebSocketEvent("ws-disconnect"),
            ERROR = new WebSocketEvent("ws-error"),
            MESSAGE = new WebSocketEvent("ws-message");
    protected String name;

    public WebSocketEvent(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }
}
