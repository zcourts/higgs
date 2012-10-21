package info.crlog.higgs.protocols.http

import java.net.URL

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object HttpDemo {
  def main(args: Array[String]) {
    val client = new HttpClient()
    //    val req = new HttpRequest(new URL("http://crlog.info"), HttpMethod.GET)
    //    req.send((res: HTTPResponse) => {
    //      println(res)
    //    })
//    client.GET(new URL("https://graph.facebook.com/me/feed?access_token=asw"), (res: HTTPResponse) => {
//      println("Facebook")
//      println(res)
//    })
    client.GET(new URL("https://api.twitter.com/1.1/statuses/home_timeline.json"), (res: HTTPResponse) => {
      println("Twitter")
      println(res)
    })
//    println("xyx")
//    client.POST(new URL("http://httpbin.org/post?a=123&b=4"), (res: HTTPResponse) => {
//      println(res)
//    }, Map("a" -> 124, "123" -> "10656755") )
    //    TODO
    //    val ssl = new HttpRequest(new URL("https://graph.facebook.com/me/feed?access_token=AAAC9iVp3fpoBAGuVHs63PfduHzKrZAMC88CavXOjTGKXFfIDZB76hXVWLlu48IZBZAVZAkELNdNQARBTv4w3hRs2sswWX5AV6maiCgzVC8QZDZD"), HttpMethod.GET)
    //    ssl.send((res: HTTPResponse) => {
    //      println(res)
    //    })
  }
}
