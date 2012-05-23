package rubbish.crlog.higgs

import java.util.concurrent.Executors
import io.netty.channel._
import io.netty.bootstrap.ClientBootstrap
import protocol._
import socket.nio.NioClientSocketChannelFactory
import java.net.InetSocketAddress
import reflect.BeanProperty

/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */

trait HiggsClient {
  var host = ""
  var port = 2012
  /**
   * Channel which is bound to the specified local address which accepts incoming connections
   */
  var boundChannel: Option[ChannelFuture] = None
  //list of channels this client is connected to F
  protected val connectedChannels = scala.collection.mutable.Map.empty[Int, Channel]
  // Configure the client.
  protected val bootstrap: ClientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool))
  var version = Version.V1
  /**
   *
   */
  @BeanProperty
  var encoder = new HiggsEncoder()
  @BeanProperty
  var decoder = new HiggsDecoder()
  @BeanProperty
  var higgsChannel = new HiggsChannel()

  def connect() {
    this.init()
    //  throw new HiggsConnectException("Unable to connect to host: %s:%s".format(host, port))
    try {
      // Start the connection attempt.
      boundChannel = Some(bootstrap.connect(new InetSocketAddress(host, port)))
    } catch {
      case e: Exception => throw new HiggsConnectException("Unable to connect to host: %s:%s, message: %s".format(host, port, e.getMessage))
    }

  }

  /**
   * Initialize the server bootstrap
   * @return true if initialized successfully, false otherwise.
   */
  protected def init() = {
    val pipeline = new PipelineFactory(higgsChannel, encoder, decoder);
    bootstrap.setPipelineFactory(pipeline)
  }

}
