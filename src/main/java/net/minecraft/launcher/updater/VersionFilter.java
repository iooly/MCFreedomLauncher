package net.minecraft.launcher.updater;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import net.minecraft.launcher.versions.ReleaseType;
import java.util.Set;

public class VersionFilter
{
    private final Set<ReleaseType> types;
    private int maxCount;
    
    public VersionFilter() {
        super();
        this.types = new HashSet<ReleaseType>();
        this.maxCount = 5;
        Collections.<ReleaseType>addAll(this.types, ReleaseType.values());
    }
    
    public Set<ReleaseType> getTypes() {
        return this.types;
    }
    
    public VersionFilter onlyForTypes(final ReleaseType... types) {
        this.types.clear();
        this.includeTypes(types);
        return this;
    }
    
    public VersionFilter includeTypes(final ReleaseType... types) {
        if (types != null) {
            Collections.<ReleaseType>addAll(this.types, types);
        }
        return this;
    }
    
    public VersionFilter excludeTypes(final ReleaseType... types) {
        if (types != null) {
            for (final ReleaseType type : types) {
                this.types.remove(type);
            }
        }
        return this;
    }
    
    public int getMaxCount() {
        return this.maxCount;
    }
    
    public VersionFilter setMaxCount(final int maxCount) {
        this.maxCount = maxCount;
        return this;
    }
}
