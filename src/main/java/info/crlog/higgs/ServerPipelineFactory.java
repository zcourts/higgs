package info.crlog.higgs;

import info.crlog.higgs.messaging.HiggsDecoder;
import info.crlog.higgs.messaging.HiggsEncoder;
import info.crlog.higgs.ssl.SslContextFactory;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPipelineFactory;
import static io.netty.channel.Channels.pipeline;
import io.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.frame.Delimiters;
import io.netty.handler.ssl.SslHandler;
import javax.net.ssl.SSLEngine;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class ServerPipelineFactory implements
        ChannelPipelineFactory {

    protected ServerHandler handler;
    protected boolean useSSL;
    protected HiggsDecoder decoder;
    protected HiggsEncoder encoder;

    public ServerPipelineFactory(ServerHandler handler,
            HiggsEncoder encoder,
            HiggsDecoder decoder,
            boolean usessl) {
        this.handler = handler;
        this.encoder = encoder;
        this.decoder = decoder;
        useSSL = usessl;
    }

    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();
        if (useSSL) {
            // Add SSL handler first to encrypt and decrypt everything.
            // In this example, we use a bogus certificate in the server side
            // and accept any invalid certificates in the client side.
            // You will need something more complicated to identify both
            // and server in the real world.
            //
            // Read securechat.SslContextFactory
            // if you need client certificate authentication.

            SSLEngine engine =
                    SslContextFactory.getServerContext().createSSLEngine();
            engine.setUseClientMode(false);

            pipeline.addLast("ssl", new SslHandler(engine));
            // On top of the SSL handler, add the text line codec.
            pipeline.addLast("framer", new DelimiterBasedFrameDecoder(
                    8192, Delimiters.lineDelimiter()));
        }
        pipeline.addLast("decoder", decoder);
        pipeline.addLast("encoder", encoder);

        // and then business logic.
        pipeline.addLast("handler", handler);

        return pipeline;
    }
}
