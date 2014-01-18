package net.minecraft.launcher;

import com.google.gson.Gson;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import net.minecraft.launcher.process.JavaProcess;
import net.minecraft.launcher.process.JavaProcessLauncher;
import net.minecraft.launcher.process.JavaProcessRunnable;
import net.minecraft.launcher.profile.LauncherVisibilityRule;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.ui.tabs.CrashReportTab;
import net.minecraft.launcher.updater.DateTypeAdapter;
import net.minecraft.launcher.updater.LocalVersionList;
import net.minecraft.launcher.updater.VersionList;
import net.minecraft.launcher.updater.VersionSyncInfo;
import net.minecraft.launcher.updater.download.DownloadJob;
import net.minecraft.launcher.updater.download.DownloadListener;
import net.minecraft.launcher.updater.download.Downloadable;
import net.minecraft.launcher.updater.download.assets.AssetIndex;
import net.minecraft.launcher.versions.CompleteVersion;
import net.minecraft.launcher.versions.ExtractRules;
import net.minecraft.launcher.versions.Library;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GameLauncher implements JavaProcessRunnable, DownloadListener
{
    private static final Logger LOGGER;
    private final Object lock;
    private final Launcher launcher;
    private final List<DownloadJob> jobs;
    private final Gson gson;
    private final DateTypeAdapter dateAdapter;
    private CompleteVersion version;
    private LauncherVisibilityRule visibilityRule;
    private boolean isWorking;
    private File nativeDir;
    
    public GameLauncher(final Launcher launcher) {
        super();
        this.lock = new Object();
        this.jobs = new ArrayList<DownloadJob>();
        this.gson = new Gson();
        this.dateAdapter = new DateTypeAdapter();
        this.launcher = launcher;
    }
    
    private void setWorking(final boolean working) {
        synchronized (this.lock) {
            if (this.nativeDir != null) {
                GameLauncher.LOGGER.info("Deleting " + this.nativeDir);
                if (!this.nativeDir.isDirectory() || FileUtils.deleteQuietly(this.nativeDir)) {
                    this.nativeDir = null;
                }
                else {
                    GameLauncher.LOGGER.warn("Couldn't delete " + this.nativeDir + " - scheduling for deletion upon exit");
                    try {
                        FileUtils.forceDeleteOnExit(this.nativeDir);
                    }
                    catch (Throwable t) {}
                }
            }
            this.isWorking = working;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    GameLauncher.this.launcher.getLauncherPanel().getBottomBar().getPlayButtonPanel().checkState();
                }
            });
        }
    }
    
    public boolean isWorking() {
        return this.isWorking;
    }
    
    public void playGame() {
        synchronized (this.lock) {
            if (this.isWorking) {
                GameLauncher.LOGGER.warn("Tried to play game but game is already starting!");
                return;
            }
            this.setWorking(true);
        }
        GameLauncher.LOGGER.info("Getting syncinfo for selected version");
        final Profile profile = this.launcher.getProfileManager().getSelectedProfile();
        final String lastVersionId = profile.getLastVersionId();
        VersionSyncInfo syncInfo = null;
        if (profile.getLauncherVisibilityOnGameClose() == null) {
            this.visibilityRule = Profile.DEFAULT_LAUNCHER_VISIBILITY;
        }
        else {
            this.visibilityRule = profile.getLauncherVisibilityOnGameClose();
        }
        if (lastVersionId != null) {
            syncInfo = this.launcher.getVersionManager().getVersionSyncInfo(lastVersionId);
        }
        if (syncInfo == null || syncInfo.getLatestVersion() == null) {
            syncInfo = this.launcher.getVersionManager().getVersions(profile.getVersionFilter()).get(0);
        }
        if (syncInfo == null) {
            GameLauncher.LOGGER.warn("Tried to launch a version without a version being selected...");
            this.setWorking(false);
            return;
        }
        synchronized (this.lock) {
            GameLauncher.LOGGER.info("Queueing library & version downloads");
            try {
                this.version = this.launcher.getVersionManager().getLatestCompleteVersion(syncInfo);
            }
            catch (IOException e) {
                GameLauncher.LOGGER.error("Couldn't get complete version info for " + syncInfo.getLatestVersion(), e);
                this.setWorking(false);
                return;
            }
            if (syncInfo.getRemoteVersion() != null && syncInfo.getLatestSource() != VersionSyncInfo.VersionSource.REMOTE && !this.version.isSynced()) {
                try {
                    final CompleteVersion remoteVersion = this.launcher.getVersionManager().getRemoteVersionList().getCompleteVersion(syncInfo.getRemoteVersion());
                    this.launcher.getVersionManager().getLocalVersionList().removeVersion(this.version);
                    this.launcher.getVersionManager().getLocalVersionList().addVersion(remoteVersion);
                    ((LocalVersionList)this.launcher.getVersionManager().getLocalVersionList()).saveVersion(remoteVersion);
                    this.version = remoteVersion;
                }
                catch (IOException e) {
                    GameLauncher.LOGGER.error("Couldn't sync local and remote versions", e);
                }
                this.version.setSynced(true);
            }
            if (!this.version.appliesToCurrentEnvironment()) {
                String reason = this.version.getIncompatibilityReason();
                if (reason == null) {
                    reason = "This version is incompatible with your computer. Please try another one by going into Edit Profile and selecting one through the dropdown. Sorry!";
                }
                GameLauncher.LOGGER.error("Version " + this.version.getId() + " is incompatible with current environment: " + reason);
                JOptionPane.showMessageDialog(this.launcher.getFrame(), reason, "Cannot play game", 0);
                this.setWorking(false);
                return;
            }
            if (this.version.getMinimumLauncherVersion() > 13) {
                GameLauncher.LOGGER.error("An update to your launcher is available and is required to play " + this.version.getId() + ". Please restart your launcher.");
                this.setWorking(false);
                return;
            }
            if (!syncInfo.isInstalled()) {
                try {
                    final VersionList localVersionList = this.launcher.getVersionManager().getLocalVersionList();
                    if (localVersionList instanceof LocalVersionList) {
                        ((LocalVersionList)localVersionList).saveVersion(this.version);
                        GameLauncher.LOGGER.info("Installed " + syncInfo.getLatestVersion());
                    }
                }
                catch (IOException e) {
                    GameLauncher.LOGGER.error("Couldn't save version info to install " + syncInfo.getLatestVersion(), e);
                    this.setWorking(false);
                    return;
                }
            }
            try {
                final DownloadJob librariesJob = new DownloadJob("Version & Libraries", false, this);
                this.addJob(librariesJob);
                this.launcher.getVersionManager().downloadVersion(syncInfo, librariesJob);
                librariesJob.startDownloading(this.launcher.getDownloaderExecutorService());
                this.migrateOldAssets();
                final DownloadJob resourceJob = new DownloadJob("Resources", true, this);
                this.addJob(resourceJob);
                this.launcher.getVersionManager().downloadResources(resourceJob, this.version);
                resourceJob.startDownloading(this.launcher.getDownloaderExecutorService());
            }
            catch (IOException e) {
                GameLauncher.LOGGER.error("Couldn't get version info for " + syncInfo.getLatestVersion(), e);
                this.setWorking(false);
            }
        }
    }
    
    protected void launchGame() throws IOException {
        GameLauncher.LOGGER.info("Launching game");
        final Profile selectedProfile = this.launcher.getProfileManager().getSelectedProfile();
        if (this.version == null) {
            GameLauncher.LOGGER.error("Aborting launch; version is null?");
            return;
        }
        this.cleanOldNatives();
        this.nativeDir = new File(this.launcher.getWorkingDirectory(), "versions/" + this.version.getId() + "/" + this.version.getId() + "-natives-" + System.nanoTime());
        if (!this.nativeDir.isDirectory()) {
            this.nativeDir.mkdirs();
        }
        GameLauncher.LOGGER.info("Unpacking natives to " + this.nativeDir);
        try {
            this.unpackNatives(this.nativeDir);
        }
        catch (IOException e) {
            GameLauncher.LOGGER.error("Couldn't unpack natives!", e);
            return;
        }
        File assetsDir;
        try {
            assetsDir = this.reconstructAssets();
        }
        catch (IOException e2) {
            GameLauncher.LOGGER.error("Couldn't unpack natives!", e2);
            return;
        }
        final File gameDirectory = (selectedProfile.getGameDir() == null) ? this.launcher.getWorkingDirectory() : selectedProfile.getGameDir();
        GameLauncher.LOGGER.info("Launching in " + gameDirectory);
        if (!gameDirectory.exists()) {
            if (!gameDirectory.mkdirs()) {
                GameLauncher.LOGGER.error("Aborting launch; couldn't create game directory");
                return;
            }
        }
        else if (!gameDirectory.isDirectory()) {
            GameLauncher.LOGGER.error("Aborting launch; game directory is not actually a directory");
            return;
        }
        final JavaProcessLauncher processLauncher = new JavaProcessLauncher(selectedProfile.getJavaPath(), new String[0]);
        processLauncher.directory(gameDirectory);
        final OperatingSystem os = OperatingSystem.getCurrentPlatform();
        if (os.equals(OperatingSystem.OSX)) {
            processLauncher.addCommands("-Xdock:icon=" + this.getAssetObject("icons/minecraft.icns").getAbsolutePath(), "-Xdock:name=Minecraft");
        }
        else if (os.equals(OperatingSystem.WINDOWS)) {
            processLauncher.addCommands("-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump");
        }
        final String profileArgs = selectedProfile.getJavaArgs();
        if (profileArgs != null) {
            processLauncher.addSplitCommands(profileArgs);
        }
        else {
            final boolean is32Bit = "32".equals(System.getProperty("sun.arch.data.model"));
            final String defaultArgument = is32Bit ? "-Xmx512M" : "-Xmx1G";
            processLauncher.addSplitCommands(defaultArgument);
        }
        processLauncher.addCommands("-Djava.library.path=" + this.nativeDir.getAbsolutePath());
        processLauncher.addCommands("-cp", this.constructClassPath(this.version));
        processLauncher.addCommands(this.version.getMainClass());
        GameLauncher.LOGGER.info("Half command: " + StringUtils.join(processLauncher.getFullCommands(), " "));
        final UserAuthentication auth = this.launcher.getProfileManager().getAuthDatabase().getByUUID(selectedProfile.getPlayerUUID());
        final String[] args = this.getMinecraftArguments(this.version, selectedProfile, gameDirectory, assetsDir, auth);
        if (args == null) {
            return;
        }
        processLauncher.addCommands(args);
        final Proxy proxy = this.launcher.getProxy();
        final PasswordAuthentication proxyAuth = this.launcher.getProxyAuth();
        if (!proxy.equals(Proxy.NO_PROXY)) {
            final InetSocketAddress address = (InetSocketAddress)proxy.address();
            processLauncher.addCommands("--proxyHost", address.getHostName());
            processLauncher.addCommands("--proxyPort", Integer.toString(address.getPort()));
            if (proxyAuth != null) {
                processLauncher.addCommands("--proxyUser", proxyAuth.getUserName());
                processLauncher.addCommands("--proxyPass", new String(proxyAuth.getPassword()));
            }
        }
        processLauncher.addCommands(this.launcher.getAdditionalArgs());
        if (auth == null || auth.getSelectedProfile() == null) {
            processLauncher.addCommands("--demo");
        }
        if (selectedProfile.getResolution() != null) {
            processLauncher.addCommands("--width", String.valueOf(selectedProfile.getResolution().getWidth()));
            processLauncher.addCommands("--height", String.valueOf(selectedProfile.getResolution().getHeight()));
        }
        try {
            GameLauncher.LOGGER.debug("Running " + StringUtils.join(processLauncher.getFullCommands(), " "));
            final JavaProcess process = processLauncher.start();
            process.safeSetExitRunnable(this);
            if (this.visibilityRule != LauncherVisibilityRule.DO_NOTHING) {
                this.launcher.getFrame().setVisible(false);
            }
        }
        catch (IOException e3) {
            GameLauncher.LOGGER.error("Couldn't launch game", e3);
            this.setWorking(false);
        }
    }
    
    private File getAssetObject(final String name) throws IOException {
        final File assetsDir = new File(this.launcher.getWorkingDirectory(), "assets");
        final File indexDir = new File(assetsDir, "indexes");
        final File objectsDir = new File(assetsDir, "objects");
        final String assetVersion = (this.version.getAssets() == null) ? "legacy" : this.version.getAssets();
        final File indexFile = new File(indexDir, assetVersion + ".json");
        final AssetIndex index = this.gson.fromJson(FileUtils.readFileToString(indexFile, Charsets.UTF_8), AssetIndex.class);
        final String hash = index.getFileMap().get(name).getHash();
        return new File(objectsDir, hash.substring(0, 2) + "/" + hash);
    }
    
    private File reconstructAssets() throws IOException {
        final File assetsDir = new File(this.launcher.getWorkingDirectory(), "assets");
        final File indexDir = new File(assetsDir, "indexes");
        final File objectDir = new File(assetsDir, "objects");
        final String assetVersion = (this.version.getAssets() == null) ? "legacy" : this.version.getAssets();
        final File indexFile = new File(indexDir, assetVersion + ".json");
        final File virtualRoot = new File(new File(assetsDir, "virtual"), assetVersion);
        if (!indexFile.isFile()) {
            GameLauncher.LOGGER.warn("No assets index file " + virtualRoot + "; can't reconstruct assets");
            return virtualRoot;
        }
        final AssetIndex index = this.gson.fromJson(FileUtils.readFileToString(indexFile, Charsets.UTF_8), AssetIndex.class);
        if (index.isVirtual()) {
            GameLauncher.LOGGER.info("Reconstructing virtual assets folder at " + virtualRoot);
            for (final Map.Entry<String, AssetIndex.AssetObject> entry : index.getFileMap().entrySet()) {
                final File target = new File(virtualRoot, entry.getKey());
                final File original = new File(new File(objectDir, entry.getValue().getHash().substring(0, 2)), entry.getValue().getHash());
                if (!target.isFile()) {
                    FileUtils.copyFile(original, target, false);
                }
            }
            FileUtils.writeStringToFile(new File(virtualRoot, ".lastused"), this.dateAdapter.serializeToString(new Date()));
        }
        return virtualRoot;
    }
    
    private String[] getMinecraftArguments(final CompleteVersion version, final Profile selectedProfile, final File gameDirectory, final File assetsDirectory, final UserAuthentication authentication) {
        if (version.getMinecraftArguments() == null) {
            GameLauncher.LOGGER.error("Can't run version, missing minecraftArguments");
            this.setWorking(false);
            return null;
        }
        final Map<String, String> map = new HashMap<String, String>();
        final StrSubstitutor substitutor = new StrSubstitutor((Map<String, String>)map);
        final String[] split = version.getMinecraftArguments().split(" ");
        map.put("auth_access_token", authentication.getAuthenticatedToken());
        map.put("user_properties", new Gson().toJson(authentication.getUserProperties()));
        if (authentication.isLoggedIn() && authentication.canPlayOnline()) {
            if (authentication instanceof YggdrasilUserAuthentication) {
                map.put("auth_session", String.format("token:%s:%s", authentication.getAuthenticatedToken(), authentication.getSelectedProfile().getId()));
            }
            else {
                map.put("auth_session", authentication.getAuthenticatedToken());
            }
        }
        else {
            map.put("auth_session", "-");
        }
        if (authentication.getSelectedProfile() != null) {
            map.put("auth_player_name", authentication.getSelectedProfile().getName());
            map.put("auth_uuid", authentication.getSelectedProfile().getId());
            map.put("user_type", authentication.getUserType().getName());
        }
        else {
            map.put("auth_player_name", "Player");
            map.put("auth_uuid", new UUID(0L, 0L).toString());
            map.put("user_type", UserType.LEGACY.getName());
        }
        map.put("profile_name", selectedProfile.getName());
        map.put("version_name", version.getId());
        map.put("game_directory", gameDirectory.getAbsolutePath());
        map.put("game_assets", assetsDirectory.getAbsolutePath());
        map.put("assets_root", new File(this.launcher.getWorkingDirectory(), "assets").getAbsolutePath());
        map.put("assets_index_name", (version.getAssets() == null) ? "legacy" : version.getAssets());
        for (int i = 0; i < split.length; ++i) {
            split[i] = substitutor.replace(split[i]);
        }
        return split;
    }
    
    private void cleanOldNatives() {
        final File root = new File(this.launcher.getWorkingDirectory(), "versions/");
        GameLauncher.LOGGER.info("Looking for old natives & assets to clean up...");
        final IOFileFilter ageFilter = new AgeFileFilter(System.currentTimeMillis() - 3600L);
        for (final File version : root.listFiles((FileFilter)DirectoryFileFilter.DIRECTORY)) {
            for (final File folder : version.listFiles((FileFilter)FileFilterUtils.and(new PrefixFileFilter(version.getName() + "-natives-"), ageFilter))) {
                GameLauncher.LOGGER.debug("Deleting " + folder);
                FileUtils.deleteQuietly(folder);
            }
        }
    }
    
    private void migrateOldAssets() {
        final File sourceDir = new File(this.launcher.getWorkingDirectory(), "assets");
        final File objectsDir = new File(sourceDir, "objects");
        if (!sourceDir.isDirectory()) {
            return;
        }
        final IOFileFilter migratableFilter = FileFilterUtils.notFileFilter(FileFilterUtils.or(FileFilterUtils.nameFileFilter("indexes"), FileFilterUtils.nameFileFilter("objects"), FileFilterUtils.nameFileFilter("virtual")));
        for (final File file : new TreeSet<File>(FileUtils.listFiles(sourceDir, TrueFileFilter.TRUE, migratableFilter))) {
            final String hash = Downloadable.getDigest(file, "SHA-1", 40);
            final File destinationFile = new File(objectsDir, hash.substring(0, 2) + "/" + hash);
            if (!destinationFile.exists()) {
                GameLauncher.LOGGER.info("Migrated old asset {} into {}", file, destinationFile);
                try {
                    FileUtils.copyFile(file, destinationFile);
                }
                catch (IOException e) {
                    GameLauncher.LOGGER.error("Couldn't migrate old asset", e);
                }
            }
            FileUtils.deleteQuietly(file);
        }
        final File[] assets = sourceDir.listFiles();
        if (assets != null) {
            for (final File file2 : assets) {
                if (!file2.getName().equals("indexes") && !file2.getName().equals("objects") && !file2.getName().equals("virtual")) {
                    GameLauncher.LOGGER.info("Cleaning up old assets directory {} after migration", file2);
                    FileUtils.deleteQuietly(file2);
                }
            }
        }
    }
    
    private void unpackNatives(final File targetDir) throws IOException {
        final OperatingSystem os = OperatingSystem.getCurrentPlatform();
        final Collection<Library> libraries = this.version.getRelevantLibraries();
        for (final Library library : libraries) {
            final Map<OperatingSystem, String> nativesPerOs = library.getNatives();
            if (nativesPerOs != null && nativesPerOs.get(os) != null) {
                final File file = new File(this.launcher.getWorkingDirectory(), "libraries/" + library.getArtifactPath(nativesPerOs.get(os)));
                final ZipFile zip = new ZipFile(file);
                final ExtractRules extractRules = library.getExtractRules();
                try {
                    final Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        final ZipEntry entry = (ZipEntry)entries.nextElement();
                        if (extractRules != null && !extractRules.shouldExtract(entry.getName())) {
                            continue;
                        }
                        final File targetFile = new File(targetDir, entry.getName());
                        if (targetFile.getParentFile() != null) {
                            targetFile.getParentFile().mkdirs();
                        }
                        if (entry.isDirectory()) {
                            continue;
                        }
                        final BufferedInputStream inputStream = new BufferedInputStream(zip.getInputStream(entry));
                        final byte[] buffer = new byte[2048];
                        final FileOutputStream outputStream = new FileOutputStream(targetFile);
                        final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                        try {
                            int length;
                            while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                                bufferedOutputStream.write(buffer, 0, length);
                            }
                        }
                        finally {
                            Downloadable.closeSilently(bufferedOutputStream);
                            Downloadable.closeSilently(outputStream);
                            Downloadable.closeSilently(inputStream);
                        }
                    }
                }
                finally {
                    zip.close();
                }
            }
        }
    }
    
    private String constructClassPath(final CompleteVersion version) {
        final StringBuilder result = new StringBuilder();
        final Collection<File> classPath = version.getClassPath(OperatingSystem.getCurrentPlatform(), this.launcher.getWorkingDirectory());
        final String separator = System.getProperty("path.separator");
        for (final File file : classPath) {
            if (!file.isFile()) {
                throw new RuntimeException("Classpath file not found: " + file);
            }
            if (result.length() > 0) {
                result.append(separator);
            }
            result.append(file.getAbsolutePath());
        }
        return result.toString();
    }
    
    @Override
    public void onJavaProcessEnded(final JavaProcess process) {
        final int exitCode = process.getExitCode();
        if (exitCode == 0) {
            GameLauncher.LOGGER.info("Game ended with no troubles detected (exit code " + exitCode + ")");
            if (this.visibilityRule == LauncherVisibilityRule.CLOSE_LAUNCHER) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GameLauncher.LOGGER.info("Following visibility rule and exiting launcher as the game has ended");
                        GameLauncher.this.launcher.closeLauncher();
                    }
                });
            }
            else if (this.visibilityRule == LauncherVisibilityRule.HIDE_LAUNCHER) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        GameLauncher.LOGGER.info("Following visibility rule and showing launcher as the game has ended");
                        GameLauncher.this.launcher.getFrame().setVisible(true);
                    }
                });
            }
        }
        else {
            GameLauncher.LOGGER.error("Game ended with bad state (exit code " + exitCode + ")");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    GameLauncher.LOGGER.info("Ignoring visibility rule and showing launcher due to a game crash");
                    GameLauncher.this.launcher.getFrame().setVisible(true);
                }
            });
            String errorText = null;
            final String[] sysOut = process.getSysOutLines().getItems();
            for (int i = sysOut.length - 1; i >= 0; --i) {
                final String line = sysOut[i];
                final String crashIdentifier = "#@!@#";
                final int pos = line.lastIndexOf(crashIdentifier);
                if (pos >= 0 && pos < line.length() - crashIdentifier.length() - 1) {
                    errorText = line.substring(pos + crashIdentifier.length()).trim();
                    break;
                }
            }
            if (errorText != null) {
                final File file = new File(errorText);
                if (file.isFile()) {
                    GameLauncher.LOGGER.info("Crash report detected, opening: " + errorText);
                    InputStream inputStream = null;
                    try {
                        inputStream = new FileInputStream(file);
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        final StringBuilder result = new StringBuilder();
                        String line2;
                        while ((line2 = reader.readLine()) != null) {
                            if (result.length() > 0) {
                                result.append("\n");
                            }
                            result.append(line2);
                        }
                        reader.close();
                        this.launcher.getLauncherPanel().getTabPanel().setCrashReport(new CrashReportTab(this.launcher, this.version, file, result.toString()));
                    }
                    catch (IOException e) {
                        GameLauncher.LOGGER.error("Couldn't open crash report", e);
                    }
                    finally {
                        Downloadable.closeSilently(inputStream);
                    }
                }
                else {
                    GameLauncher.LOGGER.error("Crash report detected, but unknown format: " + errorText);
                }
            }
        }
        this.setWorking(false);
    }
    
    @Override
    public void onDownloadJobFinished(final DownloadJob job) {
        this.updateProgressBar();
        synchronized (this.lock) {
            if (job.getFailures() > 0) {
                GameLauncher.LOGGER.error("Job '" + job.getName() + "' finished with " + job.getFailures() + " failure(s)! (took " + job.getStopWatch().toString() + ")");
                this.setWorking(false);
            }
            else {
                GameLauncher.LOGGER.info("Job '" + job.getName() + "' finished successfully (took " + job.getStopWatch().toString() + ")");
                if (this.isWorking() && !this.hasRemainingJobs()) {
                    try {
                        this.launchGame();
                    }
                    catch (Throwable ex) {
                        GameLauncher.LOGGER.fatal("Fatal error launching game. Report this to http://mojang.atlassian.net please!", ex);
                    }
                }
            }
        }
    }
    
    @Override
    public void onDownloadJobProgressChanged(final DownloadJob job) {
        this.updateProgressBar();
    }
    
    protected void updateProgressBar() {
        final float progress = this.getProgress();
        final boolean hasTasks = this.hasRemainingJobs();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GameLauncher.this.launcher.getLauncherPanel().getProgressBar().setVisible(hasTasks);
                GameLauncher.this.launcher.getLauncherPanel().getProgressBar().setValue((int)(progress * 100.0f));
            }
        });
    }
    
    protected float getProgress() {
        synchronized (this.lock) {
            float max = 0.0f;
            float result = 0.0f;
            for (final DownloadJob job : this.jobs) {
                final float progress = job.getProgress();
                if (progress >= 0.0f) {
                    result += progress;
                    ++max;
                }
            }
            return result / max;
        }
    }
    
    public boolean hasRemainingJobs() {
        synchronized (this.lock) {
            for (final DownloadJob job : this.jobs) {
                if (!job.isComplete()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public void addJob(final DownloadJob job) {
        synchronized (this.lock) {
            this.jobs.add(job);
        }
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
}
