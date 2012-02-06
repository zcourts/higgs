package info.crlog.higgs.protocol

import org.jboss.netty.handler.codec.compression.{ZlibDecoder, ZlibWrapper, ZlibEncoder}
import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class SubscriberPipelineFactory(
                                 decoder: Class[_ <: HiggsDecoder],
                                 encoder: Class[_ <: HiggsEncoder],
                                 serverHandler: Class[_ <: HiggsSubscriber],
                                 listener: MessageListener
                                 ) extends ChannelPipelineFactory {
  override def getPipeline: ChannelPipeline = {
    val pipeline: ChannelPipeline = Channels.pipeline()
    pipeline.addLast("deflater", new ZlibEncoder(ZlibWrapper.GZIP))
    pipeline.addLast("inflater", new ZlibDecoder(ZlibWrapper.GZIP))
    pipeline.addLast("decoder", decoder.newInstance())
    pipeline.addLast("encoder", encoder.newInstance())
    //note we create a handler for every new channel  because it has stateful properties.
    //i.e. a message may be multi part so we may keep the start of a message on a channel
    //and later get the rest
    pipeline.addLast("handler", serverHandler.getConstructor(classOf[MessageListener]).newInstance(listener))
    //listener: MessageListener
    return pipeline
  }
}