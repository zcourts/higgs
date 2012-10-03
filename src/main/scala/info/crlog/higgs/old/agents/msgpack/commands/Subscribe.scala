package info.crlog.higgs.agents.msgpack.commands

/**
 * The subscription command is sent when a client subscribes to messages of a given type
 * @param clazz the name of the class to which the client is to be subscribed
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class Subscribe(var clazz: String) extends Command {
  //default constructor required for message pack de-serialization
  def this() = this("")
}
