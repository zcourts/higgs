package com.fillta.higgs.boson.demo;

import com.fillta.higgs.boson.BosonServer;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.google.common.base.Optional;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DemoServer {
    protected DemoServer() {
    }

    public static void main(String... args) {
        BosonServer server = new BosonServer(8080);
        server.onException(new ChannelEventListener() {
            public void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex) {
                ex.get().printStackTrace();
            }
        });
        server.registerPackage(Listener.class.getPackage().getName());
        server.bind();
    }
}
