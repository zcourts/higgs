package info.crlog.higgs.protocols.boson.json

import info.crlog.higgs.Serializer
import com.codahale.jerkson.{ParsingException, Json}
import info.crlog.higgs.util.StringUtil

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonSerializer extends Serializer[Message, Array[Byte]] {
  //  val mapper = new ObjectMapper()
  //  mapper.registerModule(DefaultScalaModule)

  //comment while testing to ensure all expected fields are covered
  //mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def serialize(obj: Message) = {
    //    mapper.writeValueAsBytes(obj)
    StringUtil.getBytes(Json.generate(obj))
  } //StringUtil.getBytes(mapper.writeValueAsString(obj))

  def deserialize(obj: Array[Byte]) = {
    //    mapper.readValue(obj, classOf[Message])
    Json.parse[Message](obj)
  }

  /**
   * Un marshall the response from the given message.
   * It expects message.data.response to be a JSON string which can be
   * de-serialized into the implicitly provided type T
   * @param response
   * @param mf
   * @tparam T
   * @return   null if message.data.response is not set. Otherwise an instance of the
   *           the given type T
   */
  def unmarshal[T, U](response: Message, callback: (T, InvalidBosonResponse) => U)
                     (implicit mf: Manifest[T]) {
    //if Message is the param to the callback then just cast and pass it
    if (classOf[Message] == mf.erasure) {
      callback(response.asInstanceOf[T], null)
    } else {
      try {
        //otherwise try to de-serialize
        response.data match {
          case None | null => throw new InvalidBosonResponse("No response found in message", response, null)
          case r: Any => {
            try {
              println(r)
              //variable primitive is set in serialization
              if (response.primitive) {
                if (r.asInstanceOf[AnyRef].getClass.isAssignableFrom(mf.erasure)) {
                  //if is primitive then cast to the type
                  callback(r.asInstanceOf[T], null)
                } else {
                  callback(null.asInstanceOf[T], new InvalidBosonResponse("Response and expected type do not match", response, null))
                }
              } else {
                try {
                  callback(Json.parse[T](r.toString), null)
                } catch {
                  case e: ParsingException => {
                    throw new InvalidBosonResponse("Invalid response type", response, e)
                  }
                }
                //callback(mapper.readValue(r.toString, mf.erasure).asInstanceOf[T], null)
              }
            } catch {
              case e => {
                throw new InvalidBosonResponse("Unable to parse response", response, e)
              }
            }
          }
        }
      } catch {
        case e: InvalidBosonResponse => {
          callback(null.asInstanceOf[T], e)
        }
      }
    }
  }
}
