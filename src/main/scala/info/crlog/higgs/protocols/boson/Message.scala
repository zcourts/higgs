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
case class Message() {
  var method: String=""
  var arguments: Array[_]=Array()
  var callback: String = ""
  var protocolVersion: Short = 0x1

  def this(
  method: String,
  arguments: Array[_],
  callback: String = "",
  protocolVersion: Short = 0x1
  ) = {
    this()
    this.method=method
    this.arguments=arguments
    this.callback=callback
    this.protocolVersion=protocolVersion
  } //required for instantiation via reflection
}
