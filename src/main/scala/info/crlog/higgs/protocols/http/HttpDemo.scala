package info.crlog.higgs.protocols.http

import java.net.URL
import java.nio.file.Files

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object HttpDemo {
  def main(args: Array[String]) {
    val file = Files.createTempFile("higgs.test", ".tmp").toFile()
    val client = new HttpRequestBuilder()
    //    for (x <- 1 to 100) {
    client.query("a", "b")
      //        .query("c", x)
      .cookie("c", "d")
      .header("X-val", "yes")
      .compress(true)
      .url(new URL("https://httpbin.org/delete"))
      .DELETE()
      .build((r) => {
      println(r)
    })
      .url(new URL("https://httpbin.org/get"))
      .GET()
      .build((r) => {
      println(r)
    })
      .url(new URL("https://httpbin.org/post"))
      .POST()
      .file(new HttpFile("post.txt", file))
      .form("name", "Courtney")
      .build((r) => {
      println(r)
    })
      .url(new URL("https://httpbin.org/put"))
      .PUT()
      .form("name", "Courtney Robinson")
      .build((r) => {
      println(r)
    })
    //    }
  }
}
