package com.mojang.launcher.updater.download;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

/**
 * Created by YeFeNg on 2016/2/11.
 */
public class NotExistsDownloadable extends Downloadable {

    private static final Logger LOGGER = LogManager.getLogger();

    public NotExistsDownloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload) {
        super(proxy, remoteFile, localFile, forceDownload);
    }

    @Override
    public String download() throws IOException {
        this.numAttempts += 1;
        ensureFileWritable(getTarget());
        File target = getTarget();
        if (target.exists() && target.isFile()) {
            return "target exists local, use local";
        } else {
            HttpURLConnection connection = makeConnection(getUrl());
            int status = connection.getResponseCode();
            if (status / 100 == 2) {
                updateExpectedSize(connection);
                InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), getMonitor());
                FileOutputStream outputStream = new FileOutputStream(getTarget());
                copyAndDigest(inputStream, outputStream, "MD5", 32);
                return "Downloaded successfully";
            }
            if (getTarget().isFile()) {
                return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good";
            }
            throw new RuntimeException("Server responded with " + status);
        }
    }
}
