package io.higgs.core;

import io.higgs.core.reflect.dependency.DependencyProvider;
import io.higgs.core.reflect.dependency.Injector;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class InvokableMethod implements Sortable<InvokableMethod> {
    protected final Method classMethod;
    protected final Queue<ObjectFactory> factories;
    protected final Class<?> klass;
    protected Logger log = LoggerFactory.getLogger(getClass());
    protected String path;
    protected int priority;

    public InvokableMethod(Queue<ObjectFactory> factories, Class<?> klass, Method classMethod) {
        if (factories == null || klass == null || classMethod == null) {
            throw new IllegalStateException("Cannot create an InvokableMethod with a null factories, class OR method");
        }
        this.klass = klass;
        this.factories = factories;
        this.classMethod = classMethod;
        parsePath();
    }

    protected void parsePath() {
        String classPath = null, methodPath = null;
        //get any path set on the method
        if (classMethod.isAnnotationPresent(Path.class)) {
            Path path = classMethod.getAnnotation(Path.class);
            methodPath = path.value() != null && !path.value().isEmpty() ? path.value() : "/";
        }
        //get any path set on the entire class so it can be prepended to method paths
        if (klass.isAnnotationPresent(Path.class)) {
            Path path = klass.getAnnotation(Path.class);
            classPath = path.value() != null && !path.value().isEmpty() ? path.value() : "/";
        }

        if (classPath == null) {
            classPath = "/";
        }
        if (methodPath == null) {
            methodPath = "/";
        }
        if (methodPath.startsWith("/") && classPath.endsWith("/")) {
            methodPath = methodPath.substring(1);
        }
        if (!methodPath.startsWith("/") && !classPath.endsWith("/")) {
            classPath += "/";
        }
        path = classPath + methodPath;
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
    }

    public String rawPath() {
        return path;
    }

    /**
     * Invoke this method using the parameters provided
     *
     * @param ctx      channel context
     * @param path     method path extracted from incoming request
     * @param msg      the original message
     * @param params   parameters extracted from message
     * @param provider
     */
    public Object invoke(ChannelHandlerContext ctx, String path, Object msg, Object[] params,
                         DependencyProvider provider)
            throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object instance = createInstance();

        DependencyProvider deps = provider == null ? DependencyProvider.from() : provider;

        deps.add(ctx, ctx.channel(), ctx.executor(), msg);
        Injector.inject(instance, deps);

        Object[] depParams = injectParameters(ctx, msg, params, instance, deps);
        if (instance instanceof EventListenerMethod) {
            ((EventListenerMethod) instance).init();
        }
        return classMethod.invoke(instance, depParams);
    }

    protected Object createInstance() throws InstantiationException, IllegalAccessException {
        Object instance = null;
        for (ObjectFactory factory : factories) {
            if (factory.canCreateInstanceOf(klass)) {
                instance = factory.newInstance(klass);
                break;
            }
        }
        if (instance == null) {
            instance = klass.newInstance();
        }
        return instance;
    }

    protected Object[] injectParameters(ChannelHandlerContext ctx, Object msg, Object[] params, Object instance,
                                        DependencyProvider deps) {
        //inject method dependencies
        return Injector.inject(classMethod.getParameterTypes(), params, deps);
    }

    public Class<?> klass() {
        return klass;
    }

    public Method method() {
        return classMethod;
    }

    /**
     * Sorts methods in decending order
     *
     * @param that the method to compare to
     */
    @Override
    public int compareTo(InvokableMethod that) {
        return that.priority() - this.priority();
    }

    @Override
    public int setPriority(int value) {
        int old = priority;
        priority = value;
        return old;
    }

    @Override
    public int priority() {
        return priority;
    }

    /**
     * Invoked when a method has been registered
     */
    public void registered() {
        log.info(String.format("REGISTERED > %1$-20s | %2$-30s | %3$-50s", classMethod.getName(),
                path(), classMethod.getReturnType().getName()));
    }

    public ResourcePath path() {
        return new ResourcePath(path);
    }

    /**
     * Using some or all the available info provided, determine if the path matches this method
     *
     * @param path the path to match against
     * @param ctx  the context which the request belongs to
     * @param msg  the request/message
     * @return Returns true if this invokable method matches the given path
     */
    public abstract boolean matches(String path, ChannelHandlerContext ctx, Object msg);

    @Override
    public int hashCode() {
        int result = classMethod.hashCode();
        result = 31 * result + (factories.hashCode());
        result = 31 * result + (klass.hashCode());
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InvokableMethod that = (InvokableMethod) o;

        if (!classMethod.equals(that.classMethod)) {
            return false;
        }
        if (!factories.equals(that.factories)) {
            return false;
        }
        if (!klass.equals(that.klass)) {
            return false;
        }
        if (path != null ? !path.equals(that.path) : that.path != null) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "InvokableMethod{" +
                "\n path='" + path + '\'' +
                ",\n classMethod=" + classMethod +
                ",\n factories=" + factories +
                ",\n klass=" + klass.getName() +
                '}';
    }
}
