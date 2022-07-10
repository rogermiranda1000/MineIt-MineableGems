package com.rogermiranda1000.mineit.mineable_gems.recompiler;

public class CompileException extends Exception {
    public CompileException() {
        super();
    }

    public CompileException(String err) {
        super(err);
    }

    public CompileException(String err, Error []errors) {
        super(err + "\n" + CompileException.getErrorsMessage(errors));
    }

    private static String getErrorsMessage(Error []errors) {
        StringBuilder sb = new StringBuilder();
        for (Error error : errors) {
            sb.append(error.toString());
            sb.append('\n');
        }
        return sb.toString();
    }
}
