package info.crlog.higgs.omsg

import java.io.Serializable
import io.netty.channel.Channel
import java.util.UUID
import collection.mutable
import collection.mutable.ListBuffer


/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class SerializableMsgClient(host: String, port: Int)
  extends OMsgClient[OMsg[AnyRef]](host, port) {
  //store a set of pending request IDs
  val callbacks = mutable.Map.empty[UUID, ListBuffer[(AnyRef, (Channel, AnyRef) => Unit)]]

  case class PreparedRequest(id: UUID, req: OMsg[Serializable], me: SerializableMsgClient) {
    def send() {
      me.send(req)
    }
  }

  /**
   * Make a request to which you expect a direct response.
   * This allows you to make a request of any given type and expect a response
   * of a totally different type.
   * Once a request is made the server can continue to send direct responses to this
   * request, If the client wishes to no longer receive these responses then
   * unsubscribe(id)   would un-subscribe the client from requests of the given ID
   * The ID of this request is returned that can be used later to do this
   * @param msg the message to send in the request
   * @param fn  the function to be invoked when a response is received
   * @param instance an instance of the expected response
   * @tparam M
   */
  def prepare[M](msg: AnyRef, fn: (Channel, M) => Unit, instance: M = new AnyRef()) = {
    val req = new OMsg(msg.asInstanceOf[Serializable]) //generate new request
    //listen for SMCRequests
    super.listen(classOf[OMsg[AnyRef]], (c: Channel, s: Serializable) => {
      //we know the request ID exists, its checked below before this is invoked so:
      callbacks(req.id) foreach {
        case callback => {
          //make sure types match
          if (callback._1.getClass.isAssignableFrom(s.asInstanceOf[OMsg[AnyRef]].msg.getClass)) {
            callback._2(c, s.asInstanceOf[OMsg[AnyRef]].msg) //if the do invoke
          } //else the callback isn't invoked
        }
      }
    },
      //when OMsg is received, do we have a pending request for it?
      //if we do then process it otherwise we don't want to know about it
      (id: Class[OMsg[AnyRef]], m: Serializable) => {
        if (m.isInstanceOf[OMsg[M]]) {
          callbacks get (m.asInstanceOf[OMsg[M]].id) match {
            case None => false //don't have ID for this request
            case Some(req) => {
              true //one of our callbacks made this request
            }
          }
        } else {
          false
        }
      })
    val pr = new PreparedRequest(req.id, req, this)
    subscribe(pr, fn.asInstanceOf[(Channel, Any) => Unit], instance)
    pr
  }

  /**
   * Prevent a previously subscribed function from receiving any further responses to the
   * past request.
   * @param id
   */
  def unsubscribe(id: UUID) {
    this.callbacks -= id
  }

  /**
   * Subscribe a given function to the request ID for it to handle responses of the given
   * type M
   * @param req the prepare request instance receive from invoking #prepare
   * @param instance an instance of the expected type, used to verify that the expected and received type
   *                 are compatible
   * @param fn
   */
  def subscribe[M](req: PreparedRequest, fn: (Channel, M) => Unit, instance: M) {
    callbacks.
      //add to list of request ID callbacks
      getOrElseUpdate(req.id, ListBuffer.empty) += (
      (instance.asInstanceOf[AnyRef], fn.asInstanceOf[(Channel, AnyRef) => Unit])
      )
  }
}
