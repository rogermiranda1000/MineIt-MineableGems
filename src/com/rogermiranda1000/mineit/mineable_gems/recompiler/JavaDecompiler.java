package com.rogermiranda1000.mineit.mineable_gems.recompiler;

public interface JavaDecompiler {
    /**
     * Decompiles a .jar class
     * @param jarPath   .jar path
     * @param className Class#getName()
     * @return          Class code
     */
    public String decompileClass(String jarPath, String className) throws Exception;
}
