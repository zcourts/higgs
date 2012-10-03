package info.crlog.higgs.agents.omsg

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object ClientDemo {
  def main(arg: Array[String]) {
    val client = new OMsgClient("localhost", 9099)
    client.connect(() => {
      println("client conntected")
      client.listen(classOf[NullPointerException], (msg: NullPointerException) => {
        println(msg.getMessage)
      })
      client.send(new IllegalStateException("Test illegal state exception"))
    })

  }
}
