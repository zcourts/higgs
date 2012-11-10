package info.crlog.higgs.protocols.boson.json

import info.crlog.higgs.method
import json.BosonServer

//class Listener {
//  @method("test")
//  def test(a: Int)= {
//    println(a)
//    Array("blast")
//    new Message("test", Some(Seq("d")), Some("callback"), 103454)
//  }
//}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object DemoServer {
  def main(args: Array[String]) {
    val server = new BosonServer("localhost", 12001)
    server.register(new Listener)
    server.bind(() => {})
  }
}
