package io.higgs.core

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
object EventLoopGroups {
  lazy val nio: EventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime.availableProcessors() * 2)
}
