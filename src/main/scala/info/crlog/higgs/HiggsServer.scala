package info.crlog.higgs

import io.netty.bootstrap.ClientBootstrap
import io.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import protocol.{HiggsDecoder, HiggsEncoder, HiggsChannel, HiggsPipelineFactory}
import reflect.BeanProperty
import io.netty.channel.{Channel, ChannelFuture}

/**
 * Simple SSL chat client modified from {@link TelnetClient}.
 * Courtney Robinson <courtney@crlog.info>
 */
trait HiggsServer {
  @BeanProperty
  var host: String = "localhost"
  @BeanProperty
  var port: Int = 2012
  private val bootstrap: ClientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool))
  @BeanProperty
  var handler = new HiggsChannel
  @BeanProperty
  var encoder = new HiggsEncoder()
  @BeanProperty
  var decoder = new HiggsDecoder()
  var channel: Option[Channel] = None

  def connect() {
    bootstrap.setPipelineFactory(new HiggsPipelineFactory(handler, encoder, decoder))
    val future: ChannelFuture = bootstrap.connect(new InetSocketAddress(host, port))
    // Wait until the connection attempt succeeds or fails.
    channel = Some(future.awaitUninterruptibly.getChannel)
    if (!future.isSuccess) {
      bootstrap.releaseExternalResources
      channel = None
      future.getCause.printStackTrace
    }
  }

  def send(msg: Message) {
    channel match {
      case None => throw new RuntimeException("Client is not connected to a remote host")
      case Some(client) => {
        client.write(msg)
      }
    }
  }
}