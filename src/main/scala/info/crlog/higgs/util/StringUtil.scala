package info.crlog.higgs.util

import java.nio.charset.{Charset, CharsetDecoder, CharsetEncoder}
import info.crlog.higgs.api.HiggsConstants
import java.nio.{CharBuffer, ByteBuffer}


/**
 * @see  http://www.joelonsoftware.com/articles/Unicode.html
 * @see http://www.exampledepot.com/egs/java.nio.charset/ConvertChar.html
 * @author Courtney Robinson <courtney@crlog.info> @ 06/02/12
 */

object StringUtil {
  // Create the encoder and decoder for ISO-8859-1
  val charset: Charset = Charset.forName(HiggsConstants.PROTOCOL_ENCODING);
  val decoder: CharsetDecoder = charset.newDecoder();
  val encoder: CharsetEncoder = charset.newEncoder();

  /**
   * Encode a string to a byte array using
   * @param value  The string to encode
   * @return   A byte array representation of the string, encoded using    <code>HiggsConstants.PROTOCOL_ENCODING</code> as the character set encoder
   */
  def getBytes(value: String): Array[Byte] = {
    // The new ByteBuffer is ready to be read.
    val bbuf: ByteBuffer = encoder.encode(CharBuffer.wrap(value));
    bbuf.array()
  }

  /**
   * Convert a byte array to a string using  <code>HiggsConstants.PROTOCOL_ENCODING</code> as the character set for decoding
   * @param data the byte array to convert to a string
   * @return A string representation of the input byte array
   */
  def getString(data: Array[Byte]): String = {
    decoder.decode(ByteBuffer.wrap(data)).toString
  }
}
