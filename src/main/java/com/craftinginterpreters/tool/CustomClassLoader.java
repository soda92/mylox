package com.craftinginterpreters.tool;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class CustomClassLoader extends ClassLoader {

    @Override
    public Class<?> findClass(String className) {
        byte[] b;
        try {
            System.out.println("request class: " + className);
            b = loadClassFromFile(className);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return defineClass(className, b, 0, b.length);
    }

    private String path = "";

    public void setPath(String path) {
        this.path = path;
    }

    private byte[] loadClassFromFile(String className) throws IOException {
        FileInputStream inputStream = new FileInputStream(this.path + "\\" + className + ".class");
        byte[] buffer;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        int nextValue;
        try {
            while ((nextValue = inputStream.read()) != -1) {
                byteStream.write(nextValue);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer = byteStream.toByteArray();
        inputStream.close();
        return buffer;
    }
}
