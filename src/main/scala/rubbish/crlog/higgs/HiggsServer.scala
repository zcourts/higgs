package rubbish.crlog.higgs

import protocol._
import reflect.BeanProperty
import io.netty.bootstrap.ServerBootstrap
import java.util.concurrent.Executors
import java.net.InetSocketAddress
import io.netty.channel._
import socket.nio.NioServerSocketChannelFactory


/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */

trait HiggsServer {
  var host = "localhost"
  var port = 2012
  /**
   * Channel which is bound to the specified local address which accepts incoming connections
   */
  var boundChannel: Option[Channel] = None
  // Configure the server.
  protected val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool))
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

  /**
   * Initialize the server bootstrap
   * @return true if initialized successfully, false otherwise.
   */
  protected def init() = {
    val pipeline = new PipelineFactory(higgsChannel, encoder, decoder);
    bootstrap.setPipelineFactory(pipeline)
  }

  /**
   * Binds to the default port or the port provided in the constructor.
   */
  def bind() {
    bind(host, port)
  }

  /**
   * Bind and start to accept incoming connections.
   * @param port the port to bind to
   */
  def bind(host: String, port: Int) {
    this.port = port
    this.init()
    try {
      boundChannel = Some(bootstrap.bind(new InetSocketAddress(host, port)))
    } catch {
      case e: Exception => throw new HiggsBindException("Unable to connect to port %s, message: %s".format(port, e.getMessage))
    }

  }

}
