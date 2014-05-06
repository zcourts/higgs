package io.higgs.boson.serialization.v1;

import io.higgs.boson.BosonMessage;
import io.higgs.boson.serialization.InvalidDataException;
import io.higgs.boson.serialization.InvalidRequestResponseTypeException;
import io.higgs.boson.serialization.UnsupportedBosonTypeException;
import io.higgs.boson.serialization.mutators.WriteMutator;
import io.higgs.core.reflect.ReflectionUtil;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class BosonReader {
    protected final Set<WriteMutator> mutators;
    protected Logger log = LoggerFactory.getLogger(getClass());
    protected ClassLoader loader = Thread.currentThread().getContextClassLoader();
    protected IdentityHashMap<Integer, Object> references = new IdentityHashMap<>();

    public BosonReader() {
        this(null);
    }

    public BosonReader(Set<WriteMutator> mutators) {
        this.mutators = mutators == null ? new HashSet<WriteMutator>() : mutators;
    }

    public <T> T deSerialize(ByteBuf buf) {
        Object obj = readType(buf);
        return (T) obj;
    }

    public void deSerialize(ByteBuf data, BosonMessage msg) {
        //reset reder index, BosonDecoder would have set to writerIndex
        data.readerIndex(0);
        //protocol version and message size is not a part of the message so read before loop
        //advance reader index by 1
        msg.protocolVersion = data.readByte();
        //move reader index forward by 4
        int msgSize = data.readInt();
        //so read until the reader index == obj.length
        while (data.isReadable()) {
            //read request/response types
            int type = data.readByte();

            switch (type) {
                case RESPONSE_METHOD_NAME: {
                    msg.method = readString(data, false, 0);
                    break;
                }
                case RESPONSE_PARAMETERS: {
                    msg.arguments = readArray(data, false, 0);
                    break;
                }
                case REQUEST_METHOD_NAME: {
                    msg.method = readString(data, false, 0);
                    break;
                }
                case REQUEST_CALLBACK: {
                    msg.callback = readString(data, false, 0);
                    break;
                }
                case REQUEST_PARAMETERS: {
                    msg.arguments = readArray(data, false, 0);
                    break;
                }
                default:
                    throw new InvalidRequestResponseTypeException(String.
                            format("The type %s does not match any of the supported" +
                                            " response or request types (method,callback,parameter)" +
                                            "\n data: \n %s",
                                    type, new String(data.array())
                            ), null);
            }
        }
    }

    public Object readType(ByteBuf buffer) {
        byte type = buffer.readByte();
        switch (type) {
            case BYTE:
                return readByte(buffer, true, BYTE);
            case SHORT:
                return readShort(buffer, true, SHORT);
            case INT:
                return readInt(buffer, true, INT);
            case LONG:
                return readLong(buffer, true, LONG);
            case FLOAT:
                return readFloat(buffer, true, FLOAT);
            case DOUBLE:
                return readDouble(buffer, true, FLOAT);
            case BOOLEAN:
                return readBoolean(buffer, true, BOOLEAN);
            case CHAR:
                return readChar(buffer, true, CHAR);
            case STRING:
                return readString(buffer, true, STRING);
            case LIST:
                return readList(buffer, true, LIST);
            case SET:
                return readSet(buffer, true, SET);
            case MAP:
                return readMap(buffer, true, MAP);
            case ARRAY:
                return readArray(buffer, true, ARRAY);
            case POLO:
                return readPolo(buffer, true, POLO);
            default:
                throw new UnsupportedBosonTypeException(String.format("Cannot decode object from type %s", type), null);
        }
    }

    /**
     * Check that the backing buffer is readable.
     * If it isn't throws an InvalidDataException
     *
     * @throws InvalidDataException if buffer is not readable
     */
    public void verifyReadable(ByteBuf data) {
        if (!data.isReadable()) {
            throw new InvalidDataException("BosonReader tried to read additional data from an unreadable buffer. " +
                    "Possible data corruption.", null);
        }
    }

    /**
     * Read a UTF-8 string from the buffer
     *
     * @param data
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the string
     */
    public String readString(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (STRING == type) {
            verifyReadable(data);
            //read size of type - how many bytes are in the string
            int size = data.readInt();
            if (size == 0) {
                return "";
            }
            //read type's payload and de-serialize
            ByteBuf buf = data.readBytes(size);
            byte[] arr = new byte[buf.writerIndex()];
            buf.getBytes(0, arr);
            return new String(arr, Charset.forName("utf8"));
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson STRING", type), null);
        }
    }

    /**
     * Read a single byte from the buffer
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the byte
     */
    public byte readByte(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (BYTE == type) {
            verifyReadable(data);
            return data.readByte();
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson BYTE", type), null);
        }
    }

    /**
     * Read a short (16 bits) from the buffer
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the short
     */
    public short readShort(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (SHORT == type) {
            verifyReadable(data);
            return data.readShort();
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson SHORT", type), null);
        }
    }

    /**
     * Read an int (4 bytes) from the buffer
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the int
     */
    public int readInt(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (INT == type) {
            verifyReadable(data);
            return data.readInt();
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson INT", type), null);
        }
    }

    /**
     * Read a long (8 bytes) from the buffer
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the long
     */
    public long readLong(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (LONG == type) {
            verifyReadable(data);
            return data.readLong();
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson LONG", type), null);
        }
    }

    /**
     * Read a float (32 bit floating point) from the buffer
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the float
     */
    public float readFloat(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (FLOAT == type) {
            verifyReadable(data);
            return data.readFloat();
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson FLOAT", type), null);
        }
    }

    /**
     * Read a double (64 bit floating point) from the buffer
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the double
     */
    public double readDouble(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (DOUBLE == type) {
            verifyReadable(data);
            return data.readDouble();
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson DOUBLE", type), null);
        }
    }

    /**
     * Read a a single byte from the buffer   if the byte is 1 then returns true, otherwise false
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the boolean
     */
    public boolean readBoolean(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (BOOLEAN == type) {
            verifyReadable(data);
            return data.readByte() != 0;
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson BOOLEAN", type), null);
        }
    }

    /**
     * Read a char (16 bits) from the buffer
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the char
     */
    public char readChar(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (CHAR == type) {
            verifyReadable(data);
            return data.readChar();
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson CHAR", type), null);
        }
    }

    /**
     * Read an array from the buffer
     *
     * @param data
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the array
     */
    public Object[] readArray(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (ARRAY == type) {
            //read number of elements in the array
            int size = data.readInt();
            Object[] arr = new Object[size];
            for (int i = 0; i < size; i++) {
                type = data.readByte();
                arr[i] = readType(data, type);
            }
            return arr;
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson ARRAY", type), null);
        }
    }

    /**
     * Read a List from the buffer
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the list
     */
    public List<Object> readList(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (LIST == type) {
            //read number of elements in the array
            int size = data.readInt();
            List<Object> arr = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                verifyReadable(data);
                //get type of this element in the array
                type = data.readByte();
                //at this stage only basic data types are allowed
                arr.add(readType(data, type));
            }
            return arr;
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson LIST", type), null);
        }
    }

    public Set<Object> readSet(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (SET == type) {
            //read number of elements in the array
            int size = data.readInt();
            Set<Object> set = new HashSet<>();
            for (int i = 0; i < size; i++) {
                verifyReadable(data);
                //get type of this element in the array
                type = data.readByte();
                //at this stage only basic data types are allowed
                set.add(readType(data, type));
            }
            return set;
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson SET", type), null);
        }
    }

    /**
     * Read a map (list of key -> value pairs) from the buffer
     *
     * @param verified     if true then the verifiedType param is used to match the type, if false then
     *                     a single byte is read from the buffer to determine the type
     * @param verifiedType the data type to be de-serialized
     * @return the map
     */
    public Map<Object, Object> readMap(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (MAP == type) {
            int size = data.readInt();
            Map<Object, Object> kv = new HashMap<>();
            for (int i = 0; i < size; i++) {
                verifyReadable(data);
                int keyType = data.readByte();
                Object key = readType(data, keyType);
                int valueType = data.readByte();
                Object value = readType(data, valueType);
                kv.put(key, value);
            }
            return kv;
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson MAP", type), null);
        }
    }

    public Object readPolo(ByteBuf data, boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        if (POLO == type) {
            verifyReadable(data);
            //get reference
            int ref = data.readInt();
            //get class name
            String poloClassName = readString(data, false, -1);
            if (poloClassName == null || poloClassName.isEmpty()) {
                throw new InvalidDataException("Cannot de-serialise a POLO without it's fully qualified class name " +
                        "being provided", null);
            }
            //get number of fields serialized
            int size = data.readInt();
            WriteMutator mutator = null;
            for (WriteMutator m : mutators) {
                if (m.canCreate(poloClassName)) {
                    mutator = m;
                    break;
                }
            }
            if (mutator != null) {
                return readPoloMutator(data, mutator, poloClassName, ref, size);
            } else {
                return readPoloReflection(data, poloClassName, ref, size);
            }
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson POLO", type), null);
        }
    }

    private Object readPoloMutator(ByteBuf data, WriteMutator mutator, String className, int ref, int size) {
        Object instance = mutator.newInstance(className);
        references.put(ref, instance);
        for (int i = 0; i < size; i++) {
            verifyReadable(data);
            //polo keys are required to be strings
            String key = readString(data, false, 0);
            verifyReadable(data);
            int valueType = data.readByte();
            Object value = readType(data, valueType);
            //TODO if false is returned try to set the field via reflection
            mutator.set(instance, key, value);
        }
        return instance;
    }

    private Object readPoloReflection(ByteBuf data, String poloClassName, int ref, int size) {
        //try to load the class if available
        try {
            Class<?> klass;
            try {
                klass = loader.loadClass(poloClassName);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(String.format("Cannot load the requested class %s",
                        poloClassName), e);
            }
            Object instance = klass.newInstance();
            //Put the instance in the reference table
            references.put(ref, instance);
            //get ALL (public,private,protect,package) fields declared in the class - excludes inherited fields
            Set<Field> fields = ReflectionUtil.getAllFields(new HashSet<Field>(), klass, 0);
            //create a map of fields names -> Field
            Map<String, Field> fieldset = new HashMap<>();
            for (Field field : fields) {
                if (!Modifier.isFinal(field.getModifiers())) {
                    //only add non-final fields
                    fieldset.put(field.getName(), field);
                }
            }
            for (int i = 0; i < size; i++) {
                verifyReadable(data);
                //polo keys are required to be strings
                String key = readString(data, false, 0);
                verifyReadable(data);
                int valueType = data.readByte();
                Object value = readType(data, valueType);
                Field field = fieldset.get(key);
                if (field != null && value != null) {
                    field.setAccessible(true);
                    //if field's type is an array  create an array of it's type
                    Class<?> fieldType = field.getType();
                    String cname = value == null ? "null" : value.getClass().getName();
                    if (fieldType.isArray()) {
                        if (value.getClass().isArray()) {
                            int length = Array.getLength(value);
                            //create an array of the expected type
                            Object arr = Array.newInstance(fieldType.getComponentType(), length);
                            for (int j = 0; j < length; j++) {
                                try {
                                    //get current array value
                                    Object arrayValue = Array.get(value, j);
                                    Array.set(arr, j, arrayValue); //set the value at the current index, i
                                } catch (IllegalArgumentException iae) {
                                    log.warn(String.format("Field \":%s\" of class \"%s\" is an array but " +
                                                    "failed to set value at index \"%s\" - type \"%s\"",
                                            key, klass.getName(), j, cname
                                    ));
                                }
                            }
                            try {
                                field.set(instance, arr);
                            } catch (IllegalAccessException e) {
                                log.debug(String.format("Unable to access field \"%s\" of class \"%s\" ", key,
                                        klass.getName()));
                            }
                        } else {
                            log.warn(String.format("Field \":%s\" of class \"%s\" is an array but value " +
                                    "received is \"%s\" of type \"%s\"", key, klass.getName(), value, cname));
                        }
                    } else {
                        try {
                            field.set(instance, value);
                        } catch (IllegalArgumentException iae) {
                            String vclass = value.getClass().getName();
                            log.warn(String.format("Field \"%s\" of class \"%s\" is of type %s " +
                                            "but value received is \"%s\" of type \"%s\"",
                                    key, klass.getName(), vclass, value, cname
                            ));
                        } catch (IllegalAccessException e) {
                            log.debug(String.format("Unable to access field \"%s\" of class \"%s\" ",
                                    key, klass.getName()));
                        }
                    }
                } else {
                    if (value != null) {
                        log.warn(String.format("Field %s received with value %s but the " +
                                "field does not exist in class %s", key, value, poloClassName));
                    }
                }
            }
            return instance;
        } catch (InstantiationException e) {
            log.warn("Unable to create an instance", e);
        } catch (IllegalAccessException e) {
            log.debug("Unable to access field", e);
        }
        return null;
    }

    public Object readReference(ByteBuf data, final boolean verified, int verifiedType) {
        int type = verifiedType;
        if (!verified) {
            type = data.readByte();
        }
        Object obj;
        if (REFERENCE == type) {
            int reference = data.readInt();
            obj = references.get(reference);
            return obj;
        } else {
            throw new UnsupportedBosonTypeException(String.format("type %s is not a Boson reference", type), null);
        }
    }

    /**
     * Read the next type from the buffer.
     * The type param must match one of Boson's supported types otherwise an exception is thrown
     *
     * @param type the 1 byte integer representing a Boson type
     * @return the type
     */
    public Object readType(ByteBuf data, int type) {
        switch (type) {
            case BYTE:
                return readByte(data, true, type);
            case SHORT:
                return readShort(data, true, type);
            case INT:
                return readInt(data, true, type);
            case LONG:
                return readLong(data, true, type);
            case FLOAT:
                return readFloat(data, true, type);
            case DOUBLE:
                return readDouble(data, true, type);
            case BOOLEAN:
                return readBoolean(data, true, type);
            case CHAR:
                return readChar(data, true, type);
            case NULL:
                return null;
            case STRING:
                return readString(data, true, type);
            case ARRAY:
                return readArray(data, true, type);
            case LIST:
                return readList(data, true, type);
            case SET:
                return readSet(data, true, type);
            case MAP:
                return readMap(data, true, type);
            case POLO:
                return readPolo(data, true, type);
            case REFERENCE:
                return readReference(data, true, type);
            default: {
                throw new UnsupportedBosonTypeException(String.format("type %s is not a valid boson type", type), null);
            }
        }
    }
}
