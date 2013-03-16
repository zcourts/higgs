package io.higgs.queueingStrategies;

import io.higgs.DecodedMessage;
import io.higgs.functional.Tuple;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class LinkedBlockingQueueStrategy<T, IM> extends QueueingStrategy<T, IM> {
    private LinkedBlockingQueue<Tuple<ChannelHandlerContext, DecodedMessage<T, IM>>> queue =
            new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor threadPool;

    public LinkedBlockingQueueStrategy(QueueingStrategy<T, IM> strategy, ThreadPoolExecutor threadPool) {
        super(strategy);
        this.threadPool = threadPool;
    }

    /**
     * Adds the given message to a queue for it to be processed by 1 or more other threads
     * allowing the calling thread to proceed without blocking.
     *
     * @param ctx the channel context
     * @param msg the message to queue
     */
    @Override
    public void enqueue(ChannelHandlerContext ctx, DecodedMessage<T, IM> msg) {
        queue.add(new Tuple<>(ctx, msg));
        processMessage();
    }

    private void processMessage() {
        threadPool.execute(new Runnable() {
            public void run() {
                Tuple<ChannelHandlerContext, DecodedMessage<T, IM>> tuple;
                while ((tuple = queue.poll()) != null && !threadPool.isShutdown()) {
                    if (tuple != null) {
                        invokeListeners(tuple.key, tuple.value);
                    }
                }
            }
        });
    }
}
