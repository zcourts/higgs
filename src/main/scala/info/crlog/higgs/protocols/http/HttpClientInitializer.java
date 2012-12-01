package info.crlog.higgs.protocols.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HttpClientInitializer extends ChannelInitializer<SocketChannel> {
    HttpRequestBuilder req;
    RequestProcessor client;

    HttpClientInitializer(HttpRequestBuilder req, RequestProcessor client) {
        this.req = req;
        this.client = client;
    }

    @Override
    public void initChannel(SocketChannel channel) throws Exception {
        if (req.useSSL()) {
            //add ssl first if enabled
            client.ssl(channel.pipeline());
        }
        if (req.compressionEnabled()) {
            // Compress
            channel.pipeline().addLast("deflater", new HttpContentCompressor());
            channel.pipeline().addLast("inflater", new HttpContentDecompressor());
        }
        channel.pipeline().addLast("codec", new HttpClientCodec());
        // to be used since huge file transfer
        channel.pipeline().addLast("chunkedWriter", new ChunkedWriteHandler());
        channel.pipeline().addLast("handler", client.clientHandler());
    }

    ;
}
