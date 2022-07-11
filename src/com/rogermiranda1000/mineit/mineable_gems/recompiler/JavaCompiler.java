package com.rogermiranda1000.mineit.mineable_gems.recompiler;

public interface JavaCompiler {
    /**
     * Compile a .java and get the errors
     * @param javaFilePath .java path
     * @param classpaths    Dependencies
     * @param version       Java compile version (1.8 for Java 8)
     * @return Errors and the compiled class path
     */
    public Compilation compile(String javaFilePath, String []classpaths, String version) throws Exception;
}
