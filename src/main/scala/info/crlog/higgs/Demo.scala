package info.crlog.higgs

import agents.http.{HttpClient, HTTPEventListener}
import java.net.URL
import io.netty.channel.{Channel, ChannelHandlerContext}
import scala.collection.mutable.Map

/**
 * Courtney Robinson <courtney@crlog.info>
 */

object Demo {
  def main(args: Array[String]) {
    val token = "AAAC9iVp3fpoBAGuVHs63PfduHzKrZAMC88CavXOjTGKXFfIDZB76hXVWLlu48IZBZAVZAkELNdNQARBTv4w3hRs2sswWX5AV6maiCgzVC8QZDZD"
    var url = "https://graph.facebook.com/me/feed?access_token=" + token
    //    url = "http://httpbin.org/get?q=test"
    val client = new HttpClient()
    client.DELETE(new URL("http://httpbin.org/delete"), new HTTPEventListener {
      def onEvent(event: FutureResponse.Event, ctx: ChannelHandlerContext, ex: Option[Throwable]) {
      }

      def onMessage(channel: Channel, msg: String) {
        println(msg)
      }
    })
    client.GET(new URL("http://httpbin.org/get"), new HTTPEventListener {
      def onEvent(event: FutureResponse.Event, ctx: ChannelHandlerContext, ex: Option[Throwable]) {
      }

      def onMessage(channel: Channel, msg: String) {
        println(msg)
      }
    })
    client.POST(new URL("http://httpbin.org/post"), new HTTPEventListener {
      def onEvent(event: FutureResponse.Event, ctx: ChannelHandlerContext, ex: Option[Throwable]) {
      }

      def onMessage(channel: Channel, msg: String) {
        println(msg)
      }
    }, Map("data" -> "true"))

    client.PUT(new URL("http://httpbin.org/put"), new HTTPEventListener {
      def onEvent(event: FutureResponse.Event, ctx: ChannelHandlerContext, ex: Option[Throwable]) {
      }

      def onMessage(channel: Channel, msg: String) {
        println(msg)
      }
    }, Map("data" -> "true"))
  }
}
