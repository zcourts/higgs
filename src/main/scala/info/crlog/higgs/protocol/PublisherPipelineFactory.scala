package info.crlog.higgs.protocol

import org.jboss.netty.channel.{ChannelPipeline, ChannelPipelineFactory,Channels}
import org.jboss.netty.handler.codec.compression.{ZlibDecoder, ZlibWrapper, ZlibEncoder}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class PublisherPipelineFactory(
                             decoder: Class[_ <: HiggsDecoder],
                             encoder: Class[_ <: HiggsEncoder],
                             clientHandler: Class[_ <: HiggsPublisher]
                             ) extends ChannelPipelineFactory {

  override def getPipeline: ChannelPipeline = {
    val pipeline: ChannelPipeline = Channels.pipeline()
    pipeline.addLast("deflater", new ZlibEncoder(ZlibWrapper.GZIP))
    pipeline.addLast("inflater", new ZlibDecoder(ZlibWrapper.GZIP))
    pipeline.addLast("decoder", decoder.newInstance())
    pipeline.addLast("encoder", encoder.newInstance())
    pipeline.addLast("handler", clientHandler.newInstance())
    return pipeline
  }
}