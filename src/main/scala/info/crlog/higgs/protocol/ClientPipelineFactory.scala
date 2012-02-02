package info.crlog.higgs.protocol

import boson.{ClientHandler, BosonEncoder, BosonDecoder}
import org.jboss.netty.channel.{ChannelPipeline, ChannelPipelineFactory}
import org.jboss.netty.handler.codec.compression.{ZlibDecoder, ZlibWrapper, ZlibEncoder}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class ClientPipelineFactory extends ChannelPipelineFactory {

  override def getPipeline: ChannelPipeline = {
    lazy val pipeline: ChannelPipeline = pipeline
    pipeline.addLast("deflater", new ZlibEncoder(ZlibWrapper.GZIP))
    pipeline.addLast("inflater", new ZlibDecoder(ZlibWrapper.GZIP))
    pipeline.addLast("decoder", new BosonDecoder)
    pipeline.addLast("encoder", new BosonEncoder)
    pipeline.addLast("handler", new ClientHandler)
    return pipeline
  }
}