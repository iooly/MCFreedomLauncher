package com.mojang.authlib.legacy;

import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import org.apache.commons.lang3.Validate;
import com.mojang.authlib.Agent;
import java.net.Proxy;
import com.mojang.authlib.HttpAuthenticationService;

public class LegacyAuthenticationService extends HttpAuthenticationService
{
    protected LegacyAuthenticationService(final Proxy proxy) {
        super(proxy);
    }
    
    @Override
    public LegacyUserAuthentication createUserAuthentication(final Agent agent) {
        Validate.<Agent>notNull(agent);
        if (agent != Agent.MINECRAFT) {
            throw new IllegalArgumentException("Legacy authentication cannot handle anything but Minecraft");
        }
        return new LegacyUserAuthentication(this);
    }
    
    @Override
    public LegacyMinecraftSessionService createMinecraftSessionService() {
        return new LegacyMinecraftSessionService(this);
    }
}
