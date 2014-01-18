package com.mojang.authlib;

import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import java.util.HashMap;
import java.util.Collection;
import java.util.Map;

public abstract class BaseUserAuthentication implements UserAuthentication
{
    protected static final String STORAGE_KEY_PROFILE_NAME = "displayName";
    protected static final String STORAGE_KEY_PROFILE_ID = "uuid";
    protected static final String STORAGE_KEY_USER_NAME = "username";
    protected static final String STORAGE_KEY_USER_ID = "userid";
    private final AuthenticationService authenticationService;
    private final Map<String, Collection<String>> userProperties;
    private String userid;
    private String username;
    private String password;
    private GameProfile selectedProfile;
    private UserType userType;
    
    protected BaseUserAuthentication(final AuthenticationService authenticationService) {
        super();
        this.userProperties = new HashMap<String, Collection<String>>();
        Validate.<AuthenticationService>notNull(authenticationService);
        this.authenticationService = authenticationService;
    }
    
    @Override
    public boolean canLogIn() {
        return !this.canPlayOnline() && StringUtils.isNotBlank(this.getUsername()) && StringUtils.isNotBlank(this.getPassword());
    }
    
    @Override
    public void logOut() {
        this.password = null;
        this.userid = null;
        this.setSelectedProfile(null);
        this.getModifiableUserProperties().clear();
        this.setUserType(null);
    }
    
    @Override
    public boolean isLoggedIn() {
        return this.getSelectedProfile() != null;
    }
    
    @Override
    public void setUsername(final String username) {
        if (this.isLoggedIn() && this.canPlayOnline()) {
            throw new IllegalStateException("Cannot change username whilst logged in & online");
        }
        this.username = username;
    }
    
    @Override
    public void setPassword(final String password) {
        if (this.isLoggedIn() && this.canPlayOnline() && StringUtils.isNotBlank(password)) {
            throw new IllegalStateException("Cannot set password whilst logged in & online");
        }
        this.password = password;
    }
    
    protected String getUsername() {
        return this.username;
    }
    
    protected String getPassword() {
        return this.password;
    }
    
    @Override
    public void loadFromStorage(final Map<String, String> credentials) {
        this.logOut();
        this.setUsername(credentials.get("username"));
        if (credentials.containsKey("userid")) {
            this.userid = credentials.get("userid");
        }
        else {
            this.userid = this.username;
        }
        if (credentials.containsKey("displayName") && credentials.containsKey("uuid")) {
            this.setSelectedProfile(new GameProfile(credentials.get("uuid"), credentials.get("displayName")));
        }
    }
    
    @Override
    public Map<String, String> saveForStorage() {
        final Map<String, String> result = new HashMap<String, String>();
        if (this.getUsername() != null) {
            result.put("username", this.getUsername());
        }
        if (this.getUserID() != null) {
            result.put("userid", this.getUserID());
        }
        else if (this.getUsername() != null) {
            result.put("username", this.getUsername());
        }
        if (this.getSelectedProfile() != null) {
            result.put("displayName", this.getSelectedProfile().getName());
            result.put("uuid", this.getSelectedProfile().getId());
        }
        return result;
    }
    
    protected void setSelectedProfile(final GameProfile selectedProfile) {
        this.selectedProfile = selectedProfile;
    }
    
    @Override
    public GameProfile getSelectedProfile() {
        return this.selectedProfile;
    }
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(this.getClass().getSimpleName());
        result.append("{");
        if (this.isLoggedIn()) {
            result.append("Logged in as ");
            result.append(this.getUsername());
            if (this.getSelectedProfile() != null) {
                result.append(" / ");
                result.append(this.getSelectedProfile());
                result.append(" - ");
                if (this.canPlayOnline()) {
                    result.append("Online");
                }
                else {
                    result.append("Offline");
                }
            }
        }
        else {
            result.append("Not logged in");
        }
        result.append("}");
        return result.toString();
    }
    
    public AuthenticationService getAuthenticationService() {
        return this.authenticationService;
    }
    
    @Override
    public String getUserID() {
        return this.userid;
    }
    
    @Override
    public Map<String, Collection<String>> getUserProperties() {
        if (this.isLoggedIn()) {
            return Collections.<String, Collection<String>>unmodifiableMap((Map<? extends String, ? extends Collection<String>>)this.getModifiableUserProperties());
        }
        return Collections.<String, Collection<String>>unmodifiableMap((Map<? extends String, ? extends Collection<String>>)new HashMap<String, Collection<String>>());
    }
    
    protected Map<String, Collection<String>> getModifiableUserProperties() {
        return this.userProperties;
    }
    
    @Override
    public UserType getUserType() {
        if (this.isLoggedIn()) {
            return (this.userType == null) ? UserType.LEGACY : this.userType;
        }
        return null;
    }
    
    protected void setUserType(final UserType userType) {
        this.userType = userType;
    }
    
    protected void setUserid(final String userid) {
        this.userid = userid;
    }
}
