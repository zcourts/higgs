package io.higgs.http.client.future;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class Reader<T> {
    protected final Logger log = LoggerFactory.getLogger(Reader.class.getName());
    protected static final Charset utf8 = Charset.forName("UTF-8");
    protected ByteBuf buffer = Unpooled.buffer();
    protected ByteBufInputStream data = new ByteBufInputStream(buffer);
    protected Set<Function<T>> functions = new HashSet<>();
    private boolean completed;

    public Reader() {
    }

    public Reader(Function<T> function) {
        if (function == null) {
            throw new IllegalArgumentException("Function cannot be null, use another constructor");
        }
        listen(function);
    }

    /**
     * Invoked each time a block of data is received
     *
     * @param data the data
     */
    public abstract void data(ByteBuf data);

    /**
     * Called once at the end of a stream when all data is received
     */
    public abstract void done();

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            done();
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    /**
     * @param function Adds a function to be invoked by this reader
     */
    public void listen(Function<T> function) {
        if (function != null) {
            functions.add(function);
        }
    }

}
