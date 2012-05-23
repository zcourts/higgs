package rubbish.crlog.higgs

import io.netty.channel._
import protocol.HiggsChannelEvent
import collection.mutable.{ListBuffer, Map}

/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */

trait HiggsChannelCallback {
  protected val contextChannelStateEventCallbacks = Map.empty[String, ListBuffer[(ChannelHandlerContext, ChannelStateEvent) => Unit]]
  protected val contextChannelEventCallbacks = Map.empty[String, ListBuffer[(ChannelHandlerContext, ChannelEvent) => Unit]]
  protected val contextMessageEventCallbacks = Map.empty[String, ListBuffer[(ChannelHandlerContext, MessageEvent) => Unit]]
  protected val contextExceptionEventCallbacks = Map.empty[String, ListBuffer[(ChannelHandlerContext, ExceptionEvent) => Unit]]
  protected val contextWriteCompletionEventCallbacks = Map.empty[String, ListBuffer[(ChannelHandlerContext, WriteCompletionEvent) => Unit]]
  protected val contextChildChannelStateEventCallbacks = Map.empty[String, ListBuffer[(ChannelHandlerContext, ChildChannelStateEvent) => Unit]]

  def onChannelBound(fn: (ChannelHandlerContext, ChannelStateEvent) => Unit) {
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelBound, ListBuffer.empty) += fn
  }

  def onChannelClosed(fn: (ChannelHandlerContext, ChannelStateEvent) => Unit) {
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelClosed, ListBuffer.empty) += fn
  }

  def onConnected(fn: (ChannelHandlerContext, ChannelStateEvent) => Unit) {
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onConnected, ListBuffer.empty) += fn
  }

  def onDisconnected(fn: (ChannelHandlerContext, ChannelStateEvent) => Unit) {
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelDisconnected, ListBuffer.empty) += fn
  }

  def onInterestChanged(fn: (ChannelHandlerContext, ChannelStateEvent) => Unit) {
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelInterestChanged, ListBuffer.empty) += fn
  }

  def onOpen(fn: (ChannelHandlerContext, ChannelStateEvent) => Unit) {
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onOpen, ListBuffer.empty) += fn
  }

  def onUnbound(fn: (ChannelHandlerContext, ChannelStateEvent) => Unit) {
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelUnbound, ListBuffer.empty) += fn
  }

  def onClosed(fn: (ChannelHandlerContext, ChannelStateEvent) => Unit) {
    contextChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChannelClosed, ListBuffer.empty) += fn
  }

  def onChildOpen(fn: (ChannelHandlerContext, ChildChannelStateEvent) => Unit) {
    contextChildChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChildChannelOpen, ListBuffer.empty) += fn
  }

  def onChildClosed(fn: (ChannelHandlerContext, ChildChannelStateEvent) => Unit) {
    contextChildChannelStateEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onChildChannelClosed, ListBuffer.empty) += fn
  }

  def onHandleUpstream(fn: (ChannelHandlerContext, ChannelEvent) => Unit) {
    contextChannelEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onHandleUpstream, ListBuffer.empty) += fn
  }

  def onExceptionCaught(fn: (ChannelHandlerContext, ExceptionEvent) => Unit) {
    contextExceptionEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onExceptionCaught, ListBuffer.empty) += fn
  }

  def onMessageReceived(fn: (ChannelHandlerContext, MessageEvent) => Unit) {
    contextMessageEventCallbacks.getOrElseUpdate(HiggsChannelEvent.onMessageReceived, ListBuffer.empty) += fn
  }

}
