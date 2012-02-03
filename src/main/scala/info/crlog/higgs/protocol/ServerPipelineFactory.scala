package info.crlog.higgs.protocol

import boson.{BosonDecoder, BosonEncoder, ServerHandler}
import org.jboss.netty.handler.codec.compression.{ZlibDecoder, ZlibWrapper, ZlibEncoder}
import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class ServerPipelineFactory extends ChannelPipelineFactory {
  def getPipeline: ChannelPipeline = {
    val pipeline: ChannelPipeline =  Channels.pipeline()
    pipeline.addLast("deflater", new ZlibEncoder(ZlibWrapper.GZIP))
    pipeline.addLast("inflater", new ZlibDecoder(ZlibWrapper.GZIP))
    pipeline.addLast("decoder", new BosonDecoder)
    pipeline.addLast("encoder", new BosonEncoder)
    // Please note we create a handler for every new channel  because it has stateful properties.
    //i.e. a message may be multi part so we may keep the start of a message on a channel
    //and later get the rest
    pipeline.addLast("handler", new ServerHandler)
    return pipeline
  }
}