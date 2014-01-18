package net.minecraft.launcher.profile;

import java.util.Set;
import java.util.HashSet;
import javax.swing.SwingUtilities;
import java.util.Iterator;
import java.util.Collection;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import java.util.UUID;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.launcher.updater.FileTypeAdapter;
import java.lang.reflect.Type;
import net.minecraft.launcher.updater.DateTypeAdapter;
import java.util.Date;
import com.google.gson.TypeAdapterFactory;
import net.minecraft.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import com.google.gson.GsonBuilder;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.launcher.authentication.AuthenticationDatabase;
import net.minecraft.launcher.events.RefreshedProfilesListener;
import java.util.List;
import java.io.File;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import net.minecraft.launcher.Launcher;

public class ProfileManager
{
    public static final String DEFAULT_PROFILE_NAME = "(Default)";
    private final Launcher launcher;
    private final JsonParser parser;
    private final Gson gson;
    private final Map<String, Profile> profiles;
    private final File profileFile;
    private final List<RefreshedProfilesListener> refreshedProfilesListeners;
    private String selectedProfile;
    private AuthenticationDatabase authDatabase;
    
    public ProfileManager(final Launcher launcher) {
        super();
        this.parser = new JsonParser();
        this.profiles = new HashMap<String, Profile>();
        this.refreshedProfilesListeners = Collections.<RefreshedProfilesListener>synchronizedList(new ArrayList<RefreshedProfilesListener>());
        this.launcher = launcher;
        this.profileFile = new File(launcher.getWorkingDirectory(), "launcher_profiles.json");
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.registerTypeAdapter(File.class, new FileTypeAdapter());
        builder.registerTypeAdapter(AuthenticationDatabase.class, new AuthenticationDatabase.Serializer());
        builder.setPrettyPrinting();
        this.gson = builder.create();
        this.authDatabase = new AuthenticationDatabase(new YggdrasilAuthenticationService(launcher.getProxy(), launcher.getClientToken().toString()));
    }
    
    public void saveProfiles() throws IOException {
        final RawProfileList rawProfileList = new RawProfileList();
        rawProfileList.profiles = this.profiles;
        rawProfileList.selectedProfile = this.getSelectedProfile().getName();
        rawProfileList.clientToken = this.launcher.getClientToken();
        rawProfileList.authenticationDatabase = this.authDatabase;
        FileUtils.writeStringToFile(this.profileFile, this.gson.toJson(rawProfileList));
    }
    
    public boolean loadProfiles() throws IOException {
        this.profiles.clear();
        this.selectedProfile = null;
        if (this.profileFile.isFile()) {
            final JsonObject object = this.parser.parse(FileUtils.readFileToString(this.profileFile)).getAsJsonObject();
            if (object.has("clientToken")) {
                this.launcher.setClientToken(this.gson.<UUID>fromJson(object.get("clientToken"), UUID.class));
            }
            final RawProfileList rawProfileList = (RawProfileList)this.gson.<RawProfileList>fromJson(object, RawProfileList.class);
            this.profiles.putAll(rawProfileList.profiles);
            this.selectedProfile = rawProfileList.selectedProfile;
            this.authDatabase = rawProfileList.authenticationDatabase;
            this.fireRefreshEvent();
            return true;
        }
        this.fireRefreshEvent();
        return false;
    }
    
    public void fireRefreshEvent() {
        final List<RefreshedProfilesListener> listeners = new ArrayList<RefreshedProfilesListener>(this.refreshedProfilesListeners);
        final Iterator<RefreshedProfilesListener> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            final RefreshedProfilesListener listener = (RefreshedProfilesListener)iterator.next();
            if (!listener.shouldReceiveEventsInUIThread()) {
                listener.onProfilesRefreshed(this);
                iterator.remove();
            }
        }
        if (!listeners.isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (final RefreshedProfilesListener listener : listeners) {
                        listener.onProfilesRefreshed(ProfileManager.this);
                    }
                }
            });
        }
    }
    
    public Profile getSelectedProfile() {
        if (this.selectedProfile == null || !this.profiles.containsKey(this.selectedProfile)) {
            if (this.profiles.get("(Default)") != null) {
                this.selectedProfile = "(Default)";
            }
            else if (this.profiles.size() > 0) {
                this.selectedProfile = ((Profile)this.profiles.values().iterator().next()).getName();
            }
            else {
                this.selectedProfile = "(Default)";
                this.profiles.put("(Default)", new Profile(this.selectedProfile));
            }
        }
        return this.profiles.get(this.selectedProfile);
    }
    
    public Map<String, Profile> getProfiles() {
        return this.profiles;
    }
    
    public Launcher getLauncher() {
        return this.launcher;
    }
    
    public void addRefreshedProfilesListener(final RefreshedProfilesListener listener) {
        this.refreshedProfilesListeners.add(listener);
    }
    
    public void setSelectedProfile(final String selectedProfile) {
        final boolean update = !this.selectedProfile.equals(selectedProfile);
        this.selectedProfile = selectedProfile;
        if (update) {
            this.fireRefreshEvent();
        }
    }
    
    public AuthenticationDatabase getAuthDatabase() {
        return this.authDatabase;
    }
    
    public void trimAuthDatabase() {
        final Set<String> uuids = new HashSet<String>(this.authDatabase.getknownUUIDs());
        for (final Profile profile : this.profiles.values()) {
            uuids.remove(profile.getPlayerUUID());
        }
        for (final String uuid : uuids) {
            this.authDatabase.removeUUID(uuid);
        }
    }
    
    private static class RawProfileList
    {
        public Map<String, Profile> profiles;
        public String selectedProfile;
        public UUID clientToken;
        public AuthenticationDatabase authenticationDatabase;
        
        private RawProfileList() {
            super();
            this.profiles = new HashMap<String, Profile>();
            this.clientToken = UUID.randomUUID();
            this.authenticationDatabase = new AuthenticationDatabase(new YggdrasilAuthenticationService(Launcher.getInstance().getProxy(), Launcher.getInstance().getClientToken().toString()));
        }
    }
}
