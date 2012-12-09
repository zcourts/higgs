package com.fillta.higgs.queueingStrategies;

import com.fillta.higgs.MessageTopicFactory;
import com.fillta.higgs.buffer.CircularBuffer;
import com.fillta.higgs.buffer.CircularBufferConsumer;
import com.fillta.higgs.buffer.Sequence;
import com.fillta.higgs.util.Function1;
import com.fillta.higgs.util.Tuple;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ForkJoinPool;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class CircularBufferQueueingStrategy<T, IM> extends QueueingStrategy<T, IM> {
    //use default buffer size of 1M
    protected CircularBuffer<Tuple<ChannelHandlerContext, IM>> buffer = new CircularBuffer<>();
    //see http://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html
    protected ForkJoinPool threadPool;
    CircularBufferConsumer<IM> consumer;
    private int consumerThreshold;
    private Sequence sequence = new Sequence(buffer);

    public CircularBufferQueueingStrategy(ForkJoinPool threadPool,
                                          MessageTopicFactory<T, IM> topicFactory,
                                          int consumerThreshold) {
        super(topicFactory);
        this.threadPool = threadPool;
        this.consumerThreshold = consumerThreshold;
        consumer = new CircularBufferConsumer(this.threadPool,
                new Function1<Tuple<ChannelHandlerContext, IM>>() {
                    @Override
                    public void call(Tuple<ChannelHandlerContext, IM> a) {
                        if (a != null) {
                            invokeListeners(a.key, a.value);
                        }
                    }
                }, buffer, this.consumerThreshold, -1,1);
    }

    /**
     * Queues the given message and submits the {@link #consumer} to the thread pool.
     * The consumer maintains its state (sequence) so it knows where to start or continue reading
     * from in the buffer.
     *
     * @param ctx the channel context
     * @param msg the message to queue
     */
    @Override
    public void enqueue(ChannelHandlerContext ctx, IM msg) {
        buffer.add(new Tuple(ctx, msg));
        threadPool.invoke(consumer);
    }
}
