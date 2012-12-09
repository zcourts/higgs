package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.protocols.boson.BosonClient
import com.fillta.higgs.boson.demo.{NestedField, PoloExample}


/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object DemoClient {
  def main(args: Array[String]) {
    val client = new BosonClient("BosonTest", 8080, "localhost")
    client.connect()
    var count = 0
    val max = readInt()
    val polo = new PoloExample(100)
    for (i <- 1 to max) {
      client.invoke("polo",
        Array(polo),
        (m: PoloExample) => {
          println(m)
          count += 1
          // println(count)
        }, subscribe = false)
      //      client.invoke("all",
      //        Array(1.2F, 140, null, Map("a" -> 120), Array(1, 2, 356), false, "a string",
      //          List("Some List"), polo),
      //        (m: Array[AnyRef]) => {
      //          //          for (i <- m) {
      //          //            println(i)
      //          //          }
      //          count += 1
      //         // println(count)
      //        }, subscribe = false)
    }
  }
}
