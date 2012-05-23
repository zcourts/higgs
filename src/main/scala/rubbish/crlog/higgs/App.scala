package rubbish.crlog.higgs

import rubbish.crlog.higgs.agents.{HiggsBroadcaster, HiggsRadio}
import rubbish.crlog.higgs.Message
import rubbish.crlog.higgs.protocol.boson.BosonMessage
import io.netty.bootstrap.{ClientBootstrap, ServerBootstrap}
import io.netty.buffer.{ChannelBuffer, ChannelBuffers}
import io.netty.channel._
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import socket.nio.{NioClientSocketChannelFactory, NioServerSocketChannelFactory}

//class MyChannel(str: String) extends SimpleChannelUpstreamHandler {
//  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
//    println("From ->" + str)
//    if (e.getMessage.isInstanceOf[ChannelBuffer])
//      println(new String(e.getMessage.asInstanceOf[ChannelBuffer].array()))
//    else
//      println(e.getMessage)
//    ctx.getChannel.write(ChannelBuffers.wrappedBuffer("server replies".getBytes))
//  }
//}
object App {
  //  def main(args: Array[String]) {
  //    val server = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool))
  //    server.setPipelineFactory(new ChannelPipelineFactory {
  //      def getPipeline: ChannelPipeline = {
  //        return Channels.pipeline(new MyChannel("server handler"))
  //      }
  //    })
  //    val client: ClientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool))
  //    client.setPipelineFactory(new ChannelPipelineFactory {
  //      def getPipeline: ChannelPipeline = {
  //        return Channels.pipeline(new MyChannel("client handler"))
  //      }
  //    })
  //    val s = server.bind(new InetSocketAddress("192.168.0.6", 2022))
  //    val c = client.connect(new InetSocketAddress("192.168.0.6", 2022)).awaitUninterruptibly().getChannel
  //
  //    s.write(ChannelBuffers.wrappedBuffer("server".getBytes))
  //    c.write(ChannelBuffers.wrappedBuffer("client".getBytes))
  //  }

  def main(args: Array[String]) {
    val broadcaster = new HiggsBroadcaster(2022)
    broadcaster.host = "localhost";
    // "192.168.0.6"
    val radio = new HiggsRadio("localhost", 2022)

    broadcaster.higgsChannel.onExceptionCaught((ctx: ChannelHandlerContext, e: ExceptionEvent) => {
      throw e.getCause
    })
    radio.higgsChannel.onExceptionCaught((ctx: ChannelHandlerContext, e: ExceptionEvent) => {
      throw e.getCause
    })
    radio.onMessage((msg: Message) => {
      println("GOT:" + msg)
    })

    println("listening")
    broadcaster.bind()
    println("Connecting")
    radio.listen()

    Thread.sleep(3000)
    println("Sending msg")
    broadcaster.broadcast(new BosonMessage("Test"))
  }
}
