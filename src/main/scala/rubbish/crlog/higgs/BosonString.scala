package rubbish.crlog.higgs

import util.StringUtilities

/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */
class BosonString extends ByteConvertible {

  def this(content: Array[Byte]) {
    this()
    this.content.append(util.getString(content))
  }

  def this(content: String) {
    this()
    this.content.append(content)
  }

  /**
   * Append content to this BosonString
   *
   * @param s the content to append.
   */
  def append(s: String) {
    content.append(s)
  }

  def asBytes: Array[Byte] = {
    content.result().getBytes()
  }

  def asString: String = {
    content.result()
  }

  private val content = new StringBuilder()
  private val util = new StringUtilities
}

object BosonString {
  def empty() = {
    new BosonString
  }

  implicit def StringToBosonString(str: String): BosonString = {
    new BosonString(str)
  }
}


