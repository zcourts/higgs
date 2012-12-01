package info.crlog.higgs;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

/**
 * Must be written in Java while Scala version is < 2.10.X
 *
 * @author Courtney Robinson <courtney@crlog.info>
 * @see <a href="https://github.com/netty/netty/issues/781">https://github.com/netty/netty/issues/781</a>
 */
public class ClientInitializer extends ChannelInitializer<SocketChannel> {
    Client client;

    ClientInitializer(Client client) {
        this.client = client;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        if (client.usingSSL()) {
            //add SSL first if enabled
            client.ssl(ch.pipeline());
        }
        if (client.compress()) {
            // Enable stream compression
            ch.pipeline().addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
            ch.pipeline().addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        }
        if (!client.usingCodec()) {
            // Add the encoder/decoder
            ch.pipeline().addLast("decoder", client.decoder());
            ch.pipeline().addLast("encoder", client.encoder());
        }
        //messaging logic
        client.handler(ch);
    }
}
