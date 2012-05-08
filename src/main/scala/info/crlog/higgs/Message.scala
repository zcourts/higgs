package info.crlog.higgs

/**
 * NOTE: Messages need to have a channel associated with them to be able to respond
 * Responding to a message is "context free" the only difference between using
 * Messages.send and info.crlog.higgs.Message.respond is info.crlog.higgs.Message.respond directly sends a message back to
 * where ever the first message came from. Otherwise/In general sending a message goes out to all subscribed
 * recipients
 * Courtney Robinson <courtney@crlog.info>
 */
abstract trait Message[K <: ByteConvertible, V <: ByteConvertible] {
  def getProperty(key: K): V

  def addProperty(key: K, value: V)

  /**
   * When de-serializing properties are ready one at a time.
   * This method is invoked once per property and is expected for the implementation to
   * update its properties list with each property passed in via this method.
   * In both cases the implementation is expected to know how to deserialize the byte array of the key
   * and value.
   *
   * @param key   a byte array which is the key/name of this property
   * @param value the value of this property.
   */
  def addProperty(key: Array[Byte], value: Array[Byte])

  /**
   * The idea here is that the message implementation should know how to convert
   * each property to a byte array and should only do the conversion when this method is invoked
   * and instead store properties internally in a more usable form. i.e. if it was a map of
   * strings  store the strings as is and when this method is invoked call getBytes on each string.
   *
   * @return A map of all the properties in this message as a byte array
   */
  def getProperties: Nothing
}

