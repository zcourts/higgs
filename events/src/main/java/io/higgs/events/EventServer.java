package io.higgs.events;

import io.higgs.core.HiggsServer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalServerChannel;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class EventServer extends HiggsServer {
    protected final LocalAddress address;

    public EventServer(LocalAddress address) {
        this.address = address;
        bossGroup = new LocalEventLoopGroup();
    }

    /**
     * Start the server causing it to bind to the provided {@link #port}
     *
     * @throws UnsupportedOperationException if the server's already started
     */
    public void start() {
        if (channel != null) {
            throw new UnsupportedOperationException("Server already started");
        }
        try {
            bootstrap
                    .group(bossGroup)
                    .channel(LocalServerChannel.class)
                    .handler(new ChannelInitializer<LocalServerChannel>() {
                        public void initChannel(LocalServerChannel ch) throws Exception {
                            // ch.pipeline().addLast(new LoggingHandler(LogLevel.ERROR));
                        }
                    })
                    .childHandler(new ChannelInitializer<LocalChannel>() {
                        @Override
                        public void initChannel(LocalChannel ch) throws Exception {
                            ch.pipeline().addLast(
                                    // new LoggingHandler(LogLevel.ERROR),
                                    new EventHandler(methods));
                        }
                    });

            // Bind and start to accept incoming connections.
            channel = bootstrap.bind(address).sync().channel();
        } catch (Throwable t) {
            log.warn("Error starting server", t);
        }
    }

    public <A> void registerMethod(FunctionEventMethod<A> method) {
        methods.add(method);
    }
}
