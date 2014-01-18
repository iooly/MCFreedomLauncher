package com.mojang.authlib;

import java.util.Collection;
import java.util.Map;
import com.mojang.authlib.exceptions.AuthenticationException;

public interface UserAuthentication
{
    boolean canLogIn();
    
    void logIn() throws AuthenticationException;
    
    void logOut();
    
    boolean isLoggedIn();
    
    boolean canPlayOnline();
    
    GameProfile[] getAvailableProfiles();
    
    GameProfile getSelectedProfile();
    
    void selectGameProfile(GameProfile p0) throws AuthenticationException;
    
    void loadFromStorage(Map<String, String> p0);
    
    Map<String, String> saveForStorage();
    
    void setUsername(String p0);
    
    void setPassword(String p0);
    
    String getAuthenticatedToken();
    
    String getUserID();
    
    Map<String, Collection<String>> getUserProperties();
    
    UserType getUserType();
}
