package info.crlog.higgs.agents.websocket.server

import com.codahale.jerkson.Json._

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class TextMessage(topic: String, data: Map[String, String]) {

  def asJson() = generate(this)
}

object TextMessage {
  implicit def strToMsg(str: String) = parse[TextMessage](str)

  def apply(str: String) = strToMsg(str)
}