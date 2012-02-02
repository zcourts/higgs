package info.crlog.higgs

//use _root_. to make package resolution absolute, otherwise scala prepends info.crlog.higgs

import _root_.java.util.concurrent.Executors
import _root_.java.net.InetSocketAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.bootstrap.ClientBootstrap
import protocol.boson.ClientHandler
import protocol.{MessageListener, ClientPipelineFactory}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class HiggsClient(host: String, port: Int) {
  // Configure the client.
  val bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

  // Set up the event pipeline factory.
  bootstrap.setPipelineFactory(new ClientPipelineFactory);

  // Make a new connection.
  val connectFuture = bootstrap.connect(new InetSocketAddress(host, port));

  // Wait until the connection is made successfully.
  val channel = connectFuture.awaitUninterruptibly().getChannel();
  // Get the handler instance
  val handler = channel.getPipeline.getLast.asInstanceOf[ClientHandler]
  /**
   * Adds a message listener that will be notified when messages are received
   */
  def addListener(listener: MessageListener) = {
    handler.addListener(listener)
  }

  def shutdown() = {
    // Shut down all thread pools to exit.
    bootstrap.releaseExternalResources();
  }
}