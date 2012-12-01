package info.crlog.higgs;

import io.netty.channel.socket.SocketChannel;

/**
 * For background see https://github.com/netty/netty/issues/781
 * But essentially a Scala bug generates incorrect byte code resulting a Scala class
 * trying to access package-private objects incorrectly
 * see https://issues.scala-lang.org/browse/SI-1430
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsChannelPropertyWorkAround {
    public HiggsChannelPropertyWorkAround(SocketChannel ch, ClientHandler<?, ?, ?> handler) {
        ch.pipeline().addLast("handler", handler);
    }

    public HiggsChannelPropertyWorkAround(SocketChannel ch, ServerHandler<?, ?, ?> handler) {
        ch.pipeline().addLast("handler", handler);
    }
}
