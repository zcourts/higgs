package info.crlog.higgs.protocols.jrpc.demo

import info.crlog.higgs.method
import io.netty.channel.Channel
import info.crlog.higgs.protocols.jrpc.RPC

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class TestServerListener {

  @method
  def test(a: String) {
    println("test called with param %s" format (a))
  }

  @method("get_user")
  def getUser(a: String) = {
    println("get user called %s" format (a))
    true
  }

  @method("channel")
  def channel(a: Channel) {
    println("channel %s" format (a))
  }

  @method("rpc")
  def rpc(a: RPC) {
    println("rpc %s" format (a))
  }

  @method("rpcchannel")
  def rpcchannel(a: Channel, rpc: RPC) {
    println("rpcchannel %s" format (a))
  }

  @method("mixchannel")
  def mixchannel(a: Channel, rpc: RPC, name: String) {
    println("mixchannel %s" format (a))
  }

  @method("test_response")
  def testResponse(i: Int) = {
    println(i)
    100L
  }
}
