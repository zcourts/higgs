package info.crlog.higgs.protocols.boson

import java.lang.reflect.Field
import java.{util, lang}
import collection.mutable.ListBuffer
import org.slf4j.LoggerFactory

/**
 * Used by BosonReader.
 * When a POLO is de-serialized, if it cannot be assigned to the field it is for
 * this class takes it and attempts to convert the POLO to the expected type.
 * For e.g. Given a scala.collection.immutable.Map as a field and a value of
 * scala.collection.mutable.Map as a value, it'll painfully iterate, copy and set
 * the field to an immutable Map from its mutable version.
 * Supported types:
 * <pre>
 * <table>
 * <tr>
 * <td>Interface/Top level class</td>
 * <td>Supported Sub classes</td>
 * </tr>
 * <tr>
 * <td>{@code scala.collection.Map}</td>
 * <td>
 * {@code scala.collection.immutable.Map} <br />
 * {@code scala.collection.mutable.Map}  <br />
 * All other subclasses of Map
 * </td>
 * </tr>
 * <tr>
 * <td>{@code array}</td>
 * <td>
 * {@code scala.collection.Seq} <br />
 * {@code Array}  <br />
 * </td>
 * </tr>
 * </table>
 * </pre>
 * @author Courtney Robinson <courtney@crlog.info>
 */
class FieldTypeConverter(value: Any, fieldType: Class[_], instance: Any, field: Field) {
  val log = LoggerFactory.getLogger(getClass())
  val intclass = classOf[Int]
  val javaintclass = classOf[lang.Integer]
  val longclass = classOf[Long]
  val javalongclass = classOf[lang.Long]
  val floatclass = classOf[Float]
  val javafloatclass = classOf[lang.Float]
  val doubleclass = classOf[Double]
  val javadoubleclass = classOf[lang.Double]

  def convert() {
    if (fieldType.isAssignableFrom(intclass) || fieldType.isAssignableFrom(javaintclass)) {
      field.setInt(instance, Int.unbox(value.asInstanceOf[AnyRef]))
    } else if (fieldType.isAssignableFrom(longclass) || fieldType.isAssignableFrom(javalongclass)) {
      field.setLong(instance, Long.unbox(value.asInstanceOf[AnyRef]))
    } else if (fieldType.isAssignableFrom(floatclass) || fieldType.isAssignableFrom(javafloatclass)) {
      field.setFloat(instance, Float.unbox(value.asInstanceOf[AnyRef]))
    } else if (fieldType.isAssignableFrom(doubleclass) || fieldType.isAssignableFrom(javadoubleclass)) {
      field.setDouble(instance, Double.unbox(value.asInstanceOf[AnyRef]))
    } else {
      convertObject()
    }
  }

  def convertObject() {
    if (fieldType.isAssignableFrom(classOf[List[Any]])
      || fieldType.isAssignableFrom(classOf[ListBuffer[Any]])
      || fieldType.isAssignableFrom(classOf[util.List[Any]])
    ) {
      //BosonReader#readList will always return an immutable List
      if (value.asInstanceOf[AnyRef].isInstanceOf[List[Any]]) {
        if (fieldType.isAssignableFrom(classOf[ListBuffer[Any]])) {
          convertListBuffer()
        } else if (fieldType.isAssignableFrom(classOf[List[Any]])) {
          convertScalaList()
        } else {
          convertJavaList()
        }
      } else {
        log.warn("Field \":%s\" of class \"%s\" is a list but value received is \"%s\" of type \"%s\"".format(
          field.getName(), field.getDeclaringClass().getName(), value, if (value == null) "null" else value.asInstanceOf[AnyRef].getClass().getName()
        ))
      }
    } else if (
      fieldType.isAssignableFrom(classOf[collection.immutable.Map[Any, Any]])
        || fieldType.isAssignableFrom(classOf[collection.mutable.Map[Any, Any]])
        || fieldType.isAssignableFrom(classOf[util.Map[Any, Any]])
    ) {
      if (value.asInstanceOf[AnyRef].isInstanceOf[Map[Any, Any]]) {
        if (fieldType.isAssignableFrom(classOf[util.Map[Any, Any]])) {
          convertJavaMap()
        } else {
          convertScalaMap()
        }
      } else {
        log.warn("Field \":%s\" of class \"%s\" is a map but value received is \"%s\" of type \"%s\"".format(
          field.getName(), field.getDeclaringClass().getName(), value, if (value == null) "null" else value.asInstanceOf[AnyRef].getClass().getName()
        ))
      }
    } else {
      log.warn("Cannot set field:\n Field \":%s\" of class \"%s\" is of type %s but value received is \"%s\" of type \"%s\"".format(
        field.getName(), field.getDeclaringClass().getName(), field.getType().getName(),
        value, if (value == null) "null" else value.asInstanceOf[AnyRef].getClass().getName()
      ))
    }
  }

  protected def convertListBuffer() {
    //create a list buffer and populate it
    val list = ListBuffer.empty[Any]
    val obj = value.asInstanceOf[AnyRef].asInstanceOf[List[Any]]
    list ++= obj
    field.set(instance, list)
  }

  protected def convertScalaList() {
    val list = ListBuffer.empty[Any]
    val obj = value.asInstanceOf[AnyRef].asInstanceOf[List[Any]]
    list ++= obj
    field.set(instance, list.toList)
  }

  protected def convertJavaList() {
    val list = new util.ArrayList[Any]()
    val obj = value.asInstanceOf[AnyRef].asInstanceOf[List[Any]]
    obj foreach ((item) => {
      list.add(item)
    })
    field.set(instance, list)
  }


  protected def convertScalaMap() {
    //BosonReader#readMap will return an immutable map
    val obj = value.asInstanceOf[AnyRef].asInstanceOf[Map[Any, Any]]
    if (fieldType.isAssignableFrom(classOf[collection.mutable.Map[Any, Any]])) {
      val map = collection.mutable.Map.empty[Any, Any]
      map ++= obj
      //set mutable map
      field.set(instance, map)
    } else {
      //set immutable map
      field.set(instance, obj)
    }
  }

  protected def convertJavaMap() {
    val map = new util.HashMap[Any, Any]()
    val obj = value.asInstanceOf[AnyRef].asInstanceOf[Map[Any, Any]]
    obj foreach ((tuple) => {
      map.put(tuple._1, tuple._2)
    })
    field.set(instance, map)
  }
}
