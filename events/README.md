# Higgs Events

Multi-threaded publish-subscribe library for in process (in-proc) communication.
This takes advantage of Netty's EventLoop and EventLoopGroup's performance

## Pub/Sub

In both cases (publish and subscribing) ALL events are emitted under a string name (it's topic).

### Subscribing

An object or it's methods can subscribe to events in a few ways.

1. By name, where a string value is provided as the topic of the event
2. By class, where a .class value is provided
3. By instance, if you have an instance of an object all it's methods can be subscribed to topics

When you subscribe to an event you can use a function or an entire object.

If an object is used then all it's methods annotated with the @method annotation are
subscribed to the event group using the name parameter of the @method as the event topic
to subscribe to. In such case multiple parameters can be accepted.

An object's methods can have some additional parameters automatically injected that are
 not emitted with the event. These are

1. ChannelHandlerContext each event has 1 context which is paired with an event name
2. Channel  like channel context there is 1 channel per event
3. Event the event object that was created when the event was emitted
4. EventExecutor Used to submit background tasks to the executor


If a Function is used then only one parameter can be accepted.
(TODO, functions should be able to accept an Event object which will then have multiple parameters)

### Publishing

Objects can be emitted with a string value provided for the topic of the event

If an event is emitted with multiple parameters then the subscribed method must accept
multiple parameters. __Order matters__, if the order matches the subscribed method's
formal parameters then they are used as is, otherwise null is used.

There is an exception to this. The parameters mentioned above that can be automatically
injected can appear anywhere in a method's list of parameters

## Internals

Events can be grouped (not to confused with the purpose of EventLoopGroup).
This grouping is to separate events into distinct sets.

For example you create an event group called "api" and another group called "private".
You then subscribe to the event "get-user" on both even groups.

At some point your application emits a get-user event on the private event group. Only subscribers to this event group will be notified. Subscribers on the api event group won't know of the event. i.e. event group is a way of isolating events.

__WARNING__ each event group gets its own set of resources (i.e. it's own thread pool etc).
Creating too many will exhaust system resources.

## Example

Below is a complete working example of this in action

```java

public class Demo {
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

 public class RandomObject {
     private final int val;

     public RandomObject(int i) {
         this.val = i;
     }

     @Override
     public String toString() {
         return "RandomObject{" +
                 "val=" + val +
                 '}';
     }
 }

```