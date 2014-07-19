package io.higgs.events;


import io.higgs.core.InvokableMethod;
import io.higgs.core.StaticUtil;
import io.higgs.core.func.Function1;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Events {
    private static NonBlockingHashMap<String, Events> groups = new NonBlockingHashMap<>();
    protected final LocalAddress address;
    private final EventLoopGroup group;
    private final List<SingletonFactory> registeredFactories = new ArrayList<>();
    protected EventServer server;
    protected NonBlockingHashMap<String, Channel> channels = new NonBlockingHashMap<>();

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

    /**
     * Create a new event group with it's own set of resources
     *
     * @param name the name of this group
     * @return a new group if one with the same name doesn't already exist in which case the existing one is returned
     */
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
     * Execute a task in the event loop
     *
     * @param task the task
     * @param <T>
     */
    public <T> void execute(final Task task) {
        eventLoop().submit(new Runnable() {
            public void run() {
                task.apply();
            }
        });
    }

    /**
     * @return The event loop used to schedule tasks
     */
    public EventLoop eventLoop() {
        return server.channel().eventLoop();
    }

    /**
     * @return the server used for pub-sub events  in this event group
     */
    public EventServer server() {
        return server;
    }

    /**
     * @deprecated
     */
    public <A> void on(Function1<A> function, final String... events) {
        Event[] e = new Event[events.length];
        for (int i = 0; i < events.length; i++) {
            final int finalI = i;
            e[i] = new Event() {
                public String name() {
                    return events[finalI];
                }
            };
        }
        on(function, e);
    }

    /**
     * Note that by providing a function its {@link Function1#apply(Object)} method
     * can be invoked from multiple threads
     *
     * @param function the function to invoke for these events
     * @param events   a set of events to subscribe the function to
     * @param <A>      the type the function accepts, only events matching this type will cause invocation
     */
    public <A> void on(Function1<A> function, Event... events) {
        if (events.length == 0) {
            throw new IllegalArgumentException("At least one event is required");
        }
        for (Event event : events) {
            server.registerMethod(new FunctionEventMethod<>(event.name(), function));
        }
    }

    /**
     * Subscribe all eligible class's in this package for events
     *
     * @param pkg
     */
    public void subscribe(Package pkg) {
        server.registerPackage(pkg);
    }

    /**
     * Subscribe all eligible class's in this package AND it's sub packages for events
     * An eligible class is any class where at least one of it's methods is annotated with {@link io.higgs.core.method}
     *
     * @param pkg
     */
    public void subscribeAll(Package pkg) {
        server.registerPackageAndSubpackages(pkg);
    }

    public void subscribe(Class<?> klass) {
        server.registerClass(klass);
    }

    /**
     * @param instance an instance to register
     */
    public void subscribe(Object instance) {
        if (instance == null) {
            throw new IllegalArgumentException("cannot register null instance");
        }
        SingletonFactory factory = new SingletonFactory(server, instance);
        registeredFactories.add(factory);
        server.registerObjectFactory(factory);
    }

    /**
     * Unsubscribe all referentially equal subscriptions of the instance provided.
     * If the same instance was subscribed multiple times, ALL  subscriptions will be removed
     * i.e. where subscribedObject == instance ONLY
     * That is to say, even if subscribedObject.equals(instance) would return true
     * an instance is only removed if subscribedObject == instance (ref)
     *
     * @param instance the instance to unsubscribe
     */
    public void unsubscribe(Object instance) {
        for (SingletonFactory f : registeredFactories) {
            if (f.instance() == instance) {
                server.deRegister(f);
            }
        }
    }

    /**
     * Try to use the type safe {@link #emit(Event, Object...)} instead
     *
     * @deprecated
     */
    public ChannelFuture emit(final String event, Object... param) {
        return emit(new Event() {
            @Override
            public String name() {
                return event;
            }
        }, param);
    }

    /**
     * Emit an event with the given name and parameters
     *
     * @param event thhe name of the event
     * @param param one or more parameters to pass to subscribers
     * @return a future which will be notified when the event has finished, been cancelled or had an error
     */
    public ChannelFuture emit(Event event, Object... param) {
        if (event == null || event.name() == null) {
            throw new IllegalArgumentException("event name cannot be null");
        }
        Channel channel = channels.get(event.name());
        //make sure the channel is writable
        if (channel != null && !channel.isActive()) {
            channel = null;
        }
        if (channel == null) {
            channel = bootstrapChannel();
            channels.put(event.name(), channel);
        }
        return StaticUtil.write(channel, new EventMessage(event.name(), param));
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
