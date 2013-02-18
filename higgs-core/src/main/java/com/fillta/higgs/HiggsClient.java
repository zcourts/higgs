package com.fillta.higgs;

import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.google.common.base.Optional;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;

/**
 */
public abstract class HiggsClient<T, OM, IM, SM> extends EventProcessor<T, OM, IM, SM> {
    protected final NioEventLoopGroup nioEventLoopGroup;
    protected long reconnectTimeout = 10000;
    protected boolean enableGZip;

    public HiggsClient() {
        nioEventLoopGroup = new NioEventLoopGroup(maxThreads);
    }

    /**
     * ASynchronously connect to the given host:port
     *
     * @param serviceName the name of the service being connected to. Useful for debugging when multiple
     *                    connects mail fail, a human readable name makes logs easier to read
     * @param host        the host/ip to connect to
     * @param port        the port on the host
     * @param reconnect   if true then should connection fail it will automatically be re-attempted
     * @param ssl         If true then SSL will be added t the pipeline.
     * @return a future which notifies when the connection succeeds or fails
     */
    public ConnectFuture<T, OM, IM, SM> connect(final String serviceName, final String host, final int port,
                                                final boolean reconnect, final boolean ssl) {
        return connect(serviceName, host, port, reconnect, ssl, null);
    }

    protected ConnectFuture<T, OM, IM, SM> connect(final String serviceName, final String host, final int port,
                                                   final boolean reconnect, final boolean ssl,
                                                   ConnectFuture<T, OM, IM, SM> connFuture) {
        if (connFuture != null &&
                (connFuture.getState() == ConnectFuture.State.CONNECTING
                        || connFuture.getState() == ConnectFuture.State.CONNECTED)) {
            return connFuture;
        }
        Bootstrap b = new Bootstrap();
        ChannelInitializer<SocketChannel> initializer = newInitializer(ssl, true);
        b.group(eventLoopGroup())
                .channel(channelClass())
                .handler(initializer)
                .remoteAddress(new InetSocketAddress(host, port));
        final ConnectFuture<T, OM, IM, SM> future = newConnectFuture(reconnect, connFuture);
        future.setState(ConnectFuture.State.CONNECTING);
        if (!future.isReconnectListenerAdded()) {
            addReconnectListener(serviceName, host, port, reconnect, ssl, future);
        }
        ChannelFuture f = b.connect();
        future.setFuture(f);
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) {
                if (channelFuture.isSuccess()) {
                    future.setState(ConnectFuture.State.CONNECTED);
                    log.debug(String.format("Connected to %s", serviceName));
                } else {
                    if (reconnect) {
                        try {
                            log.debug(String.format("Connecting to %s on %s:%s failed. Attempting to retry in" +
                                    " %s milliseconds", serviceName, host, port, reconnectTimeout),
                                    channelFuture.cause());
                            future.setState(ConnectFuture.State.DISCONNECTED);
                            Thread.sleep(reconnectTimeout);
                            connect(serviceName, host, port, reconnect, ssl, future);
                        } catch (InterruptedException e) {
                            //ignore
                        }
                    } else {
                        future.setState(ConnectFuture.State.DISCONNECTED);
                        log.debug(String.format("Connecting to %s on %s:%s failed. Auto-reconnect is disabled," +
                                " your must manually re-connect", serviceName, host, port), channelFuture.cause());
                    }
                }
            }
        });
        return future;
    }

    protected void addReconnectListener(final String serviceName, final String host, final int port,
                                        final boolean reconnect, final boolean ssl,
                                        final ConnectFuture future) {
        if (!future.isReconnectListenerAdded()) {
            future.setReconnectListenerAdded(true);
            onException(new ChannelEventListener() {
                protected void sleep(long time) {
                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException ignored) {
                        //ignore
                    }
                }

                public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
                    HiggsClient.this.threadPool().submit(new Runnable() {
                        public void run() {
                            if (ex.get() instanceof ConnectException) {
                                log.warn(String.format("Failed to connect to %s on %s:%s, attempting to retry",
                                        serviceName, host, port));
                                future.setState(ConnectFuture.State.DISCONNECTED);
                                sleep(reconnectTimeout);
                                connect(serviceName, host, port, reconnect, ssl, future);
                            } else {
                                if (ex.get() instanceof ClosedChannelException) {
                                    log.warn(String.format("Client connection to %s on %s:%s socket closed",
                                            serviceName, host, port));
                                    future.setState(ConnectFuture.State.DISCONNECTED);
                                    sleep(reconnectTimeout);
                                    connect(serviceName, host, port, reconnect, ssl, future);
                                } else {
                                    if (ex.get() instanceof IOException) {
                                        log.warn(String.format("Connection to %s on %s:%s, was forcibly closed, the" +
                                                " server may be unavailable, attempting to reconnect", serviceName,
                                                host, port));
                                        future.setState(ConnectFuture.State.DISCONNECTED);
                                        sleep(reconnectTimeout);
                                        connect(serviceName, host, port, reconnect, ssl, future);
                                    } else {
                                        if (ex.get() instanceof ChannelException) {
                                            //todo
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * Creates a new connect future from the given parameters.
     *
     * @param reconnect  if true then the connect request should be re-attempted on failure until cancelled
     * @param connFuture if a reconnect is being attempted this represents the existing connect future being passed
     *                   between connections. This will have the current state and should returned if this
     *                   param is not null
     * @return a new connect future if the connFuture param is null otherwise return the connFuture provided
     */
    protected ConnectFuture<T, OM, IM, SM> newConnectFuture(boolean reconnect,
                                                            ConnectFuture<T, OM, IM, SM> connFuture) {
        return connFuture == null ? new ConnectFuture<>(this, reconnect) : connFuture;
    }

    protected void beforeSetupPipeline(ChannelPipeline pipeline) {
        if (enableGZip) {
            pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
            pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setEnableGZip(boolean enableGZip) {
        this.enableGZip = enableGZip;
    }

    /**
     * @return The event loop group to be used for connections.
     *         Ideally only once instance should be returned every time since each instance creates their own
     *         thread pools. However, allowing this to be overridden makes it possible to use other event loops
     *         such as UDP etc...
     */
    public EventLoopGroup eventLoopGroup() {
        return nioEventLoopGroup;
    }

    public Class<? extends Channel> channelClass() {
        return NioSocketChannel.class;
    }
}
