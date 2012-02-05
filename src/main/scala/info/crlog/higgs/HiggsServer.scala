package info.crlog.higgs

import org.jboss.netty.bootstrap.ServerBootstrap
import java.util.concurrent.Executors
import java.net.InetSocketAddress
import protocol._
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */
class HiggsServer(host: String, port: Int,
                  decoder: Class[_ <: HiggsDecoder],
                  encoder: Class[_ <: HiggsEncoder],
                  serverHandler: Class[_ <: HiggsSubscriber] ,
                  listener: MessageListener
                   ) {
  // Configure the server.
  val channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
  val bootstrap = new ServerBootstrap(channelFactory)
  val pipeline = new SubscriberPipelineFactory(decoder, encoder, serverHandler,listener)
  // Set up the event pipeline factory.
  bootstrap.setPipelineFactory(pipeline)

  // Bind and start to accept incoming connections.
  val channel = bootstrap.bind(new InetSocketAddress(host, port))

  val handler = pipeline.getPipeline.getLast.asInstanceOf[HiggsSubscriber]
  // val handler = channel.getPipeline.getLast.asInstanceOf[HiggsSubscriber]

  def shutdown() {
    bootstrap.releaseExternalResources()
  }
}