package com.rogermiranda1000.mineit.mineable_gems.recompiler;

public class MatchNotFoundException extends RuntimeException {
    public MatchNotFoundException() {
        super();
    }

    public MatchNotFoundException(String err) {
        super(err);
    }
}
