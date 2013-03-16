package io.higgs.ws.demo;

import com.google.common.base.Optional;
import io.higgs.events.ChannelMessage;
import io.higgs.events.listeners.ChannelEventListener;
import io.higgs.functional.Function1;
import io.higgs.ws.JsonRequestEvent;
import io.higgs.ws.WebSocketServer;
import io.netty.channel.ChannelHandlerContext;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.thymeleaf.templateresolver.AbstractTemplateResolver;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class WebSocketServerDemo {
    static int count;

    protected WebSocketServerDemo() {
    }

    public static void main(String... args) {
        Logger.getLogger(AbstractTemplateResolver.class).setLevel(Level.DEBUG);
        WebSocketServer server = new WebSocketServer(3535);
        server.onException(new ChannelEventListener() {
            public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
                ex.get().printStackTrace();
            }
        });
        server.HTTP.getConfig().template_config.cacheable = false;
        server.HTTP.register(Api.class);
        server.listen("test", new Function1<ChannelMessage<JsonRequestEvent>>() {
            public void apply(final ChannelMessage<JsonRequestEvent> a) {
                System.out.println(++count + " : " + a.message);
            }
        });
        server.bind();
    }
}
