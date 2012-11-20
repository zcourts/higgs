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
      client.invoke("test", Array(math.random * i, new PoloExample(100)), (m: PoloExample) => {
        println(m.i)
      }, false)
    }
  }
}
