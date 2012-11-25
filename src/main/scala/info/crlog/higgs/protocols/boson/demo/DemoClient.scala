package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.protocols.boson.BosonClient


/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object DemoClient {
  def main(args: Array[String]) {
    //    val obj = new PoloExample()
    //    for (field <- classOf[PoloExample].getDeclaredFields()) {
    //      field.setAccessible(true)
    //      println(field.getName(), field.get(obj))
    //    }
    val client = new BosonClient("BosonTest", 12001, "localhost")
    client.connect()
    for (i <- 1 to 1) {
      client.invoke("all",
        Array(1.2F, 140, null, Map("a" -> 120), Array(1, 2, 356), false, "a string",
          List("Some List"), new PoloExample(100)),
        (m: Array[AnyRef]) => {
          println(m)
        }, false)
    }
  }
}
