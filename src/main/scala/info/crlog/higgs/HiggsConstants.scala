package info.crlog.higgs

/**
 * A set of constants used through out the Higss library for  Type safety and consistency
 * User: Courtney Robinson <courtney@crlog.info>
 */

object HiggsConstants extends Enumeration {
  type HiggsConstants = Value
  /**
   * Sets a Higgs instance to being a server, i.e. Receives messages and responds
   */
  val HIGGS_PUBLISHER = Value("PUBLISHER")
  /**
   * Sets a Higgs instance to being a client, i.e. Send messages
   */
  val HIGGS_SUBSCRIBER = Value("SUBSCRIBER")
  /**
   * Sets a Higgs instance to being a custom socket type which allows a user to
   * supply a custom protocol
   */
  val SOCKET_OTHER = Value("SOCKET_OTHER")
  /**
   * When used as the topic for a message listener then that listener will receive all messages
   */
  val TOPIC_ALL = Value("")

  val PROTOCOL_ENCODING = Value("UTF-8")

  //--------------------------
  val MIN_READ_BUFFER_SIZE = 64
  val INITIAL_READ_BUFFER_SIZE = 16384
  val MAX_READ_BUFFER_SIZE = 65536
  val THREAD_POOL_SIZE = 16
  val CHANNEL_MEMORY_LIMIT = MAX_READ_BUFFER_SIZE * 2
  val GLOBAL_MEMORY_LIMIT: Long = Runtime.getRuntime().maxMemory() / 3

  //auto to string on all constants when used in place that expects string
  implicit def constantToString(v: Value): String = {
    v.toString
  }
}