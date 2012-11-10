package info.crlog.higgs.protocols.jrpc

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class IllegalResponseException(response: Any,
                                    msg: String = "Object received from remote method cannot be used as a parameter to the callback provided")
  extends RuntimeException(msg) {

}
