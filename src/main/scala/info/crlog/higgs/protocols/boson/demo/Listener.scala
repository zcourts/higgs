package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.method

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Listener {
  @method("test")
  def test(a: Double, str: String) = {
    //println(a, str)
    a
  }
}
