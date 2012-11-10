package info.crlog.higgs.protocols.boson.json

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object DemoClient {
  def main(args: Array[String]) {
//    val mapper = new ObjectMapper()
//    mapper.registerModule(DefaultScalaModule)
//    println(mapper.
//      writeValueAsString(new Message("test", Some(Seq("d")), Some("callback"), 102)))



        val client = new BosonClient("localhost", 12001)
        client.connect()
        for (i <- 1 to 1) {
          client.invoke("test", Seq(i), (m: Array[String]) => {
            println("received:",m)
          },false)
    //      client.invoke("test", Seq(i), (m: Message) => {
    //        println("msg:",m)
    //      },false)
        }
  }
}
