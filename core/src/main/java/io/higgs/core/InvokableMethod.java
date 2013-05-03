package io.higgs.core;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class InvokableMethod implements Sortable<InvokableMethod> {
    protected Logger log = LoggerFactory.getLogger(getClass());
    protected final Method classMethod;
    protected final ObjectFactory factory;
    protected final Class<?> klass;
    protected String[] pathAttributes;
    protected final String path;
    protected Attr attrs = new Attr();

    public InvokableMethod(ObjectFactory factory, Method classMethod) {
        this(factory, null, classMethod);
    }

    public InvokableMethod(Class<?> klass, Method classMethod) {
        this(null, klass, classMethod);
    }

    public InvokableMethod(ObjectFactory factory, Class<?> klass, Method classMethod) {
        if (factory == null && klass == null) {
            throw new IllegalStateException("Cannot create an InvokableMethod with a null factory AND class");
        }
        if (classMethod == null) {
            throw new IllegalStateException("Cannot create an InvokableMethod with a null classMethod");
        }
        if (klass == null) {
            this.klass = factory.newInstance().getClass();
        } else {
            this.klass = klass;
        }
        this.factory = factory;
        this.classMethod = classMethod;
        path = parsePath();
    }

    protected String parsePath() {
        String classPath = null, methodPath = null;
        if (klass.isAnnotationPresent(method.class)) {
            method path = klass.getAnnotation(method.class);
            classPath = path.value() != null && !path.value().isEmpty() ? path.value() : "/";
        }
        if (classMethod.isAnnotationPresent(method.class)) {
            method path = classMethod.getAnnotation(method.class);
            methodPath = path.value() != null && !path.value().isEmpty() ? path.value() : "/";
            if (path.attr() != null && path.attr().length > 0) {
                pathAttributes = path.attr();
            }
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
        return classPath + methodPath;
    }

    public ResourcePath path() {
        return new ResourcePath(path);
    }

    /**
     * Invoke this method using the parameters provided
     *
     * @param ctx    channel context
     * @param path   method path extracted from incoming request
     * @param msg    the original message
     * @param params parameters extracted from message
     */
    public Object invoke(ChannelHandlerContext ctx, String path, Object msg, Object[] params)
            throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object instance;
        if (factory != null) {
            instance = factory.newInstance();
        } else {
            instance = klass.newInstance();
        }
        return classMethod.invoke(instance, params);
    }

    /**
     * @return The set of attributes set on this method or an empty array
     */
    public String[] pathAttr() {
        return pathAttributes == null ? new String[0] : pathAttributes;
    }

    public Class<?> klass() {
        return klass;
    }

    public Method method() {
        return classMethod;
    }

    public Attr attrs() {
        return attrs;
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

    /**
     * @return A priority which determines the order in which methods are checked
     */
    @Override
    public int priority() {
        return 0;
    }

    /**
     * Invoked when a method has been registered
     */
    public void registered() {
        log.info(String.format("REGISTERED > %1$-20s | %2$-30s | %3$-50s", classMethod.getName(),
                path(), classMethod.getReturnType().getName()));
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
    public String toString() {
        return "InvokableMethod{" +
                "\n path='" + path + '\'' +
                ",\n pathAttributes=" + Arrays.toString(pathAttributes) +
                ",\n attrs=" + attrs +
                ",\n classMethod=" + classMethod +
                ",\n factory=" + factory +
                ",\n klass=" + klass.getName() +
                '}';
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

        if (attrs != null ? !attrs.equals(that.attrs) : that.attrs != null) {
            return false;
        }
        if (classMethod != null ? !classMethod.equals(that.classMethod) : that.classMethod != null) {
            return false;
        }
        if (factory != null ? !factory.equals(that.factory) : that.factory != null) {
            return false;
        }
        if (klass != null ? !klass.equals(that.klass) : that.klass != null) {
            return false;
        }
        if (path != null ? !path.equals(that.path) : that.path != null) {
            return false;
        }
        if (!Arrays.equals(pathAttributes, that.pathAttributes)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = classMethod != null ? classMethod.hashCode() : 0;
        result = 31 * result + (factory != null ? factory.hashCode() : 0);
        result = 31 * result + (klass != null ? klass.hashCode() : 0);
        result = 31 * result + (pathAttributes != null ? Arrays.hashCode(pathAttributes) : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (attrs != null ? attrs.hashCode() : 0);
        return result;
    }
}
