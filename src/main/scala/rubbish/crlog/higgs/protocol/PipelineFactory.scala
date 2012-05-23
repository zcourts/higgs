package rubbish.crlog.higgs.protocol

import javax.net.ssl.SSLEngine
import io.netty.handler.ssl.SslHandler
import securechat.SecureChatSslContextFactory
import rubbish.crlog.higgs.HiggsChannel
import io.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory}

/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */
/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
class PipelineFactory(handler: HiggsChannel,
                      encoder: HiggsEncoder,
                      decoder: HiggsDecoder) extends ChannelPipelineFactory {
  def getPipeline: ChannelPipeline = {
    val pipeline: ChannelPipeline = Channels.pipeline()
    // Add SSL handler first to encrypt and decrypt everything.
    // In this example, we use a bogus certificate in the server side
    // and accept any invalid certificates in the client side.
    // You will need something more complicated to identify both
    // and server in the real world.
    //
    // Read securechat.SecureChatSslContextFactory
    // if you need client certificate authentication.
    val engine: SSLEngine = SecureChatSslContextFactory.getServerContext.createSSLEngine
    engine.setUseClientMode(false)
    pipeline.addLast("ssl", new SslHandler(engine))
    //    pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter))
    pipeline.addLast("decoder", decoder)
    pipeline.addLast("encoder", encoder)
    pipeline.addLast("handler", handler)
    return pipeline
  }
}


