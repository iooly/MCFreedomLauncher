package com.mojang.authlib;

import org.apache.commons.lang3.StringUtils;

public class GameProfile
{
    private final String id;
    private final String name;
    private boolean legacy;
    
    public GameProfile(final String id, final String name) {
        super();
        if (StringUtils.isBlank(id) && StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name and ID cannot both be blank");
        }
        this.id = id;
        this.name = name;
    }
    
    public String getId() {
        return this.id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public boolean isComplete() {
        return StringUtils.isNotBlank(this.getId()) && StringUtils.isNotBlank(this.getName());
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final GameProfile that = (GameProfile)o;
        return this.id.equals(that.id) && this.name.equals(that.name);
    }
    
    @Override
    public int hashCode() {
        int result = this.id.hashCode();
        result = 31 * result + this.name.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return "GameProfile{id='" + this.id + '\'' + ", name='" + this.name + '\'' + '}';
    }
    
    public boolean isLegacy() {
        return this.legacy;
    }
}