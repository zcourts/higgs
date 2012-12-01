package info.crlog.higgs.protocols.boson.v1

import info.crlog.higgs.Serializer
import info.crlog.higgs.protocols.boson.Message
import com.yammer.metrics.Metrics
import java.util.concurrent.TimeUnit

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class BosonSerializer extends Serializer[Message, Array[Byte]] {
  val mWriter = Metrics.newTimer(getClass(), "boson-write-time", TimeUnit.MILLISECONDS, TimeUnit.SECONDS)
  val mReader = Metrics.newTimer(getClass(), "boson-read-time", TimeUnit.MILLISECONDS, TimeUnit.SECONDS)

  def serialize(obj: Message): Array[Byte] = {
    val ctx = mWriter.time()
    val s = new BosonWriter(obj).get()
    ctx.stop()
    s
  }

  def deserialize(obj: Array[Byte]): Message = {
    val ctx = mReader.time()
    val d = new BosonReader(obj).get()
    ctx.stop()
    d
  }
}
