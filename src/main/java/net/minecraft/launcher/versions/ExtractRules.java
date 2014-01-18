package net.minecraft.launcher.versions;

import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class ExtractRules
{
    private List<String> exclude;
    
    public ExtractRules() {
        super();
        this.exclude = new ArrayList<String>();
    }
    
    public ExtractRules(final String... exclude) {
        super();
        this.exclude = new ArrayList<String>();
        if (exclude != null) {
            Collections.<String>addAll(this.exclude, exclude);
        }
    }
    
    public ExtractRules(final ExtractRules rules) {
        super();
        this.exclude = new ArrayList<String>();
        for (final String exclude : rules.exclude) {
            this.exclude.add(exclude);
        }
    }
    
    public List<String> getExcludes() {
        return this.exclude;
    }
    
    public boolean shouldExtract(final String path) {
        if (this.exclude != null) {
            for (final String rule : this.exclude) {
                if (path.startsWith(rule)) {
                    return false;
                }
            }
        }
        return true;
    }
}
