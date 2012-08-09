package example.websocket.sslserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * A HTTP server which serves Web Socket requests at:
 *
 * https://localhost:8081/sample.websocket
 *
 * Open your browser at https://localhost:8081/, then the demo page will be loaded and a Web Socket connection will be
 * made automatically.
 *
 * This server illustrates support for the different web socket specification versions and will work with:
 *
 * <ul>
 * <li>Safari 5+ (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 6-13 (draft-ietf-hybi-thewebsocketprotocol-00)
 * <li>Chrome 14+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * <li>Chrome 16+ (RFC 6455 aka draft-ietf-hybi-thewebsocketprotocol-17)
 * <li>Firefox 7+ (draft-ietf-hybi-thewebsocketprotocol-10)
 * </ul>
 */
public class WebSocketSslServer {

    private final int port;

    public WebSocketSslServer(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        try {
            b.eventLoop(new NioEventLoop(), new NioEventLoop())
             .channel(new NioServerSocketChannel())
             .localAddress(port)
             .childHandler(new WebSocketSslServerInitializer());

            Channel ch = b.bind().sync().channel();
            System.out.println("Web socket server started at port " + port + '.');
            System.out.println("Open your browser and navigate to https://localhost:" + port + '/');
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
            port = 8443;
        }

        String keyStoreFilePath = System.getProperty("keystore.file.path");
        if (keyStoreFilePath == null || keyStoreFilePath.isEmpty()) {
            System.out.println("ERROR: System property keystore.file.path not set. Exiting now!");
            System.exit(1);
        }

        String keyStoreFilePassword = System.getProperty("keystore.file.password");
        if (keyStoreFilePassword == null || keyStoreFilePassword.isEmpty()) {
            System.out.println("ERROR: System property keystore.file.password not set. Exiting now!");
            System.exit(1);
        }

        new WebSocketSslServer(port).run();
    }
}
