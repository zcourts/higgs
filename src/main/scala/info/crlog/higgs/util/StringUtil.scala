package info.crlog.higgs.util

import java.nio.ByteBuffer
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
  private val encoding: String = "UTF-8"
  private var charset: Charset = Charset.forName(encoding)
  private val decoder: CharsetDecoder = charset.newDecoder
  private val encoder: CharsetEncoder = charset.newEncoder

  /**
   * Encode a string to a byte array using
   *
   * @param value The string to encode
   * @return A byte array representation of the string, encoded using the
   *         current charset OR null if an exception is raised;
   */
  def getBytes(value: String): Array[Byte] = {
    value.getBytes(charset)
  }

  /**
   * Convert a byte array to a string using the current charset
   *
   * @param data the byte array to convert to a string
   * @return A string representation of the input byte array OR null on error
   */
  def getString(data: Array[Byte]): String = {
    decoder.decode(ByteBuffer.wrap(data)).toString
  }

  def setCharset(c: String) {
    charset = Charset.forName(c)
  }

  def setCharset(c: Charset) {
    charset = c
  }
}

object StringUtil extends StringUtil {

}