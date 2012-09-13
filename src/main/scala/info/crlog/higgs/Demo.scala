package info.crlog.higgs

import agents.http.{HttpClient, HTTPEventListener}
import java.net.URL
import io.netty.channel.Channel

/**
 * Courtney Robinson <courtney@crlog.info>
 */


object Demo {

  def main(args: Array[String]) {
    val client = new HttpClient()
    val token = "AAAC9iVp3fpoBAGuVHs63PfduHzKrZAMC88CavXOjTGKXFfIDZB76hXVWLlu48IZBZAVZAkELNdNQARBTv4w3hRs2sswWX5AV6maiCgzVC8QZDZD"
    var url = "https://graph.facebook.com/me/feed?access_token=" + token
        client.GET(new URL(url), new HTTPEventListener {
          def onMessage(channel: Channel, msg: String) {
            println(msg)
          }
        })
//
//    //    url = "http://httpbin.org/get?q=test"
//    client.DELETE(new URL("http://httpbin.org/delete"), new HTTPEventListener {
//      def onMessage(channel: Channel, msg: String) {
//        println(msg)
//      }
//    })
//    client.GET(new URL("http://httpbin.org/get"), new HTTPEventListener {
//      def onMessage(channel: Channel, msg: String) {
//        println(msg)
//      }
//    })
//    client.POST(new URL("http://httpbin.org/post"), new HTTPEventListener {
//      def onMessage(channel: Channel, msg: String) {
//        println(msg)
//      }
//    }, Map("data" -> "true"))
//
//    client.PUT(new URL("http://httpbin.org/put"), new HTTPEventListener {
//      def onMessage(channel: Channel, msg: String) {
//        println(msg)
//      }
//    }, Map("data" -> "true"))
  }
}
