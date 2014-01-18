package net.minecraft.launcher.versions;

import java.net.MalformedURLException;
import net.minecraft.launcher.updater.download.ChecksummedDownloadable;
import java.net.URL;
import net.minecraft.launcher.updater.download.Downloadable;
import java.net.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.io.File;
import net.minecraft.launcher.OperatingSystem;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class CompleteVersion implements Version
{
    private String id;
    private Date time;
    private Date releaseTime;
    private ReleaseType type;
    private String minecraftArguments;
    private List<Library> libraries;
    private String mainClass;
    private int minimumLauncherVersion;
    private String incompatibilityReason;
    private String assets;
    private List<Rule> rules;
    private transient boolean synced;
    
    public CompleteVersion() {
        super();
        this.synced = false;
    }
    
    public CompleteVersion(final String id, final Date releaseTime, final Date updateTime, final ReleaseType type, final String mainClass, final String minecraftArguments) {
        super();
        this.synced = false;
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        if (releaseTime == null) {
            throw new IllegalArgumentException("Release time cannot be null");
        }
        if (updateTime == null) {
            throw new IllegalArgumentException("Update time cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Release type cannot be null");
        }
        if (mainClass == null || mainClass.length() == 0) {
            throw new IllegalArgumentException("Main class cannot be null or empty");
        }
        if (minecraftArguments == null) {
            throw new IllegalArgumentException("Process arguments cannot be null or empty");
        }
        this.id = id;
        this.releaseTime = releaseTime;
        this.time = updateTime;
        this.type = type;
        this.mainClass = mainClass;
        this.libraries = new ArrayList<Library>();
        this.minecraftArguments = minecraftArguments;
    }
    
    public CompleteVersion(final CompleteVersion version) {
        this(version.getId(), version.getReleaseTime(), version.getUpdatedTime(), version.getType(), version.getMainClass(), version.getMinecraftArguments());
        this.minimumLauncherVersion = version.minimumLauncherVersion;
        this.incompatibilityReason = version.incompatibilityReason;
        for (final Library library : version.getLibraries()) {
            this.libraries.add(new Library(library));
        }
    }
    
    public CompleteVersion(final Version version, final String mainClass, final String minecraftArguments) {
        this(version.getId(), version.getReleaseTime(), version.getUpdatedTime(), version.getType(), mainClass, minecraftArguments);
    }
    
    @Override
    public String getId() {
        return this.id;
    }
    
    @Override
    public ReleaseType getType() {
        return this.type;
    }
    
    @Override
    public Date getUpdatedTime() {
        return this.time;
    }
    
    @Override
    public Date getReleaseTime() {
        return this.releaseTime;
    }
    
    public List<Library> getLibraries() {
        return this.libraries;
    }
    
    public String getMainClass() {
        return this.mainClass;
    }
    
    @Override
    public void setUpdatedTime(final Date time) {
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
        this.time = time;
    }
    
    @Override
    public void setReleaseTime(final Date time) {
        if (time == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
        this.releaseTime = time;
    }
    
    @Override
    public void setType(final ReleaseType type) {
        if (type == null) {
            throw new IllegalArgumentException("Release type cannot be null");
        }
        this.type = type;
    }
    
    public void setMainClass(final String mainClass) {
        if (mainClass == null || mainClass.length() == 0) {
            throw new IllegalArgumentException("Main class cannot be null or empty");
        }
        this.mainClass = mainClass;
    }
    
    public Collection<Library> getRelevantLibraries() {
        final List<Library> result = new ArrayList<Library>();
        for (final Library library : this.libraries) {
            if (library.appliesToCurrentEnvironment()) {
                result.add(library);
            }
        }
        return result;
    }
    
    public Collection<File> getClassPath(final OperatingSystem os, final File base) {
        final Collection<Library> libraries = this.getRelevantLibraries();
        final Collection<File> result = new ArrayList<File>();
        for (final Library library : libraries) {
            if (library.getNatives() == null) {
                result.add(new File(base, "libraries/" + library.getArtifactPath()));
            }
        }
        result.add(new File(base, "versions/" + this.getId() + "/" + this.getId() + ".jar"));
        return result;
    }
    
    public Collection<String> getExtractFiles(final OperatingSystem os) {
        final Collection<Library> libraries = this.getRelevantLibraries();
        final Collection<String> result = new ArrayList<String>();
        for (final Library library : libraries) {
            final Map<OperatingSystem, String> natives = library.getNatives();
            if (natives != null && natives.containsKey(os)) {
                result.add("libraries/" + library.getArtifactPath(natives.get(os)));
            }
        }
        return result;
    }
    
    public Set<String> getRequiredFiles(final OperatingSystem os) {
        final Set<String> neededFiles = new HashSet<String>();
        for (final Library library : this.getRelevantLibraries()) {
            if (library.getNatives() != null) {
                final String natives = library.getNatives().get(os);
                if (natives == null) {
                    continue;
                }
                neededFiles.add("libraries/" + library.getArtifactPath(natives));
            }
            else {
                neededFiles.add("libraries/" + library.getArtifactPath());
            }
        }
        return neededFiles;
    }
    
    public Set<Downloadable> getRequiredDownloadables(final OperatingSystem os, final Proxy proxy, final File targetDirectory, final boolean ignoreLocalFiles) throws MalformedURLException {
        final Set<Downloadable> neededFiles = new HashSet<Downloadable>();
        for (final Library library : this.getRelevantLibraries()) {
            String file = null;
            if (library.getNatives() != null) {
                final String natives = library.getNatives().get(os);
                if (natives != null) {
                    file = library.getArtifactPath(natives);
                }
            }
            else {
                file = library.getArtifactPath();
            }
            if (file != null) {
                final URL url = new URL(library.getDownloadUrl() + file);
                final File local = new File(targetDirectory, "libraries/" + file);
                if (local.isFile() && library.hasCustomUrl()) {
                    continue;
                }
                neededFiles.add(new ChecksummedDownloadable(proxy, url, local, ignoreLocalFiles));
            }
        }
        return neededFiles;
    }
    
    @Override
    public String toString() {
        return "CompleteVersion{id='" + this.id + '\'' + ", time=" + this.time + ", type=" + this.type + ", libraries=" + this.libraries + ", mainClass='" + this.mainClass + '\'' + ", minimumLauncherVersion=" + this.minimumLauncherVersion + '}';
    }
    
    public String getMinecraftArguments() {
        return this.minecraftArguments;
    }
    
    public void setMinecraftArguments(final String minecraftArguments) {
        if (minecraftArguments == null) {
            throw new IllegalArgumentException("Process arguments cannot be null or empty");
        }
        this.minecraftArguments = minecraftArguments;
    }
    
    public int getMinimumLauncherVersion() {
        return this.minimumLauncherVersion;
    }
    
    public void setMinimumLauncherVersion(final int minimumLauncherVersion) {
        this.minimumLauncherVersion = minimumLauncherVersion;
    }
    
    public boolean appliesToCurrentEnvironment() {
        if (this.rules == null) {
            return true;
        }
        Rule.Action lastAction = Rule.Action.DISALLOW;
        for (final Rule rule : this.rules) {
            final Rule.Action action = rule.getAppliedAction();
            if (action != null) {
                lastAction = action;
            }
        }
        return lastAction == Rule.Action.ALLOW;
    }
    
    public void setIncompatibilityReason(final String incompatibilityReason) {
        this.incompatibilityReason = incompatibilityReason;
    }
    
    public String getIncompatibilityReason() {
        return this.incompatibilityReason;
    }
    
    public boolean isSynced() {
        return this.synced;
    }
    
    public void setSynced(final boolean synced) {
        this.synced = synced;
    }
    
    public String getAssets() {
        return this.assets;
    }
    
    public void setAssets(final String assets) {
        this.assets = assets;
    }
}
