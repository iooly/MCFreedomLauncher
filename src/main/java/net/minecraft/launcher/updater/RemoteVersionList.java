package net.minecraft.launcher.updater;

import java.io.IOException;
import net.minecraft.launcher.Http;
import java.net.URL;
import net.minecraft.launcher.OperatingSystem;
import net.minecraft.launcher.versions.CompleteVersion;
import java.net.Proxy;

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
        return Http.performGet(new URL("https://s3.amazonaws.com/Minecraft.Download/" + path), this.proxy);
    }
    
    public Proxy getProxy() {
        return this.proxy;
    }
}
