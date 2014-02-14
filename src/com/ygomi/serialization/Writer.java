package com.ygomi.serialization;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Writer {

    private Object beginObject = null;
    private int capacity = 4096;
    private HashMap<Object, Field[]> classFieldsMap = new HashMap<Object, Field[]>();
    public List<Object> objectsWritten = new ArrayList<Object>();
    private int count = 0;
    private ArrayList<String> classNameList = new ArrayList<String>();
    private byte[] buffer = null;
    private int position = 0;
    private DataOutputStream mOutput = null;
    private YgmObjectOutputStream ygmOutput = null;
    private Object defaultObject = null;

    public Writer(YgmObjectOutputStream ygmOutput, DataOutputStream dataOutput) {
        buffer = new byte[capacity];
        this.ygmOutput = ygmOutput;
        this.mOutput = dataOutput;
    }

    public Writer(int buffSize, YgmObjectOutputStream ygmOutput,
            DataOutputStream dataOutput) {
        this.capacity = buffSize;
        buffer = new byte[capacity];
        this.ygmOutput = ygmOutput;
        this.mOutput = dataOutput;
    }

    public void writeObject(Object obj, boolean defaultWrite) throws Exception {

        if (obj == null) {
            writeByte(' ');
            return;
        }

        if (++count < 2) {
            beginObject = obj;
        }

        if (!defaultWrite) {
            if (objectsWritten.contains(obj.hashCode())) {
                writeByte('*');
                writeInt(objectsWritten.indexOf(obj.hashCode()));
                return;
            }
            
            objectsWritten.add(obj.hashCode());
            
            if (checkBaseType(obj)) {
                count = 0;

                return;
            }

            checkSerializable(obj);

            String className = obj.getClass().getName();
            
            if (classNameList.contains(className)) {
                writeByte('O');
                writeInt(classNameList.indexOf(className));
            } else {
                writeByte('N');
                writeString(className);
                classNameList.add(className);
            }
            
            if (obj.getClass().isArray()) {
                this.writeArray(obj);
                mOutput.write(buffer, 0, position);
                position = 0;
                count = 0;
                return;
            }
            
            if (isOverWrite(obj, ygmOutput)) {
                count = 0;
                return;
            }
        }

        int code = obj.getClass().hashCode();
        
        if (!classFieldsMap.containsKey(code)) {
            classFieldsMap.put(code, getClassField(obj.getClass()));
        }
        
        Field[] fields = (Field[]) classFieldsMap.get(code);

        for (int j = 0; j < fields.length; j++) {
            fields[j].setAccessible(true);
            if (Modifier.isFinal(fields[j].getModifiers())) {
                continue;
            }

            /*
             * because the order of writing and reading is one to one,the name
             * of Fields need not writen
             */
            // Fields value
            // String
            if (fields[j].getType().getName()
                    .equals(java.lang.String.class.getName())) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('s');
                    continue;
                }
                try {
                    if (fields[j].get(obj) == null) {
                        require(1);
                        buffer[position++] = (byte) ('s');
                        continue;
                    }
                    /*
                     * The length of String is not known,so use writeUTF()
                     * NOTE:writeBytes(String s)writen char takes one
                     * byte��writeChars(String s) takes two bytes
                     */
                    require(1);
                    buffer[position++] = (byte) ('S');
                    String s = (String) fields[j].get(obj);
                    writeInt(s.length());
                    require(s.length());
                    for (int m = 0; m < s.length(); m++) {
                        buffer[position++] = (byte) s.charAt(m);
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (fields[j].getType().getName().equals("int")) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('i');
                    int value = 0;
                    writeInt(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('i');
                    int value = fields[j].getInt(obj);
                    writeInt(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Integer
            } else if (fields[j].getType().getName()
                    .equals(java.lang.Integer.class.getName())) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('I');
                    int value = 0;
                    writeInt(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('I');
                    int value = ((Integer) fields[j].get(obj)).intValue();
                    writeInt(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // float
            } else if (fields[j].getType().getName().equals("float")) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('f');
                    float value = 0.0f;
                    writeInt(Float.floatToIntBits(value));
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('f');
                    float value = fields[j].getFloat(obj);
                    writeInt(Float.floatToIntBits(value));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Float
            } else if (fields[j].getType().getName()
                    .equals(java.lang.Float.class.getName())) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('F');
                    float value = 0.0f;
                    writeInt(Float.floatToIntBits(value));
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('F');
                    float value = ((Float) fields[j].get(obj)).floatValue();
                    writeInt(Float.floatToIntBits(value));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // double
            } else if (fields[j].getType().getName().equals("double")) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('d');
                    double value = 0.0d;
                    writeDouble(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('d');
                    double value = fields[j].getDouble(obj);
                    writeDouble(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Double
            } else if (fields[j].getType().getName()
                    .equals(java.lang.Double.class.getName())) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('D');
                    double value = 0.0d;
                    writeDouble(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('D');
                    double value = ((Double) fields[j].get(obj)).doubleValue();
                    writeDouble(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // char
            } else if (fields[j].getType().getName().equals("char")) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('c');
                    char value = 0;
                    writeChar(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('c');
                    char value = fields[j].getChar(obj);
                    writeChar(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Character
            } else if (fields[j].getType().getName()
                    .equals(java.lang.Character.class.getName())) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('C');
                    char value = 0;
                    writeChar(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('C');
                    char value = ((Character) fields[j].get(obj)).charValue();
                    writeChar(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // bool
            } else if (fields[j].getType().getName().equals("boolean")) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('b');
                    boolean value = false;
                    writeBoolean(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('b');
                    boolean value = fields[j].getBoolean(obj);
                    writeBoolean(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Boolean
            } else if (fields[j].getType().getName()
                    .equals(java.lang.Boolean.class.getName())) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('B');
                    boolean value = false;
                    writeBoolean(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('B');
                    boolean value = ((Boolean) fields[j].get(obj))
                            .booleanValue();
                    writeBoolean(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // long
            } else if (fields[j].getType().getName().equals("long")) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('l');
                    long value = 0;
                    writeLong(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('l');
                    long value = fields[j].getLong(obj);
                    writeLong(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Long
            } else if (fields[j].getType().getName()
                    .equals(java.lang.Long.class.getName())) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('L');
                    long value = 0;
                    writeLong(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('L');
                    long value = ((Long) fields[j].get(obj)).longValue();
                    writeLong(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // short
            } else if (fields[j].getType().getName().equals("short")) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('r');
                    short value = 0;
                    writeShort(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('r');
                    short value = fields[j].getShort(obj);
                    writeShort(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Short
            } else if (fields[j].getType().getName()
                    .equals(java.lang.Short.class.getName())) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('R');
                    short value = 0;
                    writeShort(value);
                    continue;
                }
                try {
                    require(1);
                    buffer[position++] = (byte) ('R');
                    short value = ((Short) fields[j].get(obj)).shortValue();
                    writeShort(value);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // byte
            } else if (fields[j].getType().getName().equals("byte")) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(2);
                    buffer[position++] = (byte) ('t');
                    buffer[position++] = 0;
                    continue;
                }
                try {
                    require(2);
                    buffer[position++] = (byte) ('t');
                    buffer[position++] = fields[j].getByte(obj);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Byte
            } else if (fields[j].getType().getName()
                    .equals(java.lang.Byte.class.getName())) {
                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(2);
                    buffer[position++] = (byte) ('T');
                    buffer[position++] = 0;
                    continue;
                }
                try {
                    require(2);
                    buffer[position++] = (byte) ('T');
                    buffer[position++] = ((Byte) fields[j].get(obj))
                            .byteValue();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // other object
            } else {

                if (Modifier.isTransient(fields[j].getModifiers())) {
                    require(1);
                    buffer[position++] = (byte) ('N');
                    continue;
                }
                try {
                    Object object = fields[j].get(obj);
                     if (object == null) {
                     require(1);
                     buffer[position++] = (byte) ('N');
                     continue;
                     }
                    if (objectsWritten.contains(object.hashCode())) {
                        
                        require(1);
                        buffer[position++] = (byte) ('*');
                        writeInt(objectsWritten.indexOf(object.hashCode()));
                        continue;
                    }
                    require(1);
                    buffer[position++] = (byte) ('O');
                    writeObject(object, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (beginObject == obj) {
            mOutput.write(buffer, 0, position);
            position = 0;
            count = 0;
        }
        return;
    }

    public void defaultWriteObject() throws Exception {
        writeObject(defaultObject, true);
    }

    // ********************************write
    // Array*************************************
    private void writeArray(Object obj) throws Exception {
        if (obj == null)
            throw new IllegalArgumentException("array cannot be null.");
        int length = Array.getLength(obj);
        this.writeInt(length);
        int n = -1;
        String arrayType = obj.getClass().getSimpleName();
        if (arrayType.equals("int[]")) {

            while (++n < length) {
                this.writeInt(Array.getInt(obj, n));
            }
        } else if (arrayType.equals("double[]")) {
            while (++n < length) {
                this.writeDouble(Array.getDouble(obj, n));
            }
        } else if (arrayType.equals("long[]")) {
            while (++n < length) {
                this.writeLong(Array.getLong(obj, n));
            }
        } else if (arrayType.equals("short[]")) {
            while (++n < length) {
                this.writeShort(Array.getShort(obj, n));
            }
        } else if (arrayType.equals("float[]")) {
            while (++n < length) {
                writeInt(Float.floatToIntBits(Array.getFloat(obj, n)));
            }
        } else if (arrayType.equals("byte[]")) {
            while (++n < length) {
                require(1);
                buffer[position++] = Array.getByte(obj, n);
            }
        } else if (arrayType.equals("char[]")) {
            while (++n < length) {
                writeChar(Array.getChar(obj, n));
            }
        } else if (arrayType.equals("String[]")) {
            while (++n < length) {
                String s = (String) Array.get(obj, n);
                if (s == null) {
                    writeInt(0);
                    continue;
                }
                writeInt(s.length());
                require(s.length());
                for (int m = 0; m < s.length(); m++) {
                    buffer[position++] = (byte) s.charAt(m);
                }
            }
        } else {
            while (++n < length) {
                Object object = Array.get(obj, n);
                if (object == null) {
                    require(1);
                    buffer[position++] = (byte) ('E');
                    continue;
                }
                require(1);
                buffer[position++] = (byte) ('Y');// Mark that next object
                writeObject(object, false);
            }
        }
    }

    // ********************************write
    // Array*************************************
    public void close() throws IOException {
        flush();
        this.mOutput.close();
    }

    public void flush() throws IOException {
        mOutput.write(buffer, 0, position);
    }

    public void write(int val) throws IOException {
        require(1);
        buffer[position++] = (byte) val;
    }

    public void write(byte[] bytes) throws IOException {
        if (bytes == null)
            throw new IllegalArgumentException("bytes cannot be null.");
        writeBytes(bytes, 0, bytes.length);
    }

    public void write(byte[] bytes, int offset, int length) throws IOException {
        writeBytes(bytes, offset, length);
    }

    public void writeByte(int value) throws IOException {
        require(1);
        buffer[position++] = (byte) value;
    }

    public void writeBytes(byte[] bytes, int offset, int count)
            throws IOException {
        if (bytes == null)
            throw new IllegalArgumentException("bytes cannot be null.");
        if (count > bytes.length)
            throw new IllegalArgumentException(
                    "count cannot bigger then bytes.length.");

        int copyCount = Math.min(capacity - position, count);
        while (true) {
            System.arraycopy(bytes, offset, buffer, position, copyCount);
            position += copyCount;
            count -= copyCount;
            if (count == 0)
                return;
            offset += copyCount;
            copyCount = Math.min(capacity, count);
            require(copyCount);
        }
    }

    public void writeBytes(String s) throws IOException {
        require(s.length());
        for (int m = 0; m < s.length(); m++) {

            buffer[position++] = (byte) s.charAt(m);
        }
    }

    public void writeString(String s) throws IOException {
        if (s == null) {
            writeInt(0);
            return;
        }
        writeInt(s.length());
        require(s.length());
        for (int m = 0; m < s.length(); m++) {
            buffer[position++] = (byte) s.charAt(m);
        }
    }

    public void writeInt(int value) throws IOException {
        require(4);
        buffer[position++] = (byte) (value >> 24);
        buffer[position++] = (byte) (value >> 16);
        buffer[position++] = (byte) (value >> 8);
        buffer[position++] = (byte) value;
    }

    public void writeFloat(float val) throws IOException {
        writeInt(Float.floatToIntBits(val));
    }

    public void writeLong(long value) throws IOException {
        require(8);
        buffer[position++] = (byte) (value >>> 56);
        buffer[position++] = (byte) (value >>> 48);
        buffer[position++] = (byte) (value >>> 40);
        buffer[position++] = (byte) (value >>> 32);
        buffer[position++] = (byte) (value >>> 24);
        buffer[position++] = (byte) (value >>> 16);
        buffer[position++] = (byte) (value >>> 8);
        buffer[position++] = (byte) value;
    }

    public void writeShort(int value) throws IOException {
        require(2);
        buffer[position++] = (byte) (value >>> 8);
        buffer[position++] = (byte) value;
    }

    public void writeBoolean(boolean value) throws IOException {
        require(1);
        buffer[position++] = (byte) (value ? 1 : 0);
    }

    public void writeChar(char value) throws IOException {
        require(2);
        buffer[position++] = (byte) (value >>> 8);
        buffer[position++] = (byte) value;
    }

    public void writeChars(String s) throws IOException {
        for (int n = 0; n < s.length(); n++) {
            writeChar(s.charAt(n));
        }
    }

    public void writeDouble(double value) throws IOException {
        writeLong(Double.doubleToLongBits(value));
    }

    private boolean require(int required) throws IOException {
        if (capacity - position >= required)
            return false;
        if (required > Integer.MAX_VALUE)
            throw new RuntimeException("Buffer overflow. Max capacity: "
                    + Integer.MAX_VALUE + ", required: " + required);

        // while (capacity - position < required) {
        // if (capacity == Integer.MAX_VALUE)
        // throw new RuntimeException("Buffer overflow. Available: "
        // + (capacity - position) + ", required: " + required);
        // // Grow buffer.
        // // capacity = Math.min(capacity * 2, maxCapacity);
        // capacity *= 2;
        // byte[] newBuffer = new byte[capacity];
        // System.arraycopy(buffer, 0, newBuffer, 0, position);
        // buffer = newBuffer;
        // }

        if (capacity - position < required) {
            mOutput.write(buffer, 0, position);
        }
        position = 0;
        return true;
    }

    // Check superclass and get all fields
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

   
    private boolean isOverWrite(Object obj, YgmObjectOutputStream mOutput)
            throws Exception {

        defaultObject = obj;
        Method[] methods = getAllMethods(obj.getClass());// obj.getClass().getDeclaredMethods();
        if (methods[0] == null) {
            return false;
        }
        for (Method method : methods) {
            method.setAccessible(true);
            try {
                method.invoke(obj, mOutput);
            } catch (InvocationTargetException n) {
                n.printStackTrace();
            }
        }
        return true;
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

                if (method.getName().contains("writeObject")) {
                    methods.add(method);
                    return;
                }
            }
            return;
        }

        getMethod(superClass, methods);
        met = aClazz.getDeclaredMethods();
        for (Method method : met) {
            if (method.getName().contains("writeObject")) {
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

    private void checkSerializable(Object obj) throws Exception {
        if (obj instanceof Serializable) {
            return;
        }
        throw new java.io.NotSerializableException(obj.getClass().toString());
    }

    private boolean checkBaseType(Object obj) throws Exception {
        String name = obj.getClass().getName();
        if (name.equals("java.lang.Integer")) {
            require(1);
            buffer[position++] = (byte) ('I');
            this.writeInt(((Integer) obj).intValue());
            return true;
        } else if (name.equals("java.lang.String")) {
            require(1);
            buffer[position++] = (byte) ('S');
            this.writeString(obj.toString());
            return true;
        } else if (name.equals("java.lang.Double")) {
            require(1);
            buffer[position++] = (byte) ('D');
            this.writeDouble(((Double) obj).doubleValue());
            return true;
        } else if (name.equals("java.lang.Long")) {
            require(1);
            buffer[position++] = (byte) ('L');
            this.writeLong(((Long) obj).longValue());
            return true;
        } else if (name.equals("java.lang.Boolean")) {
            require(1);
            buffer[position++] = (byte) ('B');
            this.writeBoolean(((Boolean) obj).booleanValue());
            return true;
        } else if (name.equals("java.lang.Short")) {
            require(1);
            buffer[position++] = (byte) ('R');
            this.writeShort(((Short) obj).shortValue());
            return true;
        } else if (name.equals("java.lang.Byte")) {
            require(1);
            buffer[position++] = (byte) ('T');
            this.writeByte(((Byte) obj).byteValue());
            return true;
        } else if (name.equals("java.lang.Character")) {
            require(1);
            buffer[position++] = (byte) ('C');
            this.writeChar(((Character) obj).charValue());
            return true;
        } else if (name.equals("java.lang.Float")) {
            require(1);
            buffer[position++] = (byte) ('F');
            this.writeFloat(((Float) obj).floatValue());
            return true;
        } else {
            require(1);
            buffer[position++] = (byte) ('O');
        }
        return false;
    }

}