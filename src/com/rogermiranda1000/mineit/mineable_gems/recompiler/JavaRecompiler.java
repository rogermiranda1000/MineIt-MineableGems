package com.rogermiranda1000.mineit.mineable_gems.recompiler;

import org.bukkit.craftbukkit.libs.jline.internal.Nullable;

import java.io.File;
import java.io.FileWriter;

public class JavaRecompiler {
    public static final String JAVA_8 = "1.8";

    private final JavaDecompiler decompiler;
    private final JavaCompiler compiler;
    private final CodeReplacer []replacers;
    private final CompilationErrorFixer fixer;

    public JavaRecompiler(JavaDecompiler decompiler, CodeReplacer []replacers, JavaCompiler compiler, @Nullable CompilationErrorFixer fixer) {
        this.decompiler = decompiler;
        this.compiler = compiler;
        this.replacers = replacers;
        this.fixer = (fixer != null) ? fixer : ((source, errors) -> source);
    }

    /**
     * Recompiles a .jar
     * @param jarPath           .jar path
     * @param className         Class#getName()
     * @param compileClasspaths Dependencies
     * @param javaVersion       Java version (1.8 for Java 8)
     */
    public void recompile(String jarPath, String className, String []compileClasspaths, String javaVersion) throws Exception {
        // get the expected code
        String code = this.decompiler.decompileClass(jarPath, className);
        for (CodeReplacer cr : this.replacers) code = cr.replace(code);

        final File out = new File(className.substring(className.lastIndexOf('.')+1) + ".java"); // <class name>.java
        File compiled = null;
        try {
            FileWriter writer = new FileWriter(out);
            writer.write(code);
            writer.close();

            Compilation compile = this.compiler.compile(out.getPath(), compileClasspaths, javaVersion);
            if (compile.getErrors().length > 0) {
                // errors; try to fix
                code = this.fixer.fix(code, compile.getErrors());

                writer = new FileWriter(out);
                writer.write(code);
                writer.close();

                compile = this.compiler.compile(out.getPath(), compileClasspaths, javaVersion);
            }
            if (compile.getErrors().length > 0) throw new CompileException("Unable to solve the errors.", compile.getErrors());

            compiled = new File(compile.getCompilationPath());
            if (!JarHelper.addClassToJar(jarPath, className, compiled)) throw new CompileException("Class " + className + " not found inside " + jarPath);
        } catch (Exception ex) {
            out.delete();
            if (compiled != null) compiled.delete();
            throw ex;
        } finally {
            out.delete();
            if (compiled != null) compiled.delete();
        }
    }
}
