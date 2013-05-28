package io.higgs.events.demo;

import io.higgs.core.method;
import io.higgs.events.Event;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ClassExample {
    /**
     * ctx,channel and event can appear at any location in the list of parameters
     * but the order of other parameters must match the order of parameters emitted
     * in this case, Events.emit("test","some string",100) must be the order
     * <p/>
     * doing this would fail Events.emit("test",100,"some string")
     * Imagine a method that has parameters (int,int,String,String)
     * if the order wasn't enforced when something like
     * Events.emit(1,2,"a","b") is emitted then b could go in place of a or 1 in place of 2
     *
     * @param ctx      the channel context, each event has 1 context which is paired with an
     *                 event name, in this case "test" so all test events emitted will have the same context
     * @param channel  like channel context there is 1 channel per event
     * @param event    every time the test event is emitted a new {@link Event} object will be created
     * @param executor submit background tasks to the executor
     */
    @method("test") //subscribe to events emitted with the name "test"
    public void test(
            ChannelHandlerContext ctx,
            String a,
            Channel channel,
            int b,
            Event event,
            RandomObject object,
            EventExecutor executor) {
        System.out.println(
                Thread.currentThread().getName() +
                        ":First test subscriber :" + a + " " + b
        );
        //executor.schedule() //schedule a background task
        //executor.scheduleAtFixedRate()
        //executor.submit() //runnable
        //etc
    }

    @method("test")
    public void test2(String a, int b) {
        System.out.println(
                Thread.currentThread().getName() +
                        ": Second test subscriber :" + a + " " + b
        );
    }
}
