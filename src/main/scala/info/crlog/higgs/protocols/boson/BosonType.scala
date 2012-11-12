package info.crlog.higgs.protocols.boson

/**
 * The set of data types supported by the Boson protocol
 * The value (int) of each type is the flag used to prefix the payload of the given type
 * @author Courtney Robinson <courtney@crlog.info>
 */
object BosonType {
  //val converter = new ByteConverter()
  //base data types
  val BYTE = 1
  val SHORT = 2
  val INT = 3
  val LONG = 4
  val FLOAT = 5
  val DOUBLE = 6
  val BOOLEAN = 7
  val CHAR = 8
  val NULL = 9
  val STRING = 10
  val ARRAY = 11
  val LIST = 12
  val MAP = 13
  val POLO = 14

  //request response flags
  val REQUEST_METHOD_NAME = -127
  val REQUEST_PARAMETERS = -126
  val REQUEST_CALLBACK = -125
  val RESPONSE_METHOD_NAME = -124
  val RESPONSE_PARAMETERS = -123
}
