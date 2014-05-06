package io.higgs.boson.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import io.higgs.boson.BosonMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class KryoWriter {
    protected final BosonMessage msg;
    private Kryo kryo = new Kryo();

    public KryoWriter(BosonMessage msg) {
        this.msg = msg;
        kryo.register(ArrayList.class);
    }

    public ByteBuf serialize() {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufOutputStream stream = new ByteBufOutputStream(buffer);
        Output out = new Output(stream);
        kryo.writeObject(out, msg);
        out.close();
        return Unpooled.buffer()
                .writeInt(buffer.writerIndex())
                .writeBytes(buffer);
    }
}
