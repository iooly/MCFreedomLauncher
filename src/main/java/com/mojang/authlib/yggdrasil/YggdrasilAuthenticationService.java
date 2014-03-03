package com.mojang.authlib.yggdrasil;

import com.google.gson.*;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.Response;
import net.minecraft.launcher.Launcher;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.URL;

public class YggdrasilAuthenticationService extends HttpAuthenticationService
{
    private final String clientToken;
    private final Gson gson;
    
    public YggdrasilAuthenticationService(final Proxy proxy, final String clientToken) {
        super(proxy);
        this.clientToken = clientToken;
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(GameProfile.class, new GameProfileSerializer());
        builder.registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer());
        this.gson = builder.create();
    }
    
    @Override
    public UserAuthentication createUserAuthentication(final Agent agent) {
        return new YggdrasilUserAuthentication(this, agent);
    }
    
    @Override
    public MinecraftSessionService createMinecraftSessionService() {
        return new YggdrasilMinecraftSessionService(this);
    }
    
    protected <T extends Response> T makeRequest(final URL url, final Object input, final Class<T> classOfT,String username) throws AuthenticationException {

        if (Launcher.isOnlineMode()) {
            try {
                 String jsonResult = (input == null) ? this.performGetRequest(url) : this.performPostRequest(url, this.gson.toJson(input), "application/json");

//                String username="Energy";
//
//                jsonResult="{\n" +
//                       "    \"accessToken\": \"aer48sdf684sadf6f4\",\n" +
//                       "    \"clientToken\": \"3ad0e077-0ea1-4a55-a89e-a65e189219f\",\n" +
//                       "    \"selectedProfile\": {\"name\":\""+username+"\",\"id\":\"PLAYER_UUID\"},\n" +
//                       "    \"availableProfiles\":[{\"name\":\""+username+"\",\"id\":\"PLAYER_UUID\"},{\"name\":\""+username+"\",\"id\":\"PLAYER_UUID\"}],\n" +
//                       "    \"error\": \"\",\n" +
//                       "    \"errorMessage\":\"\"\n" +
//                       "    \n" +
//                       "}" ;
                final T result = (T)this.gson.<T>fromJson(jsonResult, classOfT);
                if (result == null) {
                    return null;
                }
                if (!StringUtils.isNotBlank(result.getError())) {
                   return result;
               }
               if ("UserMigratedException".equals(result.getCause())) {
                    throw new UserMigratedException(result.getErrorMessage());
               }
               if (result.getError().equals("ForbiddenOperationException")) {
                    throw new InvalidCredentialsException(result.getErrorMessage());
                }
                throw new AuthenticationException(result.getErrorMessage());
            }
            catch (IOException e) {
                throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
            }
            catch (IllegalStateException e2) {
                throw new AuthenticationUnavailableException("Cannot contact authentication server", e2);
            }
            catch (JsonParseException e3) {
                throw new AuthenticationUnavailableException("Cannot contact authentication server", e3);
            }
        } else {
           // String jsonResult = (input == null) ? this.performGetRequest(url) : this.performPostRequest(url, this.gson.toJson(input), "application/json");

            // username="Energy";

            String jsonResult="{\n" +
                    "    \"accessToken\": \"aer48sdf684sadf6f4\",\n" +
                    "    \"clientToken\": \"3ad0e077-0ea1-4a55-a89e-a65e189219f\",\n" +
                    "    \"selectedProfile\": {\"name\":\""+username+"\",\"id\":\"PLAYER_UUID\"},\n" +
                    "    \"availableProfiles\":[{\"name\":\""+username+"\",\"id\":\"PLAYER_UUID\"},{\"name\":\""+username+"\",\"id\":\"PLAYER_UUID\"}],\n" +
                    "    \"error\": \"\",\n" +
                    "    \"errorMessage\":\"\"\n" +
                    "    \n" +
                    "}" ;
            final T result = this.gson.fromJson(jsonResult, classOfT);
            return result;
        }
    }
    
    public String getClientToken() {
        return this.clientToken;
    }

    private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
        @Override
        public GameProfile deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final JsonObject object = (JsonObject) json;
            final String id = object.has("id") ? object.getAsJsonPrimitive("id").getAsString() : null;
            final String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
            return new GameProfile(id, name);
        }

        @Override
        public JsonElement serialize(final GameProfile src, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonObject result = new JsonObject();
            if (src.getId() != null) {
                result.addProperty("id", src.getId());
            }
            if (src.getName() != null) {
                result.addProperty("name", src.getName());
            }
            return result;
        }
    }
}
