package info.crlog.higgs

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
trait Serializer[Msg, SerializedMsg] {
  def serialize(obj: Msg): SerializedMsg

  def deserialize(obj: SerializedMsg): Msg
}
