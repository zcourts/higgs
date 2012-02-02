package info.crlog.higgs.protocol

import reflect.BeanProperty

/**
 * @author Courtney Robinson <courtney@crlog.info> @ 01/02/12
 */

trait MessageListener {
  @BeanProperty
  var topic: String

  def onMessage(msg: Message)
}