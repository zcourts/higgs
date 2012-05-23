package rubbish.crlog.higgs.agents

import io.netty.channel.{MessageEvent, ChannelHandlerContext}
import rubbish.crlog.higgs.protocol.boson.BosonMessage
import rubbish.crlog.higgs.{Message, HiggsClient}


/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */

class HiggsRadio extends HiggsClient {
  def this(host: String, port: Int) = {
    this()
    this.host = host
    this.port = port
  }

  def onMessage(fn: (Message) => Unit) {
    higgsChannel.onMessageReceived(
      (ctx: ChannelHandlerContext, e: MessageEvent) => {
        var msg: Option[BosonMessage] = None
        if (e.getMessage.isInstanceOf[BosonMessage]) {
          msg = Some(e.getMessage.asInstanceOf[BosonMessage])
        } else {
          msg = Some(new BosonMessage(e.getMessage))
          println("Invalid message, MSG:" + e.getMessage)
        }
        msg match {
          case None =>println("Messag is none?") //hmmm, what to do here...?
          case Some(message) => {
            fn(message)
          }
        }
      }
    )
  }

  /**
   * Connect to the given host and start listening
   */
  def listen() {
    connect()
  }
}
