package io.higgs.http.server.demo;

import com.google.common.base.Optional;
import io.higgs.events.listeners.ChannelEventListener;
import io.higgs.http.server.HttpServer;
import io.higgs.http.server.config.ServerConfig;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpServerDemo {
    protected HttpServerDemo() {
    }

    public static void main(String... args) throws IOException, InterruptedException {
        HttpServer server = new HttpServer(ServerConfig.class, args.length == 0 ? "./config.yml" : args[0]);
        server.onException(new ChannelEventListener() {
            public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
                if (ex.isPresent()) {
                    LoggerFactory.getLogger(getClass()).warn("Error", ex.get());
                }
            }
        });
        server.register(Api.class);
        server.bind();
    }
}
