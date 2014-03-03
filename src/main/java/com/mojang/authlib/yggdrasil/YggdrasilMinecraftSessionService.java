package com.mojang.authlib.yggdrasil;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService
{
    private static final Logger LOGGER;
    private static final String BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";
    private static final URL JOIN_URL;
    private static final URL CHECK_URL;
    private final PublicKey publicKey;
    private final Gson gson;
    
    protected YggdrasilMinecraftSessionService(final YggdrasilAuthenticationService authenticationService) {
        super(authenticationService);
        this.gson = new Gson();
        try {
            final X509EncodedKeySpec spec = new X509EncodedKeySpec(IOUtils.toByteArray(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der")));
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new Error("Missing/invalid yggdrasil public key!");
        }
    }
    
    @Override
    public void joinServer(final GameProfile profile, final String authenticationToken, final String serverId) throws AuthenticationException {
        final JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;
        this.getAuthenticationService().<Response>makeRequest(YggdrasilMinecraftSessionService.JOIN_URL, request, Response.class,profile.getName());
    }
    
    @Override
    public GameProfile hasJoinedServer(final GameProfile user, final String serverId) throws AuthenticationUnavailableException {
        final Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("username", user.getName());
        arguments.put("serverId", serverId);
        final URL url = HttpAuthenticationService.concatenateURL(YggdrasilMinecraftSessionService.CHECK_URL, HttpAuthenticationService.buildQuery(arguments));
        try {
            final HasJoinedMinecraftServerResponse response = this.getAuthenticationService().<HasJoinedMinecraftServerResponse>makeRequest(url, null, HasJoinedMinecraftServerResponse.class, user.getName());
            if (response != null && response.getId() != null) {
                final GameProfile result = new GameProfile(response.getId(), user.getName());
                if (response.getProperties() != null) {
                    result.getProperties().putAll(response.getProperties());
                }
                return result;
            }
            return null;
        }
        catch (AuthenticationUnavailableException e) {
            throw e;
        }
        catch (AuthenticationException e2) {
            return null;
        }
    }
    
    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(final GameProfile profile, final boolean requireSecure) {
        final Property textureProperty = Iterables.<Property>getFirst((Iterable<? extends Property>) (profile.getProperties()).get("textures"), (Property) null);
        if (textureProperty == null) {
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }
        if (!textureProperty.hasSignature()) {
            YggdrasilMinecraftSessionService.LOGGER.error("Signature is missing from textures payload");
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }
        if (!textureProperty.isSignatureValid(this.publicKey)) {
            YggdrasilMinecraftSessionService.LOGGER.error("Textures payload has been tampered with (signature invalid)");
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }
        MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.decodeBase64(textureProperty.getValue()), Charsets.UTF_8);
            result = this.gson.<MinecraftTexturesPayload>fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException e) {
            YggdrasilMinecraftSessionService.LOGGER.error("Could not decode textures payload", e);
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }
        if (result.getProfileId() == null || !result.getProfileId().equals(profile.getId())) {
            YggdrasilMinecraftSessionService.LOGGER.error("Decrypted textures payload was for another user (expected id {} but was for {})", profile.getId(), result.getProfileId());
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }
        if (result.getProfileName() == null || !result.getProfileName().equals(profile.getName())) {
            YggdrasilMinecraftSessionService.LOGGER.error("Decrypted textures payload was for another user (expected name {} but was for {})", profile.getName(), result.getProfileName());
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }
        if (requireSecure) {
            if (result.isPublic()) {
                YggdrasilMinecraftSessionService.LOGGER.error("Decrypted textures payload was public but we require secure data");
                return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
            }
            final Calendar limit = Calendar.getInstance();
            limit.add(5, -1);
            final Date validFrom = new Date(result.getTimestamp());
            if (validFrom.before(limit.getTime())) {
                YggdrasilMinecraftSessionService.LOGGER.error("Decrypted textures payload is too old ({0}, but we need it to be at least {1})", validFrom, limit);
                return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
            }
        }
        return (result.getTextures() == null) ? new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>() : result.getTextures();
    }

    @Override
    public GameProfile fillProfileProperties(final GameProfile profile) {
        if (profile.getId() == null || profile.getId().length() == 0) {
            return profile;
        }
        try {
            final URL url = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/profile/" + profile.getId());
            final MinecraftProfilePropertiesResponse response = this.getAuthenticationService().<MinecraftProfilePropertiesResponse>makeRequest(url, null, MinecraftProfilePropertiesResponse.class, profile.getName());
            YggdrasilMinecraftSessionService.LOGGER.debug("Successfully fetched profile properties for " + profile);
            final GameProfile result = new GameProfile(response.getId(), response.getName());
            result.getProperties().putAll(response.getProperties());
            profile.getProperties().putAll(response.getProperties());
            return result;
        } catch (AuthenticationException e) {
            YggdrasilMinecraftSessionService.LOGGER.warn("Couldn't look up profile properties for " + profile, e);
            return profile;
        }
    }

    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService)super.getAuthenticationService();
    }
    
    static {
        LOGGER = LogManager.getLogger();
        JOIN_URL = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/join");
        CHECK_URL = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/hasJoined");
    }
}
