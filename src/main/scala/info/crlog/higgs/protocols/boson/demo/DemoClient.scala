package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.protocols.boson.BosonClient


/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object DemoClient {
  def main(args: Array[String]) {
    val client = new BosonClient("BosonTest", 12001, "localhost")
    client.connect()
    for (i <- 1 to readInt()) {
      val polo = new PoloExample(100)
      polo.i = i
      polo.name = "name-" + i
      polo.nested.list = List("12345", "£", "£", "$")
      val nf = new NestedField
      nf.map = Map("£_$" -> "%")
      nf.a = (i * math.random).toInt
      nf.b = (i * math.random).toLong
      nf.c = i * math.random
      nf.d = (i * math.random).toFloat
      polo.nested.array = Array(nf)
      client.invoke("all",
        Array(1.2F, 140, null, Map("a" -> 120), Array(1, 2, 356), false, "a string",
          List("Some List"), polo),
        (m: Array[AnyRef]) => {
          for (i <- m) {
            println(i)
          }
        }, subscribe = false)
    }
  }
}
