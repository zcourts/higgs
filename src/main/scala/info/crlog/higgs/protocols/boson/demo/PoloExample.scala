package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.protocols.boson.v1.BosonProperty

case class PoloExample() {
  def this(j: Int) = {
    this()
    i = j
  }

  @BosonProperty
  var i = 0
  var name = "Test non-annotated field"
  private var str = "Test private non-annotated field"
  @BosonProperty(ignore = true)
  var ignored: String = null
}