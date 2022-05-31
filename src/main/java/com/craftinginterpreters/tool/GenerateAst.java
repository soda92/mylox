package com.craftinginterpreters.tool;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
//            System.err.println("Usage: generate_ast <output directory");
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            System.err.println("Current absolute path is: " + s);
//            System.exit(64);
        }
        String outputDir = "src/main/java/com/craftinginterpreters/lox";
        defineAst(outputDir, "Expr", Arrays.asList(
                "Binary: Expr left, Token operator, Expr right",
                "Grouping: Expr expression",
                "Literal: Object value",
                "Unary: Token operator, Expr right"
        ));
    }

    private static void defineAst(
            String outputDir, String baseName, List<String> types
    ) throws IOException {
        String path = outputDir + '/' + baseName + ".java";
        Path currentRelativePath = Paths.get(path);
        String s = currentRelativePath.toAbsolutePath().toString();
        System.out.println("Current file path is: " + s);
        PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + "{");

        // The AST classes.
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }
        writer.println("}");
        writer.close();
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("  static class "+ className+" extends "+ baseName +" {");
        // Constructor.
        writer.println("    " + className + "(" + fieldList + ") {");

        // Store parameters in fields.
        String[] fields = fieldList.split(", ");
        for(String field : fields){
            String name = field.split(" ")[1];
            writer.println("      this." + name + " = "+ name + ";");
        }

        writer.println("    }");

        // Fields.
        writer.println();
        for(String field: fields){
            writer.println("    final "+ field + ";");
        }

        writer.println("  }");
    }
}
