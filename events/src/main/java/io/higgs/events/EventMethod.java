package io.higgs.events;

import io.higgs.core.InvokableMethod;
import io.higgs.core.ObjectFactory;
import io.netty.channel.ChannelHandlerContext;

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

    public void registered() {
        log.info(String.format("REGISTERED > %1$-20s | %2$-30s | %3$-50s", classMethod.getName(),
                path(), classMethod.getReturnType().getName()));
    }
}
