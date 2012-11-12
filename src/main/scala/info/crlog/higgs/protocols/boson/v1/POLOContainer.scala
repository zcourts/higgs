package info.crlog.higgs.protocols.boson.v1

import java.lang.reflect.Field
import java.{util, lang}
import info.crlog.higgs.protocols.boson.IllegalBosonArgumentException

/**
 * POLOs are deserialized into a map.
 * This class is a container for that Map and contains the methods required to
 * turn the map into a POJO
 * @param fields A set of fields that can be de-serialized into a Java/Scala object
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class POLOContainer(fields: Map[String, Any]) {
  def as[T](ignoreUnknownFields: Boolean = true)(implicit mf: Manifest[T]): T = {
    val klass = mf.erasure.asInstanceOf[Class[T]]
    as(klass, ignoreUnknownFields)
  }

  def as[T](klass: Class[T], ignoreUnknownFields: Boolean): T = {
    val instance: T = klass.newInstance()
    //get public fields of the object and all its super classes
    val publicFields = klass.getFields
    //get ALL (public,private,protect,package) fields declared in the class - excludes inherited fields
    val classFields = klass.getDeclaredFields
    //create a set of fields removing duplicates
    val fieldset: Set[Field] = Set[Field]() ++ classFields ++ publicFields

    for (field <- fieldset) {
      fields.get(field.getName()) match {
        case None => {
          if (!ignoreUnknownFields) {
            throw new IllegalArgumentException("Field %s exists in class %s but not found in POLO"
              format(field.getName, klass.getName))
          }
        }
        case Some(value) => {
          field.setAccessible(true)
          field.set(instance, value)
        }
      }
    }
    instance
  }

  /**
   * Check if the given object is a possible POLO.
   * An object is a possible POLO if it is NOT an instance of one of the other supported Boson data types
   * @param obj
   * @return
   */
  def isPossiblePOLO(obj: Class[_]): Boolean = {
    if (obj == classOf[Byte] || obj == classOf[lang.Byte]) {
      return false
    } else if (obj == classOf[Short] || obj == classOf[lang.Short]) {
      return false
    } else if (obj == classOf[Int] || obj == classOf[lang.Integer]) {
      return false
    } else if (obj == classOf[Long] || obj == classOf[lang.Long]) {
      return false
    } else if (obj == classOf[Float] || obj == classOf[lang.Float]) {
      return false
    } else if (obj == classOf[Double] || obj == classOf[lang.Double]) {
      return false
    } else if (obj == classOf[Boolean] || obj == classOf[lang.Boolean]) {
      return false
    } else if (obj == classOf[Char] || obj == classOf[lang.Character]) {
      return false
    } else if (obj == classOf[String] || obj == classOf[lang.String]) {
      return false
    } else if (obj.isArray ||
      classOf[Array[Any]].isAssignableFrom(obj)
    ) {
      return false
    } else if (classOf[List[Any]].isAssignableFrom(obj)
      || classOf[util.List[Any]].isAssignableFrom(obj)) {
      return false
    } else if (classOf[collection.Map[Any, Any]].isAssignableFrom(obj)
      || classOf[util.Map[Any, Any]].isAssignableFrom(obj)) {
      return false
    } else {
      return true
    }
  }
}

/**
 * this is just here as a class because both the server and client uses the same logic and
 * when it changes in the future this provides a central place to update.
 * If the given param is an instance of POLOContainer, that means the method argument given
 * is a POLO and should be replaced with the parameter field of this class.
 * Use isPOLO to test if the given param is in fact a POLO
 * @param param
 * @param methodArgument
 */
class POLOContainerToType(param: Any, methodArgument: Class[_]) {
  var isPOLO = false
  //if its a polo
  val parameter: AnyRef =
    if (param.isInstanceOf[POLOContainer]) {
      val polo = param.asInstanceOf[POLOContainer]
      //make sure the method argument is not a boson primitive
      if (polo.isPossiblePOLO(methodArgument)) {
        val tmp = polo.as(methodArgument, true)
        isPOLO = true
        tmp.asInstanceOf[AnyRef]
      } else {
        //seeing as we have a POLOContainer and a boson supported data type
        throw new IllegalBosonArgumentException("POLO found where %s is expected"
          format (methodArgument.getName()), null)
      }
    } else {
      param.asInstanceOf[AnyRef]
    }
}
