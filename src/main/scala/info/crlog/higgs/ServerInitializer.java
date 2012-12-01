package info.crlog.higgs;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ServerInitializer extends ChannelInitializer<SocketChannel> {
    private Server server;

    public ServerInitializer(Server<?, ?, ?> c) {
        server = c;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        if (server.usingSSL()) {
            //add SSL first if enabled
            server.ssl(ch.pipeline());
        }
        if (server.compress()) {
            // Enable stream compression
            ch.pipeline().addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
            ch.pipeline().addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        }
        if (!server.usingCodec()) {
            // Add the encoder/decoder
            ch.pipeline().addLast("decoder", server.decoder());
            ch.pipeline().addLast("encoder", server.encoder());
        }
        //messaging logic
        server.handler(ch);
    }
}
