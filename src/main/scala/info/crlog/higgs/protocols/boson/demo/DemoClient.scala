package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.protocols.boson.BosonClient
import java.lang.Double

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object DemoClient {
  def main(args: Array[String]) {
    val client = new BosonClient("BosonTest", 12001,"192.168.0.4")
    client.connect()
    for (i <- 1 to readInt()) {
      client.invoke("test", Array(math.random * i,new PoloExample(100)), (m: Double) => {
       // println(m)
      }, false)
    }
  }
}
