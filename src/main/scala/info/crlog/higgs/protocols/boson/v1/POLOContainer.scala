package info.crlog.higgs.protocols.boson.v1

import java.lang.reflect.Field
import java.{util, lang}
import info.crlog.higgs.protocols.boson.IllegalBosonArgumentException
import org.slf4j.LoggerFactory

/**
 * POLOs are deserialized into a map.
 * This class is a container for that Map and contains the methods required to
 * turn the map into a POJO
 * @param fields A set of fields that can be de-serialized into a Java/Scala object
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class POLOContainer(fields: Map[String, Any]) {
  val log = LoggerFactory.getLogger(getClass())
  def as[T](ignoreUnknownFields: Boolean = true)(implicit mf: Manifest[T]): T = {
    val klass = mf.erasure.asInstanceOf[Class[T]]
    as(klass, ignoreUnknownFields)
  }

  def as[T](klass: Class[T], ignoreUnknownFields: Boolean): T = {
    val instance: T = klass.newInstance()
    //superclass's fields
    val sf = klass.getSuperclass()
    val publicFields = if (sf == null) Array.empty[Field] else sf.getDeclaredFields()
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
          var valueInstance = value
          if (value != null) {
            val poloclass = classOf[POLOContainer]
            val fieldclass = field.getType()
            val valueclass = value.asInstanceOf[AnyRef].getClass()
            //if we're trying to set a field that's not a POLOContainer and the value object is
            //try to convert it to the expected type...
            if (!poloclass.isAssignableFrom(fieldclass) &&
              poloclass.isAssignableFrom(valueclass)) {
              valueInstance = value.asInstanceOf[POLOContainer]
                .as(fieldclass, ignoreUnknownFields)
            }
          }
          //make sure type is assignable
          if (valueInstance != null
            && field.getType().isAssignableFrom(valueInstance.asInstanceOf[AnyRef].getClass())) {
            field.set(instance, valueInstance)
          } else{
            val vclass=if(valueInstance==null) null else valueInstance.asInstanceOf[AnyRef].getClass
            log.warn("Cannot assign %s of type %s to field %s of type %s in class %s".format(
            valueInstance,vclass,field.getName(),field.getType().getName(),instance.asInstanceOf[AnyRef].getClass().getName()
            ))
          }
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
class POLOContainerToType(var param: Any, methodArgument: Class[_]) {
  //verifyScalaJavaPrimitive()
  var isPOLO = false
  //if its a polo
  var parameter: AnyRef =
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

  /**
   * Boxed Java and Scala primitives are not exactly compatible at Runtime
   * since the Scala Int* etc classes do not extend java.lang.Integer*
   * This method checks if the given param is a primitive and if the
   * given class is either a Scala or Java equivalent of that primitive.
   * If it is, it converts the param to the expected method argument be
   * it a Java or Scala Boxing class.
   */
  def verifyScalaJavaPrimitive() {
    if (param != null) {
      val value = param.asInstanceOf[AnyRef]
      val paramClass = value.getClass
      //if param is scala.Byte and methodArgument is java.lang.Byte
      if (paramClass == classOf[scala.Byte] && methodArgument == classOf[java.lang.Byte]) {
        param = scala.Byte.box(value.asInstanceOf[Byte])
        return
      }
      //if param is java.lang.Byte and methodArgument is scala.Byte
      if (paramClass == classOf[java.lang.Byte] && methodArgument == classOf[scala.Byte]) {
        param = scala.Byte.unbox(value)
        return
      }
      //if param is scala.Short and methodArgument is java.lang.Short
      if (paramClass == classOf[scala.Short] && methodArgument == classOf[java.lang.Short]) {
        param = scala.Short.box(value.asInstanceOf[Short])
        return
      }
      //if param is java.lang.Short and methodArgument is scala.Short
      if (paramClass == classOf[java.lang.Short] && methodArgument == classOf[scala.Short]) {
        param = scala.Short.unbox(value)
        return
      }
      //if param is scala.Int and methodArgument is java.lang.Integer
      if (paramClass == classOf[scala.Int] && methodArgument == classOf[java.lang.Integer]) {
        param = scala.Int.box(value.asInstanceOf[Int])
        return
      }
      //if param is java.lang.Integer and methodArgument is scala.Int
      if (paramClass == classOf[java.lang.Integer] && methodArgument == classOf[scala.Int]) {
        param = scala.Int.unbox(value)
        return
      }
      //if param is scala.Long and methodArgument is java.lang.Long
      if (paramClass == classOf[scala.Long] && methodArgument == classOf[java.lang.Long]) {
        param = scala.Long.box(value.asInstanceOf[Long])
        return
      }
      //if param is java.lang.Long and methodArgument is scala.Long
      if (paramClass == classOf[java.lang.Long] && methodArgument == classOf[scala.Long]) {
        param = scala.Byte.unbox(value)
        return
      }
      //if param is scala.Float and methodArgument is java.lang.Float
      if (paramClass == classOf[scala.Float] && methodArgument == classOf[java.lang.Float]) {
        param = scala.Float.box(value.asInstanceOf[Float])
        return
      }
      //if param is java.lang.Float and methodArgument is scala.Float
      if (paramClass == classOf[java.lang.Float] && methodArgument == classOf[scala.Float]) {
        param = scala.Float.unbox(value)
        return
      }
      //if param is scala.Double and methodArgument is java.lang.Double
      if (paramClass == classOf[scala.Double] && methodArgument == classOf[java.lang.Double]) {
        param = scala.Double.box(value.asInstanceOf[Double])
        return
      }
      //if param is java.lang.Double and methodArgument is scala.Douvle
      if (paramClass == classOf[java.lang.Double] && methodArgument == classOf[scala.Double]) {
        param = scala.Double.unbox(value)
        return
      }
      //if param is scala.Boolean and methodArgument is java.lang.Boolean
      if (paramClass == classOf[scala.Boolean] && methodArgument == classOf[java.lang.Boolean]) {
        param = scala.Boolean.box(value.asInstanceOf[Boolean])
        return
      }
      //if param is java.lang.Boolean and methodArgument is scala.Boolean
      if (paramClass == classOf[java.lang.Boolean] && methodArgument == classOf[scala.Boolean]) {
        param = scala.Boolean.unbox(value)
        return
      }
      //if param is scala.Char and methodArgument is java.lang.Character
      if (paramClass == classOf[scala.Char] && methodArgument == classOf[java.lang.Character]) {
        param = scala.Char.box(value.asInstanceOf[Char])
        return
      }
      //if param is java.lang.Character and methodArgument is scala.Char
      if (paramClass == classOf[java.lang.Character] && methodArgument == classOf[scala.Char]) {
        param = scala.Char.unbox(value)
        return
      }
    }
  }
}
