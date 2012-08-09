package info.crlog.higgs.agents.http

import info.crlog.higgs.EventListener

/**
 * Simple HTTP event listener class.
 * Do not share event listeners among HTTP requests
 * Courtney Robinson <courtney@crlog.info>
 */

trait HTTPEventListener extends EventListener[String] {
  val response = new FutureHTTPResponse
  response ++ this
}
