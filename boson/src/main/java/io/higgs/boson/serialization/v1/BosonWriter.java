package io.higgs.boson.serialization.v1;

import io.higgs.boson.BosonMessage;
import io.higgs.boson.serialization.BosonProperty;
import io.higgs.boson.serialization.mutators.ReadMutator;
import io.higgs.core.reflect.ReflectionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.higgs.boson.BosonType.ARRAY;
import static io.higgs.boson.BosonType.BOOLEAN;
import static io.higgs.boson.BosonType.BYTE;
import static io.higgs.boson.BosonType.CHAR;
import static io.higgs.boson.BosonType.DOUBLE;
import static io.higgs.boson.BosonType.FLOAT;
import static io.higgs.boson.BosonType.INT;
import static io.higgs.boson.BosonType.LIST;
import static io.higgs.boson.BosonType.LONG;
import static io.higgs.boson.BosonType.MAP;
import static io.higgs.boson.BosonType.NULL;
import static io.higgs.boson.BosonType.POLO;
import static io.higgs.boson.BosonType.REFERENCE;
import static io.higgs.boson.BosonType.REQUEST_CALLBACK;
import static io.higgs.boson.BosonType.REQUEST_METHOD_NAME;
import static io.higgs.boson.BosonType.REQUEST_PARAMETERS;
import static io.higgs.boson.BosonType.RESPONSE_METHOD_NAME;
import static io.higgs.boson.BosonType.RESPONSE_PARAMETERS;
import static io.higgs.boson.BosonType.SET;
import static io.higgs.boson.BosonType.SHORT;
import static io.higgs.boson.BosonType.STRING;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonWriter {
    /**
     * The maximum number of times methods can invoked themselves.
     */
    public static final int MAX_RECURSION_DEPTH = 10;
    public static final Charset utf8 = Charset.forName("utf-8");
    protected final HashMap<Object, Integer> references = new HashMap<>();
    protected final AtomicInteger reference = new AtomicInteger();
    protected final Set<ReadMutator> mutators;
    private Logger log = LoggerFactory.getLogger(getClass());

    public BosonWriter() {
        this(null);
    }

    public BosonWriter(Set<ReadMutator> mutators) {
        this.mutators = mutators == null ? new HashSet<ReadMutator>() : mutators;
    }

    /**
     * Serialize any object to a series of bytes.
     * This is the same as {@link #serialize(io.higgs.boson.BosonMessage)} except it doesn't contain
     * any Boson headers required for network transport
     *
     * @param msg the message to serialize
     * @return a series of bytes representing the message
     */
    public ByteBuf serialize(Object msg) {
        ByteBuf buffer = Unpooled.buffer();
        validateAndWriteType(buffer, msg);
        buffer.readerIndex(0);
        return buffer;
    }

    /**
     * Serializes a {@link io.higgs.boson.BosonMessage} to the wire format specified by the protocol spec
     *
     * @param msg the message to serialize
     * @return the message serialized to a series of bytes
     */
    public ByteBuf serialize(BosonMessage msg) {
        ByteBuf buffer = Unpooled.buffer();
        //first thing to write is the protocol version
        buffer.writeByte(msg.protocolVersion);
        //pad the buffer with 4 bytes which will be updated after serialization to set the size of the message
        buffer.writeInt(0);
        //then write the message itself
        if (msg.callback != null && !msg.callback.isEmpty()) {
            //otherwise its a request
            serializeRequest(buffer, msg);
        } else {
            //if there's no callback then its a response...responses don't send callbacks
            serializeResponse(buffer, msg);
        }
        //calculate the total size of the message. we wrote 5 bytes to the buffer before serializing
        //this means byte 6 until buffer.writerIndex() = total message size
        //set message size at index = 1 with value writerIndex() - 5 bytes
        buffer.setInt(1, buffer.writerIndex() - 5);
        buffer.readerIndex(0);
        return buffer;
    }

    protected void serializeResponse(ByteBuf buffer, BosonMessage msg) {
        //write the method name
        buffer.writeByte(RESPONSE_METHOD_NAME); //write type/flag - 1 byte
        writeString(buffer, msg.method);
        //write the parameters
        buffer.writeByte(RESPONSE_PARAMETERS); //write type/flag - int = 4 bytes
        validateAndWriteType(buffer, msg.arguments); //write the size/length and payload
    }

    protected void serializeRequest(ByteBuf buffer, BosonMessage msg) {
        buffer.writeByte(REQUEST_METHOD_NAME); //write type/flag - 1 byte
        //write the method name
        writeString(buffer, msg.method);
        //write the callback name
        buffer.writeByte(REQUEST_CALLBACK); //write type/flag - 1 byte
        writeString(buffer, msg.callback);
        //write the parameters
        buffer.writeByte(REQUEST_PARAMETERS); //write type/flag - int = 4 bytes
        validateAndWriteType(buffer, msg.arguments); //write the size/length and payload
    }

    public void writeByte(ByteBuf buffer, byte b) {
        buffer.writeByte(BYTE);
        buffer.writeByte(b);
    }

    public void writeNull(ByteBuf buffer) {
        buffer.writeByte(NULL);
    }

    public void writeShort(ByteBuf buffer, short s) {
        buffer.writeByte(SHORT);
        buffer.writeShort(s);
    }

    public void writeInt(ByteBuf buffer, int i) {
        buffer.writeByte(INT);
        buffer.writeInt(i);
    }

    public void writeLong(ByteBuf buffer, long l) {
        buffer.writeByte(LONG);
        buffer.writeLong(l);
    }

    public void writeFloat(ByteBuf buffer, float f) {
        buffer.writeByte(FLOAT);
        buffer.writeFloat(f);
    }

    public void writeDouble(ByteBuf buffer, double d) {
        buffer.writeByte(DOUBLE);
        buffer.writeDouble(d);
    }

    public void writeBoolean(ByteBuf buffer, boolean b) {
        buffer.writeByte(BOOLEAN);
        if (b) {
            buffer.writeByte(1);
        } else {
            buffer.writeByte(0);
        }
    }

    public void writeChar(ByteBuf buffer, char c) {
        buffer.writeByte(CHAR);
        buffer.writeChar(c);
    }

    public void writeString(ByteBuf buffer, String s) {
        buffer.writeByte(STRING); //type
        byte[] str = s.getBytes(utf8);
        buffer.writeInt(str.length); //size
        buffer.writeBytes(str); //payload
    }

    public void writeList(ByteBuf buffer, List<Object> value) {
        buffer.writeByte(LIST); //type
        buffer.writeInt(value.size()); //size
        for (Object param : value) {
            if (param == null) {
                writeNull(buffer);
            } else {
                validateAndWriteType(buffer, param); //payload
            }
        }
    }

    public void writeSet(ByteBuf buffer, Set<Object> value) {
        buffer.writeByte(SET); //type
        buffer.writeInt(value.size()); //size
        for (Object param : value) {
            if (param == null) {
                writeNull(buffer);
            } else {
                validateAndWriteType(buffer, param); //payload
            }
        }
    }

    /**
     * Write an array of any supported boson type to the given buffer.
     * If the buffer contains any unsupported type, this will fail by throwing an UnsupportedBosonTypeException
     *
     * @param value the value to write
     */
    public void writeArray(ByteBuf buffer, Object[] value) {
        buffer.writeByte(ARRAY); //type
        buffer.writeInt(value.length); //size
        for (Object param : value) {
            validateAndWriteType(buffer, param); //payload
        }
    }

    public void writeMap(ByteBuf buffer, Map<?, ?> value) {
        buffer.writeByte(MAP); //type
        buffer.writeInt(value.size()); //size
        for (Object key : value.keySet()) {
            Object v = value.get(key);
            validateAndWriteType(buffer, key); //key payload
            validateAndWriteType(buffer, v); //value payload
        }
    }

    /**
     * Serialize any* Java object.
     * Circular reference support based on
     * http://beza1e1.tuxen.de/articles/deepcopy.html
     * http://stackoverflow.com/questions/5157764/java-detect-circular-references-during-custom-cloning
     *
     * @param obj the object to write
     * @param ref
     * @return true on success
     */
    public void writePolo(ByteBuf buffer, Object obj, int ref) {
        if (obj == null) {
            validateAndWriteType(buffer, obj);
            return;
        }
        Map<String, Object> data = new HashMap<>();
        Class<?> klass = obj.getClass();
        ReadMutator mutator = null;
        for (ReadMutator m : mutators) {
            if (m.canReadFields(klass, obj)) {
                mutator = m;
                break;
            }
        }
        if (mutator != null) {
            writePoloFieldsViaMutator(mutator, klass, obj, data);
        } else {
            writePoloFieldsViaReflection(klass, obj, data);
        }
        //if at least one field is allowed to be serialized
        buffer.writeByte(POLO); //type
        //write the POLO's reference number
        buffer.writeInt(ref);
        writeString(buffer, klass.getName()); //class name
        buffer.writeInt(data.size()); //size
        for (String key : data.keySet()) {
            Object value = data.get(key);
            writeString(buffer, key); //key payload must be a string
            validateAndWriteType(buffer, value); //value payload
        }
    }

    private void writePoloFieldsViaMutator(ReadMutator mutator, Class<?> klass, Object obj, Map<String, Object> data) {
        List<String> fields = mutator.fields(klass, obj);
        for (String field : fields) {
            Object value = mutator.get(klass, obj, field);
            data.put(field, value);
        }
    }

    private void writePoloFieldsViaReflection(Class<?> klass, Object obj, Map<String, Object> data) {
        Class<BosonProperty> propertyClass = BosonProperty.class;
        boolean ignoreInheritedFields = false;
        if (klass.isAnnotationPresent(propertyClass)) {
            ignoreInheritedFields = klass.getAnnotation(propertyClass).ignoreInheritedFields();
        }
        //get ALL (public,private,protect,package) fields declared in the class - includes inherited fields
        Set<Field> fields = ReflectionUtil.getAllFields(new HashSet<Field>(), klass, 0);
        for (Field field : fields) {
            //if inherited fields are to be ignored then fields must be declared in the current class
            if (ignoreInheritedFields && klass != field.getDeclaringClass()) {
                continue;
            }
            if (Modifier.isFinal(field.getModifiers())) {
                continue; //no point in serializing final fields
            }
            field.setAccessible(true);
            boolean add = true;
            String name = field.getName();
            //add if annotated with BosonProperty
            if (field.isAnnotationPresent(propertyClass)) {
                BosonProperty ann = field.getAnnotation(propertyClass);
                if (ann != null && !ann.value().isEmpty()) {
                    name = ann.value();
                }
                if ((ann != null) && ann.ignore()) {
                    add = false;
                }
                //if configured to ignore inherited fields then
                //only fields declared in the object's class are allowed
                if ((ann != null) && ann.ignoreInheritedFields() && field.getDeclaringClass() != klass) {
                    add = false;
                }
            }
            if (add) {
                try {
                    data.put(name, field.get(obj));
                } catch (IllegalAccessException e) {
                    log.warn(String.format("Unable to access field %s in class %s", field.getName(),
                            field.getDeclaringClass().getName()), e);
                }
            }
        }
    }

    /**
     * @param buffer the buffer to write to
     * @param param  the param to write to the buffer
     */
    public void validateAndWriteType(ByteBuf buffer, Object param) {
        if (param == null) {
            writeNull(buffer);
        } else {
            if (param instanceof Byte || Byte.class.isAssignableFrom(param.getClass())) {
                writeByte(buffer, (Byte) param);
            } else if (param instanceof Short || Short.class.isAssignableFrom(param.getClass())) {
                writeShort(buffer, (Short) param);
            } else if (param instanceof Integer || Integer.class.isAssignableFrom(param.getClass())) {
                writeInt(buffer, (Integer) param);
            } else if (param instanceof Long || Long.class.isAssignableFrom(param.getClass())) {
                writeLong(buffer, (Long) param);
            } else if (param instanceof Float || Float.class.isAssignableFrom(param.getClass())) {
                writeFloat(buffer, (Float) param);
            } else if (param instanceof Double || Double.class.isAssignableFrom(param.getClass())) {
                writeDouble(buffer, (Double) param);
            } else if (param instanceof Boolean || Boolean.class.isAssignableFrom(param.getClass())) {
                writeBoolean(buffer, (Boolean) param);
            } else if (param instanceof Character || Character.class.isAssignableFrom(param.getClass())) {
                writeChar(buffer, (Character) param);
            } else if (param instanceof String || String.class.isAssignableFrom(param.getClass())) {
                writeString(buffer, (String) param);
            } else if (param instanceof List || List.class.isAssignableFrom(param.getClass())) {
                writeList(buffer, (List<Object>) param);
            } else if (param instanceof Set || Set.class.isAssignableFrom(param.getClass())) {
                writeSet(buffer, (Set<Object>) param);
            } else if (param instanceof Map || Map.class.isAssignableFrom(param.getClass())) {
                writeMap(buffer, (Map<Object, Object>) param);
            } else if (param.getClass().isArray()) {
                //array values can be reference types but not the arrays themselves
                writeArray(buffer, (Object[]) param);
            } else {
                if (param instanceof Throwable) {
                    throw new UnsupportedOperationException("Cannot serialize throwable", (Throwable) param);
                }
                //in reference list?
                Integer ref = references.get(param);
                //no
                if (ref == null) {
                    //assign unique reference number
                    ref = reference.getAndIncrement();
                    //add to reference list
                    references.put(param, ref);
                    writePolo(buffer, param, ref);
                } else {
                    //yes -  write reference
                    writeReference(buffer, ref);
                }
            }
        }
    }

    private void writeReference(ByteBuf buffer, Integer ref) {
        //if the object has been written already then write a negative reference
        buffer.writeByte(REFERENCE);
        buffer.writeInt(ref);
    }
}
