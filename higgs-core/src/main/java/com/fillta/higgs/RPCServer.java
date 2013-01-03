package com.fillta.higgs;

import com.fillta.functional.Function1;
import com.fillta.higgs.events.ChannelMessage;
import com.fillta.higgs.reflect.PackageScanner;
import com.google.common.base.Optional;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public abstract class RPCServer<OM, IM, SM> extends HiggsServer<String, OM, IM, SM> {
	Class<method> methodClass = method.class;
	protected boolean onlyRegisterAnnotatedMethods = true;

	public RPCServer(int port) {
		super(port);
	}

	/**
	 * Chose whether ALL methods in a registered class are registered or
	 * just the ones that have been explicitly annotated with {@link method}
	 *
	 * @param v if true only explicitly marked methods are registered
	 */
	public void setOnlyRegisterAnnotatedMethods(boolean v) {
		onlyRegisterAnnotatedMethods = v;
	}

	/**
	 * Register a package. All classes found in the package will be checked
	 * If any class/method has the method annotation it'll be inspected
	 * and registered depending on its settings.
	 * Note that the classes must have an accessible no argument constructor or they won't be
	 * registered
	 *
	 * @param pkg Fully qualified package name as in com.domain.product
	 */
	public void registerPackage(String pkg) {
		List<Class<?>> classes = PackageScanner.get(pkg);
		for (Class<?> c : classes) {
			register(c);
		}
	}

	/**
	 * Register the given class and its methods that are annotated
	 * Not that the class must have an accessible no argument constructor
	 *
	 * @param klass
	 */
	public void register(Class<?> klass) {
		boolean registered = false;
		for (Constructor constructor : klass.getConstructors()) {
			if (!registered && //not registered yet
					constructor.getParameterTypes().length == 0 //find the no-arg constructor
					) {
				try {
					register(constructor.newInstance()); //create a new instance of the class and register
				} catch (InstantiationException e) {
					log.warn(String.format("%s ignored. Failed to instantiate", klass.getName()), e);
				} catch (IllegalAccessException e) {
					log.warn(String.format("%s ignored. Cannot access constructor, is it private?", klass.getName()), e);
				} catch (InvocationTargetException e) {
					log.warn(String.format("%s ignored. Unable to invoke constructor", klass.getName()), e);
				}
				registered = true; //registered now
			}
			if (!registered) {
				log.warn(String.format("%s ignored. No No-arg constructor found", klass.getName()));
			}
		}
	}

	/**
	 * Register an object whose methods will be invoked if annotated and configured to receive
	 * messages.
	 *
	 * @param obj any object
	 */
	public void register(Object obj) {
		Class klass = obj.getClass();
		log.debug(String.format("Registering methods of %s", klass.getName()));
		//is the annotation applied to the whole class or not?
		boolean registerAllMethods = !klass.isAnnotationPresent(methodClass);
		Method[] methods = klass.getMethods(); //get the class' methods
		for (Method method : methods) {
			if (registerAllMethods) {
				boolean hasListener = method.isAnnotationPresent(methodClass);
				//opt out if the annotation is present and optout is set to true
				boolean optout;
				optout = hasListener && method.getAnnotation(methodClass).optout();
				if (!optout) {
					//register all methods is true, the method hasn't been opted out
					doRegister(klass, obj, method);
				} else {
					//String.format("%1$50s", s)
					log.debug(String.format("method %s not registered, optout=true", method.getName()));
				}
			} else if (method.isAnnotationPresent(methodClass)
					&& !method.getAnnotation(methodClass).optout()) {
				//if we're not registering all methods, AND this method has the annotation
				//AND optout is not set to true
				doRegister(klass, obj, method);
			}
		}
	}

	/**
	 * Figures out the method name to be used for the given method and
	 * make a subscription for that method under that inferred name.
	 * Method names (topics) are determined by the methodName of the method's methodName
	 * which, if not set defaults to the fully qualified name of the class the method
	 * belongs to plus the method name e.g. com.domain.prodct.className.method
	 *
	 * @param klass
	 * @param instance
	 * @param method
	 */
	public void doRegister(Class<?> klass, final Object instance, final Method method) {
		//make sure if set, only annotated methods are registered (default)
		if (onlyRegisterAnnotatedMethods && !method.isAnnotationPresent(methodClass)) {
			return;
		}
		final String methodName;
		if (method.isAnnotationPresent(methodClass)
				&& !method.getAnnotation(methodClass).value().isEmpty()) {
			//if a method name is provided then use it
			methodName = method.getAnnotation(methodClass).value();
		} else {
			//if no method name is provided, use the fully qualified class and method name
			methodName = klass.getName() + "." + method.getName();
		}
		if (listening(methodName)) {
			//TODO support overloaded methods
			//throw new IllegalArgumentException(String.format("Method name %s is already registered!", methodName));
			log.warn(String.format("Method name %s is already registered, ignored, overloaded methods not yet supported!", methodName));
			return;
		}
		//listen for this method name to be invoked then call the given method on
		//the instance provided
		listen(methodName, new Function1<ChannelMessage<IM>>() {
			public void apply(final ChannelMessage<IM> a) {
				processIncomingRequest(a, method, instance, methodName);
			}
		});
		log.info(String.format("REGISTERED > %1$-20s | %2$-20s | %3$-5s", method.getName(), methodName, method.getReturnType().getName()));
	}

	public void processIncomingRequest(final ChannelMessage<IM> request, Method method, Object instance, String methodName) {
		//method.invoke returns null if the underlying method returns "void"/Unit
		//so if no error occurred the return type should be Unit
		Optional<Object> returns = Optional.absent();
		Optional<Throwable> error = Optional.absent();
		Class<?>[] argTypes = method.getParameterTypes();
		Object[] args = getArguments(argTypes, request);
		try {
			args = extractIncomingRequestParameters(argTypes, args, request, methodName);
			//invoke the request method with the extracted parameters
			Object res = method.invoke(instance, args);
			if (res != null) {
				returns = Optional.of(res);
			}
		} catch (Throwable e) {
			error = Optional.of(e);
			logDetailedFailMessage(method, methodName, args, argTypes, e);
		}
		//send a response
		respond(request.channel, newResponse(methodName, request, returns, error));
	}


	protected Object[] extractIncomingRequestParameters(Class<?>[] argTypes, Object[] args, ChannelMessage<IM> request, String methodname) {
		int requestIndex = Arrays.asList(argTypes).indexOf(ChannelMessage.class);
		//if the method wants the request object then inject it into the parameters passed to it
		if (requestIndex != -1) {
			if (requestIndex != 0) {
				//request must be first parameter the method accepts
				throw new IllegalArgumentException(String.format("To accept a request the method (%s) must have " +
						"ChannelMessage as the first parameter", methodname));
			}
			//extend the arguments received by 1
			Object[] tmp = new Object[args.length + 1];
			//set request to first parameter
			tmp[0] = request;
			//merge existing arg array with request
			System.arraycopy(args, 0, tmp, 1, args.length);
			args = tmp;//re-assign
		}
		return args;
	}


	/**
	 * When a message of type M is received, subclasses should know how to extract
	 * the parameters from the message. These parameters must be an ordered sequence of objects
	 * as they'll be used, in the order given as the parameters to the method to be invoked
	 *
	 * @return
	 */
	public abstract Object[] getArguments(final Class<?>[] argTypes, ChannelMessage<IM> request);

	/**
	 * When invoked concrete classes are to return a response object suitable for sending back to the client.
	 * This response object should contain the returns field passed.
	 *
	 * @param methodName the name of the method invoked
	 * @param request    the request object used to invoke the server method
	 * @param returns    the value returned from invoking the server method
	 * @param error      is present, an exception which occurred while trying to process the request
	 * @return
	 */
	protected abstract OM newResponse(String methodName, ChannelMessage<IM> request, Optional<Object> returns, Optional<Throwable> error);

	private void logDetailedFailMessage(Method method, String methodName, Object[] args, Class<?>[] argTypes, Throwable e) {
		String expected = "[";
		for (Class<?> argType : argTypes) {
			expected += argType.getName() + ",";
		}
		expected += "]";
		String actual = "[";
		for (Object arg1 : args) {
			actual += arg1.getClass().getName() + ",";
		}
		actual += "]";
		String argvalues = Arrays.deepToString(args);
		log.warn(String.format("Error invoking method %s with arguments %s : Path to method %s The method \n" +
				"expected: %s \n" +
				"received: %s", methodName, argvalues, method.getDeclaringClass().getName() + "." + method.getName(),
				expected, actual), e);
	}
}