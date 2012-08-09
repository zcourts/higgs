package info.crlog.higgs

import api.{HiggsConstants, HiggsServer, HiggsClient}
import protocol._
import protocol.boson.{BosonMessage, BosonEncoder, BosonDecoder}
import reflect.BeanProperty
import collection.mutable.ListBuffer

/**
 * TODO: Add docs
 */

class HiggsAgent {
  var socketType: HiggsConstants.Value = HiggsConstants.HIGGS_PUBLISHER
  type ListenersList = ListBuffer[(Message) => Unit]
  /**
   * The decoder Higgs uses to decode messages
   */
  @BeanProperty //default protocol decoder
  var decoder: Class[_ <: HiggsDecoder] = classOf[BosonDecoder]
  @BeanProperty //default protocol encoder
  var encoder: Class[_ <: HiggsEncoder] = classOf[BosonEncoder]
  @BeanProperty //default client request handler
  var clientHandler: Class[_ <: HiggsPublisher] = classOf[protocol.boson.Publisher]
  var message: Class[_ <: Message] = classOf[BosonMessage]
  @BeanProperty //default server request handler
  var serverHandler: Class[_ <: HiggsSubscriber] = classOf[protocol.boson.Subscriber]
  protected var publisher: Option[HiggsClient] = None
  protected var subscriber: Option[HiggsServer] = None

  @BeanProperty
  var host = "127.0.0.1"
  @BeanProperty
  var port = 2012

  def stop() {
    if (socketType.equals(HiggsConstants.HIGGS_PUBLISHER)) {
      publisher.get.shutdown()
    } else {
      subscriber.get.channel.unbind()
      subscriber.get.shutdown()
    }
  }

}
