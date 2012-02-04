package info.crlog.higgs.protocol
/**
 * @author Courtney Robinson <courtney@crlog.info> @ 01/02/12
 */

trait MessageListener {
  def onMessage(msg: Message)
}