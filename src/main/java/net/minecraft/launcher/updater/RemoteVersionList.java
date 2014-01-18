package net.minecraft.launcher.updater;

import net.minecraft.launcher.Http;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.versions.CompleteVersion;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;

public class RemoteVersionList extends VersionList
{
    private final Proxy proxy;
    
    public RemoteVersionList(final Proxy proxy) {
        super();
        this.proxy = proxy;
    }
    
    @Override
    public boolean hasAllFiles(final CompleteVersion version, final OperatingSystem os) {
        return true;
    }
    
    @Override
    protected String getContent(final String path) throws IOException {
        return Http.performGet(new URL(LauncherConstants.URL_DOWNLOAD_BASE + path), this.proxy);
    }
    
    public Proxy getProxy() {
        return this.proxy;
    }
}
