package com.rogermiranda1000.mineit.mineable_gems.recompiler;

public class CompileException extends Exception {
    public CompileException() {
        super();
    }

    public CompileException(String err) {
        super(err);
    }

    public CompileException(String err, Error []errors) {
        super(err + ((errors.length == 0) ? "" : ("\n" + CompileException.getErrorsMessage(errors))));
    }

    public CompileException(String err, Error e) {
        this(err, new Error[]{e});
    }

    private static String getErrorsMessage(Error []errors) {
        if (errors.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (Error error : errors) {
            sb.append(error.toString());
            sb.append('\n');
        }
        sb.setLength(sb.length()-1); // remove last \n
        return sb.toString();
    }
}
