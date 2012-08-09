package info.crlog.higgs.agents.transceiver

import info.crlog.higgs.Server
import io.netty.channel.{ChannelInboundMessageHandlerAdapter, ChannelPipeline}
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.{HttpResponseEncoder, HttpChunkAggregator, HttpRequestDecoder}

/**
 * Courtney Robinson <courtney@crlog.info>
 */

class TServer(host: String, port: Int) extends Server(host, port) {
  def setupPipeline(ch: SocketChannel): Boolean = {
    val pipeline: ChannelPipeline = ch.pipeline
    pipeline.addLast("decoder", new HttpRequestDecoder)
    pipeline.addLast("aggregator", new HttpChunkAggregator(65536))
    pipeline.addLast("encoder", new HttpResponseEncoder)
    true //not adding handler to the pipeline
  }

  override def getServerHandler(): ChannelInboundMessageHandlerAdapter[AnyRef] = new TServerHandler

}
