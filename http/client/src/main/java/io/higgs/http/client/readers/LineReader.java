package io.higgs.http.client.readers;

import io.higgs.core.func.Function2;
import io.higgs.http.client.Response;
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
    public LineReader(Function2<String, Response> function) {
        super(function);
    }

    public LineReader() {
    }

//NOTE: we don't override done because it is required to be invoked once
// everything is received to ensure entire content is read

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

    public void writeLine(String line) {
        for (Function2<String, Response> function : functions) {
            function.apply(line, response);
        }
    }
}
