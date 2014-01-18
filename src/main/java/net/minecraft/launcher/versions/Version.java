package net.minecraft.launcher.versions;

import java.util.Date;

public interface Version
{
    String getId();
    
    ReleaseType getType();
    
    void setType(ReleaseType p0);
    
    Date getUpdatedTime();
    
    void setUpdatedTime(Date p0);
    
    Date getReleaseTime();
    
    void setReleaseTime(Date p0);
}
