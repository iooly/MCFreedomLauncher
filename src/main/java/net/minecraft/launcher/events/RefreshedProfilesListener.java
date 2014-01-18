package net.minecraft.launcher.events;

import net.minecraft.launcher.profile.ProfileManager;

public interface RefreshedProfilesListener
{
    void onProfilesRefreshed(ProfileManager p0);
    
    boolean shouldReceiveEventsInUIThread();
}
