package com.rogermiranda1000.mineit.mineable_gems.recompiler.replacers;

import com.rogermiranda1000.mineit.mineable_gems.recompiler.MatchNotFoundException;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Search in the decompiled code for the regex, and replace
 */
public class RegexCodeReplacer implements CodeReplacer {
    private final Pattern searchPattern;
    private final Function<String[], String> replace;
    private final boolean justOneTime;

    /**
     * Throw an error if not match
     */
    private final boolean isEssential;

    public RegexCodeReplacer(String regex, Function<String[], String> replace, boolean isEssential, boolean justOneTime) {
        this.searchPattern = Pattern.compile(regex);
        this.replace = replace;
        this.isEssential = isEssential;
        this.justOneTime = justOneTime;
    }

    /**
     * @throws MatchNotFoundException only if isEssential
     */
    @Override
    public String replace(String original) throws MatchNotFoundException {
        boolean isFirst = true;
        do {
            Matcher m = this.searchPattern.matcher(original);
            if (!m.find()) {
                if (this.isEssential && isFirst) throw new MatchNotFoundException();
                return original;
            }

            String[] groups = new String[m.groupCount()];
            for (int i = 0; i < groups.length; i++) groups[i] = m.group(i + 1);

            original = original.substring(0, m.end()) + this.replace.apply(groups) + original.substring(m.end()); // append at the middle
            isFirst = false;
        } while (!this.justOneTime);
        return original; // only reached by justOneTime objects
    }
}
