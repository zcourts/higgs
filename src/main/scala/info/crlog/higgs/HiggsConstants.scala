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
  //auto to string on all constants when used in place that expects string
  implicit def constantToString(v: Value): String = {
    v.toString
  }
}