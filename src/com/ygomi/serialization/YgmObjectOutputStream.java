package com.ygomi.serialization;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class YgmObjectOutputStream extends ObjectOutputStream implements Serializable{

    private Writer mWrite = null;
    private DataOutputStream dataOutputStream = null;

    public YgmObjectOutputStream(OutputStream output) throws Exception {

        super();
        if (output == null) {
            throw new IllegalArgumentException("output cannot be null.");
        }
        dataOutputStream = new DataOutputStream(output);
        mWrite = new Writer(this, dataOutputStream);
    }

    public YgmObjectOutputStream(OutputStream output, int buffSize)
            throws Exception {

        super();
        if (output == null) {
            throw new IllegalArgumentException("output cannot be null.");
        }
        dataOutputStream = new DataOutputStream(output);
        mWrite = new Writer(buffSize, this, dataOutputStream);
    }

    public void ygmWriteObject(Object obj) throws Exception {

        //mWrite.objectsWritten.clear();
        mWrite.writeObject(obj, false);
    }

    // *******************OverRides************************************
    public void defaultWriteObject() {

        //mWrite.objectsWritten.clear();
        try {
            mWrite.defaultWriteObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeObjectOverride(Object obj) {
        try {
            ygmWriteObject(obj);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public void close() throws IOException {

        mWrite.close();
    }

    public void flush() throws IOException {

        mWrite.flush();
    }

    // **************************************
    public PutField putFields() {
        return null;
    }

    public void reset() {
    }

    public void useProtocolVersion(int version) {
    }

    public void writeFields() {
    }

    public void writeUnshared(Object obj) {
    }

    // **************************************
    public void write(byte[] b) throws IOException {

        mWrite.write(b);

    }

    public void write(int b) throws IOException {

        mWrite.write(b);

    }

    public void write(byte[] b, int off, int len) throws IOException {

        mWrite.write(b, off, len);

    }

    public void writeBoolean(boolean v) throws IOException {

        mWrite.writeBoolean(v);

    }

    public void writeByte(int v) throws IOException {

        mWrite.writeByte(v);

    }

    public void writeBytes(String s) throws IOException {

        mWrite.writeBytes(s);

    }

    public void writeChar(int v) throws IOException {

        mWrite.writeChar((char) v);

    }

    public void writeChars(String s) throws IOException {

        mWrite.writeChars(s);

    }

    public void writeDouble(double v) throws IOException {

        mWrite.writeDouble(v);

    }

    public void writeFloat(float v) throws IOException {

        mWrite.writeFloat(v);

    }

    public void writeInt(int v) throws IOException {

        mWrite.writeInt(v);

    }

    public void writeLong(long v) throws IOException {

        mWrite.writeLong(v);

    }

    public void writeShort(int v) throws IOException {

        mWrite.writeShort(v);

    }

    public void writeUTF(String str) throws IOException {

        mWrite.writeString(str);

    }

}
