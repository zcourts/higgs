package io.higgs.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class MessageHandler<C extends ServerConfig, T> extends ChannelInboundMessageHandlerAdapter<T> {

    private Queue<InvokableMethod> methods;
    protected Logger log = LoggerFactory.getLogger(getClass());
    protected final C config;

    public MessageHandler(C config) {
        this.config = config;
    }

    public void setMethods(Queue<InvokableMethod> methods) {
        this.methods = methods;
    }

    /**
     * Finds a method which matches the given path AND is an instance of the provided method class
     *
     * @param path        the path to match against
     * @param ctx         the context which the request belongs to
     * @param msg         the request/message
     * @param methodClass the concrete implementation of {@link InvokableMethod} which must also match
     * @return the first method which matches or null if none
     */
    public <M extends InvokableMethod> M findMethod(String path, ChannelHandlerContext ctx,
                                                    Object msg, Class<M> methodClass) {
        List<InvokableMethod> sortedMethods = new FixedSortedList<>(methods);
        for (InvokableMethod method : sortedMethods) {
            if (method.matches(path, ctx, msg)) {
                if (method.getClass().isAssignableFrom(methodClass)) {
                    return (M) method;
                } else {
                    log.debug(String.format("%s matches %s but types are incompatible." +
                            " Registered method %s and expected method %s",
                            path, method.path().getUri(), method.getClass().getName(), methodClass.getName()));
                }
            }
        }
        return null;
    }

    protected void logDetailedFailMessage(boolean uncaught, Object[] args, Throwable e, Method method) {
        String expected = "[";
        for (Class<?> argType : method.getParameterTypes()) {
            expected += argType.getName() + ",";
        }
        expected += "]";
        String actual = "[";
        for (Object arg1 : args) {
            if (arg1 != null) {
                actual += arg1.getClass().getName() + ",";
            } else {
                actual += "null,";
            }
        }
        actual += "]";
        String argvalues = Arrays.deepToString(args);
        log.warn(String.format("%sError invoking method %s with arguments %s : Path to method %s The method \n" +
                "expected: %s \n" +
                "received: %s", uncaught ? "Uncaught exception while invoking method - " : "",
                method.getName(), argvalues,
                method.getDeclaringClass().getName() + "." + method.getName(),
                expected, actual), e);
    }

    protected void logDetailedFailMessage(Object[] args, Throwable e, Method method) {
        logDetailedFailMessage(false, args, e, method);
    }
}
