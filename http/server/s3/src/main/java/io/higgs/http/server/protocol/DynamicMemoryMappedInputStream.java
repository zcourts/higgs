package io.higgs.http.server.protocol;

import io.higgs.core.func.Function1;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Jersey {@link javax.ws.rs.ext.Provider}s require an {@link java.io.InputStream}.
 * Netty by its nature just doesn't map will to that being completely async.
 * <p/>
 * This implementation relies on a Netty connection (implying TCP based) being alive.
 * For as long as the connection is alive, it is valid to call {@link #read()}.
 * <p/>
 * When this happens, if no data is available to be read, the read operation will block.
 * The implication being that the reader must be on a different thread to the producer
 * creating the memory mapped file.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DynamicMemoryMappedInputStream extends InputStream {

    protected final ChannelHandlerContext ctx;
    protected final MMappedDecoder decoder;
    protected final FileChannel fileChannel;
    /**
     * The position of the current read offset
     */
    protected int readPosition;
    protected int markedPosition;

    public DynamicMemoryMappedInputStream(ChannelHandlerContext ctx, MMappedDecoder decoder, FileChannel fc) {
        this.ctx = ctx;
        this.decoder = decoder;
        this.fileChannel = fc;
    }

    @Override
    public long skip(final long n) throws IOException {
        return decoder.syncAndRun(new Function1<MappedByteBuffer, Long>() {
            @Override
            public Long apply(MappedByteBuffer buffer) {
                long l = Math.min(n, available());
                readPosition += l;
                return l;
            }
        });
    }

    @Override
    public int available() {
        return decoder.syncAndRun(new Function1<MappedByteBuffer, Integer>() {
            @Override
            public Integer apply(MappedByteBuffer buffer) {
                return buffer.limit() - readPosition;
            }
        });
    }

    @Override
    public void close() throws IOException {
        fileChannel.close();
//        ctx.close(); //????
    }

    @Override
    public synchronized void mark(int readlimit) {
        //ignore read limit, the data is in a file so we don't care
        markedPosition = readPosition;
    }

    @Override
    public synchronized void reset() throws IOException {
        readPosition = markedPosition;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    public int read() throws IOException {
        if (!ctx.channel().isOpen()) {
            return -1;
        }
        return decoder.syncAndRun(new Function1<MappedByteBuffer, Integer>() {
            @Override
            public Integer apply(MappedByteBuffer buf) {
                if (available() < 1) {
                    decoder.waitOnBuffer();
                    try {
                        return read();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return buf.get(readPosition++) & 0xFF;
            }
        });
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, readPosition, b.length);
    }

    public int read(final byte[] bytes, final int offset, final int length) throws IOException {
        if (!ctx.channel().isOpen()) {
            return -1;
        }
        return decoder.syncAndRun(new Function1<MappedByteBuffer, Integer>() {
            @Override
            public Integer apply(MappedByteBuffer buffer) {
                int l = Math.min(length, available());
                if (available() < 1) {
                    decoder.waitOnBuffer();
                    try {
                        return read(bytes, offset, l);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                buffer.get(bytes, offset, l);
                return l;
            }
        });
    }
}
