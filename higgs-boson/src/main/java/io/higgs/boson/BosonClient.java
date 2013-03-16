package io.higgs.boson;

import io.higgs.ConnectFuture;
import io.higgs.HiggsClient;
import io.higgs.boson.serialization.BosonDecoder;
import io.higgs.boson.serialization.BosonEncoder;
import io.higgs.boson.serialization.v1.BosonReader;
import io.higgs.boson.serialization.v1.BosonWriter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonClient extends HiggsClient<String, BosonMessage, BosonMessage, ByteBuf> {
    protected boolean compression;
    protected boolean ssl;

    protected boolean setupPipeline(ChannelPipeline pipeline) {
        if (compression) {
            pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
            pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        }
        pipeline.addLast("decoder", new BosonDecoder());
        pipeline.addLast("encoder", new BosonEncoder());
        return true; //auto add handler
    }

    public ByteBuf serialize(Channel ctx, BosonMessage msg) {
        return new BosonWriter(msg).serialize();
    }

    public BosonMessage deserialize(ChannelHandlerContext ctx, ByteBuf msg) {
        return new BosonReader(msg).deSerialize();
    }

    public String getTopic(BosonMessage msg) {
        return msg.method;
    }

    /**
     * ASynchronously connect to the given host:port
     *
     * @param serviceName the name of the service being connected to. Useful for debugging when multiple
     *                    connects mail fail, a human readable name makes logs easier to read
     * @param host        the host/ip to connect to
     * @param port        the port on the host
     * @param reconnect   if true then should connection fail it will automatically be re-attempted
     * @return a future which notifies when the connection succeeds or fails
     */
    public BosonConnectFuture connect(String serviceName, String host, int port, boolean reconnect) {
        return (BosonConnectFuture) connect(serviceName, host, port, reconnect, ssl, null);
    }

    protected ConnectFuture newConnectFuture(boolean reconnect, ConnectFuture connFuture) {
        return connFuture == null ? new BosonConnectFuture(this, reconnect) : connFuture;
    }
}
