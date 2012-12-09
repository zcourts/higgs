package com.fillta.higgs.queueingStrategies;

import com.fillta.higgs.MessageTopicFactory;
import com.fillta.higgs.util.Tuple;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedTransferQueue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class LinkedBlockingQueueStrategy<T, IM> extends QueueingStrategy<T, IM> {
    private LinkedTransferQueue<Tuple<ChannelHandlerContext, IM>> queue = new LinkedTransferQueue<>();

    public LinkedBlockingQueueStrategy(final ForkJoinPool threadPool,
                                       final MessageTopicFactory<T, IM> topicFactory) {
        super(topicFactory);
        for (int i = 0; i < threadPool.getParallelism(); i++) {
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    while (!threadPool.isShutdown()) {
                        try {
                            Tuple<ChannelHandlerContext, IM> tuple = queue.take();
                            if (tuple != null) {
                                invokeListeners(tuple.key, tuple.value);
                            }
                        } catch (InterruptedException e) {

                        }
                    }
                }
            });
        }
    }

    /**
     * Adds the given message to a queue for it to be processed by 1 or more other threads
     * allowing the calling thread to proceed without blocking.
     *
     * @param ctx the channel context
     * @param msg the message to queue
     */
    @Override
    public void enqueue(ChannelHandlerContext ctx, IM msg) {
        queue.add(new Tuple(ctx, msg));
    }
}
