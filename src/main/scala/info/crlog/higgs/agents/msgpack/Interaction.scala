package info.crlog.higgs.agents.msgpack

import io.netty.channel.ChannelHandlerContext


/**
 * A default constructor is required (i.e. no params) hence
 * NOTE: All fields expected to be sent and decoded must be declared as var NOT val.
 * Courtney Robinson <courtney@crlog.info>
 */
class Interaction {
  var context: ChannelHandlerContext = null

  def respond[T <: Interaction](msg: T) = {
    context.channel.write(Packing.packBytes(msg))
    this
  }

}
