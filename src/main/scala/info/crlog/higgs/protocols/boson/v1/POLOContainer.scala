package info.crlog.higgs.protocols.boson.v1

import java.lang.reflect.Field

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

  def as[T](klass: Class[T], ignoreUnknownFields: Boolean = true): T = {
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

}
