package com.mojang.authlib.yggdrasil;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.Response;
import net.minecraft.launcher.Launcher;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;

public class YggdrasilAuthenticationService extends HttpAuthenticationService
{
    private final String clientToken;
    private final Gson gson;
    
    public YggdrasilAuthenticationService(final Proxy proxy, final String clientToken) {
        super(proxy);
        this.gson = new Gson();
        this.clientToken = clientToken;
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
}
