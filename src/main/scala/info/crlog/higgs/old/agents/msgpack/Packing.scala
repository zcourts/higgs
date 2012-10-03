package info.crlog.higgs.agents.msgpack

import org.msgpack.MessagePack
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.buffer.Unpooled

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Packing {

  val msgpack = new MessagePack()

  def packBytes(msg: AnyRef): Array[Byte] = {
    val packer = msgpack.createBufferPacker()
    val cls = msg.getClass
    msgpack.register(cls)
    packer.write(cls.getName)
    packer.write(msg)
    packer.toByteArray
  }

  def unpackBytes(data: Array[Byte]): (String, Class[Any], Any) = {
    val unpacker = msgpack.createBufferUnpacker()
    unpacker.wrap(data)
    val className = unpacker.readString()
    val clazz: Class[Any] = Class.forName(className).asInstanceOf[Class[Any]]
    msgpack.register(clazz)
    return (className, clazz, unpacker.read(clazz))
  }

  /**
   *
   * @param msg
   * @return
   */
  def pack(msg: AnyRef): BinaryWebSocketFrame = {
    new BinaryWebSocketFrame(
      Unpooled.copiedBuffer(packBytes(msg))
    )
  }

  def unpackMessage(data: BinaryWebSocketFrame) = {
    unpack(data)._3
  }

  def unpack(data: BinaryWebSocketFrame) = {
    val unpacker = msgpack.createBufferUnpacker()
    unpacker.wrap(data.getBinaryData.array())
    val className = unpacker.readString()
    val clazz: Class[Any] = Class.forName(className).asInstanceOf[Class[Any]]
    msgpack.register(clazz)
    (className, clazz, unpacker.read(clazz))
  }

}

object Packing extends Packing {

}