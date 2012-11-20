package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.method

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Listener {
  @method("test")
  def test(a: Double, polo: Map[AnyRef, AnyRef]) = {
    println(a, polo.head._2.isInstanceOf[PoloExample])
    polo
  }

  @method("nodejs")
  def nodejs(
              a: Float, b: Int, nullObj: Object,
              m: Map[String, Int],
              arr: Array[AnyRef],
              bool: Boolean, str: String
              ) = {
    println(
      a, b, nullObj,
      m,
      arr.mkString("[", ",", "]"),
      bool, str
    )
    Array(a, b, nullObj, m, arr, bool, str)
  }

}
