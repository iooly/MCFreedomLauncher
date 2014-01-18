package net.minecraft.launcher.updater.download.assets;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import net.minecraft.launcher.updater.download.MonitoringInputStream;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.net.URL;
import java.net.Proxy;
import net.minecraft.launcher.updater.download.Downloadable;

public class AssetDownloadable extends Downloadable
{
    private final String expectedHash;
    private final long expectedFilesize;
    
    public AssetDownloadable(final Proxy proxy, final URL remoteFile, final File localFile, final boolean forceDownload, final String expectedHash, final long expectedFilesize) {
        super(proxy, remoteFile, localFile, forceDownload);
        this.expectedHash = expectedHash;
        this.expectedFilesize = expectedFilesize;
    }
    
    @Override
    public String download() throws IOException {
        ++this.numAttempts;
        this.ensureFileWritable();
        if (this.getTarget().isFile() && FileUtils.sizeOf(this.getTarget()) == this.expectedFilesize) {
            return "Have local file and it's the same size; assuming it's okay!";
        }
        try {
            final HttpURLConnection connection = this.makeConnection(this.getUrl());
            final int status = connection.getResponseCode();
            if (status / 100 == 2) {
                this.updateExpectedSize(connection);
                final InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), this.getMonitor());
                final FileOutputStream outputStream = new FileOutputStream(this.getTarget());
                final String hash = Downloadable.copyAndDigest(inputStream, outputStream, "SHA", 40);
                if (hash.equalsIgnoreCase(this.expectedHash)) {
                    return "Downloaded successfully and hash matched";
                }
                throw new RuntimeException(String.format("Hash did not match downloaded file (Expected %s, downloaded %s)", this.expectedHash, hash));
            }
            else {
                if (this.getTarget().isFile()) {
                    return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good";
                }
                throw new RuntimeException("Server responded with " + status);
            }
        }
        catch (IOException e) {
            if (this.getTarget().isFile()) {
                return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage() + "') but have local file, assuming it's good";
            }
            throw e;
        }
    }
}
