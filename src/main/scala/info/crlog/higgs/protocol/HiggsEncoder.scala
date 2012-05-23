package info.crlog.higgs.protocol

import boson.BosonMessage
import io.netty.handler.codec.oneone.OneToOneEncoder
import io.netty.channel.{Channel, ChannelHandlerContext}
import rubbish.crlog.higgs.Message

/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */

class HiggsEncoder extends OneToOneEncoder {
  def encode(ctx: ChannelHandlerContext, channel: Channel, msg: Any): AnyRef = {
    // Convert to a BosonMessage
    var message: Message = null
    if (msg.isInstanceOf[Message]) {
      message = msg.asInstanceOf[Message]
    }
    else {
      message = new BosonMessage(msg)
    }
    message.serialize()
  }
}
