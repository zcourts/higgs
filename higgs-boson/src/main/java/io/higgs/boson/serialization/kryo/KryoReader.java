package io.higgs.boson.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import io.higgs.boson.BosonMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.util.ArrayList;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class KryoReader {
    protected ByteBuf data;
    private Kryo kryo = new Kryo();

    public KryoReader(ByteBuf msg) {
        this.data = msg;
        kryo.register(ArrayList.class);
    }

    public BosonMessage deSerialize() {
        ByteBufInputStream stream = new ByteBufInputStream(data);
        Input in = new Input(stream);
        return kryo.readObject(in, BosonMessage.class);
    }
}
