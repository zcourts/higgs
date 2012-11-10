package info.crlog.higgs.protocols.boson

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class Message(method: String, arguments: Array[Any] = Array(), callback: String = "") {
  def this() = this("", Seq()) //required for instantiation via reflection
}
