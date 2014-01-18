package com.mojang.authlib.minecraft;

import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.GameProfile;

public interface MinecraftSessionService
{
    void joinServer(GameProfile p0, String p1, String p2) throws AuthenticationException;
    
    GameProfile hasJoinedServer(GameProfile p0, String p1) throws AuthenticationUnavailableException;
}
