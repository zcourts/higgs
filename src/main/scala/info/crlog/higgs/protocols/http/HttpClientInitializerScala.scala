package info.crlog.higgs.protocols.http

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.{HttpClientCodec, HttpContentDecompressor, HttpContentCompressor}
import io.netty.handler.stream.ChunkedWriteHandler

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class HttpClientInitializerScala(req: HttpRequestBuilder, client: RequestProcessor) extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) {
    if (req.useSSL) {
      //add ssl first if enabled
      client.ssl(channel.pipeline())
    }
    if (req.compressionEnabled) {
      // Compress
      channel.pipeline().addLast("deflater", new HttpContentCompressor(1))
      // Remove the following line if you don't want automatic content decompression.
      channel.pipeline().addLast("inflater", new HttpContentDecompressor)
    }
    channel.pipeline().addLast("codec", new HttpClientCodec)
    // to be used since huge file transfer
    channel.pipeline().addLast("chunkedWriter", new ChunkedWriteHandler)
    channel.pipeline().addLast("handler", client.clientHandler)
  }

}
