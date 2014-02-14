package com.ygomi.serialization;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputValidation;
import java.io.ObjectInputStream;

public class YgmObjectInputStream extends ObjectInputStream {

    private DataInputStream mInput = null;
    private Reader mRead = null;


    public YgmObjectInputStream(InputStream input) throws Exception {
        super();
        if (input == null) {
            throw new IllegalArgumentException("output cannot be null.");
        }
        mInput = new DataInputStream(input);
        mRead = new Reader(this, mInput);
    }

    public YgmObjectInputStream(InputStream input, int buffSize)
            throws Exception {
        super();
        if (input == null) {
            throw new IllegalArgumentException("output cannot be null.");
        }
        mInput = new DataInputStream(input);
        mRead = new Reader(buffSize, this, mInput);
    }

    public <T> T ygmReadObject() throws Exception {


        return mRead.readObject(mInput);
    }



    // ****************OverRides***************************
    public void defaultReadObject(){

        try{
            
            mRead.defaultReadObject();
        }catch(Exception e){
            
            e.printStackTrace();
        }
        
    }

    public Object readObjectOverride() {

        try {
            return mRead.readObject(mInput);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public int available() throws IOException {

        return mRead.available();
    }

    public void close() throws IOException {

        mRead.close();
    }

    // *****************************************
    public void mark(int readlimit) {
    }

    public boolean markSupported() {
        return false;
    }

    public GetField readFields() {
        return null;
    }

    public Object readUnshared() {
        return null;
    }

    public void registerValidation(ObjectInputValidation obj, int prio) {
    }

    public void reset() throws IOException {
    }

    public String readLine() throws IOException {
        return null;
    }

    // *****************************************
    public int read() throws IOException {

        return mRead.read();
    }

    public int read(byte[] b) throws IOException {

        return mRead.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {

        return mRead.read(b, off, len);
    }

    public boolean readBoolean() throws IOException {

        return mRead.readBoolean();
    }

    public byte readByte() throws IOException {

        return mRead.readByte();
    }

    public char readChar() throws IOException {

        return mRead.readChar();
    }

    public double readDouble() throws IOException {

        return mRead.readDouble();
    }

    public float readFloat() throws IOException {

        return mRead.readFloat();
    }

    public void readFully(byte[] b) throws IOException {

        mRead.read(b);

    }

    public void readFully(byte[] b, int off, int len) throws IOException {

        mRead.read(b, off, len);
    }

    public int readInt() throws IOException {

        return mRead.readInt();
    }

    public long readLong() throws IOException {

        return mRead.readLong();
    }

    public short readShort() throws IOException {

        return mRead.readShort();
    }

    public int readUnsignedByte() throws IOException {

        return mRead.readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException {

        return mRead.readUnsignedShort();
    }

    public String readUTF() throws IOException {

        return mRead.readString();
    }

    public long skip(long n) throws IOException {

        return mRead.skip(n);
    }

    public int skipBytes(int n) throws IOException {

        return mRead.skipBytes(n);
    }

}
