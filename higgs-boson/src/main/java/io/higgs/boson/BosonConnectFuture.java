package io.higgs.boson;

import com.google.common.base.Optional;
import io.higgs.ConnectFuture;
import io.higgs.HiggsClient;
import io.higgs.events.ChannelMessage;
import io.higgs.functional.Function1;
import io.higgs.functional.Function2;
import io.netty.buffer.ByteBuf;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonConnectFuture extends ConnectFuture<String, BosonMessage, BosonMessage, ByteBuf> {

    public BosonConnectFuture(HiggsClient<String, BosonMessage, BosonMessage, ByteBuf> client, boolean reconnect) {
        super(client, reconnect);
    }

    /**
     * Invoke the given method on the connected remote host
     *
     * @param method    the method to be invoked
     * @param function  the callback to be invoked with the response returned
     * @param arguments the arguments to pass to the remote function
     */
    public void invokeF(String method, Function1<ChannelMessage<BosonMessage>> function, Object... arguments) {
        invokeF(method, arguments, function, true);
    }

    /**
     * Invoke the given method on the connected remote host
     *
     * @param method      the method to be invoked
     * @param arguments   the arguments to pass to the remote function
     * @param function    the callback to be invoked with the response returned
     * @param unsubscribe If true then the function is unsubscribed when the first response is received,
     *                    if false then it isn't and must manually be unsubscribed later -
     *                    This allows future responses to be "streamed"
     */
    public void invokeF(final String method, Object[] arguments, final Function1<ChannelMessage<BosonMessage>> function,
                        final boolean unsubscribe) {
        String callback = String.valueOf(System.nanoTime());
        client.listen(callback, new Function1<ChannelMessage<BosonMessage>>() {
            public void apply(ChannelMessage<BosonMessage> a) {
                if (unsubscribe) {
                    client.unsubscribe(method, this);
                }
                function.apply(a);
            }
        });
        send(new BosonMessage(arguments, method, callback));
    }

    /**
     * By default when an invoke* method is called, it unsubscribes the given function when the first response
     * is received. Sometimes it is desired that a function remains subscribed to accept any number of future responses.
     * This method is allows this.
     *
     * @param method    the method to be invoked on the server
     * @param arguments the parameters o pass to the method
     * @param function  the function to be invoked when responses are received, now or in the future...
     * @param <R>
     */
    public <R> void invokeSubscribe(final String method, final Function1<R> function, Object... arguments) {
        invoke(method, arguments, new Function2<R, Optional<? extends Throwable>>() {
            public void apply(final R o, final Optional<? extends Throwable> optional) {
                if (optional.isPresent()) {
                    log.warn("Unable to do subscription", o);
                }
                function.apply(o);
            }
        }, false);
    }

    /**
     * Invoke the given method on the remote host
     *
     * @param method      the method to be invoked
     * @param arguments   the arguments to pass to the given method
     * @param function    a callback to be invoked when a response is received
     *                    If the expected type is not convertible from the response received this
     *                    function's second parameter will be populated with an exception.
     *                    That exception has a field called "param" which provides access to the response
     *                    the server returned.
     * @param unsubscribe If true then the function is unsubscribed when the first response is received,
     *                    if false then it isn't and must manually be unsubscribed later -
     *                    This allows future responses to be "streamed"
     * @param <R>         The expected type the server should return
     */
    public <R> void invoke(final String method, Object[] arguments,
                           final Function2<R, Optional<? extends Throwable>> function, boolean unsubscribe) {
        final Optional<Throwable> none = Optional.absent();
        invokeF(method, arguments, new Function1<ChannelMessage<BosonMessage>>() {
            public void apply(ChannelMessage<BosonMessage> a) {
                if (a.message.arguments.length > 0) {
                    Object param = a.message.arguments[0];
                    if (param != null) {
                        try {
                            function.apply((R) param, none);
                        } catch (ClassCastException cce) {
                            function.apply(null, Optional.of(new IllegalBosonResponseType(param, cce)));
                        }
                    }
                }
            }
        }, unsubscribe);
    }

    /**
     * Invoke the given method on the remote host
     *
     * @param method    the method to be invoked
     * @param arguments the arguments to pass to the given method
     * @param function  a callback to be invoked when a response is received
     * @param <R>       The expected type the server should return
     */
    public <R> void invoke(String method, Object[] arguments, final Function1<R> function) {
        invoke(method, arguments, new Function2<R, Optional<? extends Throwable>>() {
            public void apply(R o, Optional<? extends Throwable> cause) {
                if (cause.isPresent()) {
                    log.warn("Invalid response received and client function.", cause.get());
                } else {
                    function.apply(o);
                }
            }
        });
    }

    /**
     * A proxy to {@link #invoke(String, Object[], io.higgs.functional.Function1)}
     */
    public <R> void invoke(String method, final Function1<R> function, Object... arguments) {
        invoke(method, arguments, function);
    }

    /**
     * A proxy to {@link #invoke(String, Object[], io.higgs.functional.Function2, boolean)}
     */
    public <R> void invoke(String method, final Function2<R, Optional<? extends Throwable>> function,
                           Object... arguments) {
        invoke(method, arguments, function, true);
    }

    /**
     * A proxy to {@link #invoke(String, Object[], io.higgs.functional.Function1)}
     * Invokes the given remote method and ignores any response returned
     */
    public void invoke(String method, Object... arguments) {
        invoke(method, arguments, new Function1<Object>() {
            public void apply(Object a) {
                //NOOP
            }
        });
    }

    public <R> void invoke(final String method, Object[] arguments, final Function2<R,
            Optional<? extends Throwable>> function) {
        invoke(method, arguments, function, true);
    }
}
