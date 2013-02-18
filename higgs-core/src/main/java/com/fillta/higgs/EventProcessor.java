package com.fillta.higgs;

import com.fillta.functional.Function1;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.events.HiggsEvent;
import com.fillta.higgs.events.listeners.ChannelEventListener;
import com.fillta.higgs.queueingStrategies.CircularBufferQueueingStrategy;
import com.fillta.higgs.queueingStrategies.LinkedBlockingQueueStrategy;
import com.fillta.higgs.queueingStrategies.QueueingStrategy;
import com.fillta.higgs.queueingStrategies.SameThreadQueueingStrategy;
import com.fillta.higgs.ssl.SSLConfigFactory;
import com.fillta.higgs.ssl.SSLContextFactory;
import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.InternalLoggerFactory;
import io.netty.util.internal.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.fillta.higgs.events.HiggsEvent.CHANNEL_ACTIVE;
import static com.fillta.higgs.events.HiggsEvent.CHANNEL_INACTIVE;
import static com.fillta.higgs.events.HiggsEvent.CHANNEL_REGISTERED;
import static com.fillta.higgs.events.HiggsEvent.CHANNEL_UNREGISTERED;
import static com.fillta.higgs.events.HiggsEvent.EXCEPTION_CAUGHT;
import static com.fillta.higgs.events.HiggsEvent.MESSAGE_RECEIVED;

/**
 * @param <T>  Type of the topic this event processor is for
 * @param <OM> The outgoing message type e.g. HTTPRequest
 * @param <IM> The incoming message type e.g. HTTPResponse
 * @param <SM> The serialized form of both messages, typically {@code byte[]} or more efficiently
 *             {@link io.netty.buffer.ByteBuf} since byte[] gets converted to it anyway...
 * @author Courtney Robinson <courtney@crlog.info>
 */
@ChannelHandler.Sharable
public abstract class EventProcessor<T, OM, IM, SM> extends ChannelInboundMessageHandlerAdapter<SM> {
    static {
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
    }

