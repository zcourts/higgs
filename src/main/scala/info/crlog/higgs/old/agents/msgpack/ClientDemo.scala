package info.crlog.higgs.agents.msgpack


/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object ClientDemo {
  def main(arg: Array[String]) {
    val client = new MsgpackClient("localhost", 9099)
    client.connect(() => {
      println("client conntected")
      client.listen(classOf[Me], (msg: Me) => {
        println(msg.name)
      })
      client.send(new Me("Test"))
    })

  }
}
