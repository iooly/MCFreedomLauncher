package com.mojang.authlib.yggdrasil.response;

import com.mojang.authlib.properties.PropertyMap;

public class MinecraftProfilePropertiesResponse extends Response {
    private String id;
    private String name;
    private PropertyMap properties;

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public PropertyMap getProperties() {
        return this.properties;
    }
}
