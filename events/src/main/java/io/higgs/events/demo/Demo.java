package io.higgs.events.demo;

import io.higgs.core.func.Function1;
import io.higgs.events.Events;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Demo {
    private Demo() {
    }

    public static void main(String... args) {
        // if you really must completely isolate events then use Events.group("group-name");
        //but each call to Events.group creates a fresh set of resources...
        //too much and you'll run out of memory - i.e it's rare you'll need to
        //Events.get() is multi-threaded
        Events events = Events.get();

        //subscribe this class' methods
        events.subscribe(ClassExample.class);

        //subscribe an instance - as many instances can be registered as you want
        // event instances of the same class (but they'll all be invoked if the event topic matches)...
        events.subscribe(new ClassExample());
        //subscribe this function to these events execute this function
        events.on(new Function1<String>() {
            public void apply(String s) {
                System.out.println(Thread.currentThread().getName() + " Event received : " + s);
            }
        }, "event-name", "test", "event3");

        events.emit("event-name", "event name topic");

        for (int i = 0; i < 10; i++) {
            //both ClassExample.test and the function above subscribe to the topic "test"
            // so both will be invoked but the function will only get the first parameter
            //where as the class's method accepts a string,int,RandomObject so will get all
            events.emit("test", "test event", i, new RandomObject(i));
        }
    }
}
