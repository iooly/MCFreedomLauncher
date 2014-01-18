package net.minecraft.launcher.updater.download;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.net.URL;
import java.net.Proxy;

public class ChecksummedDownloadable extends Downloadable
{
    private String checksum;
    
    public ChecksummedDownloadable(final Proxy proxy, final URL remoteFile, final File localFile, final boolean forceDownload) {
        super(proxy, remoteFile, localFile, forceDownload);
    }
    
    @Override
    public String download() throws IOException {
        ++this.numAttempts;
        this.ensureFileWritable();
        final File target = this.getTarget();
        final File checksumFile = new File(target.getAbsolutePath() + ".sha");
        String localHash = null;
        if (target.isFile()) {
            localHash = Downloadable.getDigest(target, "SHA-1", 40);
        }
        if (target.isFile() && checksumFile.isFile()) {
            this.checksum = this.readFile(checksumFile, "");
            if (this.checksum.length() == 0 || this.checksum.trim().equalsIgnoreCase(localHash)) {
                return "Local file matches local checksum, using that";
            }
            this.checksum = null;
            FileUtils.deleteQuietly(checksumFile);
        }
        if (this.checksum == null) {
            try {
                final HttpURLConnection connection = this.makeConnection(new URL(this.getUrl().toString() + ".sha1"));
                final int status = connection.getResponseCode();
                if (status / 100 == 2) {
                    final InputStream inputStream = connection.getInputStream();
                    try {
                        FileUtils.writeStringToFile(checksumFile, this.checksum = IOUtils.toString(inputStream, Charsets.UTF_8));
                    }
                    catch (IOException e2) {
                        this.checksum = "";
                    }
                    finally {
                        IOUtils.closeQuietly(inputStream);
                    }
                }
                else if (checksumFile.isFile()) {
                    this.checksum = this.readFile(checksumFile, "");
                }
                else {
                    this.checksum = "";
                }
            }
            catch (IOException e) {
                if (!target.isFile()) {
                    throw e;
                }
                this.checksum = this.readFile(checksumFile, "");
            }
        }
        try {
            final HttpURLConnection connection = this.makeConnection(this.getUrl());
            final int status = connection.getResponseCode();
            if (status / 100 == 2) {
                this.updateExpectedSize(connection);
                final InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), this.getMonitor());
                final FileOutputStream outputStream = new FileOutputStream(this.getTarget());
                final String digest = Downloadable.copyAndDigest(inputStream, outputStream, "SHA", 40);
                if (this.checksum == null || this.checksum.length() == 0) {
                    return "Didn't have checksum so assuming our copy is good";
                }
                if (this.checksum.trim().equalsIgnoreCase(digest)) {
                    return "Downloaded successfully and checksum matched";
                }
                throw new RuntimeException(String.format("Checksum did not match downloaded file (Checksum was %s, downloaded %s)", this.checksum, digest));
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
    
    private String readFile(final File file, final String def) {
        try {
            return FileUtils.readFileToString(file);
        }
        catch (Throwable ignored) {
            return def;
        }
    }
}
