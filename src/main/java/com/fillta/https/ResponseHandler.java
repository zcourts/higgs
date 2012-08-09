package com.fillta.https;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: poornachand
 * Date: Jul 26, 2012
 * Time: 11:55:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class ResponseHandler extends ChannelInboundMessageHandlerAdapter<DefaultHttpResponse> {

    private static final Logger logger = Logger.getLogger(
            ResponseHandler.class.getName());

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.log(
                Level.WARNING,
                "Unexpected exception from downstream.", cause);
        ctx.close();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, DefaultHttpResponse msg) throws Exception {
        System.out.println(msg);

        ByteBuf buffer = msg.getContent();
        String encoding = msg.getHeader("Content-Encoding");
        if (encoding != null && "gzip".equalsIgnoreCase(encoding.trim())){
            byte[] b = new byte[buffer.capacity()];
            buffer.getBytes(0, b);
            System.out.println(b.length);
            System.out.println(b.toString());
            GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(b));
            int t = -1;
            while ((t = in.read()) != -1) {
                System.out.print((char) t);
            }
        }else{
            System.out.println(buffer.toString(Charset.defaultCharset()));
        }

    }
}

