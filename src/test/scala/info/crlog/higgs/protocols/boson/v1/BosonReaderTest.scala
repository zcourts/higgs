package info.crlog.higgs.protocols.boson.v1

import org.specs2.mutable.Specification
import info.crlog.higgs.util.StringUtil
import info.crlog.higgs.protocols.boson.UnsupportedBosonTypeException

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonReaderTest extends Specification {
  "BosonReader" should {
    val str = StringUtil.getBytes("Test string some arbitrary string...sort of....")
    val reader = new BosonReader(str)
    "Initialize obj" in {
      reader.obj must equalTo(str)
    }
    "Invalid read operations should throw an UnsupportedBosonTypeException" in {
      reader.readByte(false, 0) must throwA[UnsupportedBosonTypeException]
      reader.readShort(false, 0) must throwA[UnsupportedBosonTypeException]
      reader.readInt(false, 0) must throwA[UnsupportedBosonTypeException]
      reader.readLong(false, 0) must throwA[UnsupportedBosonTypeException]
      reader.readFloat(false, 0) must throwA[UnsupportedBosonTypeException]
      reader.readDouble(false, 0) must throwA[UnsupportedBosonTypeException]
      reader.readChar(false, 0) must throwA[UnsupportedBosonTypeException]
      reader.readString(false, 0) must throwA[UnsupportedBosonTypeException]
      reader.readArray(false, 0) must throwA[UnsupportedBosonTypeException]
      reader.readMap(false, 0) must throwA[UnsupportedBosonTypeException]
    }
  }
}
