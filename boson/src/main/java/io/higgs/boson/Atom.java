package io.higgs.boson;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Smallest object that can be accelerated through a Beam.
 * On their own, they're virtually useless but when all associated Atoms are combined into a
 * {@link Particle} the particle becomes a useful event/object or message.
 * <p/>
 * An Atom is composed of bytes...duh - Atoms are interesting.
 * <p/>
 * The first 4 bytes are an integer representing the numeric ID of the particle the Atom belongs to.
 * The next next 2 bytes represent the size of the Atom. Sizes are limited so that if an Atom fails to
 * arrive at the next accelerator, it can request it be resent.
 * The next 4 bytes represent the sequence ID of the Atom within a particle.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Atom {
    protected final ByteBuf data = Unpooled.buffer();

    public Atom(long sequenceID, ByteBuf content) {
        //write id
        Varint.writeUnsignedVarLong(sequenceID, data);
        data.writeBytes(content);
    }
}
