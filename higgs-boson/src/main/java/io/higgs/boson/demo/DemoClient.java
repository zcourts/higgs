package io.higgs.boson.demo;

import com.google.common.base.Optional;
import io.higgs.boson.BosonClient;
import io.higgs.boson.BosonConnectFuture;
import io.higgs.events.listeners.ChannelEventListener;
import io.higgs.functional.Function1;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DemoClient {
    protected DemoClient() {
    }

    public static void main(String... args) {
        BosonClient client = new BosonClient();
        client.onException(new ChannelEventListener() {
            public void triggered(ChannelHandlerContext ctx, Optional<Throwable> ex) {
                ex.get().printStackTrace();
            }
        });

        BosonConnectFuture future = client.connect("BosonDemo", "localhost", 8080, true);
        for (int i = 0; i < 500000; i++) {
            //even if we're not connected the client will buffer messages until we are, then send them
            future.invoke("polo", new Function1<PoloExample>() {
                public void apply(PoloExample a) {
                    System.out.println(a.i);
                }
            }, new PoloExample(i));
        }
        System.out.println("Done sending");
    }
}
