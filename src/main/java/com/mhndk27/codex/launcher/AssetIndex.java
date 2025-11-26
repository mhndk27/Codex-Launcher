package com.mhndk27.codex.launcher;

import java.util.Map;

/**
 * AssetIndex: Model class for the assets index file (e.g., 1.21.json).
 * يجب أن تطابق أسماء الحقول تماماً أسماء الحقول في ملف JSON.
 */
public class AssetIndex {
    
    // هذا الحقل (Field) يجب أن يكون اسمه "objects"
    private Map<String, AssetObject> objects;

    public Map<String, AssetObject> getObjects() {
        return objects;
    }

    /**
     * Inner class AssetObject: Holds the file hash and size.
     */
    public static class AssetObject {
        
        // هذا الحقل (Field) يجب أن يكون اسمه "hash"
        private String hash; 
        
        // هذا الحقل (Field) يجب أن يكون اسمه "size"
        private long size;

        public String getHash() {
            return hash;
        }

        public long getSize() {
            return size;
        }
    }
}