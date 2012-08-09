package info.crlog.higgs.util

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder

/**
 * @author Courtney Robinson <courtney@crlog.rubbish> @ 06/02/12
 * @see <link>http://www.joelonsoftware.com/articles/Unicode.html</link>
 * @see
 * <link>http://www.exampledepot.com/egs/java.nio.charset/ConvertChar.html</link>
 */
class StringUtil {
  /**
   * Encode a string to a byte array using
   *
   * @param value The string to encode
   * @return A byte array representation of the string, encoded using the
   *         current charset OR null if an exception is raised;
   */
  def getBytes(value: String): Array[Byte] = {
    try {
      val bbuf: ByteBuffer = encoder.encode(CharBuffer.wrap(value))
      return bbuf.array
    }
    catch {
      case e: Exception => {
        return "".getBytes
      }
    }
  }

  /**
   * Convert a byte array to a string using the current charset
   *
   * @param data the byte array to convert to a string
   * @return A string representation of the input byte array OR null on error
   */
  def getString(data: Array[Byte]): String = {
    try {
      return decoder.decode(ByteBuffer.wrap(data)).toString
    }
    catch {
      case e: Exception => {
        return ""
      }
    }
  }

  def setCharset(c: String) {
    charset = Charset.forName(c)
  }

  def setCharset(c: Charset) {
    charset = c
  }

  private var encoding: String = "UTF-8"
  private var charset: Charset = Charset.forName(encoding)
  private var decoder: CharsetDecoder = charset.newDecoder
  private var encoder: CharsetEncoder = charset.newEncoder
}

