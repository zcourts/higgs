package com.fillta.higgs.boson;

import com.fillta.higgs.HiggsClient;
import com.fillta.higgs.HiggsInitializer;
import com.fillta.higgs.HiggsSingleMessageClientRequest;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.util.Function1;
import com.fillta.higgs.util.Function2;
import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;

public class BosonClientRequest extends HiggsSingleMessageClientRequest<String, BosonMessage, ByteBuf> {
    public BosonClientRequest(final HiggsClient<String, BosonMessage, BosonMessage, ByteBuf> client, String serviceName, String host, int port, boolean decompress, boolean useSSL, HiggsInitializer initializer) {
        super(client, serviceName, host, port, decompress, useSSL, initializer);
    }

    /**
     * Invoke the given method on the connected remote host
     *
     * @param method    the method to be invoked
     * @param function  the callback to be invoked with the response returned
     * @param arguments the arguments to pass to the remote function
     */
    public void invokeF(String method, Function1<ChannelMessage<BosonMessage>> function, Object... arguments) {
        invokeF(method, arguments, function);
    }

    /**
     * Invoke the given method on the connected remote host
     *
     * @param method    the method to be invoked
     * @param arguments the arguments to pass to the remote function
     * @param function  the callback to be invoked with the response returned
     */
    public void invokeF(String method, Object[] arguments, Function1<ChannelMessage<BosonMessage>> function) {
        String callback = String.valueOf(System.nanoTime());
        client.listen(callback, function);
        send(new BosonMessage(arguments, method, callback));
    }

    /**
     * Invoke the given method on the remote host
     *
     * @param method    the method to be invoked
     * @param arguments the arguments to pass to the given method
     * @param function  a callback to be invoked when a response is received
     *                  If the expected type is not convertible from the response received this
     *                  function's second parameter will be populated with an exception.
     *                  That exception has a field called "param" which provides access to the response
     *                  the server returned.
     * @param <R>       The expected type the server should return
     */
    public <R> void invoke(String method, Object[] arguments, final Function2<R, Optional<? extends Throwable>> function) {
        final Optional<Throwable> none = Optional.absent();
        invokeF(method, arguments, new Function1<ChannelMessage<BosonMessage>>() {
            public void call(ChannelMessage<BosonMessage> a) {
                if (a.message.arguments.length > 0) {
                    Object param = a.message.arguments[0];
                    if (param != null) {
                        try {
                            function.call((R) param, none);
                        } catch (ClassCastException cce) {
                            function.call(null, Optional.of(new IllegalBosonResponseType(param, cce)));
                        }
                    }
                }
            }
        });
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
            public void call(R o, Optional<? extends Throwable> cause) {
                if (cause.isPresent()) {
                    log.warn("Invalid response received and client function.", cause.get());
                } else {
                    function.call(o);
                }
            }
        });
    }

    /**
     * A proxy to {@link #invoke(String, Object[], com.fillta.higgs.util.Function1)}
     */
    public <R> void invoke(String method, final Function1<R> function, Object... arguments) {
        invoke(method, arguments, function);
    }

    /**
     * A proxy to {@link #invoke(String, Object[], com.fillta.higgs.util.Function2)}
     */
    public <R> void invoke(String method, final Function2<R, Optional<? extends Throwable>> function, Object... arguments) {
        invoke(method, arguments, function);
    }
}