package info.crlog.higgs.protocols.boson.json

import java.lang
import org.codehaus.jackson.annotate.JsonProperty

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Message(
               t: String,
               arg: Option[Seq[AnyRef]] = None,
               cb: Option[String] = None,
               d: Any = null
               ) {
  def this() = this("", None, None, Map())

  //see https://github.com/FasterXML/jackson-module-scala/issues/47
  @JsonProperty var topic = t
  @JsonProperty var data = d
  @JsonProperty var arguments = arg
  @JsonProperty var callback = cb
  var primitive = false

  //serialized in JSON to indicate if a response is a primitive type.
  //if false an attempt will be made to deserialize a pojo
  def getPrimitive(): Boolean = {
    if (data == null)
      return false
    val obj = data.asInstanceOf[AnyRef].getClass()
    if (obj == classOf[String])
    //not exactly a primitive but its not necessarily a JSON object
      return true
    if (obj == classOf[Int] || obj == classOf[lang.Integer])
      return true
    if (obj == classOf[Byte] || obj == classOf[lang.Byte])
      return true
    if (obj == classOf[Long] || obj == classOf[lang.Long])
      return true
    if (obj == classOf[Short] || obj == classOf[lang.Short])
      return true
    if (obj == classOf[Float] || obj == classOf[lang.Float])
      return true
    if (obj == classOf[Double] || obj == classOf[lang.Float])
      return true
    if (obj == classOf[Boolean] || obj == classOf[lang.Boolean])
      return true
    if (obj == classOf[Char] || obj == classOf[lang.Character])
      return true
    false
  }
}
