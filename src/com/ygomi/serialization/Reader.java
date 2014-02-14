package com.ygomi.serialization;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Reader {

    private byte[] buffer = null;
    private int capacity = 4096;
    private int position = 0;
    private ArrayList<String> classNameList = new ArrayList<String>();
    private HashMap<Object, Field[]> readClassFieldsMap = new HashMap<Object, Field[]>();
    private DataInputStream mInput = null;
    private YgmObjectInputStream ygmInputStream = null;
    private Object defaultObject = null;
    private List<Object> objectsReaded = new ArrayList<Object>();

    public Reader(YgmObjectInputStream ygmInputStream, DataInputStream dataInput)
            throws IOException {
        this.ygmInputStream = ygmInputStream;
        this.mInput = dataInput;
        buffer = new byte[capacity];
        mInput.read(buffer);
    }

    public Reader(int buffSize, YgmObjectInputStream ygmInputStream,
            DataInputStream dataInput) throws IOException {
        this.capacity = buffSize;
        this.ygmInputStream = ygmInputStream;
        this.mInput = dataInput;
        buffer = new byte[capacity];
        mInput.read(buffer);
    }

    @SuppressWarnings("unchecked")
    public <T> T readObject(DataInputStream input) throws Exception {
        if (input == null)
            throw new IllegalArgumentException("input cannot be null.");

        char b = (char) this.readByte();
        if (' ' == b) {
            return null;
        }

        if ('*' == b) {
            int n = this.readInt();
            return (T) objectsReaded.get(n);
        }

        Object baseTypeObject = checkBaseType((byte) b);
        if (baseTypeObject != null) {
            objectsReaded.add(baseTypeObject);
            return (T) baseTypeObject;
        }
        // ******check whether is baseType**************

        // ********get class name**************
        byte o_n = readByte();
        String className = null;
        if ('N' == o_n) {
            className = this.readString();
            classNameList.add(className);
        } else if ('O' == o_n) {
            className = classNameList.get(this.readInt());
        }

        Class<?> type = Class.forName(className);
        // ********get class name**************

        if (type.isArray()) {
            return (T) readArray(type, input);
        }
        // *****************************

        // ******check whether is baseType**************
        Object o = isOverWrite(type, ygmInputStream);

        if (null!= o) {
            return (T) o;
        }

        T object = null;

        int code = type.hashCode();

        if (!readClassFieldsMap.containsKey(code)) {
            readClassFieldsMap.put(code, getClassField(type));
        }

        Field[] fieldsRead = (Field[]) readClassFieldsMap.get(code);
        // **************************************************
        Constructor<?> c = type.getConstructor();
        c.setAccessible(true);
        object = (T) c.newInstance();
        // **************************************************
        objectsReaded.add(object);
        readFields(fieldsRead, object);
        return object;
    }

    private Object readReferenceObject(DataInputStream input, Field field,
            Object object) throws Exception {
        char b = (char) this.readByte();

        if (' ' == b) {
            return null;
        }

        Object baseTypeObject = checkBaseType((byte) b);

        if (baseTypeObject != null) {
            return baseTypeObject;
        }

        byte o_n = readByte();

        String className = null;

        if ('N' == o_n) {
            className = this.readString();
            classNameList.add(className);
        } else if ('O' == o_n) {
            className = classNameList.get(this.readInt());
        }

        Class<?> type = Class.forName(className);

        if (type.isArray()) {
            return readArray(type, input);
        }

        Object o = isOverWrite(type, ygmInputStream);

        if (o != null) {
            return o;
        }

        Object obj = null;

        int code = type.hashCode();

        obj = field.get(object);

        if (obj == null) {
            // **************************************************
            Constructor<?> c = type.getConstructor();
            c.setAccessible(true);
            obj = c.newInstance();
            // **************************************************
            objectsReaded.add(obj);
        }

        if (!readClassFieldsMap.containsKey(code)) {
            readClassFieldsMap.put(code, getClassField(type));
        }

        Field[] fieldsRead = (Field[]) readClassFieldsMap.get(code);

        readFields(fieldsRead, obj);

        return obj;
    }

    @SuppressWarnings("unchecked")
    public <T> T defaultReadObject() throws Exception {

        Class<?> type = defaultObject.getClass();

        int code = type.hashCode();

        if (!readClassFieldsMap.containsKey(code)) {
            readClassFieldsMap.put(code, getClassField(type));
        }

        Field[] fieldsRead = (Field[]) readClassFieldsMap.get(code);

        readFields(fieldsRead, defaultObject);

        // objectsReaded.clear();
        return (T) defaultObject;
    }

    // ********************************read
    // Array*************************************
    private Object readArray(Class<?> type, DataInputStream input)
            throws Exception {

        Class<?> componentType = type.getComponentType();
        int newLength = this.readInt();
        Object newArray = Array.newInstance(componentType, newLength);
        objectsReaded.add(newArray);
        String arrayType = componentType.getSimpleName();
        int n = -1;

        if (arrayType.equals("int")) {
            int[] arry = (int[]) newArray;
            while (++n < newLength) {
                arry[n] = this.readInt();
            }
            return newArray;
        } else if (arrayType.equals("double")) {
            double[] arry = (double[]) newArray;
            while (++n < newLength) {
                arry[n] = this.readDouble();
            }
            return newArray;
        } else if (arrayType.equals("long")) {
            long[] arry = (long[]) newArray;
            while (++n < newLength) {
                arry[n] = this.readLong();
            }
            return newArray;
        } else if (arrayType.equals("short")) {

            short[] arry = (short[]) newArray;
            while (++n < newLength) {
                arry[n] = this.readShort();
            }
            return newArray;
        } else if (arrayType.equals("char")) {
            char[] arry = (char[]) newArray;
            while (++n < newLength) {
                arry[n] = this.readChar();
            }
            return newArray;
        } else if (arrayType.equals("byte")) {

            byte[] arry = (byte[]) newArray;
            while (++n < newLength) {
                arry[n] = this.readByte();
            }
            return newArray;
        } else if (arrayType.equals("float")) {
            float[] arry = (float[]) newArray;
            while (++n < newLength) {
                arry[n] = this.readFloat();
            }
            return newArray;
        } else if (arrayType.equals("String")) {
            String[] arry = (String[]) newArray;
            while (++n < newLength) {
                int length = this.readInt();
                if (length == 0) {
                    continue;
                }
                byte[] chars = new byte[length];
                this.readBytes(chars);
                String s = new String(chars);
                arry[n] = s;
            }
            return newArray;
        } else {
            Object[] arry = (Object[]) newArray;
            while (++n < newLength) {
                char c = (char) this.readByte();
                if (c == 'E') {
                    arry[n] = null;
                    continue;
                }
                arry[n] = this.readElement(input);
            }
            return newArray;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T readElement(DataInputStream input) throws Exception {

        // ******check whether is baseType**************
        byte c = this.readByte();

        if ('*' == c) {
            return (T) objectsReaded.get(this.readInt());
        }

        Object baseTypeObject = checkBaseType(c);

        if (baseTypeObject != null) {
            objectsReaded.add(baseTypeObject);
            return (T) baseTypeObject;
        }
        // ******check whether is baseType**************

        // ********get class name**************
        byte o_n = readByte();
        String className = null;

        if ('N' == o_n) {
            className = this.readString();
            classNameList.add(className);
        } else {
            className = classNameList.get(this.readInt());
        }

        Class<?> type = Class.forName(className);

        // ********get class name**************

        Object o = isOverWrite(type, ygmInputStream);

        if (o != null) {
            return (T) o;
        }

        T object = null;

        int code = type.hashCode();

        if (!readClassFieldsMap.containsKey(code)) {
            readClassFieldsMap.put(code, getClassField(type));
        }

        Field[] fieldsRead = (Field[]) readClassFieldsMap.get(code);

        // **************************************************
        Constructor<?> constructor = type.getConstructor();
        constructor.setAccessible(true);
        object = (T) constructor.newInstance();
        // **************************************************
        // object = (T) type.newInstance();// (T)classObjectMap.get(code);
        objectsReaded.add(object);
        readFields(fieldsRead, object);

        return object;
    }

    private void readFields(Field[] fieldsRead, Object obj) throws Exception {
        for (int n = 0; n < fieldsRead.length; n++) {
            if (Modifier.isFinal(fieldsRead[n].getModifiers())) {
                continue;
            }
            fieldsRead[n].setAccessible(true);
            char c = (char) this.readByte();
            switch (c) {
            // String
            case 'S':
                byte[] chars = new byte[this.readInt()];
                this.readBytes(chars);
                String s = new String(chars);
                fieldsRead[n].set(obj, s);
                break;
            // String = null
            case 's':
                fieldsRead[n].set(obj, null);
                break;
            // int
            case 'i':
                fieldsRead[n].setInt(obj, this.readInt());
                break;
            // Integer
            case 'I':
                fieldsRead[n].set(obj, new Integer(this.readInt()));
                break;
            // float
            case 'f':
                fieldsRead[n].setFloat(obj, this.readFloat());
                break;
            // Float
            case 'F':
                fieldsRead[n].set(obj, new Float(this.readFloat()));
                break;
            // double
            case 'd':
                fieldsRead[n].setDouble(obj, this.readDouble());
                break;
            // Double
            case 'D':
                fieldsRead[n].set(obj, new Double(this.readDouble()));
                break;
            // char
            case 'c':
                fieldsRead[n].setChar(obj, this.readChar());
                break;
            // Character
            case 'C':
                fieldsRead[n].set(obj, new Character(this.readChar()));
                break;
            // boolean
            case 'b':
                fieldsRead[n].setBoolean(obj, this.readBoolean());
                break;
            // Boolean
            case 'B':
                fieldsRead[n].set(obj, new Boolean(this.readBoolean()));
                break;
            // long
            case 'l':
                fieldsRead[n].setLong(obj, this.readLong());
                break;
            // Long
            case 'L':
                fieldsRead[n].set(obj, new Long(this.readLong()));
                break;
            // byte
            case 't':
                fieldsRead[n].setByte(obj, this.readByte());
                break;
            // Byte
            case 'T':
                fieldsRead[n].set(obj, new Byte(this.readByte()));
                break;
            // short
            case 'r':
                fieldsRead[n].setShort(obj, this.readShort());
                break;
            // Short
            case 'R':
                fieldsRead[n].set(obj, new Short(this.readShort()));
                break;
            // Parameter is object and is null
            case ' ':
                fieldsRead[n].set(obj, null);
                // Parameter is object and is Tran
            case 'N':
                fieldsRead[n].set(obj, null);
                break;
            // cycle reference
            case '*':
                fieldsRead[n].set(obj, objectsReaded.get(this.readInt()));
                break;
            // Array
            case 'Z':
                fieldsRead[n].set(obj,
                        readArray(fieldsRead[n].getType(), mInput));
                break;
            // other object
            /* get the true class the fieldsRead[n] contain */
            default:
                fieldsRead[n].set(obj,
                        readReferenceObject(mInput, fieldsRead[n], obj));
                break;
            }

        }
    }

    // ********************************read
    // Array*************************************

    private Field[] getClassField(Class<?> aClazz) {

        if (!isSerializable(aClazz.getSuperclass())) {
            return aClazz.getDeclaredFields();
        }

        if (aClazz.getSuperclass() == Object.class) {
            return aClazz.getDeclaredFields();
        }

        Class<?> superclass = aClazz.getSuperclass();
        Field[] spuerField = getClassField(superclass);
        Field[] subField = aClazz.getDeclaredFields();
        Field[] allField = new Field[spuerField.length + subField.length];
        System.arraycopy(spuerField, 0, allField, 0, spuerField.length);
        System.arraycopy(subField, 0, allField, spuerField.length,
                subField.length);
        return allField;
    }

    public int available() throws IOException {
        return mInput.available();
    }

    public void close() throws IOException {
        mInput.close();
    }

    public int read() throws IOException {
        require(1);
        return buffer[position++] & 0xFF;
    }

    public int read(byte[] bytes) throws IOException {
        return read(bytes, 0, bytes.length);
    }

    public int read(byte[] bytes, int offset, int count) throws IOException {
        if (bytes == null)
            throw new IllegalArgumentException("bytes cannot be null.");
        int startingCount = count;
        int copyCount = Math.min(capacity - position, count);

        while (true) {
            System.arraycopy(buffer, position, bytes, offset, copyCount);
            position += copyCount;
            count -= copyCount;
            if (count == 0)
                break;
            offset += copyCount;
            copyCount = Math.min(capacity, count);
            require(copyCount);
        }
        return startingCount - count;
    }

    public String readString() throws IOException {
        int n = this.readInt();
        if (n == 0) {
            return null;
        }
        byte[] chars = new byte[n];
        this.readBytes(chars);
        String s = new String(chars);
        return s;
    }

    public byte readByte() throws IOException {
        require(1);
        return buffer[position++];
    }

    public int readUnsignedShort() throws IOException {
        require(2);
        return ((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF);
    }

    public int readUnsignedByte() throws IOException {
        require(1);
        return buffer[position++] & 0xFF;
    }

    public void skip(int count) throws IOException {
        int skipCount = Math.min(capacity - position, count);
        while (true) {
            position += skipCount;
            count -= skipCount;
            if (count == 0)
                break;
            skipCount = Math.min(count, capacity);
            require(skipCount);
        }
    }

    public int skipBytes(int len) throws IOException {
        int remaining = len;
        while (remaining > 0) {
            int skip = Math.min(capacity, (int) remaining);
            skip(skip);
            remaining -= skip;
        }
        return len;
    }

    public long skip(long count) throws IOException {
        long remaining = count;
        while (remaining > 0) {
            int skip = Math.min(capacity, (int) remaining);
            skip(skip);
            remaining -= skip;
        }
        return count;
    }

    /**
     * Reads bytes.length bytes and writes them to the specified byte[],
     * starting at index 0.
     */
    public void readBytes(byte[] bytes) throws IOException {
        readBytes(bytes, 0, bytes.length);
    }

    public void readBytes(byte[] bytes, int offset, int count)
            throws IOException {
        if (bytes == null)
            throw new IllegalArgumentException("bytes cannot be null.");
        int copyCount = Math.min(capacity - position, count);
        while (true) {
            System.arraycopy(buffer, position, bytes, offset, copyCount);
            position += copyCount;
            count -= copyCount;
            if (count == 0)
                break;
            offset += copyCount;
            copyCount = Math.min(count, capacity);
            require(copyCount);
        }
    }

    public int readInt() throws IOException {
        require(4);
        byte[] buffer = this.buffer;
        int position = this.position;
        this.position = position + 4;
        return (buffer[position] & 0xFF) << 24 //
                | (buffer[position + 1] & 0xFF) << 16 //
                | (buffer[position + 2] & 0xFF) << 8 //
                | buffer[position + 3] & 0xFF;
    }

    /** Reads a 4 byte float. */
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /** Reads a 2 byte short. */
    public short readShort() throws IOException {
        require(2);
        return (short) (((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
    }

    /** Reads an 8 byte long. */
    public long readLong() throws IOException {
        require(8);
        byte[] buffer = this.buffer;
        return (long) buffer[position++] << 56 //
                | (long) (buffer[position++] & 0xFF) << 48 //
                | (long) (buffer[position++] & 0xFF) << 40 //
                | (long) (buffer[position++] & 0xFF) << 32 //
                | (long) (buffer[position++] & 0xFF) << 24 //
                | (buffer[position++] & 0xFF) << 16 //
                | (buffer[position++] & 0xFF) << 8 //
                | buffer[position++] & 0xFF;
    }

    /** Reads a 1 byte boolean. */
    public boolean readBoolean() throws IOException {
        require(1);
        return buffer[position++] == 1;
    }

    /** Reads a 2 byte char. */
    public char readChar() throws IOException {
        require(2);
        return (char) (((buffer[position++] & 0xFF) << 8) | (buffer[position++] & 0xFF));
    }

    /** Reads an 8 bytes double. */
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    private boolean require(int required) throws IOException {
        if (capacity - position >= required)
            return false;
        if (required > Integer.MAX_VALUE)
            throw new RuntimeException("Buffer overflow. Max capacity: "
                    + Integer.MAX_VALUE + ", required: " + required);

        if (capacity - position < required) {
            for (int i = 0; i < capacity - position; i++) {
                buffer[i] = buffer[position + i];
            }
            mInput.read(buffer, capacity - position, position);
        }
        position = 0;
        return true;
    }

    private Object isOverWrite(Class<?> type, YgmObjectInputStream mInput)
            throws Exception {

        Method[] methods = getAllMethods(type);
        if (methods[0] == null) {
            return null;
        }
        Object object = null;
        // **************************************************
        Constructor<?> constructor = type.getConstructor();
        constructor.setAccessible(true);
        object = constructor.newInstance();
        // **************************************************
        // object = type.newInstance();
        objectsReaded.add(object);
        for (Method method : methods) {
            method.setAccessible(true);
            defaultObject = object;
            method.invoke(object, mInput);
        }
        return object;
    }

    private void getMethod(Class<?> aClazz, ArrayList<Method> methods) {
        Method[] met = null;
        if (!isSerializable(aClazz)) {
            return;
        }
        Class<?> superClass = aClazz.getSuperclass();
        if (superClass == Object.class) {
            met = aClazz.getDeclaredMethods();
            for (Method method : met) {
                if (method.getName().contains("readObject")) {
                    methods.add(method);
                    return;
                }
            }
            return;
        }

        getMethod(superClass, methods);

        met = aClazz.getDeclaredMethods();

        for (Method method : met) {
            if (method.getName().contains("readObject")) {
                methods.add(method);
                return;
            }
        }
    }

    private Method[] getAllMethods(Class<?> clazz) {
        ArrayList<Method> methods = new ArrayList<Method>();
        getMethod(clazz, methods);
        Method[] m = new Method[1];
        return methods.toArray(m);
    }

    private boolean isSerializable(Class<?> clazz) {
        Class<?>[] faces = clazz.getInterfaces();
        for (Class<?> face : faces) {

            if (face.getName().equals("java.io.Serializable")) {
                return true;
            }
        }
        return false;
    }

    private Object checkBaseType(byte bt) throws Exception {

        switch (bt) {
        case 'S':
            return this.readString();
        case 'I':
            return this.readInt();
        case 'D':
            return this.readDouble();
        case 'L':
            return this.readLong();
        case 'B':
            return this.readBoolean();
        case 'R':
            return this.readShort();
        case 'T':
            return this.readByte();
        case 'C':
            return this.readChar();
        case 'F':
            return this.readFloat();
        default:
            return null;
        }

    }
}