package rubbish.crlog.higgs.protocol

import rubbish.crlog.higgs.ByteConvertible
import java.nio.ByteBuffer
import rubbish.crlog.higgs.util.StringUtilities

/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */

class Buffer extends ByteConvertible {

  private var internalBuffer: Option[ByteBuffer] = None

  def this(a: Array[Byte]) {
    this()
    internalBuffer = Some(ByteBuffer.wrap(a))
  }

  def this(str: String) {
    this()
    internalBuffer = Some(ByteBuffer.wrap(str.getBytes))
  }

  def buffer() = {
    internalBuffer match {
      case None => ByteBuffer.allocate(0)
      case Some(b) => b
    }
  }

  def array() = {
    internalBuffer match {
      case None => "".getBytes
      case Some(b) => b.array()
    }
  }

  def hasArray(): Boolean = {
    internalBuffer match {
      case None => false
      case Some(b) => b.hasArray
    }
  }

  override def hashCode() = {
    internalBuffer match {
      case None => super.hashCode()
      case Some(b) => b.hashCode()
    }
  }

  override def equals(o: Any) = {
    internalBuffer match {
      case None => super.equals(o)
      case Some(b) => b.equals(o)
    }
  }

  def asBytes: Array[Byte] = {
    internalBuffer match {
      case None => "".getBytes
      case Some(b) => b.array()
    }
  }

  def asString: String = {
    internalBuffer match {
      case None => ""
      case Some(b) => {
        new StringUtilities().getString(b.array()) match {
          case None => ""
          case Some(v) => v
        }
      }
    }
  }

  override def toString() = asString
}

object Buffer {
  def wrap(arr: Array[Byte]): Buffer = {
    new Buffer(arr)
  }

  implicit def strToBuffer(str: String): Buffer = {
    new Buffer(str)
  }

  implicit def protocolVersionToBuffer(v: ProtocolVersion): Buffer = {
    new Buffer("" + v.version)
  }
}