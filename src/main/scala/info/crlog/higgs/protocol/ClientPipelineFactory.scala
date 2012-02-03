package info.crlog.higgs.protocol

import boson.{ClientHandler, BosonEncoder, BosonDecoder}
import org.jboss.netty.channel.{ChannelPipeline, ChannelPipelineFactory}
import org.jboss.netty.handler.codec.compression.{ZlibDecoder, ZlibWrapper, ZlibEncoder}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class ClientPipelineFactory(
                             decoder: Class[_ <: HiggsDecoder],
                             encoder: Class[_ <: HiggsEncoder],
                             clientHandler: Class[_ <: HiggsClientHandler]
                             ) extends ChannelPipelineFactory {

  override def getPipeline: ChannelPipeline = {
    lazy val pipeline: ChannelPipeline = pipeline
    pipeline.addLast("deflater", new ZlibEncoder(ZlibWrapper.GZIP))
    pipeline.addLast("inflater", new ZlibDecoder(ZlibWrapper.GZIP))
    pipeline.addLast("decoder", decoder.newInstance())
    pipeline.addLast("encoder", encoder.newInstance())
    pipeline.addLast("handler", clientHandler.newInstance())
    return pipeline
  }
}