package com.craftinginterpreters.tool;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;

public class RunCode {
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String code = """
                public class Code{
                    public void foo(){
                        System.out.println(1+1);
                        System.out.println("hello world");
                    }
                }""";
        Path currentRelativePath = Paths.get("");
        String directory = currentRelativePath.toAbsolutePath().toString();
//        System.out.println(directory);
        directory += "\\target\\tmp";
        File file = new File(directory);
        // file Extension
        String extension = ".class";
        // Implemented as lambda. filter all the files
        // having passed extension
        File[] fileList = file.listFiles((d, f) -> f.toLowerCase().endsWith(extension));
        // Delete all the included files
        assert fileList != null;
        for (File f : fileList) {
//            System.out.println(f.getAbsolutePath());
            if (!f.delete())
                throw new IOException("Not able to delete file: " + f.getAbsolutePath());
        }
        Path path = Paths.get(directory);
        Files.createDirectories(path);
        FileWriter myWriter = new FileWriter(directory + "\\" + "Code.java");
        myWriter.write(code);
        myWriter.close();
        ProcessBuilder builder = new ProcessBuilder("javac.exe", "Code.java");
        builder.directory(new File(directory));
        Process process = builder.start();
        int exitCode = process.waitFor();
        assert exitCode == 0;
        Class<?> codeClass = new CustomClassLoader().findClass(directory + "\\" + "Code.class");
        codeClass.getMethod("foo").invoke(codeClass.getDeclaredConstructor().newInstance());
    }
}

class CustomClassLoader extends ClassLoader {

    @Override
    public Class<?> findClass(String name) {
        byte[] b;
        try {
            b = loadClassFromFile(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return defineClass("Code", b, 0, b.length);
    }

    private byte[] loadClassFromFile(String filePath) throws IOException {
        FileInputStream inputStream = new FileInputStream(filePath);
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


