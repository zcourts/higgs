package info.crlog.higgs.agents.transceiver


/**
 * A Transceiver - Both a server and a client...
 * Listens on a port for incoming connections to initiate conversations and is able
 * to connect to any other transceiver to be the initiator of a conversation.
 *
 * Its messages are assigned  a channel, the receiver of that message can then directly
 * respond to the message via that channel. There is a channel for every connection
 * made by or received by a transceiver.
 *
 * This means a transceiver can "speak" to multiple other transceivers simultaneously
 * and in direct response to messages.
 *
 * Its an alternative which avoids re-connections for every message that needs to be
 * sent
 * Courtney Robinson <courtney@crlog.info>
 */

class Transceiver {
  var client: TClient = null
  var server: TServer = null
  //true if this transceiver made the connection to the remote peer first
  var initiater = false
  //true if connected to at least one remote peer
  var connected = false

  def onMsg(){

  }
  /**
   * You can connect a transceiver if and only if it is not already listening for
   * connections. If connected will throw
   * @param host
   * @param port
   */
  def connect(host: String, port: Int) {
    if (!connected) {
      client = new TClient(host, port)
      initiater = true
      connected = true
    } else {
      throw new IllegalStateException("You're trying to connect a transceiver that's already listening for connections")
    }
  }

  /**
   *
   * @param host
   * @param port
   */
  def listen(host: String, port: Int) {
    if (!connected) {
      server = new TServer(host, port)
      initiater = false
      connected = true
    } else {
      throw new IllegalStateException("You're trying to listen with a transceiver that's already connected to a remote peer")
    }
  }
}
