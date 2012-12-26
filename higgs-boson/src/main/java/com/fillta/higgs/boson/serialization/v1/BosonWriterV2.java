package com.fillta.higgs.boson.serialization.v1;

import com.fillta.higgs.boson.BosonMessage;
import com.fillta.higgs.boson.serialization.BosonProperty;
import com.fillta.higgs.reflect.ReflectionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.*;

import static com.fillta.higgs.boson.BosonType.*;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonWriterV2 {
	ByteBuf buffer = Unpooled.buffer();
	final BosonMessage msg;
	private Logger log = LoggerFactory.getLogger(getClass());
	/**
	 * The maximum number of times methods can invoked themselves.
	 */
	public static final int MAX_RECURSION_DEPTH = 10;
	private final ReflectionUtil reflection = new ReflectionUtil(MAX_RECURSION_DEPTH);

	public BosonWriterV2(BosonMessage msg) {
		this.msg = msg;
	}


	public ByteBuf serialize() {
		//first thing to write is the protocol version
		buffer.writeByte(msg.protocolVersion);
		//pad the buffer with 4 bytes which will be updated after serialization to set the size of the message
		buffer.writeInt(0);
		//then write the message itself
		if (msg.callback != null && !msg.callback.isEmpty()) {
			//otherwise its a request
			serializeRequest();
		} else {
			//if there's no callback then its a response...responses don't send callbacks
			serializeResponse();
		}
		//calculate the total size of the message. we wrote 5 bytes to the buffer before serializing
		//this means byte 6 until buffer.writerIndex() = total message size
		//set message size at index = 1 with value writerIndex() - 5 bytes
		buffer.setInt(1, buffer.writerIndex() - 5);
		return buffer;
	}

	private void serializeResponse() {
		//write the method name
		buffer.writeByte(RESPONSE_METHOD_NAME); //write type/flag - 1 byte
		writeString(msg.method);
		//write the parameters
		buffer.writeByte(RESPONSE_PARAMETERS); //write type/flag - int = 4 bytes
		validateAndWriteType(msg.arguments, null); //write the size/length and payload
		buffer.resetReaderIndex();
	}

	private void serializeRequest() {
		buffer.writeByte(REQUEST_METHOD_NAME); //write type/flag - 1 byte
		//write the method name
		writeString(msg.method);
		//write the callback name
		buffer.writeByte(REQUEST_CALLBACK); //write type/flag - 1 byte
		writeString(msg.callback);
		//write the parameters
		buffer.writeByte(REQUEST_PARAMETERS); //write type/flag - int = 4 bytes
		validateAndWriteType(msg.arguments, null); //write the size/length and payload
	}

	public void writeByte(byte b) {
		buffer.writeByte(BYTE);
		buffer.writeByte(b);
	}

	public void writeNull() {
		buffer.writeByte(NULL);
	}

	public void writeShort(short s) {
		buffer.writeByte(SHORT);
		buffer.writeShort(s);
	}

	public void writeInt(int i) {
		buffer.writeByte(INT);
		buffer.writeInt(i);
	}

	public void writeLong(long l) {
		buffer.writeByte(LONG);
		buffer.writeLong(l);
	}

	public void writeFloat(float f) {
		buffer.writeByte(FLOAT);
		buffer.writeFloat(f);
	}

	public void writeDouble(double d) {
		buffer.writeByte(DOUBLE);
		buffer.writeDouble(d);
	}

	public void writeBoolean(boolean b) {
		buffer.writeByte(BOOLEAN);
		if (b)
			buffer.writeByte(1);
		else
			buffer.writeByte(0);
	}

	public void writeChar(char c) {
		buffer.writeByte(CHAR);
		buffer.writeChar(c);
	}

	public void writeString(String s) {
		buffer.writeByte(STRING);//type
		try {
			byte[] str = s.getBytes("utf-8");
			buffer.writeInt(str.length); //size
			buffer.writeBytes(str); //payload
		} catch (UnsupportedEncodingException e) {
		}

	}

	public void writeList(List<Object> value, IdentityHashMap<Object, Object> graph) {
		buffer.writeByte(LIST); //type
		buffer.writeInt(value.size()); //size
		Iterator it = value.iterator();
		while (it.hasNext()) {
			Object param = it.next();
			if (param == null) {
				writeNull();
			} else {
				if (!circularGraph(param, graph)) {
					validateAndWriteType(param, graph); //payload
				}
			}
		}
	}

	/**
	 * Write an array of any supported boson type to the given buffer.
	 * If the buffer contains any unsupported type, this will fail by throwing an UnsupportedBosonTypeException
	 *
	 * @param value
	 */
	public void writeArray(Object[] value, IdentityHashMap<Object, Object> graph) {
		buffer.writeByte(ARRAY); //type
		//we write the component type of the array or null if its not an array
		int component = getArrayComponent(value.getClass());
		buffer.writeByte(component);
		buffer.writeInt(value.length); //size
		for (Object param : value) {
			if (!circularGraph(param, graph)) {
				validateAndWriteType(param, graph); //payload
			}
		}
	}

	public void writeMap(Map<?, ?> value, IdentityHashMap<Object, Object> graph) {
		buffer.writeByte(MAP); //type
		buffer.writeInt(value.size());//size
		Iterator<?> it = value.keySet().iterator();
		while (it.hasNext()) {
			Object key = it.next();
			Object v = value.get(key);
			if (!circularGraph(key, graph)) {
				validateAndWriteType(key, graph);//key payload
			}
			if (!circularGraph(v, graph)) {
				validateAndWriteType(v, graph); //value payload
			}
		}
	}

	/**
	 * Serialize any* Java object.
	 * Circular reference support based on
	 * http://beza1e1.tuxen.de/articles/deepcopy.html
	 * http://stackoverflow.com/questions/5157764/java-detect-circular-references-during-custom-cloning
	 *
	 * @param obj
	 * @return
	 */
	public boolean writePolo(Object obj, IdentityHashMap<Object, Object> graph) {
		if (obj == null) {
			validateAndWriteType(obj, graph);
			return false;
		}
		if (circularGraph(obj, graph)) {
			return true;
		}
		Class<BosonProperty> propertyClass = BosonProperty.class;
		Class<?> klass = obj.getClass();
		Map data = new HashMap<String, Object>();
		boolean ignoreInheritedFields = false;
		if (klass.isAnnotationPresent(propertyClass)) {
			ignoreInheritedFields = klass.getAnnotation(propertyClass).ignoreInheritedFields();
		}
		//get ALL (public,private,protect,package) fields declared in the class - includes inherited fields
		List<Field> fields = reflection.getAllFields(new ArrayList<Field>(), klass, 0);
		for (Field field : fields) {
			//if inherited fields are to be ignored then fields must be declared in the current class
			if (ignoreInheritedFields && klass != field.getDeclaringClass()) {
				continue;
			}
			field.setAccessible(true);
			boolean add = true;
			field.setAccessible(true);
			String name = field.getName();
			//add if annotated with BosonProperty
			if (field.isAnnotationPresent(propertyClass)) {
				BosonProperty ann = field.getAnnotation(propertyClass);
				if (ann != null && !ann.value().isEmpty()) {
					name = ann.value();
				}
				if (ann.ignore()) {
					add = false;
				}
				//if configured to ignore inherited fields then
				//only fields declared in the object's class are allowed
				if (ann.ignoreInheritedFields() && field.getDeclaringClass() != klass) {
					add = false;
				}
			}
			if (add) {
				try {
					data.put(name, field.get(obj));
				} catch (IllegalAccessException e) {
					log.warn(String.format("Unable to access field %s in class %s", field.getName(), field.getDeclaringClass().getName()), e);
				}
			}
		}
		//if at least one field is allowed to be serialized
		if (data.size() > 0) {
			buffer.writeByte(POLO); //type
			writeString(klass.getName());//class name
			buffer.writeInt(data.size()); //size
			Iterator<?> it = data.keySet().iterator();
			while (it.hasNext()) {
				Object key = it.next();
				Object value = data.get(key);
				writeString((String) key);//key payload must be a string
				validateAndWriteType(value, graph); //value payload
			}
		}
		//if no fields found that can be serialized then the arguments array
		//length will be more than it should be.
		return data.size() > 0;
	}

	private boolean circularGraph(final Object obj, IdentityHashMap<Object, Object> graph) {
		if (graph.containsKey(obj)) {
			//already serialized this object
			//TODO modify Boson protocol spec so that a reference is written in place of this object
			log.warn("Circular reference detected");
			return true;
		}
		//don't forget!!!
		graph.put(obj, null);
		return false;
	}

	/**
	 * The JVM would return the java keywords int, long etc for all primitive types
	 * on an array using the rules outlined below.
	 * This is of no use when serializing/de-serializing so this method converts
	 * java primitive names to their boson data type equivalent.
	 * The rest of this java doc is from Java's Class class
	 * which details how it treats array of primitives.
	 * <p/>
	 * <p> If this class object represents a primitive type or void, then the
	 * name returned is a {@code String} equal to the Java language
	 * keyword corresponding to the primitive type or void.
	 * <p/>
	 * <p> If this class object represents a class of arrays, then the internal
	 * form of the name consists of the name of the element type preceded by
	 * one or more '{@code [}' characters representing the depth of the array
	 * nesting.  The encoding of element type names is as follows:
	 * <p/>
	 * <blockquote><table summary="Element types and encodings">
	 * <tr><th> Element Type <th> &nbsp;&nbsp;&nbsp; <th> Encoding
	 * <tr><td> boolean      <td> &nbsp;&nbsp;&nbsp; <td align=center> Z
	 * <tr><td> byte         <td> &nbsp;&nbsp;&nbsp; <td align=center> B
	 * <tr><td> char         <td> &nbsp;&nbsp;&nbsp; <td align=center> C
	 * <tr><td> class or interface
	 * <td> &nbsp;&nbsp;&nbsp; <td align=center> L<i>classname</i>;
	 * <tr><td> double       <td> &nbsp;&nbsp;&nbsp; <td align=center> D
	 * <tr><td> float        <td> &nbsp;&nbsp;&nbsp; <td align=center> F
	 * <tr><td> int          <td> &nbsp;&nbsp;&nbsp; <td align=center> I
	 * <tr><td> long         <td> &nbsp;&nbsp;&nbsp; <td align=center> J
	 * <tr><td> short        <td> &nbsp;&nbsp;&nbsp; <td align=center> S
	 * </table></blockquote>
	 * <p/>
	 * <p> The class or interface name <i>classname</i> is the binary name of
	 * the class specified above.
	 * <p/>
	 * <p> Examples:
	 * <blockquote><pre>
	 * String.class.getName()
	 * returns "java.lang.String"
	 * byte.class.getName()
	 * returns "byte"
	 * (new Object[3]).getClass().getName()
	 * returns "[Ljava.lang.Object;"
	 * (new int[3][4][5][6][7][8][9]).getClass().getName()
	 * returns {@code "[[[[[[[ I "}
	 * </pre></blockquote>
	 *
	 * @return the fully qualified class name of a java primitive or null if the class
	 *         is not an array
	 */
	public int getArrayComponent(Class<?> klass) {
		String name = null;
		if (klass.isArray()) name = klass.getComponentType().getName();
		if (name.equals("boolean") || name.equals("java.lang.Boolean")) {
			return BOOLEAN;
		} else if (name.equals("byte") || name.equals("java.lang.Byte")) {
			return BYTE;
		} else if (name.equals("char") || name.equals("java.lang.Character")) {
			return CHAR;
		} else if (name.equals("double") || name.equals("java.lang.Double")) {
			return DOUBLE;
		} else if (name.equals("float") || name.equals("java.lang.Float")) {
			return FLOAT;
		} else if (name.equals("int") || name.equals("java.lang.Integer")) {
			return INT;
		} else if (name.equals("long") || name.equals("java.lang.Long")) {
			return LONG;
		} else if (name.equals("short") || name.equals("java.lang.Short")) {
			return SHORT;
		} else {
			return POLO;
		}
	}

	public void validateAndWriteType(Object param, IdentityHashMap<Object, Object> graph) {
		if (param == null) {
			writeNull();
		} else {
			if (graph == null) {
				graph = new IdentityHashMap<>();
			}
			Class<?> obj = param.getClass();
			if (obj == Byte.class) {
				writeByte((Byte) param);
			} else if (obj == Short.class) {
				writeShort((Short) param);
			} else if (obj == Integer.class) {
				writeInt((Integer) param);
			} else if (obj == Long.class) {
				writeLong((Long) param);
			} else if (obj == Float.class) {
				writeFloat((Float) param);
			} else if (obj == Double.class) {
				writeDouble((Double) param);
			} else if (obj == Boolean.class) {
				writeBoolean((Boolean) param);
			} else if (obj == Character.class) {
				writeChar((Character) param);
			} else if (obj == String.class) {
				writeString((String) param);
			} else if (List.class.isAssignableFrom(obj)) {
				writeList((List) param, graph);
			} else if (obj.isArray()) {
				writeArray((Object[]) param, graph);
			} else if (Map.class.isAssignableFrom(obj)) {
				writeMap((Map) param, graph);
			} else {
				if (!writePolo(param, graph)) {
					log.warn(String.format("%s is not a supported type, see BosonType for a list of supported types", obj.getName()));
				}
			}
		}
	}
}
