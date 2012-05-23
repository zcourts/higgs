package rubbish.crlog.higgs

import collection.mutable.ListBuffer
import io.netty.channel._
import group.{ChannelGroup, DefaultChannelGroup}
import protocol.HiggsChannelEvent
import io.netty.handler.ssl.SslHandler

/**
 * A channel wrapper which allows us to hook into all the available netty channel events
 * Courtney Robinson <courtney@crlog.rubbish>
 */

class HiggsChannel extends SimpleChannelUpstreamHandler with HiggsChannelCallback {
  val channels: ChannelGroup = new DefaultChannelGroup

  /**
   * Invoked when a {@link Channel} is open, bound to a local address, and
   * connected to a remote address.
   */
  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    // Get the SslHandler in the current pipeline.
    // We added it in SecureChatPipelineFactory.
    val sslHandler: SslHandler = ctx.getPipeline.get(classOf[SslHandler])
    // Get notified when SSL handshake is done.
    val handshakeFuture: ChannelFuture = sslHandler.handshake
    handshakeFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        if (future.isSuccess) {
          //          future.getChannel.write("Welcome to " + InetAddress.getLocalHost.getHostName + " secure chat service!\n")
          future.getChannel.write("Your session is protected by " + sslHandler.getEngine.getSession.getCipherSuite + " cipher suite.\n")
          channels.add(future.getChannel)
        }
        else {
          future.getChannel.close
        }
      }
    })
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onConnected, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * {@inheritDoc}  Down-casts the received upstream event into more
   * meaningful sub-type event and calls an appropriate handler method with
   * the down-casted event.
   */
  override def handleUpstream(ctx: ChannelHandlerContext, e: ChannelEvent) {
    super.handleUpstream(ctx, e)
    contextChannelEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onHandleUpstream, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * Invoked when a message object (e.g: {@link ChannelBuffer}) was received
   * from a remote peer.
   */
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    println("Message received " + e.getMessage)
    //    if (channel == None) {
    //      channel = Some(ctx.getChannel())
    //    }
    contextMessageEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onMessageReceived, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * Invoked when an exception was raised by an I/O thread or a
   * {@link ChannelHandler}.
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    contextExceptionEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onExceptionCaught, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
    e.getCause.printStackTrace()
  }

  /**
   * Invoked when a {@link Channel} is open, but not bound nor connected.
   * <br/>
   * <strong>Be aware that this event is fired from within the Boss-Thread so you should not execute any heavy operation in there as it will block the dispatching to other workers!</strong>
   */
  override def channelOpen(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    //super.channelOpen(ctx, e)
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onOpen, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * Invoked when a {@link Channel} is open and bound to a local address,
   * but not connected.
   * <br/>
   * <strong>Be aware that this event is fired from within the Boss-Thread so you should not execute any heavy operation in there as it will block the dispatching to other workers!</strong>
   */
  override def channelBound(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    //super.channelBound(ctx, e)
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelBound, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })

  }

  /**
   * Invoked when a {@link Channel}'s {@link Channel#getInterestOps() interestOps}
   * was changed.
   */
  override def channelInterestChanged(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    //super.channelInterestChanged(ctx, e)
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelInterestChanged, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * Invoked when a {@link Channel} was disconnected from its remote peer.
   */
  override def channelDisconnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    // Unregister the channel from the global channel list
    // so the channel does not receive messages anymore.
    channels.remove(e.getChannel)
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelDisconnected, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * Invoked when a {@link Channel} was unbound from the current local address.
   */
  override def channelUnbound(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    //super.channelUnbound(ctx, e)
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelUnbound, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * Invoked when a {@link Channel} was closed and all its related resources
   * were released.
   */
  override def channelClosed(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
    //super.channelClosed(ctx, e)
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelClosed, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * Invoked when something was written into a {@link Channel}.
   */
  override def writeComplete(ctx: ChannelHandlerContext, e: WriteCompletionEvent) {
    //super.writeComplete(ctx, e)
    contextWriteCompletionEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onWriteComplete, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * Invoked when a child {@link Channel} was open.
   * (e.g. a server channel accepted a connection)
   */
  override def childChannelOpen(ctx: ChannelHandlerContext, e: ChildChannelStateEvent) {
    //super.childChannelOpen(ctx, e)
    contextChildChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChildChannelOpen, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }

  /**
   * Invoked when a child {@link Channel} was closed.
   * (e.g. the accepted connection was closed)
   */
  override def childChannelClosed(ctx: ChannelHandlerContext, e: ChildChannelStateEvent) {
    //super.childChannelClosed(ctx, e)
    contextChildChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChildChannelClosed, ListBuffer.empty) map ({
      fn => fn(ctx, e)
    })
  }
}
