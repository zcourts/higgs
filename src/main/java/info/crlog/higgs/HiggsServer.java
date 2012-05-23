package info.crlog.higgs;

import info.crlog.higgs.messaging.HiggsDecoder;
import info.crlog.higgs.messaging.HiggsEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.nio.NioServerSocketChannelFactory;
import io.netty.example.telnet.TelnetServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Simple SSL chat server modified from {@link TelnetServer}.
 */
public abstract class HiggsServer {

    protected int port = 2012;
    protected String host = "localhost";
    protected ServerBootstrap bootstrap;
    protected ServerPipelineFactory pipeline;
    protected Channel channel;
    protected ServerHandler handler;
    protected HiggsDecoder decoder;
    protected HiggsEncoder encoder;
    protected boolean useSSL;
    protected ChannelGroup channels = new DefaultChannelGroup();

    public HiggsServer(String host, int port) {
        this.port = port;
        this.host = host;
        handler = new ServerHandler(channels, useSSL);
        initialize();
    }

    /**
     * Release external resources
     */
    public void shutdown() {
        bootstrap.releaseExternalResources();
    }

    protected void bind() {
        if (decoder == null || encoder == null) {
            throw new EncoderDecoderInitializationException();
        }
        if (channel == null || !channel.isBound()) {
            // Configure the server.
            bootstrap = new ServerBootstrap(
                    new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool()));
            pipeline = new ServerPipelineFactory(handler, encoder, decoder, useSSL);
            // Configure the pipeline factory.
            bootstrap.setPipelineFactory(pipeline);
            // Bind and start to accept incoming connections.
            channel = bootstrap.bind(new InetSocketAddress(host, port));
        }
    }

    /**
     * Perform internal initialization. Automatically invoked by the parent
     * HiggsServer on construction. Use to initialize the encoder,decoder for
     * e.g.
     */
    protected abstract void initialize();
}
