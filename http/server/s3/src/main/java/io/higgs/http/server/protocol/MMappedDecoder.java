package io.higgs.http.server.protocol;

import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.UUID;

//import static io.netty.handler.codec.http.HttpHeaders.*;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class MMappedDecoder extends MessageToMessageDecoder<HttpObject> {
    public static final int ONE_MB = 1048576;
    protected final String id;
    protected Path underlyingFile;
    protected MappedByteBuffer file;
    protected HttpRequest request;
    protected HttpResponse response;
    protected FileInputStream stream;
    protected FileOutputStream out;
    protected ByteBufInputStream byteInputStream;

    public MMappedDecoder() {
        this.id = new UUID(System.nanoTime(), new Random().nextInt()).toString();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
        if (msg instanceof HttpRequest) {
            response = new HttpResponse();
            request = (HttpRequest) msg;
            out.add(request);
            request.setResponse(response);
            //todo if content length is more than 1MB then don't send to file just do an in memory buffer
            //for that we need a blocking `ByteBuf implementation to feed to the ByteBufInputStream
            //which will block on read until we tell it LastHttpContent is received...
            //if (isContentLengthSet(request) && getContentLength(request) >= ONE_MB)
            underlyingFile = Files.createTempFile("hs3-mapped-file-", "-" + id);
            this.out = new FileOutputStream(underlyingFile.toFile());
            stream = new FileInputStream(underlyingFile.toFile());
            request.setInputStream(stream);
            cleanupOnClose(ctx);
        } else if (msg instanceof HttpContent) {
            HttpContent data = (HttpContent) msg;
            byte[] o = new byte[data.content().readableBytes()];
            data.content().readBytes(o);
            this.out.write(o);
            if (msg instanceof LastHttpContent) {
                this.out.close();
            }
            data.content().readerIndex(data.content().writerIndex());
        } else {
            //BAIL, we don't know what's happening here...
            throw new IllegalStateException("Unexpected data type, is the pipeline configure properly?"
                    + msg.getClass().getName());
        }
    }

    private void cleanupOnClose(ChannelHandlerContext ctx) {
        ctx.channel().closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                underlyingFile.toFile().delete(); //clean up when the connection dies
            }
        });
    }
}
