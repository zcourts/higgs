package info.crlog.higgs

import java.io.Serializable
import io.netty.channel.Channel
import protocols.jrpc.reflect.PackageScanner
import java.lang.reflect.Method

/**
 * Listens for String topics (method names)
 * and the message is an array of parameters for the topic/method
 * @author Courtney Robinson <courtney@crlog.info>
 */
abstract class RPCServer[M](host: String, port: Int, compress: Boolean)(implicit mf: Manifest[M])
  extends Server[String, M, Array[Byte]](host, port, compress) {

  /**
   * Register a package. All classes found in the package will be checked
   * If any class/method has the method annotation it'll be inspected
   * and registered depending on its settings.
   * Note that the classes must have an accessible no argument constructor or they won't be
   * registered
   * @param pkg Fully qualified package name as in com.domain.product
   */
  def registerPackage(pkg: String) {
    log.info("Registering package %s" format (pkg))
    PackageScanner.get(pkg) foreach {
      case klass => {
        log.info("Class %s found" format (klass.getName))
        register(klass)
      }
    }
  }

  /**
   * Register the given class and its methods that are annotated
   * Not that the class must have an accessible no argument constructor
   * @param klass
   */
  def register(klass: Class[_]) {
    var registered = false
    klass.getConstructors foreach {
      case constructor => {
        if (!registered && //not registered yet
          constructor.getParameterTypes.length == 0 //find the no-arg constructor
        ) {
          register(constructor.newInstance()) //create a new instance of the class and register
          registered = true //registered now
        }
      }
    }
    if (!registered) {
      log.warn("%s ignored. No No-arg constructor found" format (klass.getName))
    }
  }

  /**
   * Register an object whose methods will be invoked if annotated and configured to receive
   * messages.
   * @param obj any object
   */
  def register(obj: Any) {
    val klass = obj.asInstanceOf[AnyRef].getClass
    log.info("Registering methods of %s" format (klass.getName))
    //is the annotation applied to the whole class or not?
    val registerAllMethods = klass.isAnnotationPresent(classOf[method])
    val methods = klass.getMethods //get the class' methods
    for (method <- methods) {
      if (registerAllMethods) {
        val hasListener = method.isAnnotationPresent(classOf[method])
        //opt out if the annotation is present and optout is set to true
        val optout = if (hasListener && method.getAnnotation(classOf[method]).optout()) true else false
        if (!optout) {
          //register all methods is true, the method hasn't been opted out
          doRegister(klass, obj, method)
        }
      } else if (method.isAnnotationPresent(classOf[method])
        && !method.getAnnotation(classOf[method]).optout()) {
        //if we're not registering all methods,
        //AND this method has the annotation
        //AND optout is not set to true
        doRegister(klass, obj, method)
      }
    }
  }

  /**
   * Figures out the method name to be used for the given method and
   * make a subscription for that method under that inferred name.
   * Method names (topics) are determined by the methodName of the method's methodName
   * which, if not set defaults to the fully qualified name of the class the method
   * belongs to plus the method name e.g. com.domain.prodct.className.method
   * @param klass
   * @param instance
   * @param method
   */
  def doRegister(klass: Class[_ <: AnyRef], instance: Any, method: Method) {
    val methodName = if (method.isAnnotationPresent(classOf[method])
      && !method.getAnnotation(classOf[method]).value().isEmpty) {
      //if a method name is provided then use it
      method.getAnnotation(classOf[method]).value()
    } else {
      //if no method name is provided, use the fully qualified class and method name
      klass.getName + "." + method.getName
    }
    doListen(methodName, method, instance)
    log.info("Method %s registered with name (topic) %s" format(method.getName, methodName))
  }

  /**
   * Subscribes a given instance and method to the given method name
   * When a message is received which matches this method, the method will be invoked
   * If the method arguments match the arguments received and the method returns a methodName
   * this methodName is returned to the client. If there is an exception while invoking the
   * given method then the client receives the error
   * @param methodName
   * @param method
   * @param instance
   */
  protected def doListen(methodName: String, method: Method, instance: Any) {
    if (listening(methodName)) {
      //TODO make configurable so that multiple callbacks can have the same method name
      throw new IllegalArgumentException("Method name %s is already registered!" format (methodName))
    }
    //listen for this method name to be invoked then call the given method on
    //the instance provided
    listen(methodName, (c: Channel, params: M) => {
      //method.invoke returns null if the underlying method returns "void"/Unit
      //so if no error occurred the return type should be Unit
      var returns: Option[Serializable] = None
      var error: Option[Throwable] = None
      var args: Array[AnyRef] = getArguments(params)
      try {
        val argTypes = method.getParameterTypes()
        val channelIndex = argTypes.indexOf(classOf[Channel])
        val rpcIndex = argTypes.indexOf(mf.erasure)
        //Channel MUST be first parameter and RPC must be second parameter if method wants
        //to accept both, if only one is require it must be the first parameter
        if (channelIndex != -1 && channelIndex == 0) args = Array(c) ++ args
        if (rpcIndex != -1) {
          args = if (channelIndex == 0) {
            //if method wants channel index then original message becomes param 2
            //in this case args(0) is already set to the channel
            Array(args(0), params.asInstanceOf[AnyRef]) ++ args.slice(1, args.length)
          } else {
            Array(params.asInstanceOf[AnyRef]) ++ args
          }
        }
        //Seq to var args syntax :_* - http://www.scala-lang.org/node/5209
        val res = method.invoke(instance, args: _*)
        returns = if (res != null) Some(res.asInstanceOf[Serializable]) else None
      } catch {
        case e => {
          error = Some(e)
          log.warn("Error invoking method %s with arguments %s : Path to method %s"
            format(methodName, args, method.getDeclaringClass.getName + "." + method.getName), e)
        }
      }
      respond(c,
        newResponse(methodName, clientCallback(params), returns, error))
    })
  }

  /**
   * When a message of type M is received, subclasses should know how to extract
   * the parameters from the message. These parameters must be an ordered sequence of objects
   * as they'll be used, in the order given as the parameters to the method to be invoked
   * @param param
   * @return
   */
  def getArguments(param: M): Array[AnyRef]

  /**
   * From the message M extract and return the name of the client's callback
   * if supported. If not an empty string will do.
   * @param param
   * @return
   */
  def clientCallback(param: M): String

  /**
   * Construct a response object to be sent to the client or scala.None if no
   * response is to be sent...
   * @param remoteMethodName  the method name that was invoked on the server
   * @param clientCallbackID  the name of the client callback method/id
   * @param response  The response that was returned from the method invoked (i.e. the response itself)
   * @param error    any exception that was thrown while attempting to invoke the method
   * @return  a message that'll be serialized and sent
   */
  def newResponse(remoteMethodName: String, clientCallbackID: String,
                  response: Option[Serializable], error: Option[Throwable]): M
}
