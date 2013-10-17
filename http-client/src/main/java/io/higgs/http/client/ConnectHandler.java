package io.higgs.http.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * @author Courtney Robinson <courtney.robinson@datasift.com>
 */
public class ConnectHandler extends SimpleChannelInboundHandler<Object> {
    protected final HttpRequest request;
    protected final boolean tunneling;
    protected final SimpleChannelInboundHandler<Object> handler;
    private final InitFactory factory;

    public ConnectHandler(boolean ssl, HttpRequest request, SimpleChannelInboundHandler<Object> handler,
                          InitFactory factory) {
        this.tunneling = ssl;
        this.request = request;
        this.handler = handler;
        this.factory = factory;
    }

    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            //http://tools.ietf.org/html/rfc2817#section-5.2
            //rfc 2817 - 2xx status means we can proceed, the proxy has establish a connection
            int code = res.getStatus().code();
            if (code > 199 && code < 300) {
                if (tunneling) {
                    //add an SSL handler to the front of the pipeline
                    ClientIntializer.addSSL(ctx.pipeline(), true);
                }
            } else {
                throw new ProxyConnectionException("Proxy server indicated it was unable to establish a secure " +
                        "connection to the origin server", res);
            }
        }
        if (msg instanceof LastHttpContent) {
            //remove the connect handler so future responses go to the other handler
            ctx.pipeline().remove(this);
            ChannelPipeline pipeline = ctx.pipeline();
            factory.newInstance(false, handler, null).configurePipeline(pipeline);
            writeOriginalRequest(ctx);
        }
    }

    protected void writeOriginalRequest(ChannelHandlerContext ctx) {
        //now write the original request
        ctx.channel().writeAndFlush(request);
    }

    public static interface InitFactory {
        ClientIntializer newInstance(boolean ssl, SimpleChannelInboundHandler<Object> handler, ConnectHandler ch);
    }
}
