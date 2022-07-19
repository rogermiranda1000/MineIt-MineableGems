package com.rogermiranda1000.mineit.mineable_gems;

import java.io.IOException;
import java.util.List;

public class BuildToolsFailedException extends IOException {
    public BuildToolsFailedException(List<String> err) {
        super(err.toString());
    }
}
