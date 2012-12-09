package com.fillta.higgs.boson.demo

import info.crlog.higgs.protocols.boson.v1.BosonProperty

case class PoloExample() {
  def this(j: Int) = {
    this()
    i = j
  }

  @BosonProperty
  var i = 0
  var name = "Test non-annotated field"
  private var str = "Test private non-annotated field"
  @BosonProperty(ignore = true)
  var ignored: String = null

  var nested = new Nested()

  override def toString = {
    val buf = new StringBuilder()
    buf.append("[")
    for (field <- getClass().getDeclaredFields()) {
      field.setAccessible(true)
      val value = field.get(this)
      if (field.getType().isArray) {
        buf.append(field.getName()).append(" = ")
        for (v <- value.asInstanceOf[Array[_]]) {
          buf.append(value).append(",")
        }
      } else {
        buf.append(field.getName()).append(" = ").append(value)
      }
      buf.append(",\n")
    }
    buf.append("]")
    buf.mkString
  }
}

class Nested {
  var array = Array(new NestedField(), new NestedField(), new NestedField())
  var list = List("a", "b", "c", "d")

  override def toString = {
    val buf = new StringBuilder()
    buf.append("[")
    for (field <- getClass().getDeclaredFields()) {
      field.setAccessible(true)
      val value = field.get(this)
      if (field.getType().isArray) {
        buf.append(field.getName()).append(" = ")
        for (v <- value.asInstanceOf[Array[_]]) {
          buf.append(value).append(",")
        }
      } else {
        buf.append(field.getName()).append(" = ").append(value)
      }
      buf.append(",\n")
    }
    buf.append("]")
    buf.mkString
  }
}

class NestedField {
  var a = 0
  var b = 0L
  var c = 0D
  var d = 0F
  var map = Map("0 dim" -> "map", "sum" -> "bing bong", "bah" -> 12345)

  override def toString = {
    val buf = new StringBuilder()
    buf.append("[")
    for (field <- getClass().getDeclaredFields()) {
      field.setAccessible(true)
      val value = field.get(this)
      if (field.getType().isArray) {
        buf.append(field.getName()).append(" = ")
        for (v <- value.asInstanceOf[Array[_]]) {
          buf.append(value).append(",")
        }
      } else {
        buf.append(field.getName()).append(" = ").append(value)
      }
      buf.append(",\n")
    }
    buf.append("]")
    buf.mkString
  }
}