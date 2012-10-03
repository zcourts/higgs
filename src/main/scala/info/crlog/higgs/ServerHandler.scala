package info.crlog.higgs

import io.netty.channel.ChannelInboundMessageHandlerAdapter
import io.netty.channel.ChannelHandler.Sharable

@Sharable //only one instance of handler is required and will be shared across pipelines
case class ServerHandler[Topic, Msg, SerializedMsg](events: EventProcessor[Topic, Msg, SerializedMsg])
  extends ChannelInboundMessageHandlerAdapter[SerializedMsg]
  with HiggsHandler[Topic, Msg, SerializedMsg]