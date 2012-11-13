package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.protocols.boson.BosonClient
import java.lang.Double

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object DemoClient {
  def main(args: Array[String]) {
    val client = new BosonClient("BosonTest", 12001, "localhost")
    client.connect()
    for (i <- 1 to 1) {
      client.invoke("test", Array(math.random * i, new PoloExample(100)), (m: PoloExample) => {
        println(m.i)
      }, false)
    }
  }
}
