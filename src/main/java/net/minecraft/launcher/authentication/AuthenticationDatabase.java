package net.minecraft.launcher.authentication;

import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.launcher.Launcher;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import java.lang.reflect.Type;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import com.mojang.authlib.GameProfile;
import java.util.Iterator;
import java.util.HashMap;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.UserAuthentication;
import java.util.Map;

public class AuthenticationDatabase
{
    public static final String DEMO_UUID_PREFIX = "demo-";
    private final Map<String, UserAuthentication> authById;
    private final AuthenticationService authenticationService;
    
    public AuthenticationDatabase(final AuthenticationService authenticationService) {
        this(new HashMap<String, UserAuthentication>(), authenticationService);
    }
    
    public AuthenticationDatabase(final Map<String, UserAuthentication> authById, final AuthenticationService authenticationService) {
        super();
        this.authById = authById;
        this.authenticationService = authenticationService;
    }
    
    public UserAuthentication getByName(final String name) {
        if (name == null) {
            return null;
        }
        for (final Map.Entry<String, UserAuthentication> entry : this.authById.entrySet()) {
            final GameProfile profile = ((UserAuthentication)entry.getValue()).getSelectedProfile();
            if (profile != null && profile.getName().equals(name)) {
                return entry.getValue();
            }
            if (profile == null && getUserFromDemoUUID(entry.getKey()).equals(name)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    public UserAuthentication getByUUID(final String uuid) {
        return this.authById.get(uuid);
    }
    
    public Collection<String> getKnownNames() {
        final List<String> names = new ArrayList<String>();
        for (final Map.Entry<String, UserAuthentication> entry : this.authById.entrySet()) {
            final GameProfile profile = ((UserAuthentication)entry.getValue()).getSelectedProfile();
            if (profile != null) {
                names.add(profile.getName());
            }
            else {
                names.add(getUserFromDemoUUID(entry.getKey()));
            }
        }
        return names;
    }
    
    public void register(final String uuid, final UserAuthentication authentication) {
        this.authById.put(uuid, authentication);
    }
    
    public Set<String> getknownUUIDs() {
        return this.authById.keySet();
    }
    
    public void removeUUID(final String uuid) {
        this.authById.remove(uuid);
    }
    
    public AuthenticationService getAuthenticationService() {
        return this.authenticationService;
    }
    
    public static String getUserFromDemoUUID(final String uuid) {
        if (uuid.startsWith("demo-") && uuid.length() > "demo-".length()) {
            return "Demo User " + uuid.substring("demo-".length());
        }
        return "Demo User";
    }
    
    public static class Serializer implements JsonDeserializer<AuthenticationDatabase>, JsonSerializer<AuthenticationDatabase>
    {
        @Override
        public AuthenticationDatabase deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final TypeToken<HashMap<String, Map<String, String>>> token = new TypeToken<HashMap<String, Map<String, String>>>() {};
            final Map<String, UserAuthentication> services = new HashMap<String, UserAuthentication>();
            final Map<String, Map<String, String>> credentials = (Map<String, Map<String, String>>)context.<Map<String, Map<String, String>>>deserialize(json, token.getType());
            final YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(Launcher.getInstance().getProxy(), Launcher.getInstance().getClientToken().toString());
            for (final Map.Entry<String, Map<String, String>> entry : credentials.entrySet()) {
                final UserAuthentication auth = authService.createUserAuthentication(Agent.MINECRAFT);
                auth.loadFromStorage(entry.getValue());
                services.put(entry.getKey(), auth);
            }
            return new AuthenticationDatabase(services, authService);
        }
        
        @Override
        public JsonElement serialize(final AuthenticationDatabase src, final Type typeOfSrc, final JsonSerializationContext context) {
            final Map<String, UserAuthentication> services = src.authById;
            final Map<String, Map<String, String>> credentials = new HashMap<String, Map<String, String>>();
            for (final Map.Entry<String, UserAuthentication> entry : services.entrySet()) {
                credentials.put(entry.getKey(), ((UserAuthentication)entry.getValue()).saveForStorage());
            }
            return context.serialize(credentials);
        }
    }
}
