package info.crlog.higgs.protocol.boson

import info.crlog.higgs.protocol.Message
import reflect.BeanProperty
import info.crlog.higgs.util.StringUtil

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 31/01/12
 */

class BosonMessage extends Message {

  object FLAGS {
    /**
     * Full message with no more content
     */
    val NO_MORE_CONTENT: Byte = 0x0
    /**
     * Multi-part message which is delivered as a single message on the receiving end
     */
    val MULTI_PART_MESSAGE: Byte = 0x1
    /**
     * Split Multi-part message which delivers each part of a multi part message separately on the receiving end.
     */
    val SPLIT_MULTI_PART_MESSAGE: Byte = 0x2
    /**
     * only valid when used with split multi part message
     * 's' split multi part messages are no longer delivered at the end once all parts of the
     * message is received, instead each part of the message is delivered, without buffering
     */
    val NO_MESSAGE_BUFFER: Byte = 0x3

  }

  def this(message: String) = {
    this()
    contents = message.getBytes
  }

  /**
   * Create a message supplying its topic
   * @param t the topic of this message
   * @param c the contents of the message
   */
  def this(t: String, c: String) = {
    this()
    topic = t
    contents = c.getBytes
  }

  def this(bytes: Array[Byte]) = {
    this()
    contents = bytes
  }

  /**
   * Create a message supplying its flag,topic and message content
   * @param f   the flag for this message, must be one of the constants in BosonMessage.FLAGS.*
   * @param topic  the topic of the message, a topic's length is limited to 32,767 bytes, about 30KB i.e. the MAX value of a short
   * @param content the contents of the message
   */
  def this(f: Byte, topic: Array[Byte], content: Array[Byte]) = {
    this()
    flag = f
    this.topic = StringUtil.getString(topic)
    contents = content
  }

  /**
   * Simple,naive constructor which simply calls toString on the provided object
   * i.e. your object must override toString and return the string form you wish to be sent
   */
  def this(obj: AnyRef) = {
    this()
    if (obj.isInstanceOf[Array[Byte]]) {
      contents = obj.asInstanceOf[Array[Byte]]
    } else {
      contents = obj.toString.getBytes
    }
  }

  @BeanProperty
  var flag: Byte = FLAGS.NO_MORE_CONTENT
}