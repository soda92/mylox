package com.craftinginterpreters.tool;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.stream.Collectors;

public class RunCode {
    private static void clean(String directory) {
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
                try {
                    throw new IOException("Not able to delete file: " + f.getAbsolutePath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    private static void write(String code, String directory, String fileName) {
        Path path = Paths.get(directory);
        try {
            Files.createDirectories(path);
            FileWriter myWriter = new FileWriter(directory + "\\" + fileName);
            myWriter.write(code);
            myWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static boolean compile(String directory, String fileName) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("javac.exe", fileName);
        builder.directory(new File(directory));
        builder.redirectErrorStream(true);
        Process process = builder.start();
        int exitCode = process.waitFor();
        System.out.println(new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK")).lines().collect(Collectors.joining(System.lineSeparator())));
        return exitCode == 0;
    }

    public static Object run(String className, String methodName, String directory) throws IOException, InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        CustomClassLoader loader = new CustomClassLoader();
        loader.setPath(directory);
        Class<?> codeClass = loader.findClass(className);
        return codeClass.getMethod(methodName).invoke(codeClass.getDeclaredConstructor().newInstance());
    }

    public static void main(String[] args) {
        Path currentRelativePath = Paths.get("");
        String directory = currentRelativePath.toAbsolutePath().toString();
        directory += "\\target\\tmp";
        String className = "Code";
        Object value = null;

        try {
            clean(directory);
            boolean isSuccess = compile(directory, className + ".java");
            if (isSuccess) {
                value = run(className, "foo", directory);
            }
        } catch (IOException | InstantiationException | ClassNotFoundException | InterruptedException |
                 IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        System.out.println((String) value);
    }
}
