package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.method
import java.lang.Double

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Listener {
  @method("test")
  def test(a: Double, polo: PoloExample) = {
    //println(a, polo.i)
    a
  }
}
