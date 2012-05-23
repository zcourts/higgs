package info.crlog.higgs;

import info.crlog.higgs.messaging.HiggsDecoder;
import info.crlog.higgs.messaging.HiggsEncoder;
import io.netty.bootstrap.ClientBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.nio.NioClientSocketChannelFactory;
import io.netty.example.telnet.TelnetClient;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Simple SSL chat client modified from {@link TelnetClient}.
 */
public abstract class HiggsClient {

    protected int port = 2012;
    protected String host = "localhost";
    protected ClientBootstrap bootstrap;
    protected ClientPipelineFactory pipeline;
    protected ClientHandler handler;
    protected HiggsDecoder decoder;
    protected HiggsEncoder encoder;
    protected boolean useSSL;
    protected Channel channel;
    protected ChannelFuture future;

    public HiggsClient(String host, int port) {
        this.host = host;
        this.port = port;
        handler = new ClientHandler(useSSL);
    }

    /**
     * Release external resources
     */
    public void shutdown() {
        bootstrap.releaseExternalResources();
    }

    /**
     * Connect to the host:port provided
     *
     * @return true IF AND ONLY IF the client is not already connected and the
     * connection attempt was successful. In all other cases false is returned
     * and the connection can immediately be retried
     */
    protected boolean connect() {
        if (decoder == null || encoder == null) {
            throw new EncoderDecoderInitializationException();
        }
        if (bootstrap == null || channel == null || !channel.isConnected()) // Configure the client.
        {
            bootstrap = new ClientBootstrap(
                    new NioClientSocketChannelFactory(
                    Executors.newCachedThreadPool()));
            // Configure the pipeline factory.
            bootstrap.setPipelineFactory(new ClientPipelineFactory(handler,
                    encoder,
                    decoder,
                    useSSL));
            // Start the connection attempt.
            future = bootstrap.connect(new InetSocketAddress(
                    host,
                    port));
            // Wait until the connection attempt succeeds or fails.
            channel = future.awaitUninterruptibly().getChannel();
            if (!future.isSuccess()) {
                bootstrap.releaseExternalResources();
                channel = null;
                bootstrap = null;
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Perform internal initialization. Automatically invoked by the parent
     * HiggsServer on construction. Use to initialize the encoder,decoder for
     * e.g.
     */
    protected abstract void initialize();
}
