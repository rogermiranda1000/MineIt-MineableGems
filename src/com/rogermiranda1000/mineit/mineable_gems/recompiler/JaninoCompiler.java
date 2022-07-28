package com.rogermiranda1000.mineit.mineable_gems.recompiler;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.ICompiler;

import java.io.File;

public class JaninoCompiler implements JavaCompiler {
    @Override
    public Compilation compile(String javaFilePath, String[] classpaths, String version) throws Exception {
        ICompiler compiler = CompilerFactoryFactory.getDefaultCompilerFactory(ClassLoader.getSystemClassLoader()).newCompiler();

        File javaFile = new File(javaFilePath);
        try {
            compiler.compile(new File[]{javaFile});
        } catch (CompileException ex) {
            // TODO add to the return
            ex.printStackTrace();
        }
        return new Compilation(javaFile.getName().replaceAll(".java$", ".class") /* the compiled class is in ./<javaFilePath name>.class */, new Error[]{});
    }
}
