package info.crlog.higgs.protocols.boson

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonClient {
  val serializer = new BosonSerializer()

  def decoder() = new BosonDecoder()

  def encoder() = new BosonEncoder()

  def allTopicsKey() = ""

}
