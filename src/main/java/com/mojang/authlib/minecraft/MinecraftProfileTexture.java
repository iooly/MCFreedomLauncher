package com.mojang.authlib.minecraft;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MinecraftProfileTexture {
    private final String url;

    public MinecraftProfileTexture(final String url) {
        super();
        this.url = url;
    }

    public String getUrl() {
        return this.url;
    }

    public String getHash() {
        return FilenameUtils.getBaseName(this.url);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("url", this.url).append("hash", this.getHash()).toString();
    }

    public enum Type {
        SKIN,
        CAPE;
    }
}