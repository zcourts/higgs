package com.fillta.higgs.boson.demo;

import com.fillta.higgs.HiggsClientRequest;
import com.fillta.higgs.boson.BosonClient;
import com.fillta.higgs.boson.BosonMessage;
import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.util.Function1;
import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DemoClient {
    public static void main(String... args) {
        final BosonMessage msg = new BosonMessage();
        msg.arguments = new Object[]{"courtney"};
        msg.method = "name";
        msg.callback = "abc";
        BosonClient client = new BosonClient();
//        client.setQueueingStrategyAsBlockingQueue();
        client.on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
            public void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex) {
                ex.get().printStackTrace();
            }
        });

        client.connect("BosonDemo", "localhost", 8080, new Function1<HiggsClientRequest<String, BosonMessage, BosonMessage, ByteBuf>>() {
            @Override
            public void call(HiggsClientRequest<String, BosonMessage, BosonMessage, ByteBuf> a) {
                for (int i = 0; i < 1; i++) {
                    msg.arguments[0] = i;
                    a.send(msg);
                }
                System.out.println("Done sending");
            }
        });
    }
}