    protected Logger log = LoggerFactory.getLogger(getClass());
    protected Logger errorLogger = LoggerFactory.getLogger("exceptions");
    public static final AttributeKey<Object> REQUEST_KEY = new AttributeKey<>("event-processor-request-key");
    /**
     * One of the worse errors/bugs to have is a thread terminating because of an uncaught exception.
     * It leaves no indication of what happened and sometimes the error happens on a thread you don't
     * have control over, which means you cannot directly catch the exception.
     * This field is an alternative to just letting it die. It catches uncaught exceptions
     * and triggers a normal {@link com.fillta.higgs.events.HiggsEvent}.EXCEPTION_CAUGHT.
     * As such all exceptions can be caught and handled (logged etc) making it easier to debug issues
     * related to this.
     */
    protected Thread.UncaughtExceptionHandler unhandledExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            emit(EXCEPTION_CAUGHT, null, Optional.of(e));
        }
    };
    public static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    public static int maxThreads = availableProcessors;
    private static ThreadPoolExecutor threadPool;
    /**
     * A set of event listeners that will be notified when a given event occurs.
     * NOTE: used by multiple threads so the set's backing Map must be thread safe
     */
    protected Map<HiggsEvent, Set<ChannelEventListener>> eventSubscribers = new ConcurrentHashMap<>();
    protected QueueingStrategy<T, IM> queueingStrategy;
    private AtomicBoolean daemonThreadPool = new AtomicBoolean();
    protected final Set<HiggsInterceptor> interceptors =
            Collections.newSetFromMap(new ConcurrentHashMap<HiggsInterceptor, Boolean>());
    private boolean errorLoggerEnabled = true;

    public EventProcessor() {
        queueingStrategy = messageQueue(threadPool);
        Thread.setDefaultUncaughtExceptionHandler(unhandledExceptionHandler);
    }

    /**
     * @return A fixed size thread pool. where pool size = {@link #maxThreads}
     */
    public ThreadPoolExecutor threadPool() {
        if (threadPool == null) {
            threadPool = new ThreadPoolExecutor(
                    //minimum of 1 core thread
                    Math.max(1, maxThreads / 2),
                    //up to this many threads may be used
                    maxThreads,
                    30000L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                private AtomicInteger totalThreads = new AtomicInteger();

                public Thread newThread(final Runnable r) {
                    Thread thread = new Thread(r, getClass().getName() + "-higgs-" + totalThreads.getAndIncrement());
                    thread.setDaemon(daemonThreadPool.get());
                    Thread.setDefaultUncaughtExceptionHandler(unhandledExceptionHandler);
                    return thread;
                }
            });
            threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        }
        return threadPool;
    }

    /**
     * Provide a queueing strategy for the event processor.
     * By default this uses {@link com.fillta.higgs.queueingStrategies.SameThreadQueueingStrategy} which will
     * not use a queue and instead invoke all listeners immediately on the inbound messages
     * thread.
     *
     * @param threadPool A configured thread pool for the message queue to use if required.
     * @return the queueing strategy used to process events
     */
    protected QueueingStrategy<T, IM> messageQueue(@SuppressWarnings("UnusedParameters")
                                                   ExecutorService threadPool) {
        //noinspection unchecked
        return new SameThreadQueueingStrategy(queueingStrategy);
    }

    /**
     * Sets the Queueing strategy used to process incoming messages.
     * For a detailed explanation see the documentation of {@link QueueingStrategy}
     *
     * @param strategy the strategy to use.
     * @param copy     If true then existing subscribers from {@link #queueingStrategy} is copied to
     *                 the given strategy before it is replaced
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setQueueingStrategy(QueueingStrategy<T, IM> strategy, boolean copy) {
        if (strategy != null) {
            if (copy) {
                strategy.copy(queueingStrategy);
            }
            queueingStrategy = strategy;
        }
    }

    /**
     * Set the queueing strategy to use a {@link com.fillta.higgs.buffer.CircularBuffer}
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setQueueingStrategyAsCircularBuffer() {
        queueingStrategy = new CircularBufferQueueingStrategy<>(queueingStrategy, threadPool());
    }

    /**
     * Set the queueing strategy to use a {@link java.util.concurrent.LinkedBlockingQueue}
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setQueueingStrategyAsBlockingQueue() {
        queueingStrategy = new LinkedBlockingQueueStrategy<>(queueingStrategy, threadPool());
    }

    public void emit(HiggsEvent event, ChannelHandlerContext context, Optional<Throwable> ex) {
        Set<ChannelEventListener> set = eventSubscribers.get(event);
        if (set != null) {
            for (ChannelEventListener l : set) {
                if (l != null) {
                    l.triggered(context, ex);
                }
            }
        } else {
            if (ex.isPresent()) {
                log.warn("Unhandled exception and no exception handlers registered", ex.get());
            }
        }
    }

    public void emitMessage(ChannelHandlerContext ctx, SM msg) {
        //always associate the request with the channel
        ctx.channel().attr(REQUEST_KEY).set(msg);
        boolean intercepted = false;
        for (HiggsInterceptor interceptor : interceptors) {
            if (interceptor.matches(msg)) {
                intercepted = interceptor.intercept(ctx, msg);
                if (intercepted) {
                    break;
                }
            }
        }
        if (!intercepted) {
            IM imsg = deserialize(ctx, msg);
            //if de-serializer returns null then do not queue
            if (imsg != null) {
                T topic = getTopic(imsg);
                queueingStrategy.enqueue(ctx, new DecodedMessage<>(topic, imsg));
            }
        }
    }

    /**
     * When an exception occurs it is not always possible to know which request caused it.
     * The only thing that is always accessible is the channel, to get access to the request
     * we can associate data with each channel. In this case they data associated is the request
     * object. To get the request you need to something similar to:
     * <pre>
     * <code>
     * EventProcessor.onException(new ChannelEventListener() {
     *     public void triggered(final ChannelHandlerContext ctx, final Optional<Throwable> ex) {
     *     Attribute<Object> request = ctx.channel().attr(EventProcessor.REQUEST_KEY);
     *         if (request.get() != null && request instanceof MyRequestType) {
     *         //do something clever
     *         }
     *     }
     * })
     * </code>
     * </pre>
     * The {@link EventProcessor} guarantees that it will always set the request object.
     * You must always check the type of the object returned.
     * A {@link HiggsInterceptor} might add an unexpected type which would result in a ClassCastException. For
     * e.g. The HS3 implementation expects an {@link HttpRequest} but the WebSocket server uses an interceptor
     * and adds {@link TextWebSocketFrame}...clearly not compatible. Always check the request is of the
     * expected type before casting.
     */
    public Object getRequest(Channel channel) {
        if (channel == null) {
            return null;
        }
        Attribute<Object> request = channel.attr(REQUEST_KEY);
        return request.get();
    }

    /**
     * Registers an interceptor to this even processor
     *
     * @param interceptor the interceptor to add
     * @param <T>         any interceptor sub type
     */
    public <T extends HiggsInterceptor> void addInterceptor(T interceptor) {
        if (interceptor == null) {
            throw new NullPointerException("Cannot add a null interceptor");
        }
        interceptors.add(interceptor);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onException(ChannelEventListener listener) {
        on(HiggsEvent.EXCEPTION_CAUGHT, listener);
    }

    public <E extends ChannelEventListener> void on(HiggsEvent event, E listener) {

        Set<ChannelEventListener> set = eventSubscribers.get(event);
        if (set == null) {
            //important: concurrent hash map used because events can be triggered from multiple threads
            //this means reading the set from multiple threads
            set = Collections.newSetFromMap(new ConcurrentHashMap<ChannelEventListener, Boolean>());
            eventSubscribers.put(event, set);
        }
        set.add(listener);
    }

    /**
     * Subscribes the given function to the specified topic
     *
     * @param topic    The topic to listen for
     * @param function The function to be invoked when a message for the given topic is received
     */
    public void listen(T topic, final Function1<ChannelMessage<IM>> function) {
        queueingStrategy.listen(topic, function);
    }

    /**
     * Un-subscribe the given function under the given topic
     *
     * @param topic    the topic the function is subscribed to
     * @param function the function to be removed
     */
    public void unsubscribe(T topic, Function1<ChannelMessage<IM>> function) {
        queueingStrategy.remove(topic, function);
    }

    /**
     * Subscribes the given function to <em>all</em> messages/events.
     *
     * @param function The function should return true if it uses the messages it receives
     */
    @SuppressWarnings("UnusedDeclaration")
    public void listen(Function1<ChannelMessage<IM>> function) {
        queueingStrategy.listen(function);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void unsubscribeAll(T topic) {
        queueingStrategy.removeAll(topic);
    }

    public boolean listening(T topic) {
        return queueingStrategy.listening(topic);
    }

    /**
     * @param c   the channel the response is written to
     * @param obj the response object
     * @return The write future. If you won't be writing any more and the connection won;t be needed
     *         use .addListener(ChannelFutureListener.CLOSE) to close the connection.
     */
    public ChannelFuture respond(Channel c, OM obj) {
        return c.write(serialize(c, obj));
    }

    /**
     * Convert a message from its outgoing message "OM" format
     * into its serialized message "SM" format
     *
     * @param ctx Netty's channel handler context
     * @param msg the message to be converted
     * @return the "serialized" form of the given message
     */
    public abstract SM serialize(Channel ctx, OM msg);

    /**
     * Convert a message from its serialized form into the incoming message, "IM" form.
     *
     * @param ctx The Netty channel context
     * @param msg the serialized message
     * @return The converted message  OR null if the message is not complete and
     *         should not be queued to pass to listeners yet
     */
    public abstract IM deserialize(ChannelHandlerContext ctx, SM msg);

    /**
     * Given an incoming message,extract the message's topic.
     * If used in a multi-threaded Queueing strategy this factory should not access un-synchronized
     * shared resources.
     */
    public abstract T getTopic(IM msg);

    @SuppressWarnings("UnusedDeclaration")
    public void setDaemonThreadPool(final boolean daemonThreadPool) {
        this.daemonThreadPool.set(daemonThreadPool);
    }

    /**
     * Create a new initializer for this {@link EventProcessor} to use to configure a pipeline
     *
     * @param ssl        if true then the initializer automatically adds "ssl" to the pipeline.
     * @param clientMode if true and the ssl parameter is true then the {@link SSLEngine}
     *                   is configure in client mode.
     * @return a new initializer
     */
    protected ChannelInitializer<SocketChannel> newInitializer(final boolean ssl,
                                                               final boolean clientMode) {
        return new ChannelInitializer<SocketChannel>() {
            public void initChannel(final SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                if (ssl) {
                    SSLEngine engine = newSSLEngine(clientMode);
                    pipeline.addLast("ssl", new SslHandler(engine));
                }
                beforeSetupPipeline(pipeline);
                boolean addHandler = setupPipeline(pipeline);
                afterSetupPipeline(pipeline);
                if (pipeline.get("handler") == null && addHandler) {
                    pipeline.addLast("handler", EventProcessor.this);
                }
            }
        };
    }

    /**
     * Provides a callback before {@link #setupPipeline(ChannelPipeline)} is called
     * allowing implementers to manipulate the pipeline before
     *
     * @param pipeline the pipeline to be configured
     */
    protected void beforeSetupPipeline(final ChannelPipeline pipeline) {
    }

    /**
     * Provides a callback after {@link #setupPipeline(ChannelPipeline)} is called
     * allowing implementers to manipulate the pipeline before {@link #EventProcessor}
     * attempts to detect and add a "handler" to the pipeline
     *
     * @param pipeline the pipeline to be configured
     */
    protected void afterSetupPipeline(final ChannelPipeline pipeline) {
    }

    /**
     * Create a new {@link SSLEngine} instance that will be used in the configuration of a pipeline
     *
     * @param clientMode if true then the {@link SSLEngine} is configure in client mode.
     * @return a new engine
     */
    protected SSLEngine newSSLEngine(final boolean clientMode) {
        SSLEngine engine = SSLContextFactory.getSSLSocket(SSLConfigFactory.sslConfiguration).createSSLEngine();
        engine.setUseClientMode(clientMode);
        return engine;
    }

    /**
     * This method should configure the pipeline on invocation.
     * SSL is automatically added if the {@link #newInitializer(boolean, boolean)} method
     * received ssl==true
     * During its configuration implementers may add a "handler" to the pipeline.
     * If no "handler" is added to the initializer one will be added automatically AT THE END of
     * the pipeline IF AND ONLY IF this method returns true.
     *
     * @return true if this event processor should be added as the handler for this pipeline,
     *         false otherwise. It is important to return the correct setting for e.g. if protocol sniffing
     *         is enabled then this should always return false because the protocol sniffer should add a handler
     *         if it doesn't and a handler is added at this stage it will be too early in the pipeline which will lead
     *         to unexpected results.
     */
    protected abstract boolean setupPipeline(ChannelPipeline pipeline);

    public boolean isErrorLoggerEnabled() {
        return errorLoggerEnabled;
    }

    public void setErrorLoggerEnabled(final boolean errorLoggerEnabled) {
        this.errorLoggerEnabled = errorLoggerEnabled;
    }

    //netty related methods
    private final Optional<Throwable> absentThrowable = Optional.absent();

    @Override
    public void messageReceived(ChannelHandlerContext ctx, SM msg) throws Exception {
        emit(MESSAGE_RECEIVED, ctx, absentThrowable);
        emitMessage(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (errorLoggerEnabled) {
            errorLogger.info("Uncaught exception", cause);
        }
        emit(EXCEPTION_CAUGHT, ctx, Optional.of(cause));
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        emit(CHANNEL_REGISTERED, ctx, absentThrowable);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        emit(CHANNEL_UNREGISTERED, ctx, absentThrowable);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        emit(CHANNEL_ACTIVE, ctx, absentThrowable);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        emit(CHANNEL_INACTIVE, ctx, absentThrowable);
    }
}
