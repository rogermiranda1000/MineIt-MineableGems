package com.rogermiranda1000.mineit.mineable_gems.recompiler;

public class AlreadyRecompiledException extends RuntimeException {
    public AlreadyRecompiledException(String err) {
        super(err);
    }

    public AlreadyRecompiledException() {
        super();
    }
}
