package com.mhndk27.codex.launcher;

import java.util.List;

/**
 * VersionManifestIndex: A model class to map the structure of the main version list (version_manifest_v2.json).
 */
public class VersionManifestIndex {
    
    private List<Version> versions;

    public List<Version> getVersions() {
        return versions;
    }

    /**
     * Inner class Version: يمثل بيانات إصدار واحد من القائمة الرئيسية.
     * يحتوي على الـ ID والرابط الخاص بملف Version JSON والـ SHA1 للتحقق.
     */
    public static class Version {
        private String id;
        private String url;
        private String sha1;
        // حقول أخرى (مثل type, time, releaseTime) غير ضرورية في الوقت الحالي

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public String getSha1() {
            return sha1;
        }
    }
}