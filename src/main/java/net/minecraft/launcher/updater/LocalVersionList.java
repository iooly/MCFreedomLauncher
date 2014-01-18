package net.minecraft.launcher.updater;

import org.apache.logging.log4j.LogManager;
import java.util.Set;
import net.minecraft.launcher.OperatingSystem;
import java.io.PrintWriter;
import java.io.IOException;
import net.minecraft.launcher.versions.ReleaseType;
import java.util.Iterator;
import net.minecraft.launcher.versions.Version;
import com.google.gson.JsonSyntaxException;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.versions.CompleteVersion;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import org.apache.logging.log4j.Logger;

public class LocalVersionList extends FileBasedVersionList
{
    private static final Logger LOGGER;
    private final File baseDirectory;
    private final File baseVersionsDir;
    
    public LocalVersionList(final File baseDirectory) {
        super();
        if (baseDirectory == null || !baseDirectory.isDirectory()) {
            throw new IllegalArgumentException("Base directory is not a folder!");
        }
        this.baseDirectory = baseDirectory;
        this.baseVersionsDir = new File(this.baseDirectory, "versions");
        if (!this.baseVersionsDir.isDirectory()) {
            this.baseVersionsDir.mkdirs();
        }
    }
    
    @Override
    protected InputStream getFileInputStream(final String path) throws FileNotFoundException {
        return new FileInputStream(new File(this.baseDirectory, path));
    }
    
    @Override
    public void refreshVersions() throws IOException {
        this.clearCache();
        final File[] files = this.baseVersionsDir.listFiles();
        if (files == null) {
            return;
        }
        for (final File directory : files) {
            final String id = directory.getName();
            final File jsonFile = new File(directory, id + ".json");
            if (directory.isDirectory() && jsonFile.exists()) {
                try {
                    final String path = "versions/" + id + "/" + id + ".json";
                    final CompleteVersion version = this.gson.fromJson(this.getContent(path), CompleteVersion.class);
                    if (version.getType() == null) {
                        LocalVersionList.LOGGER.warn("Ignoring: " + path + "; it has an invalid version specified");
                        return;
                    }
                    if (version.getId().equals(id)) {
                        this.addVersion(version);
                    }
                    else if (Launcher.getInstance() != null) {
                        LocalVersionList.LOGGER.warn("Ignoring: " + path + "; it contains id: '" + version.getId() + "' expected '" + id + "'");
                    }
                }
                catch (RuntimeException ex) {
                    if (Launcher.getInstance() == null) {
                        throw new JsonSyntaxException("Loading file: " + jsonFile.toString(), ex);
                    }
                    LocalVersionList.LOGGER.error("Couldn't load local version " + jsonFile.getAbsolutePath(), ex);
                }
            }
        }
        for (final Version version2 : this.getVersions()) {
            final ReleaseType type = version2.getType();
            if (this.getLatestVersion(type) == null || this.getLatestVersion(type).getUpdatedTime().before(version2.getUpdatedTime())) {
                this.setLatestVersion(version2);
            }
        }
    }
    
    public void saveVersionList() throws IOException {
        final String text = this.serializeVersionList();
        final PrintWriter writer = new PrintWriter(new File(this.baseVersionsDir, "versions.json"));
        writer.print(text);
        writer.close();
    }
    
    public void saveVersion(final CompleteVersion version) throws IOException {
        final String text = this.serializeVersion(version);
        final File target = new File(this.baseVersionsDir, version.getId() + "/" + version.getId() + ".json");
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }
        final PrintWriter writer = new PrintWriter(target);
        writer.print(text);
        writer.close();
    }
    
    public File getBaseDirectory() {
        return this.baseDirectory;
    }
    
    @Override
    public boolean hasAllFiles(final CompleteVersion version, final OperatingSystem os) {
        final Set<String> files = version.getRequiredFiles(os);
        for (final String file : files) {
            if (!new File(this.baseDirectory, file).isFile()) {
                return false;
            }
        }
        return true;
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
}
