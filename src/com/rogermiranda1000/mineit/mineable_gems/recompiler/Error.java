package com.rogermiranda1000.mineit.mineable_gems.recompiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

public class Error {
    /**
     * Compiler error
     */
    private final String error;

    /**
     * .java error line
     */
    private final String errorLine;

    /**
     * Character of errorLine that causes the error
     */
    private final int errorCharacter;

    /**
     * File that raises the error
     */
    private final String file;

    /**
     * Line where errorLine is found
     */
    private final int line;

    public Error(String file, int line, String error, String sourceCodeError, int sourceCodeErrorCharacter) {
        this.file = file;
        this.error = error;
        this.line = line;
        this.errorLine = sourceCodeError;
        this.errorCharacter = sourceCodeErrorCharacter;
    }

    public String getFile() {
        return file;
    }

    public String getError() {
        return this.error;
    }

    public String getErrorLine() {
        return this.errorLine;
    }

    public int getErrorCharacter() {
        return this.errorCharacter;
    }

    public int getLine() {
        return this.line;
    }

    public static Error []getErrors(String errors) {
        /**
         *  CLASS.java:LINE: error: incompatible types: List<CAP#1> cannot be converted to List<String>
         *            customDrop.setBiomeFilter(section.getList("Biome-Filter", null));
         *                                                     |
         */
        // TODO get errors
        return new Error[]{};
    }

    public static Error []getErrors(BufferedReader stdError) throws IOException {
        return Error.getErrors(stdError.lines().collect(Collectors.joining()));
    }

    @Override
    public String toString() {
        return this.file + ":" + this.line + " - " + this.error;
    }
}
