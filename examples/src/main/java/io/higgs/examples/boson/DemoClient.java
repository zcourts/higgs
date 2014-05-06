package io.higgs.examples.boson;

import io.higgs.boson.serialization.mutators.ReadMutator;
import io.higgs.boson.serialization.mutators.WriteMutator;
import io.higgs.boson.serialization.v1.BosonReader;
import io.higgs.boson.serialization.v1.BosonWriter;
import io.netty.buffer.ByteBuf;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class DemoClient {
    protected DemoClient() {
    }

    public static void main(String... args) {
        Set<WriteMutator> writeMutators = new HashSet<>();
        Set<ReadMutator> readMutators = new HashSet<>();

        //
        PoloExample poloExample = new PoloExample();
        Nested nested = new Nested();
        //
        BosonWriter writer = new BosonWriter(readMutators);
        BosonReader reader = new BosonReader(writeMutators);

        ByteBuf serializedPolo = writer.serialize(poloExample);
        PoloExample deserializedPolo = reader.deSerialize(serializedPolo);

        System.out.println(poloExample);
        System.out.println(deserializedPolo);
    }
}
