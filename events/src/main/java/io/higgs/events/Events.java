package io.higgs.events;


import io.higgs.core.InvokableMethod;
import io.higgs.core.func.Function1;
import io.higgs.events.demo.ClassExample;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

import java.util.LinkedList;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Events {
    private static NonBlockingHashMap<String, Events> groups = new NonBlockingHashMap<>();
    protected EventServer server;
    protected NonBlockingHashMap<String, Channel> channels = new NonBlockingHashMap<>();
    protected final LocalAddress address;
    private final EventLoopGroup group;

    public Events(String groupName) {
        address = new LocalAddress(groupName);
        group = new LocalEventLoopGroup();
        server = new EventServer(address);
        server.registerMethodProcessor(new EventMethodProcessor());
        server.start();
    }

    public static Events get() {
        return group("*");
    }

    public static Events group(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Group name is required");
        }
        Events group = groups.get(name);
        if (group == null) {
            group = new Events(name);
            groups.put(name, group);
        }
        return group;
    }

    /**
     * Note that by providing a function its {@link Function1#apply(Object)} method
     * can be invoked from multiple threads
     *
     * @param function the function to invoke for these events
     * @param events   a set of events to subscribe the function to
     * @param <A>      the type the function accepts, only events matching this type will cause invocation
     */
    public <A> void on(Function1<A> function, String... events) {
        for (String event : events) {
            server.registerMethod(new FunctionEventMethod<>(event, function));
        }
    }

    public void subscribe(Class<ClassExample> klass) {
        server.registerClass(klass);
    }

    /**
     * @param instance an instance to register
     */
    public void subscribe(Object instance) {
        if (instance == null) {
            throw new IllegalArgumentException("cannot register null instance");
        }
        server.registerObjectFactory(new RandomFactory(server, instance));
    }

    /**
     * Emit an event with the given name and parameters
     *
     * @param event thhe name of the event
     * @param param one or more parameters to pass to subscribers
     * @return a future which will be notified when the event has finished, been cancelled or had an error
     */
    public ChannelFuture emit(String event, Object... param) {
        if (event == null) {
            throw new IllegalArgumentException("event name cannot be null");
        }
        Channel channel = channels.get(event);
        //make sure the channel is writable
        if (channel != null && !channel.isActive()) {
            channel = null;
        }
        if (channel == null) {
            channel = bootstrapChannel();
            channels.put(event, channel);
        }
        return channel.write(new Event(event, param));
    }

    private Channel bootstrapChannel() {
        Bootstrap cb = new Bootstrap();
        cb.group(group)
                .channel(LocalChannel.class)
                .handler(new ChannelInitializer<LocalChannel>() {
                    @Override
                    public void initChannel(LocalChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                // new LoggingHandler(LogLevel.ERROR),
                                new EventHandler(new LinkedList<InvokableMethod>()));
                    }
                });

        // Start the client.
        return cb.connect(address).syncUninterruptibly().channel();
    }
}
