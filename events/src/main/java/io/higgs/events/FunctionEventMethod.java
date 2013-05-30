package io.higgs.events;

import io.higgs.core.ObjectFactory;
import io.higgs.core.func.Function1;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FunctionEventMethod<A> extends EventMethod {
    private final Function1<A> function;

    public FunctionEventMethod(String event, Function1<A> function) {
        //dummy values, the invoke method is overriden so these aren't used
        super(new LinkedList<ObjectFactory>(), Function1.class,
                Function1.class.getMethods()[0]);
        this.function = function;
        path = event;
    }

    public boolean matches(String path, ChannelHandlerContext ctx, Object msg) {
        try {
            return path.matches(path);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    //won't use normal dependency injection because it only supports 1 parameter
    public Object invoke(ChannelHandlerContext ctx, String path, Object msg, Object[] params)
            throws InvocationTargetException, IllegalAccessException, InstantiationException {
        try {
            Object param = null;
            if (params.length > 0) {
                param = params[0];
            }
            function.apply((A) param);
        } catch (ClassCastException cce) {
            return new TypeMismatch(cce);
        }
        return null;
    }
}
