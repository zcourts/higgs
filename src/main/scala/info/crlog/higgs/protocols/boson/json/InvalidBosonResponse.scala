package info.crlog.higgs.protocols.boson.json

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class InvalidBosonResponse(msg: String,response:Message,cause:Throwable)
  extends RuntimeException(msg,cause) {

}
