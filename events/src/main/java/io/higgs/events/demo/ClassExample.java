package io.higgs.events.demo;

import io.higgs.core.method;
import io.higgs.events.EventMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ClassExample {
    /**
     * This will be injected before the method is invoked.
     * One method is invoked per instance
     */
    private EventExecutor instanceExecutor;

    public ClassExample() {
        //instanceExecutor will be null in the constructor but will be injected after construction
    }

    public void init() {
        //if init method exists it'll be called, use in place of constructor
        //all injectable fields will be injected at this point
        System.out.println("init, instanceInject!=null -->" + (instanceExecutor != null));
    }

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
     * @param eventMessage    every time the test event is emitted a new {@link io.higgs.events.EventMessage} object will be created
     * @param executor submit background tasks to the executor
     */
    @method("test") //subscribe to events emitted with the name "test"
    public void test(
            ChannelHandlerContext ctx,
            String a,
            Channel channel,
            int b,
            EventMessage eventMessage,
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
