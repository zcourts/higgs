package io.higgs.boson;

import com.google.common.base.Optional;
import io.higgs.RPCServer;
import io.higgs.boson.serialization.BosonDecoder;
import io.higgs.boson.serialization.BosonEncoder;
import io.higgs.boson.serialization.mutators.ReadMutator;
import io.higgs.boson.serialization.mutators.ReadWriteMutator;
import io.higgs.boson.serialization.mutators.WriteMutator;
import io.higgs.boson.serialization.v1.BosonReader;
import io.higgs.boson.serialization.v1.BosonWriter;
import io.higgs.events.ChannelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonServer extends RPCServer<BosonMessage, BosonMessage, ByteBuf> {
    protected boolean compression;
    protected boolean ssl;
    protected Set<WriteMutator> writeMutators = new HashSet<>();
    protected Set<ReadMutator> readMutators = new HashSet<>();

    public BosonServer(int port) {
        this(port, false);
        setEnableProtocolSniffing(false);
    }

    public BosonServer(int port, boolean compression) {
        this(port, compression, false);
    }

    public BosonServer(int port, boolean compression, boolean ssl) {
        super(port);
        this.compression = compression;
        this.ssl = ssl;
    }

    public Object[] getArguments(final Class<?>[] argTypes, ChannelMessage<BosonMessage> request) {
        return request.message.arguments;
    }

    protected BosonMessage newResponse(String methodName, ChannelMessage<BosonMessage> request,
                                       Optional<Object> returns, Optional<Throwable> error) {
        BosonMessage msg = new BosonMessage();
        msg.method = request.message.callback;
        Object[] args = new Object[2];
        args[0] = null;
        if (returns.isPresent()) {
            args[0] = returns.get();
        }
        args[1] = null;
        if (error.isPresent()) {
            args[1] = error.get();
        }
        msg.arguments = args;
        return msg;
    }

    @Override
    public ByteBuf serialize(final Channel ctx, final BosonMessage msg) {
        return new BosonWriter(readMutators, msg).serialize();
    }

    @Override
    public BosonMessage deserialize(final ChannelHandlerContext ctx, final ByteBuf msg) {
        return new BosonReader(writeMutators, msg).deSerialize();
    }

    @Override
    public String getTopic(final BosonMessage msg) {
        return msg.method;
    }

    @Override
    protected boolean setupPipeline(final ChannelPipeline pipeline) {
        pipeline.addLast("decoder", new BosonDecoder());
        pipeline.addLast("encoder", new BosonEncoder());
        return true; //auto add handler
    }

    public boolean addWriteMutator(WriteMutator mutator) {
        return mutator != null && writeMutators.add(mutator);
    }

    public boolean addReadMutator(ReadMutator mutator) {
        return mutator != null && readMutators.add(mutator);
    }

    /**
     * Add a mutator which can be used by both the serializer and de-serializer
     *
     * @param mutator the mutator to add
     * @return true if not null and  successfully added
     */
    public boolean addReadWriteMutator(ReadWriteMutator mutator) {
        return mutator != null && readMutators.add(mutator) && writeMutators.add(mutator);
    }
}
