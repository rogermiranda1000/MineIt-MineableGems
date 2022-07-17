package com.rogermiranda1000.mineit.mineable_gems.recompiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class RuntimeCompiler implements JavaCompiler {
    public static final String JAVA_8 = "1.8";

    public RuntimeCompiler() { }

    /**
     * @throws IOException
     */
    @Override
    public Compilation compile(String javaFilePath, String[] classpaths, String version) throws Exception {
        File javaFile = new File(javaFilePath);
        Process p = Runtime.getRuntime().exec("javac -source " + version + " -target " + version + " -classpath " + ((classpaths.length == 0) ? "." : (".:" + String.join(":", classpaths))) + " " + javaFilePath); // compile
        BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        return new Compilation(javaFile.getName().replaceAll(".java$", ".class") /* the compiled class is in ./<javaFilePath name>.class */, Error.getErrors(stdError));
    }
}
