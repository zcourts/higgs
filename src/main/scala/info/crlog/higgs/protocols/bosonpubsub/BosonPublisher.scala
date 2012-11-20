package info.crlog.higgs.protocols.bosonpubsub

import info.crlog.higgs.protocols.boson.BosonServer

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonPublisher(port: Int, host: String = "localhost", compress: Boolean = false)
  extends BosonServer(port, host, compress) {
  def send() {

  }
}
