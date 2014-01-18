package net.minecraft.launcher.profile;

import java.util.Arrays;
import net.minecraft.launcher.updater.VersionFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.io.File;
import net.minecraft.launcher.versions.ReleaseType;
import java.util.Set;

public class Profile
{
    public static final String DEFAULT_JRE_ARGUMENTS_64BIT = "-Xmx1G";
    public static final String DEFAULT_JRE_ARGUMENTS_32BIT = "-Xmx512M";
    public static final Resolution DEFAULT_RESOLUTION;
    public static final LauncherVisibilityRule DEFAULT_LAUNCHER_VISIBILITY;
    public static final Set<ReleaseType> DEFAULT_RELEASE_TYPES;
    private String name;
    private File gameDir;
    private String lastVersionId;
    private String javaDir;
    private String javaArgs;
    private Resolution resolution;
    private Set<ReleaseType> allowedReleaseTypes;
    private String playerUUID;
    private Boolean useHopperCrashService;
    private LauncherVisibilityRule launcherVisibilityOnGameClose;
    private Map<String, String> authentication;
    
    public Profile() {
        super();
    }
    
    public Profile(final Profile copy) {
        super();
        this.name = copy.name;
        this.gameDir = copy.gameDir;
        this.playerUUID = copy.playerUUID;
        this.lastVersionId = copy.lastVersionId;
        this.javaDir = copy.javaDir;
        this.javaArgs = copy.javaArgs;
        this.resolution = ((copy.resolution == null) ? null : new Resolution(copy.resolution));
        this.allowedReleaseTypes = ((copy.allowedReleaseTypes == null) ? null : new HashSet<ReleaseType>(copy.allowedReleaseTypes));
        this.useHopperCrashService = copy.useHopperCrashService;
        this.launcherVisibilityOnGameClose = copy.launcherVisibilityOnGameClose;
    }
    
    public Profile(final String name) {
        super();
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public File getGameDir() {
        return this.gameDir;
    }
    
    public void setGameDir(final File gameDir) {
        this.gameDir = gameDir;
    }
    
    public void setLastVersionId(final String lastVersionId) {
        this.lastVersionId = lastVersionId;
    }
    
    public void setJavaDir(final String javaDir) {
        this.javaDir = javaDir;
    }
    
    public void setJavaArgs(final String javaArgs) {
        this.javaArgs = javaArgs;
    }
    
    public String getLastVersionId() {
        return this.lastVersionId;
    }
    
    public String getJavaArgs() {
        return this.javaArgs;
    }
    
    public String getJavaPath() {
        return this.javaDir;
    }
    
    public Resolution getResolution() {
        return this.resolution;
    }
    
    public void setResolution(final Resolution resolution) {
        this.resolution = resolution;
    }
    
    public String getPlayerUUID() {
        return this.playerUUID;
    }
    
    public void setPlayerUUID(final String playerUUID) {
        this.playerUUID = playerUUID;
    }
    
    public Set<ReleaseType> getAllowedReleaseTypes() {
        return this.allowedReleaseTypes;
    }
    
    public void setAllowedReleaseTypes(final Set<ReleaseType> allowedReleaseTypes) {
        this.allowedReleaseTypes = allowedReleaseTypes;
    }
    
    public boolean getUseHopperCrashService() {
        return this.useHopperCrashService == null;
    }
    
    public void setUseHopperCrashService(final boolean useHopperCrashService) {
        this.useHopperCrashService = (useHopperCrashService ? null : false);
    }
    
    public VersionFilter getVersionFilter() {
        final VersionFilter filter = new VersionFilter().setMaxCount(Integer.MAX_VALUE);
        if (this.allowedReleaseTypes == null) {
            filter.onlyForTypes((ReleaseType[])Profile.DEFAULT_RELEASE_TYPES.toArray(new ReleaseType[Profile.DEFAULT_RELEASE_TYPES.size()]));
        }
        else {
            filter.onlyForTypes((ReleaseType[])this.allowedReleaseTypes.toArray(new ReleaseType[this.allowedReleaseTypes.size()]));
        }
        return filter;
    }
    
    public LauncherVisibilityRule getLauncherVisibilityOnGameClose() {
        return this.launcherVisibilityOnGameClose;
    }
    
    public void setLauncherVisibilityOnGameClose(final LauncherVisibilityRule launcherVisibilityOnGameClose) {
        this.launcherVisibilityOnGameClose = launcherVisibilityOnGameClose;
    }
    
    @Deprecated
    public Map<String, String> getAuthentication() {
        return this.authentication;
    }
    
    @Deprecated
    public void setAuthentication(final Map<String, String> authentication) {
        this.authentication = authentication;
    }
    
    static {
        DEFAULT_RESOLUTION = new Resolution(854, 480);
        DEFAULT_LAUNCHER_VISIBILITY = LauncherVisibilityRule.CLOSE_LAUNCHER;
        DEFAULT_RELEASE_TYPES = new HashSet<ReleaseType>(Arrays.asList(ReleaseType.RELEASE));
    }
    
    public static class Resolution
    {
        private int width;
        private int height;
        
        public Resolution() {
            super();
        }
        
        public Resolution(final Resolution resolution) {
            this(resolution.getWidth(), resolution.getHeight());
        }
        
        public Resolution(final int width, final int height) {
            super();
            this.width = width;
            this.height = height;
        }
        
        public int getWidth() {
            return this.width;
        }
        
        public int getHeight() {
            return this.height;
        }
    }
}
