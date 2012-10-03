package info.crlog.higgs.agents.omsg.commands

/**
 * When a client or Server does not support a class this message is sent in response
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class UnsupportedClass(var clazz: String) extends Command {
  //default constructor required for message pack de-omsg
  def this() = this("")
}

