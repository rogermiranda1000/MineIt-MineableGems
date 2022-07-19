package com.rogermiranda1000.mineit.mineable_gems.recompiler;

import com.rogermiranda1000.mineit.mineable_gems.recompiler.replacers.CodeReplacer;
import com.rogermiranda1000.mineit.mineable_gems.recompiler.replacers.RegexCodeReplacer;
import org.bukkit.craftbukkit.libs.jline.internal.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class JavaRecompiler {
    private final JavaDecompiler decompiler;
    private final JavaCompiler compiler;
    private final CodeReplacer[]replacers;
    private final CompilationErrorFixer fixer;

    /**
     * Once recompiled, mark the file so it won't be compiled again
     */
    private final boolean markAsRecompiled;

    public JavaRecompiler(JavaDecompiler decompiler, CodeReplacer []replacers, JavaCompiler compiler, @Nullable CompilationErrorFixer fixer, boolean markAsRecompiled) {
        this.decompiler = decompiler;
        this.compiler = compiler;
        this.replacers = replacers;
        this.fixer = (fixer != null) ? fixer : ((source, errors) -> source);
        this.markAsRecompiled = markAsRecompiled;
    }

    public JavaRecompiler(JavaDecompiler decompiler, CodeReplacer []replacers, JavaCompiler compiler, @Nullable CompilationErrorFixer fixer) {
        this(decompiler, replacers, compiler, fixer, false);
    }

    /**
     * The result class are compiled with a static variable that specifies if it was already compiled
     * @param jarPath           .jar path
     * @param className         Class#getName()
     * @return If the mentioned variable is present or not
     */
    public static boolean alreadyCompiled(String jarPath, String className) {
        try {
            Class<?> c = Class.forName(className, true, URLClassLoader.newInstance(new URL[]{new URL("file:" + jarPath)}, JavaRecompiler.class.getClassLoader()));
            Field f = c.getDeclaredField("ALREADY_COMPILED");
            return true;
        } catch (NoSuchFieldException ignored) {
            return false;
        } catch (ClassNotFoundException | MalformedURLException ex) {
            ex.printStackTrace();
            return false; // error
        }
    }

    /**
     * Mark a class as compiled
     * @param className         Class#getName() or class name
     * @param originalCode      Code to replace
     * @return Replaced code
     */
    private String setCompiled(String className, String originalCode) {
        return new RegexCodeReplacer("class\\s+" + className.substring(className.lastIndexOf('.')+1) + "\\s*\\{",
                        groups -> "public static final String ALREADY_COMPILED = \"Yes.\";",
                        true, true)
                .replace(originalCode);
    }

    /**
     * Recompiles a .jar
     * @param jarPath           .jar path
     * @param className         Class#getName()
     * @param compileClasspaths Dependencies
     * @param javaVersion       Java version (1.8 for Java 8)
     * @throws AlreadyRecompiledException The specified class is marked as recompiled
     */
    public void recompile(String jarPath, String className, String []compileClasspaths, String javaVersion) throws Exception {
        if (JavaRecompiler.alreadyCompiled(jarPath, className)) throw new AlreadyRecompiledException();

        // get the expected code
        String code = this.decompiler.decompileClass(jarPath, className);
        if (this.markAsRecompiled) code = this.setCompiled(className, code);
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
