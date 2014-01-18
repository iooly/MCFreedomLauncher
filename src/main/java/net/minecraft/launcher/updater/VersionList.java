package net.minecraft.launcher.updater;

import net.minecraft.launcher.OperatingSystem;
import java.util.Iterator;
import net.minecraft.launcher.versions.PartialVersion;
import java.util.Collections;
import java.io.IOException;
import net.minecraft.launcher.versions.CompleteVersion;
import java.util.Collection;
import java.lang.reflect.Type;
import java.util.Date;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.GsonBuilder;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.launcher.versions.ReleaseType;
import java.util.List;
import net.minecraft.launcher.versions.Version;
import java.util.Map;
import com.google.gson.Gson;

public abstract class VersionList
{
    protected final Gson gson;
    private final Map<String, Version> versionsByName;
    private final List<Version> versions;
    private final Map<ReleaseType, Version> latestVersions;
    
    public VersionList() {
        super();
        this.versionsByName = new HashMap<String, Version>();
        this.versions = new ArrayList<Version>();
        this.latestVersions = new EnumMap<ReleaseType, Version>(ReleaseType.class);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
        builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
        builder.enableComplexMapKeySerialization();
        builder.setPrettyPrinting();
        this.gson = builder.create();
    }
    
    public Collection<Version> getVersions() {
        return this.versions;
    }
    
    public Version getLatestVersion(final ReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        return this.latestVersions.get(type);
    }
    
    public Version getVersion(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        return this.versionsByName.get(name);
    }
    
    public CompleteVersion getCompleteVersion(final String name) throws IOException {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        final Version version = this.getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot get complete version of null");
        }
        return this.getCompleteVersion(version);
    }
    
    public CompleteVersion getCompleteVersion(final Version version) throws IOException {
        if (version instanceof CompleteVersion) {
            return (CompleteVersion)version;
        }
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        final CompleteVersion complete = this.gson.fromJson(this.getContent("versions/" + version.getId() + "/" + version.getId() + ".json"), CompleteVersion.class);
        final ReleaseType type = version.getType();
        Collections.replaceAll(this.versions, version, complete);
        this.versionsByName.put(version.getId(), complete);
        if (this.latestVersions.get(type) == version) {
            this.latestVersions.put(type, complete);
        }
        return complete;
    }
    
    protected void clearCache() {
        this.versionsByName.clear();
        this.versions.clear();
        this.latestVersions.clear();
    }
    
    public void refreshVersions() throws IOException {
        this.clearCache();
        final RawVersionList versionList = this.gson.fromJson(this.getContent("versions/versions.json"), RawVersionList.class);
        for (final Version version : versionList.getVersions()) {
            this.versions.add(version);
            this.versionsByName.put(version.getId(), version);
        }
        for (final ReleaseType type : ReleaseType.values()) {
            this.latestVersions.put(type, this.versionsByName.get(versionList.getLatestVersions().get(type)));
        }
    }
    
    public CompleteVersion addVersion(final CompleteVersion version) {
        if (version.getId() == null) {
            throw new IllegalArgumentException("Cannot add blank version");
        }
        if (this.getVersion(version.getId()) != null) {
            throw new IllegalArgumentException("Version '" + version.getId() + "' is already tracked");
        }
        this.versions.add(version);
        this.versionsByName.put(version.getId(), version);
        return version;
    }
    
    public void removeVersion(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        final Version version = this.getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot remove null");
        }
        this.removeVersion(version);
    }
    
    public void removeVersion(final Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot remove null version");
        }
        this.versions.remove(version);
        this.versionsByName.remove(version.getId());
        for (final ReleaseType type : ReleaseType.values()) {
            if (this.getLatestVersion(type) == version) {
                this.latestVersions.remove(type);
            }
        }
    }
    
    public void setLatestVersion(final Version version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot set latest version to null");
        }
        this.latestVersions.put(version.getType(), version);
    }
    
    public void setLatestVersion(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        final Version version = this.getVersion(name);
        if (version == null) {
            throw new IllegalArgumentException("Unknown version - cannot set latest version to null");
        }
        this.setLatestVersion(version);
    }
    
    public String serializeVersionList() {
        final RawVersionList list = new RawVersionList();
        for (final ReleaseType type : ReleaseType.values()) {
            final Version latest = this.getLatestVersion(type);
            if (latest != null) {
                list.getLatestVersions().put(type, latest.getId());
            }
        }
        for (final Version version : this.getVersions()) {
            PartialVersion partial = null;
            if (version instanceof PartialVersion) {
                partial = (PartialVersion)version;
            }
            else {
                partial = new PartialVersion(version);
            }
            list.getVersions().add(partial);
        }
        return this.gson.toJson(list);
    }
    
    public String serializeVersion(final CompleteVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("Cannot serialize null!");
        }
        return this.gson.toJson(version);
    }
    
    public abstract boolean hasAllFiles(final CompleteVersion p0, final OperatingSystem p1);
    
    protected abstract String getContent(final String p0) throws IOException;
    
    private static class RawVersionList
    {
        private List<PartialVersion> versions;
        private Map<ReleaseType, String> latest;
        
        private RawVersionList() {
            super();
            this.versions = new ArrayList<PartialVersion>();
            this.latest = new EnumMap<ReleaseType, String>(ReleaseType.class);
        }
        
        public List<PartialVersion> getVersions() {
            return this.versions;
        }
        
        public Map<ReleaseType, String> getLatestVersions() {
            return this.latest;
        }
    }
}
