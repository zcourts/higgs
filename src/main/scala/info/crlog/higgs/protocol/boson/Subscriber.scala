package info.crlog.higgs.protocol.boson

import org.jboss.netty.channel._
import collection.mutable.ListBuffer
import info.crlog.higgs.protocol.{Message, MessageListener, HiggsSubscriber}

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class Subscriber(listener: MessageListener) extends HiggsSubscriber(listener) {
  private val msgBuffer: Option[ListBuffer[BosonMessage]] = None
  private var buffering = false

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    var msg: Option[BosonMessage] = None
    if (e.getMessage.isInstanceOf[BosonMessage]) {
      msg = Some(e.getMessage.asInstanceOf[BosonMessage])
    } else {
      msg = Some(new BosonMessage(e.getMessage))
      println("Invalid message, MSG:" + e.getMessage)
    }
    msg match {
      case None =>  //hmmm, what to do here...?
      case Some(message) => {
        //check the flag
        message.flag match{
          case message.FLAGS.NO_MORE_CONTENT=>{
            publish(message)   //publish immediately
          }
        }
      }
    }
  }

  def publish(m: BosonMessage) {
    listener.onMessage(m)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    e.getChannel.close
  }
}