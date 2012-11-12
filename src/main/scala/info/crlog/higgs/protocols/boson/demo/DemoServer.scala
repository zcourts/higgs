package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.protocols.boson.BosonServer


/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object DemoServer {
  def main(args: Array[String]) {
    val server = new BosonServer(12001,"192.168.0.4")
    server.register(new Listener)
    server.bind(() => {})
  }
}
