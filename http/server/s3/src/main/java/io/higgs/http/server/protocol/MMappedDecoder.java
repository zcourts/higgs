package io.higgs.http.server.protocol;

import io.higgs.core.func.Function1;
import io.higgs.http.server.HttpRequest;
import io.higgs.http.server.HttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.IOException;
import java.io.RandomAccessFile;
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
public class MMappedDecoder extends ByteToMessageDecoder {
    protected final String id;
    protected Path underlyingFile;
    protected MappedByteBuffer file;
    protected long bufferSize;
    protected HttpRequest request;
    protected HttpResponse response;
    private FileChannel fileChannel;
    RandomAccessFile rand;

    public MMappedDecoder() {
        this.id = new UUID(System.nanoTime(), new Random().nextInt()).toString();
    }

    public MMappedDecoder(long bufferSize) {
        this();
        this.bufferSize = bufferSize;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 1) {
            return;
        }
        bufferSize += in.readableBytes();
        if (underlyingFile == null) {
            underlyingFile = Files.createTempFile("hs3-mapped-file-", "-" + id);
            rand = new RandomAccessFile(underlyingFile.toFile(), "rw");
            fileChannel = rand.getChannel();
            mapFile(0); //map file for the first time

            response = new HttpResponse();
            request = new HttpRequest(new DynamicMemoryMappedInputStream(ctx, this, fileChannel), response);
            out.add(request);
            //delete mapped file when the connection closes
            ctx.channel().closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    notifyFileListeners();
                    fileChannel.close();
                    underlyingFile.toFile().delete(); //clean up when the connection dies
                }
            });
        } else {
            mapFile(file.limit()); //re-map the file from the last position limit/position
        }
        synchronized (file) {
            file.put(in.nioBuffer());
        }
        in.readerIndex(in.writerIndex());
    }

    protected void mapFile(int pos) throws IOException {
        notifyFileListeners();
        if (file == null) {
            file = fileChannel.map(FileChannel.MapMode.READ_WRITE, pos, bufferSize);
        } else {
            synchronized (file) {
                file = fileChannel.map(FileChannel.MapMode.READ_WRITE, pos, bufferSize);
                file.position(pos);
            }
        }
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

    @Override
    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved0(ctx);
    }

    public void waitOnBuffer() {
        synchronized (file) {
            try {
                file.wait();
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
