package info.crlog.higgs.protocols.jrpc.demo

import info.crlog.higgs.protocols.jrpc.JRPCServer


/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object Server {
  def main(args: Array[String]) {
    val server = new JRPCServer("localhost", 12000)
    server.registerPackage(getClass.getPackage.getName)
    server.bind(() => {})
  }
}
