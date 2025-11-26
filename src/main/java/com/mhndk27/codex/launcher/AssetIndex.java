package com.mhndk27.codex.launcher;

import java.util.Map;

/**
 * AssetIndex: A model class to map the structure of the assets index JSON file.
 */
public class AssetIndex {
    
    private Map<String, AssetObject> objects;

    public Map<String, AssetObject> getObjects() {
        return objects;
    }

    public static class AssetObject {
        private String hash;
        private long size; 

        public String getHash() {
            return hash;
        }

        public long getSize() {
            return size;
        }

        /**
         * getPath(): يرجع المسار الصحيح للأصل داخل مجلد assets/objects.
         * يكون على شكل: <أول خانتين من الهاش>/<الهاش كاملاً>
         */
        public String getPath() {
            return hash.substring(0, 2) + "/" + hash;
        }
    }
}