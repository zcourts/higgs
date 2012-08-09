package example.http.snoop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 */
public class HttpSnoopServer {

    private final int port;

    public HttpSnoopServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        // Configure the server.
        ServerBootstrap b = new ServerBootstrap();

        try {
            b.eventLoop(new NioEventLoop(), new NioEventLoop())
             .channel(new NioServerSocketChannel())
             .childHandler(new  example.http.snoop.HttpSnoopServerInitializer())
             .localAddress(new InetSocketAddress(port));

            Channel ch = b.bind().sync().channel();
            ch.closeFuture().sync();
        } finally {
            b.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new HttpSnoopServer(port).run();
    }
}
