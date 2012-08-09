package example.http.snoop;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioEventLoop;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * A simple HTTP client that prints out the content of the HTTP response to
 * {@link System#out} to test {@link HttpSnoopServer}.
 */
public class HttpSnoopClient {

    private final URI uri;

    public HttpSnoopClient(URI uri) {
        this.uri = uri;
    }

    public void run() throws Exception {
        String scheme = uri.getScheme() == null? "http" : uri.getScheme();
        String host = uri.getHost() == null? "localhost" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if (scheme.equalsIgnoreCase("http")) {
                port = 80;
            } else if (scheme.equalsIgnoreCase("https")) {
                port = 443;
            }
        }

        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            System.err.println("Only HTTP(S) is supported.");
            return;
        }

        boolean ssl = scheme.equalsIgnoreCase("https");

        // Configure the client.
        Bootstrap b = new Bootstrap();
        try {
            b.eventLoop(new NioEventLoop())
             .channel(new NioSocketChannel())
             .handler(new HttpSnoopClientInitializer(ssl))
             .remoteAddress(new InetSocketAddress(host, port));

            // Make the connection attempt.
            Channel ch = b.connect().sync().channel();

            // Prepare the HTTP request.
            HttpRequest request = new DefaultHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath());
            request.setHeader(HttpHeaders.Names.HOST, host);
            request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
            request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);

            // Set some example cookies.
            request.setHeader(
                    HttpHeaders.Names.COOKIE,
                    ClientCookieEncoder.encode(
                            new DefaultCookie("my-cookie", "foo"),
                            new DefaultCookie("another-cookie", "bar")));

            // Send the HTTP request.
            ch.write(request);

            // Wait for the server to close the connection.
            ch.closeFuture().sync();
        } finally {
            // Shut down executor threads to exit.
            b.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println(
                    "Usage: " + HttpSnoopClient.class.getSimpleName() +
                    " <URL>");
            return;
        }

        URI uri = new URI(args[0]);
        new HttpSnoopClient(uri).run();
    }
}
