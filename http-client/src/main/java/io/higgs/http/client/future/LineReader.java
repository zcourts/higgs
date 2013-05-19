package io.higgs.http.client.future;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * Buffers a response stream in memory until an entire line is received.
 * Each subscribed callback is invoked once for every line received and 1 last time at the end of  a stream
 * when the remaining contents may or may not have ended with a end of line delimiter
 * <p/>
 * This was created to support services that keep connections open and write line based data.
 * The Twitter streaming API for example...
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class LineReader extends PageReader {
    public LineReader(Function<String> function) {
        super(function);
    }

    public LineReader() {
    }

//NOTE: we don't override done because it is required to be invoked once
// everything is received to ensure entire content is read

    public void writeLine(String line) {
        for (Function<String> function : functions) {
            function.apply(line);
        }
    }

    @Override
    public void data(ByteBuf content) {
        super.data(content);
        String line;
        try {
            while ((line = data.readLine()) != null) {
                writeLine(line);
            }
            buffer.discardReadBytes();
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }
}
