package com.fillta.higgs.boson.demo;

import com.fillta.higgs.HiggsClientRequest;
import com.fillta.higgs.boson.BosonClient;
import com.fillta.higgs.boson.BosonClientRequest;
import com.fillta.higgs.boson.BosonMessage;
import com.fillta.higgs.events.ChannelMessage;
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
        BosonClient client = new BosonClient();
        client.setQueueingStrategyAsBlockingQueue();
        client.on(HiggsEvent.EXCEPTION_CAUGHT, new ChannelEventListener() {
            public void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex) {
                ex.get().printStackTrace();
            }
        });

        client.connect("BosonDemo", "localhost", 8080, new Function1<BosonClientRequest>() {
            public void call(BosonClientRequest a) {
                for (int i = 0; i < 1000; i++) {
                    a.invoke("polo", new Function1<PoloExample>() {
                        public void call(PoloExample a) {
                            System.out.println(a);
                        }
                    }, new PoloExample());
                }
                System.out.println("Done sending");
            }
        });
    }
}
