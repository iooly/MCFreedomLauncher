package net.minecraft.launcher.events;

import net.minecraft.launcher.updater.VersionManager;

public interface RefreshedVersionsListener
{
    void onVersionsRefreshed(VersionManager p0);
    
    boolean shouldReceiveEventsInUIThread();
}
