package info.crlog.higgs.protocols.boson.demo

case class PoloExample() {
  def this(j: Int) = {
    this()
    i = j
  }

  @BosonProperty
  var i = 0
}