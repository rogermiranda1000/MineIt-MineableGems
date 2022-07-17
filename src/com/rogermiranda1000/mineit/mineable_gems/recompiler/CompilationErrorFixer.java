package com.rogermiranda1000.mineit.mineable_gems.recompiler;

public interface CompilationErrorFixer {
    public String fix(String source, Error []errors) throws CompileException;
}
