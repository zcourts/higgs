package info.crlog.higgs.protocols.boson

import java.nio.ByteBuffer
import info.crlog.higgs.util.StringUtil
import io.netty.buffer.Unpooled

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class ByteConverter {
  def byteToBytes(s: Int): Array[Byte] = {
    val buf = ByteBuffer.allocate(1).put(s.toByte)
    val arr = new Array[Byte](buf.limit())
    buf.get(arr, 0, buf.limit())
    arr
  }

  def byteFromBytes(s: Array[Byte]): Int = {
    Unpooled.copiedBuffer(s).readByte()
  }

  def shortToBytes(s: Short): Array[Byte] = {
    //short is 16 bits - 2 bytes
    ByteBuffer.allocate(2).putShort(s).array()
  }

  def shortFromBytes(s: Array[Byte]): Short = {
    //short is 16 bits - 2 bytes
    ByteBuffer.wrap(s).getShort
  }

  def intToBytes(i: Int): Array[Byte] = {
    //int is 32 bits - 4 bytes
    ByteBuffer.allocate(4).putInt(i).array()
  }

  def intFromBytes(i: Array[Byte]): Int = {
    //int is 32 bits - 4 bytes
    Unpooled.copiedBuffer(i).readInt()
  }

  def longToBytes(l: Long): Array[Byte] = {
    //long is 64 bits - 8 bytes
    ByteBuffer.allocate(8).putLong(l).array()
  }

  def longFromBytes(l: Array[Byte]): Long = {
    //short is 16 bits - 2 bytes
    ByteBuffer.wrap(l).getLong()
  }

  def floatToBytes(f: Float): Array[Byte] = {
    //single-precision 32-bit IEEE 754 floating point
    ByteBuffer.allocate(4).putFloat(f).array()
  }

  def floatFromBytes(f: Array[Byte]): Float = {
    //float single-precision 32-bit IEEE 754 floating point
    ByteBuffer.wrap(f).getFloat()
  }

  def doubleToBytes(d: Double): Array[Byte] = {
    //double-precision 64-bit IEEE 754 floating point
    ByteBuffer.allocate(8).putDouble(d).array()
  }

  def doubleFromBytes(d: Array[Byte]): Double = {
    //double-precision 64-bit IEEE 754 floating point
    ByteBuffer.wrap(d).getDouble()
  }

  def booleanToBytes(b: Boolean): Array[Byte] = {
    //1 or 0 where 1 === true and 0 === false
    ByteBuffer.allocate(2).putShort(if (b) 1 else 0).array()
  }

  def booleanFromBytes(b: Array[Byte]): Boolean = {
    //1 or 0 where 1 === true and 0 === false
    if (ByteBuffer.wrap(b).getShort == 1) true else false
  }

  def charToBytes(c: Char): Array[Byte] = {
    //char is 16 bits - 2 bytes
    ByteBuffer.allocate(2).putChar(c).array()
  }

  def charFromBytes(c: Array[Byte]): Char = {
    //char is 16 bits - 2 bytes
    ByteBuffer.wrap(c).getChar
  }

  def stringToBytes(s: String): Array[Byte] = {
    StringUtil.getBytes(s)
  }

  def stringFromBytes(s: Array[Byte]): String = {
    StringUtil.getString(s)
  }

}
