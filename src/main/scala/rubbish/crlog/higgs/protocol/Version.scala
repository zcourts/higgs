package rubbish.crlog.higgs.protocol


/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */
class InvalidVersionException(msg: String) extends RuntimeException(msg)

case class ProtocolVersion(version: Short)

object Version {
  val V1 = ProtocolVersion(1)

  implicit def fromInt(v: Int): Option[ProtocolVersion] = {
    try {
      if (v > Short.MaxValue)
        throw new InvalidVersionException("Protocol version is limited to Short.MAX_VALUE, 2 bytes")
      Some(ProtocolVersion(v.toShort))
    } catch {
      case e: Exception => None
    }
  }
}
