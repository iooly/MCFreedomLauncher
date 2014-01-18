package net.minecraft.launcher.updater.download;

import org.apache.logging.log4j.LogManager;
import java.security.NoSuchAlgorithmException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.math.BigInteger;
import java.io.Closeable;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.io.FileInputStream;
import java.net.Proxy;
import java.io.File;
import java.net.URL;
import org.apache.logging.log4j.Logger;

public abstract class Downloadable
{
    private static final Logger LOGGER;
    private final URL url;
    private final File target;
    private final boolean forceDownload;
    private final Proxy proxy;
    private final ProgressContainer monitor;
    protected int numAttempts;
    private long expectedSize;
    
    public Downloadable(final Proxy proxy, final URL remoteFile, final File localFile, final boolean forceDownload) {
        super();
        this.proxy = proxy;
        this.url = remoteFile;
        this.target = localFile;
        this.forceDownload = forceDownload;
        this.monitor = new ProgressContainer();
    }
    
    public ProgressContainer getMonitor() {
        return this.monitor;
    }
    
    public long getExpectedSize() {
        return this.expectedSize;
    }
    
    public void setExpectedSize(final long expectedSize) {
        this.expectedSize = expectedSize;
    }
    
    public static String getDigest(final File file, final String algorithm, final int hashLength) {
        DigestInputStream stream = null;
        try {
            stream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance(algorithm));
            final byte[] buffer = new byte[65536];
            int read;
            do {
                read = stream.read(buffer);
            } while (read > 0);
        }
        catch (Exception ignored) {
            return null;
        }
        finally {
            closeSilently(stream);
        }
        return String.format("%1$0" + hashLength + "x", new BigInteger(1, stream.getMessageDigest().digest()));
    }
    
    public abstract String download() throws IOException;
    
    protected void updateExpectedSize(final HttpURLConnection connection) {
        if (this.expectedSize == 0L) {
            this.monitor.setTotal(connection.getContentLength());
            this.setExpectedSize(connection.getContentLength());
        }
        else {
            this.monitor.setTotal(this.expectedSize);
        }
    }
    
    protected HttpURLConnection makeConnection(final URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection)url.openConnection(this.proxy);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
        connection.setRequestProperty("Cache-Control", "no-store,max-age=0,no-cache");
        connection.setRequestProperty("Expires", "0");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(30000);
        return connection;
    }
    
    public URL getUrl() {
        return this.url;
    }
    
    public File getTarget() {
        return this.target;
    }
    
    public boolean shouldIgnoreLocal() {
        return this.forceDownload;
    }
    
    public int getNumAttempts() {
        return this.numAttempts;
    }
    
    public Proxy getProxy() {
        return this.proxy;
    }
    
    public static void closeSilently(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException ex) {}
        }
    }
    
    public static String copyAndDigest(final InputStream inputStream, final OutputStream outputStream, final String algorithm, final int hashLength) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Missing Digest." + algorithm, e);
        }
        final byte[] buffer = new byte[65536];
        try {
            for (int read = inputStream.read(buffer); read >= 1; read = inputStream.read(buffer)) {
                digest.update(buffer, 0, read);
                outputStream.write(buffer, 0, read);
            }
        }
        finally {
            closeSilently(inputStream);
            closeSilently(outputStream);
        }
        return String.format("%1$0" + hashLength + "x", new BigInteger(1, digest.digest()));
    }
    
    protected void ensureFileWritable() {
        if (this.target.getParentFile() != null && !this.target.getParentFile().isDirectory()) {
            Downloadable.LOGGER.info("Making directory " + this.target.getParentFile());
            if (!this.target.getParentFile().mkdirs() && !this.target.getParentFile().isDirectory()) {
                throw new RuntimeException("Could not create directory " + this.target.getParentFile());
            }
        }
        if (this.target.isFile() && !this.target.canWrite()) {
            throw new RuntimeException("Do not have write permissions for " + this.target + " - aborting!");
        }
    }
    
    static {
        LOGGER = LogManager.getLogger();
    }
}
