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
      .cookies(Map("age" -> 100)) //or we can do
      .header("X-val", "yes")
      .headers(Map("X-a" -> 123, "X-b" -> "val"))
      .compress(true)
      .url(new URL("https://httpbin.org/delete"))
      .DELETE() //http DELETE request
      //build request and send
      .build((r) => {
      println(r) //print response
    })
      .url(new URL("https://httpbin.org/get"))
      .GET()
      .build((r) => {
      println(r)
    })
      .url(new URL("https://httpbin.org/post"))
      .POST()
      //upload a single file
      .file(new HttpFile("post.txt", file))
      //upload multiple files under the same name
      .file("my-var", List(new PartialHttpFile(file), new PartialHttpFile(file)))
      //or upload multiple files each with different names
      .file(List(new HttpFile("file-1", file), new HttpFile("file-2", file)))
      //use form to supply normal form field data i.e. none binary form fields
      .form("name", "Courtney")
      .build((r) => {
      println(r)
    })
    //TODO add PUT support
//      .url(new URL("https://httpbin.org/put"))
//      .PUT()
//      .form("name", "Courtney Robinson")
//      .build((r) => {
//      println(r)
//    })
    //notice all previous settings on the builder is kept and goes into the next request
    //if you add files for e.g. and do a POST request then do a GET only settings supported by
    //an HTTP GET request is used. to discard all previous settings use .clear() e.g.
    .clear() //now everything set previously has been discarded and a clean/new builder is returned
    .GET() //etc...
    //    }
  }
}
