package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.method
import com.fillta.higgs.boson.demo.PoloExample

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Listener {
  @method("test")
  def test(a: Double, polo: Map[AnyRef, AnyRef]) = {
    println(a, polo.head._2.isInstanceOf[PoloExample])
    polo
  }

  @method("list")
  def list(a: List[PoloExample]) = {
    println(a)
  }

  var count = 0
  new Thread(new Runnable {
    def run() {
      while (true) {
        println(count)
        Thread.sleep(1000)
      }
    }
  }).start()

  @method("all")
  def all(
           a: Float, b: Int, nullObj: Object,
           m: Map[String, Int],
           arr: Array[Int],
           bool: Boolean, str: String,
           list: List[String],
           polo: PoloExample
           ) = {
    //    println(
    //      a, b, nullObj,
    //      m,
    //      arr.mkString("[", ",", "]"),
    //      bool, str, list, polo
    //    )
    count += 1
    //    println(count)
    Array(a, b, nullObj, m, arr, bool, str, list, polo)
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
    //send back everything node js sent and also send all other types it doesn't support
    //it needs to de-serialize them properly regardless (of the fact it can't serialize them).
    List(Array(a, b, nullObj, m, arr, bool, str),
      Array(1.2F, 140, null, Map("a" -> 120), Array(1, 2, 356), false, "a string",
        List("Some List"), new PoloExample(100)))
  }

}
