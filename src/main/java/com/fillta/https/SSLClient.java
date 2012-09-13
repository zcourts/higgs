package com.fillta.https;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: poornachand
 * Date: Jul 26, 2012
 * Time: 11:12:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class SSLClient {

    private SSLConfiguration sslConfiguration;
    private String urlString;


    public void setSslConfiguration(SSLConfiguration sslConfiguration) {
        this.sslConfiguration = sslConfiguration;
    }

    public void setUrl(String urlString) {
        this.urlString = urlString;
    }

    public void parseArgs(String[] args) throws Exception {

        if (args.length / 2 != 0) {
            System.out.println("Number of arguments passed should be multiple of 2");
            throw new Exception("Number of arguments passed should be multiple of 2");
        }

        for (int i = 0; i < args.length; i++) {
            sslConfiguration = new SSLConfiguration();
            String paramName = args[i];
            if ("-url".equalsIgnoreCase(paramName.trim())) {
                urlString = args[++i];
            } else if ("-keystore".equalsIgnoreCase(paramName.trim())) {
                sslConfiguration.setKeyStorePath(args[++i].trim());
            } else if ("-keypassword".equalsIgnoreCase(paramName.trim())) {
                sslConfiguration.setKeyPassword(args[++i].trim());
            } else if ("-keystorepassword".equalsIgnoreCase(paramName.trim())) {
                sslConfiguration.setKeyStorePassword(args[++i].trim());
            } else if ("-truststore".equalsIgnoreCase(paramName.trim())) {
                sslConfiguration.setTrustStorePassword(args[++i].trim());
            } else if ("-truststorepassword".equalsIgnoreCase(paramName.trim())) {
                sslConfiguration.setTrustStorePassword(args[++i].trim());
            }
        }
    }


    public void execute() throws Exception {

        URI uri = null;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            System.out.println("Exception while parsing the url provided. Reason: " + e.getLocalizedMessage());
            throw e;
        }

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

        final boolean ssl = scheme.equalsIgnoreCase("https");

        Bootstrap b = new Bootstrap();

        try {
            b.group(new  NioEventLoopGroup())
                    .channel(new NioSocketChannel())
                    .remoteAddress(host, port)
                    .handler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();


                            SSLEngine engine =
                                    SSLContextFactory.getSSLSocket(sslConfiguration).createSSLEngine();
                            engine.setUseClientMode(true);

                            if(ssl)
                            pipeline.addLast("ssl", new SslHandler(engine));

                            pipeline.addLast("codec", new HttpClientCodec());
                            pipeline.addLast("chunked", new ChunkedWriteHandler());
                            pipeline.addLast("handler", new ResponseHandler());
                        }
                    });

            // Start the connection attempt.
            Channel ch = b.connect().sync().channel();

            // Sleep until all the key and trust store configurations loaded
            Thread.sleep(1000);

            String path = uri.getQuery() != null ? uri.getPath() + "?" + uri.getQuery() : uri.getPath();
            HttpRequest request = new DefaultHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, path);

            request.setHeader(HttpHeaders.Names.HOST, host);
            request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);

            ch.write(request);

            ch.closeFuture().sync();
        } finally {
            // The connection is closed automatically on shutdown.
            b.shutdown();
        }
    }

    public static void main(String[] args) {

        SSLClient sslClient = new SSLClient();


        SSLConfiguration sslConfiguration = new SSLConfiguration();

        // provide the truststore.ks file path
        sslConfiguration.setTrustStorePath("B:\\Courtney\\Desktop\\NettyHTTPS\\src\\main\\java\\keystores\\truststore.ks");
        sslConfiguration.setTrustStorePassword("zcourts");

        // provide the keystore.ks file path
        sslConfiguration.setKeyStorePath("B:\\Courtney\\Desktop\\NettyHTTPS\\src\\main\\java\\keystores\\keystore.ks");
        sslConfiguration.setKeyStorePassword("zcourts");
        sslConfiguration.setKeyPassword("zcourts");

//        sslClient.setUrl("https://graph.facebook.com/100000874785675");
        sslClient.setUrl("https://graph.facebook.com/me/feed?access_token=AAAC9iVp3fpoBAGuVHs63PfduHzKrZAMC88CavXOjTGKXFfIDZB76hXVWLlu48IZBZAVZAkELNdNQARBTv4w3hRs2sswWX5AV6maiCgzVC8QZDZD");
        sslClient.setSslConfiguration(sslConfiguration);

        try {
            // if input is passes through command pronpt, then use parseArgs method and call execute
//            pass the following jvm arguments while calling from command prompt.
//            -url https://graph.facebook.com/100000874785675 -keystore <keystorpath> -truststore <truststorepath>
//            -keypassword <key password> -keystorepassword <keystore password> -truststorepassword <truststore password>
//            sslClient.parseArgs(args);
            sslClient.execute();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
