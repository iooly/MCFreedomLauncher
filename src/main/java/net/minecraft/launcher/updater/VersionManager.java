package net.minecraft.launcher.updater;

import com.google.gson.Gson;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.events.RefreshedVersionsListener;
import net.minecraft.launcher.updater.download.DownloadJob;
import net.minecraft.launcher.updater.download.Downloadable;
import net.minecraft.launcher.updater.download.EtagDownloadable;
import net.minecraft.launcher.updater.download.assets.AssetDownloadable;
import net.minecraft.launcher.updater.download.assets.AssetIndex;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.ReleaseType;
import net.minecraft.launcher.versions.Version;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class VersionManager
{
    private static final Logger LOGGER;
    private final VersionList localVersionList;
    private final VersionList remoteVersionList;
    private final ThreadPoolExecutor executorService;
    private final List<RefreshedVersionsListener> refreshedVersionsListeners;
    private final Object refreshLock;
    private boolean isRefreshing;
    private final Gson gson;
    
    public VersionManager(final VersionList localVersionList, final VersionList remoteVersionList) {
        super();
        this.executorService = new ExceptionalThreadPoolExecutor(4, 8, 30L, TimeUnit.SECONDS);
        this.refreshedVersionsListeners = Collections.synchronizedList(new ArrayList<RefreshedVersionsListener>());
        this.refreshLock = new Object();
        this.gson = new Gson();
        this.localVersionList = localVersionList;
        this.remoteVersionList = remoteVersionList;
    }
    
    public void refreshVersions() throws IOException {
        synchronized (this.refreshLock) {
            this.isRefreshing = true;
        }
        try {
            VersionManager.LOGGER.info("Refreshing local version list...");
            this.localVersionList.refreshVersions();
            VersionManager.LOGGER.info("Refreshing remote version list...");
            this.remoteVersionList.refreshVersions();
        }
        catch (IOException ex) {
            synchronized (this.refreshLock) {
                this.isRefreshing = false;
            }
            throw ex;
        }
        VersionManager.LOGGER.info("Refresh complete.");
        synchronized (this.refreshLock) {
            this.isRefreshing = false;
        }
        final List<RefreshedVersionsListener> listeners = new ArrayList<RefreshedVersionsListener>(this.refreshedVersionsListeners);
        final Iterator<RefreshedVersionsListener> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            final RefreshedVersionsListener listener = (RefreshedVersionsListener)iterator.next();
            if (!listener.shouldReceiveEventsInUIThread()) {
                listener.onVersionsRefreshed(this);
                iterator.remove();
            }
        }
        if (!listeners.isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    for (final RefreshedVersionsListener listener : listeners) {
                        listener.onVersionsRefreshed(VersionManager.this);
                    }
                }
            });
        }
    }
    
    public List<VersionSyncInfo> getVersions() {
        return this.getVersions(null);
    }
    
    public List<VersionSyncInfo> getVersions(final VersionFilter filter) {
        synchronized (this.refreshLock) {
            if (this.isRefreshing) {
                return new ArrayList<VersionSyncInfo>();
            }
        }
        final List<VersionSyncInfo> result = new ArrayList<VersionSyncInfo>();
        final Map<String, VersionSyncInfo> lookup = new HashMap<String, VersionSyncInfo>();
        final Map<ReleaseType, Integer> counts = new EnumMap<ReleaseType, Integer>(ReleaseType.class);
        for (final ReleaseType type : ReleaseType.values()) {
            counts.put(type, 0);
        }
        for (final Version version : this.localVersionList.getVersions()) {
            if (version.getType() != null) {
                if (version.getUpdatedTime() == null) {
                    continue;
                }
                if (filter != null) {
                    if (!filter.getTypes().contains(version.getType())) {
                        continue;
                    }
                    if (counts.get(version.getType()) >= filter.getMaxCount()) {
                        continue;
                    }
                }
                final VersionSyncInfo syncInfo = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                lookup.put(version.getId(), syncInfo);
                result.add(syncInfo);
            }
        }
        for (final Version version : this.remoteVersionList.getVersions()) {
            if (version.getType() != null) {
                if (version.getUpdatedTime() == null) {
                    continue;
                }
                if (lookup.containsKey(version.getId())) {
                    continue;
                }
                if (filter != null) {
                    if (!filter.getTypes().contains(version.getType())) {
                        continue;
                    }
                    if (counts.get(version.getType()) >= filter.getMaxCount()) {
                        continue;
                    }
                }
                final VersionSyncInfo syncInfo = this.getVersionSyncInfo(this.localVersionList.getVersion(version.getId()), version);
                lookup.put(version.getId(), syncInfo);
                result.add(syncInfo);
                if (filter == null) {
                    continue;
                }
                counts.put(version.getType(), counts.get(version.getType()) + 1);
            }
        }
        if (result.isEmpty()) {
            for (final Version version : this.localVersionList.getVersions()) {
                if (version.getType() != null) {
                    if (version.getUpdatedTime() == null) {
                        continue;
                    }
                    final VersionSyncInfo syncInfo = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                    lookup.put(version.getId(), syncInfo);
                    result.add(syncInfo);
                    break;
                }
            }
        }
        Collections.<VersionSyncInfo>sort(result, new Comparator<VersionSyncInfo>() {
            @Override
            public int compare(final VersionSyncInfo a, final VersionSyncInfo b) {
                final Version aVer = a.getLatestVersion();
                final Version bVer = b.getLatestVersion();
                if (aVer.getReleaseTime() != null && bVer.getReleaseTime() != null) {
                    return bVer.getReleaseTime().compareTo(aVer.getReleaseTime());
                }
                return bVer.getUpdatedTime().compareTo(aVer.getUpdatedTime());
            }
        });
        return result;
    }
    
    public VersionSyncInfo getVersionSyncInfo(final Version version) {
        return this.getVersionSyncInfo(version.getId());
    }
    
    public VersionSyncInfo getVersionSyncInfo(final String name) {
        return this.getVersionSyncInfo(this.localVersionList.getVersion(name), this.remoteVersionList.getVersion(name));
    }
    
    public VersionSyncInfo getVersionSyncInfo(final Version localVersion, final Version remoteVersion) {
        boolean upToDate;
        final boolean installed = upToDate = (localVersion != null);
        if (installed && remoteVersion != null) {
            upToDate = !remoteVersion.getUpdatedTime().after(localVersion.getUpdatedTime());
        }
        if (localVersion instanceof CompleteVersion) {
            upToDate &= this.localVersionList.hasAllFiles((CompleteVersion)localVersion, OperatingSystem.getCurrentPlatform());
        }
        return new VersionSyncInfo(localVersion, remoteVersion, installed, upToDate);
    }
    
    public List<VersionSyncInfo> getInstalledVersions() {
        final List<VersionSyncInfo> result = new ArrayList<VersionSyncInfo>();
        for (final Version version : this.localVersionList.getVersions()) {
            if (version.getType() != null) {
                if (version.getUpdatedTime() == null) {
                    continue;
                }
                final VersionSyncInfo syncInfo = this.getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
                result.add(syncInfo);
            }
        }
        return result;
    }
    
    public VersionList getRemoteVersionList() {
        return this.remoteVersionList;
    }
    
    public VersionList getLocalVersionList() {
        return this.localVersionList;
    }
    
    public CompleteVersion getLatestCompleteVersion(final VersionSyncInfo syncInfo) throws IOException {
        if (syncInfo.getLatestSource() != VersionSyncInfo.VersionSource.REMOTE) {
            return this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
        }
        CompleteVersion result = null;
        IOException exception = null;
        try {
            result = this.remoteVersionList.getCompleteVersion(syncInfo.getLatestVersion());
        }
        catch (IOException e) {
            exception = e;
            try {
                result = this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
            }
            catch (IOException ex) {}
        }
        if (result != null) {
            return result;
        }
        throw exception;
    }
    
    public DownloadJob downloadVersion(final VersionSyncInfo syncInfo, final DownloadJob job) throws IOException {
        if (!(this.localVersionList instanceof LocalVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn't a LocalVersionList");
        }
        if (!(this.remoteVersionList instanceof RemoteVersionList)) {
            throw new IllegalArgumentException("Cannot download if local repo isn't a RemoteVersionList");
        }
        final CompleteVersion version = this.getLatestCompleteVersion(syncInfo);
        final File baseDirectory = ((LocalVersionList)this.localVersionList).getBaseDirectory();
        final Proxy proxy = ((RemoteVersionList)this.remoteVersionList).getProxy();
        job.addDownloadables(version.getRequiredDownloadables(OperatingSystem.getCurrentPlatform(), proxy, baseDirectory, false));
        final String jarFile = "versions/" + version.getId() + "/" + version.getId() + ".jar";
        job.addDownloadables(new EtagDownloadable(proxy, new URL(LauncherConstants.URL_DOWNLOAD_BASE + jarFile), new File(baseDirectory, jarFile), false));
        return job;
    }
    
    public DownloadJob downloadResources(final DownloadJob job, final CompleteVersion version) throws IOException {
        final File baseDirectory = ((LocalVersionList)this.localVersionList).getBaseDirectory();
        job.addDownloadables(this.getResourceFiles(((RemoteVersionList)this.remoteVersionList).getProxy(), baseDirectory, version));
        return job;
    }
    
    private Set<Downloadable> getResourceFiles(final Proxy proxy, final File baseDirectory, final CompleteVersion version) {
        final Set<Downloadable> result = new HashSet<Downloadable>();
        InputStream inputStream = null;
        final File assets = new File(baseDirectory, "assets");
        final File objectsFolder = new File(assets, "objects");
        final File indexesFolder = new File(assets, "indexes");
        String indexName = version.getAssets();
        final long start = System.nanoTime();
        if (indexName == null) {
            indexName = "legacy";
        }
        final File indexFile = new File(indexesFolder, indexName + ".json");
        try {
            final URL indexUrl = new URL("https://s3.amazonaws.com/Minecraft.Download/indexes/" + indexName + ".json");
            inputStream = indexUrl.openConnection(proxy).getInputStream();
            final String json = IOUtils.toString(inputStream);
            FileUtils.writeStringToFile(indexFile, json);
            final AssetIndex index = (AssetIndex)this.gson.<AssetIndex>fromJson(json, AssetIndex.class);
            for (final AssetIndex.AssetObject object : index.getUniqueObjects()) {
                final String filename = object.getHash().substring(0, 2) + "/" + object.getHash();
                final File file = new File(objectsFolder, filename);
                if (!file.isFile() || FileUtils.sizeOf(file) != object.getSize()) {
                    final Downloadable downloadable = new AssetDownloadable(proxy, new URL( LauncherConstants.URL_RESOURCE_BASE+ filename), file, false, object.getHash(), object.getSize());
                    downloadable.setExpectedSize(object.getSize());
                    result.add(downloadable);
                }
            }
            final long end = System.nanoTime();
            final long delta = end - start;
            VersionManager.LOGGER.debug("Delta time to compare resources: " + delta / 1000000L + " ms ");
        }
        catch (Exception ex) {
            VersionManager.LOGGER.error("Couldn't download resources", ex);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
        return result;
    }
    
    public ThreadPoolExecutor getExecutorService() {
        return this.executorService;
    }
    
    public void addRefreshedVersionsListener(final RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.add(listener);
    }
    
    public void removeRefreshedVersionsListener(final RefreshedVersionsListener listener) {
        this.refreshedVersionsListeners.remove(listener);
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
}
