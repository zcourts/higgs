package info.crlog.higgs.protocols.jrpc.demo

import info.crlog.higgs.protocols.jrpc.RPCClient

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object Client {
  def main(args: Array[String]) {
    val client = new RPCClient("RPC Server Demo", 12000)
    client.connect(() => {
      client.invoke("test_response", (o: Option[Any], e: Option[Throwable]) => {
        println(o)
      },1)
      //      client.invoke("info.crlog.higgs.protocols.jrpc.demo.TestServerListener.test",
      //        (r: Option[AnyRef], e: Option[Throwable]) => {
      //          println(r, e)
      //        }, "Courtney")
      //      client.invoke("get_user", (r: Option[AnyRef], e: Option[Throwable]) => {
      //        println(r, e)
      //      }, "zcourts")
      //      client.invoke("channel", (r: Option[AnyRef], e: Option[Throwable]) => {
      //        println(r, e)
      //      })
      //      client.invoke("rpcchannel", (r: Option[AnyRef], e: Option[Throwable]) => {
      //        println(r, e)
      //      })
      //      client.invoke("rpc", (r: Option[AnyRef], e: Option[Throwable]) => {
      //        println(r, e)
      //      })
      client.invoke("mixchannel", (r: Option[AnyRef], e: Option[Throwable]) => {
        println(r, e)
      }, "name")
    })
  }
}
