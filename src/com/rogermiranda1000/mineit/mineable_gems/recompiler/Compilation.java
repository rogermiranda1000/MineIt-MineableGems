package com.rogermiranda1000.mineit.mineable_gems.recompiler;

public class Compilation {
    private final String compilationPath;
    private final Error []errors;

    public Compilation(String path, Error []errors) {
        this.compilationPath = path;
        this.errors = errors;
    }

    public String getCompilationPath() {
        return compilationPath;
    }

    public Error[] getErrors() {
        return errors;
    }
}
