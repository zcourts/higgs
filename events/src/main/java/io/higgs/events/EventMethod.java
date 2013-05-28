package io.higgs.events;

import io.higgs.core.InvokableMethod;
import io.higgs.core.ObjectFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class EventMethod extends InvokableMethod {
    public EventMethod(Queue<ObjectFactory> factories, Class<?> klass, Method classMethod) {
        super(factories, klass, classMethod);
    }

    @Override
    public boolean matches(String path, ChannelHandlerContext ctx, Object msg) {
        return path().matches(path);
    }

    public Object invoke(ChannelHandlerContext ctx, String path, Object msg, Object[] params)
            throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object[] p = inject(ctx, path, msg, params);
        return super.invoke(ctx, path, msg, p);
    }

    private Object[] inject(ChannelHandlerContext ctx, String path, Object msg, Object[] params) {
        Class<?>[] accepts = classMethod.getParameterTypes();
        Object[] p = new Object[accepts.length];
        if (p.length > 0) {
            int incomingIdx = -1; //pre-increment used below
            for (int i = 0; i < accepts.length; i++) {
                Class<?> klass = accepts[i];
                if (klass.isAssignableFrom(ChannelHandlerContext.class)) {
                    p[i] = ctx;
                } else if (klass.isAssignableFrom(Channel.class)) {
                    p[i] = ctx.channel();
                } else if (klass.isAssignableFrom(Event.class)) {
                    p[i] = msg;
                } else if (klass.isAssignableFrom(EventExecutor.class)) {
                    p[i] = ctx.executor();
                } else if (++incomingIdx < params.length) {
                    //TODO check param for primitives and do conversion as needed
                    Object param = params[incomingIdx];
                    p[i] = param;
                }
            }
//            Map<Class<?>, Object> available = new HashMap<>();
//            available.put(ChannelHandlerContext.class, ctx);
//            available.put(Channel.class, ctx.channel());
//            available.put(Event.class, msg);
//            for (Object o : params) {
//                if (o != null) {
//                    available.put(o.getClass(), o);
//                }
//            }
//            for (int i = 0; i < p.length; i++) {
//                Object param = available.get(accepts[i]);
//                p[i] = param;
//            }
        }
        return p;
    }

    public void registered() {
        log.info(String.format("REGISTERED > %1$-20s | %2$-30s | %3$-50s", classMethod.getName(),
                path(), classMethod.getReturnType().getName()));
    }
}
