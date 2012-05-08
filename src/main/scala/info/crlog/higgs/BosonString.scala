package info.crlog.higgs

/**
 * Courtney Robinson <courtney@crlog.info>
 */
class BosonString extends ByteConvertible {
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
}


