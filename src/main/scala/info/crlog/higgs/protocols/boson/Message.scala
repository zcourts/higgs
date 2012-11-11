package info.crlog.higgs.protocols.boson

/**
 * Represents a response or request
 * @param method   the method name to invoke when this represents a request
 * @param arguments the arguments to be passed to the method in both requests and responses
 * @param callback  the name of the client fallback function to be invoked when a response is received.
 *                  If empty this is assumed to be a response.
 * @param protocolVersion A 16 bit int
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class Message(
                    var method: String,
                    var arguments: Array[Any] = Array(),
                    var callback: String = "",
                    var protocolVersion: Short = 0x1
                    ) {
  def this() = this("", Array.empty[Any]) //required for instantiation via reflection
}
