package net.minecraft.launcher.updater.download;

public interface DownloadListener
{
    void onDownloadJobFinished(DownloadJob p0);
    
    void onDownloadJobProgressChanged(DownloadJob p0);
}
