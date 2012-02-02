package info.crlog.higgs

import protocol.boson.{ServerHandler, ClientHandler, BosonEncoder, BosonDecoder}
import reflect.BeanProperty
import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.jboss.netty.channel.SimpleChannelUpstreamHandler

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class Higgs(var socketType: HiggsConstants.Value) {
  // Default protocol decoder
  @BeanProperty
  var decoder: FrameDecoder = new BosonDecoder
  //default protocol encoder
  @BeanProperty
  var encoder: OneToOneEncoder = new BosonEncoder
  //default client request handler
  @BeanProperty
  var clientHandler: SimpleChannelUpstreamHandler = new ClientHandler
  //default server request handler
  @BeanProperty
  var serverHandler: SimpleChannelUpstreamHandler = new ServerHandler
  socketType match {
    case HiggsConstants.SOCKET_CLIENT => {}
    case HiggsConstants.SOCKET_SERVER => {}
    case HiggsConstants.SOCKET_OTHER => {}
    case _ => {
      throw IllegalSocketTypeException("A Higgs instance can be of socket type " +
        "HiggsConstants.SOCKET_(CLIENT|SERVER|OTHER). If you need a custom type specify " +
        "socketType as being HiggsConstants.SOCKET_OTHER and set your custom encoder,decoder,client" +
        " handler and server handler")
    }
  }
}