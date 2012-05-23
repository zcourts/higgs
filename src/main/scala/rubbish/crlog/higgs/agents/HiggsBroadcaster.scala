package rubbish.crlog.higgs.agents

import rubbish.crlog.higgs.{Message, HiggsServer}


/**
 * A broadcaster agent binds to the local system and publishes messages to N listeners.
 * Where N is 0 to as many connected clients as the OS supports.
 * Using this agent does not offer up guarantee on who messages are delivered. Who ever happens to be listening
 * will receive messages.
 * Courtney Robinson <courtney@crlog.rubbish>
 */
class HiggsBroadcaster extends HiggsServer {

  def this(port: Int) = {
    this()
    this.port = port
  }

  def broadcast(msg: Message) {
    higgsChannel.channels.write(msg)
  }
}
