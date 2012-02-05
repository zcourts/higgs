package info.crlog.higgs

//use _root_. to make package resolution absolute, otherwise scala prepends info.crlog.higgs

import _root_.java.util.concurrent.Executors
import _root_.java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import protocol._
import boson.Publisher

/**
 * A simple client interface to encapsulate the Netty NIO connection for a client request
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 * @param host the host name to connect to
 * @param port the port on the host
 * @param decoder the class type of the protocol's decoder which must inherit from HiggsDecoder, see http://stackoverflow.com/a/4870084/400048 for   concise explanation of Scala's upper bound syntax
 * @param encoder the class type of the protocol's encoder
 * @param clientHandler the client handler which implements the "business" logic for the protocol
 */

class HiggsClient(host: String, port: Int,
                  decoder: Class[_ <: HiggsDecoder],
                  encoder: Class[_ <: HiggsEncoder],
                  clientHandler: Class[_ <: HiggsPublisher]
                   ) {

  val channelFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
  // Configure the client.
  val bootstrap = new ClientBootstrap(channelFactory);

  // Set up the event pipeline factory.
  bootstrap.setPipelineFactory(new ClientPipelineFactory(decoder, encoder, clientHandler));

  // Make a new connection.
  val connectFuture = bootstrap.connect(new InetSocketAddress(host, port));

  // Wait until the connection is made successfully.
  val channel = connectFuture.awaitUninterruptibly().getChannel();

  //Get the handler instance
  val handler = channel.getPipeline.getLast.asInstanceOf[HiggsPublisher]
  //  /**
  //   * Adds a message listener that will be notified when messages are received
  //   */
  //  def addListener(listener: MessageListener) = {
  //    handler.addListener(listener)
  //  }

  def shutdown() = {
    // Shut down all thread pools to exit.
    bootstrap.releaseExternalResources();
  }
}