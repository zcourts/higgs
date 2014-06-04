package io.higgs.http.server.protocol;

import io.higgs.core.func.Function1;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class MMappedDecoder extends MessageToMessageDecoder<HttpObject> {
    protected final String id;
    protected Path underlyingFile;
    protected MappedByteBuffer file;
    protected long bufferSize;
    protected HttpRequest request;
    protected HttpResponse response;
    protected FileChannel fileChannel;
    protected RandomAccessFile rand;
    protected DynamicMemoryMappedInputStream stream;

    public MMappedDecoder() {
        this.id = new UUID(System.nanoTime(), new Random().nextInt()).toString();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
        if (msg instanceof HttpRequest) {
            underlyingFile = Files.createTempFile("hs3-mapped-file-", "-" + id);
            rand = new RandomAccessFile(underlyingFile.toFile(), "rw");
            fileChannel = rand.getChannel();
            mapFile(null); //map file for the first time

            response = new HttpResponse();
            request = (HttpRequest) msg;
            out.add(request);
            request.setResponse(response);
            stream = new DynamicMemoryMappedInputStream(ctx, this, fileChannel);
            request.setInputStream(stream);
            cleanupOnClose(ctx);
        } else if (msg instanceof HttpContent) {
            HttpContent data = (HttpContent) msg;
            bufferSize += data.content().readableBytes();
            mapFile(data.content().nioBuffer()); //re-map the file from the last position limit/position
            data.content().readerIndex(data.content().writerIndex());
            if (data instanceof LastHttpContent) {
                stream.setDone(true);
            }
            notifyFileListeners();
        } else {
            //BAIL, we don't know what's happening here...
            throw new IllegalStateException("Unexpected data type, is the pipeline configure properly?"
                    + msg.getClass().getName());
        }
    }

    private void cleanupOnClose(ChannelHandlerContext ctx) {
        //delete mapped file when the connection closes
        ctx.channel().closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                notifyFileListeners();
                fileChannel.close();
                underlyingFile.toFile().delete(); //clean up when the connection dies
            }
        });
    }

    protected void mapFile(final ByteBuffer data) throws Exception {
        notifyFileListeners(); //notify anyone blocked/waiting on the old instance
        if (file == null) {
            file = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
            if (data != null) {
                file.put(data);
            }
            return;
        }
        syncAndRun(new Function1<MappedByteBuffer, Object>() {
            @Override
            public Object apply(MappedByteBuffer buffer) {
                int pos = file.position();
                try {
                    file = fileChannel.map(FileChannel.MapMode.READ_WRITE, pos, bufferSize);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                file.position(pos);
                if (data != null) {
                    file.put(data);
                }
                return null;
            }
        });
    }

    protected void notifyFileListeners() {
        if (file != null) {
            // every time we re-map the file because it's size increases, we discard the old instance
            //if a reader has blocked waiting on data to become available, it must be notified
            //so that it can be released from the wait on the old file and start reading from the new one
            //or acquire a new sync and wait on the new object
            synchronized (file) {
                //do a notify all instead of notify but there really should only ever be 1 thread reading
                file.notifyAll();
            }
        }
    }

    public void waitOnBuffer() {
        synchronized (file) {
            try {
                file.wait(100);
            } catch (InterruptedException ignored) {
                return;
            }
        }
    }

    public <T> T syncAndRun(Function1<MappedByteBuffer, T> fn) {
        synchronized (file) {
            return fn.apply(file);
        }
    }
}
