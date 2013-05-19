package io.higgs.http.client.future;

import io.netty.buffer.ByteBuf;

/**
 * This collects a response stream in memory and then converts it to a string when the entire stream
 * is received.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class PageReader extends Reader<String> {
    public PageReader(Function<String> function) {
        super(function);
    }

    public PageReader() {
    }

    @Override
    public void data(ByteBuf data) {
        buffer.writeBytes(data);
    }

    @Override
    public void done() {
        if (buffer.writerIndex() > 0) {
            for (Function<String> function : functions) {
                function.apply(buffer.toString(0, buffer.writerIndex(), utf8));
            }
            //we read the entire stream
            buffer.readerIndex(buffer.writerIndex());
        }
    }
}
