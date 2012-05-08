package info.crlog.higgs

/**
 * To be a message property or the key for that property an object must be byte convertible.
 * i.e you must be able to get a  byte array representation of the object
 * Courtney Robinson <courtney@crlog.info>
 */
abstract trait ByteConvertible {
  def asBytes: Array[Byte]

  def asString: String
}