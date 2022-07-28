package com.rogermiranda1000.mineit.mineable_gems.recompiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RuntimeCompiler implements JavaCompiler {
    public static final String JAVA_8 = "1.8";

    public RuntimeCompiler() { }

    /**
     * @throws IOException
     */
    @Override
    public Compilation compile(String javaFilePath, String[] classpaths, String version) throws Exception {
        File javaFile = new File(javaFilePath);
        // TODO use ToolProvider.getSystemJavaCompiler()
        Process p = Runtime.getRuntime().exec("javac -source " + version + " -target " + version + " -classpath " + ((classpaths.length == 0) ? "." : (".:" + String.join(":", classpaths))) + " " + javaFilePath); // compile
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        return new Compilation(javaFile.getName().replaceAll(".java$", ".class") /* the compiled class is in ./<javaFilePath name>.class */, RuntimeCompiler.getErrors(stdError));
    }

    private static final Pattern errorPattern = Pattern.compile("([^\\s.]+\\.java):(\\d+): error: ([^\\n]+)[ ]*\\n([^\\n]*)\\n([ ]*)\\^");
    public static Error []getErrors(String errors) {
        //System.err.println(errors);
        /**
         *  CLASS.java:LINE: error: incompatible types: List<CAP#1> cannot be converted to List<String>
         *            customDrop.setBiomeFilter(section.getList("Biome-Filter", null));
         *                                                     |
         */

        Matcher m = errorPattern.matcher(errors);
        ArrayList<Error> list = new ArrayList<>();
        while (m.find()) list.add(new Error(m.group(1), Integer.parseInt(m.group(2)), m.group(3), m.group(4), m.group(5).length()));

        return list.toArray(new Error[0]);
    }

    public static Error []getErrors(BufferedReader stdError) throws IOException {
        return RuntimeCompiler.getErrors(stdError.lines().collect(Collectors.joining("\n")));
    }
}
