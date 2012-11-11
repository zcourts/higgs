package info.crlog.higgs.protocols.boson

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class InvalidRequestResponseTypeException(msg: String, cause: Throwable)
  extends RuntimeException(msg, cause) {

}
