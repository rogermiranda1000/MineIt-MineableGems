package com.rogermiranda1000.mineit.mineable_gems.recompiler.replacers;

public class DependenciesImporter implements CodeReplacer {
    private final String add;
    public DependenciesImporter(Class<?> importClass) {
        this.add = "import " + importClass.getName() + ";";
    }

    @Override
    public String replace(String original) {
        int split = original.indexOf(';')+1;
        return original.substring(0, split) + System.lineSeparator() + this.add + original.substring(split);
    }
}
