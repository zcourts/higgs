package info.crlog.higgs.protocols.boson.demo

import info.crlog.higgs.protocols.boson.v1.BosonProperty

case class PoloExample() {
  def this(j: Int) = {
    this()
    i = j
  }

  @BosonProperty
  var i = 0
}