package info.crlog.higgs

import api.{HiggsConstants, HiggsClient}
import protocol.Message

/**
 * A ConnectAgent connects to a BindAgent and says, here's some data!
 */
class ConnectAgent extends HiggsAgent {
  socketType = HiggsConstants.HIGGS_PUBLISHER

  /**
   * Binds to the given host and port
   * @throws UnsupportedOperationException if socketType is not PUBLISHER
   */
  def connect() = {
    if (socketType.equals(HiggsConstants.HIGGS_SUBSCRIBER)) {
      throw new UnsupportedOperationException("A Higgs instance of type SUBSCRIBER cannot connect, use <code>bind</code> instead")
    }
    //wire everything together
    publisher = Some(new HiggsClient(host, port, decoder, encoder, clientHandler))
  }

  /**
   * Attempts to send a message.
   * @return true if written successfully, false otherwise
   */
  def send(msg: String): Boolean = {
    val m = message.newInstance()
    m.setContents(msg)
    send(m)
  }

  /**
   * Attempts to send a message with the given topic.
   * @return true if written successfully, false otherwise
   */
  def send(topic: String, msg: String): Boolean = {
    val m = message.newInstance()
    m.setContents(msg)
    m.topic = topic
    send(m)
  }

  /**
   * Attempts to send a message.
   * @return true if written successfully, false otherwise
   */
  def send(msg: Message): Boolean = {
    if (socketType.equals(HiggsConstants.HIGGS_SUBSCRIBER)) {
      throw new UnsupportedOperationException("Higgs instances of type SUBSCRIBER cannot be used to send messages")
    }
    val channelObj = publisher.get.channel
    if (channelObj.isWritable) {
      channelObj.write(msg)
      true
    } else {
      false
    }
  }

}
