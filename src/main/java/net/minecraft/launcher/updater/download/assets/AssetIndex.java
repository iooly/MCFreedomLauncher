package net.minecraft.launcher.updater.download.assets;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

public class AssetIndex
{
    public static final String DEFAULT_ASSET_NAME = "legacy";
    private Map<String, AssetObject> objects;
    private boolean virtual;
    
    public AssetIndex() {
        super();
        this.objects = new LinkedHashMap<String, AssetObject>();
    }
    
    public Map<String, AssetObject> getFileMap() {
        return this.objects;
    }
    
    public Set<AssetObject> getUniqueObjects() {
        return new HashSet<AssetObject>(this.objects.values());
    }
    
    public boolean isVirtual() {
        return this.virtual;
    }
    
    public class AssetObject
    {
        private String hash;
        private long size;
        
        public String getHash() {
            return this.hash;
        }
        
        public long getSize() {
            return this.size;
        }
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final AssetObject that = (AssetObject)o;
            return this.size == that.size && this.hash.equals(that.hash);
        }
        
        @Override
        public int hashCode() {
            int result = this.hash.hashCode();
            result = 31 * result + (int)(this.size ^ this.size >>> 32);
            return result;
        }
    }
}
