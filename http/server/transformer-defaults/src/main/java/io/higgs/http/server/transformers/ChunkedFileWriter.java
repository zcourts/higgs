package io.higgs.http.server.transformers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.stream.ChunkedInput;

import java.io.InputStream;

/**
 * Used in place of Netty's {@link io.netty.handler.stream.ChunkedFile} so that we can use a pure input stream
 * as opposed to file objects.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ChunkedFileWriter implements ChunkedInput<ByteBuf> {
    protected InputStream stream;
    protected int chunkSize;

    public ChunkedFileWriter(InputStream in, int chunkSize) {
        this.stream = in;
        this.chunkSize = chunkSize;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
        return stream.available() > 0;
    }

    @Override
    public void close() throws Exception {
        stream.close();
    }

    @Override
    public ByteBuf readChunk(ChannelHandlerContext ctx) throws Exception {
        ByteBuf buf = ctx.alloc().heapBuffer(chunkSize);
        boolean release = true;
        try {
            int read = stream.read(buf.array());
            buf.writerIndex(read);
            release = false;
            return buf;
        } finally {
            if (release) {
                buf.release();
            }
        }
    }
}
