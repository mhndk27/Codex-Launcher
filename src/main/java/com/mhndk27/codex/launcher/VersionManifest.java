package com.mhndk27.codex.launcher;

import java.util.List;
import java.util.Map;

/**
 * VersionManifest: A model class to map the main parts of a Minecraft version JSON file.
 */
public class VersionManifest {

    private String mainClass;
    private List<Library> libraries;
    private AssetsIndex assetsIndex; 
    private String assets; 
    private String minecraftArguments; 

    // --- Getters ---
    public String getMainClass() { return mainClass; }
    public List<Library> getLibraries() { return libraries; }
    public AssetsIndex getAssetsIndex() { return assetsIndex; } 
    public String getAssets() { return assets; } 
    public String getMinecraftArguments() { return minecraftArguments; } 

    /**
     * Inner class AssetsIndex: Contains information about the assets file.
     */
    public static class AssetsIndex {
        private String id; 
        public String getId() { return id; }
    }
    
    /**
     * Inner class Library: Represents a single dependency JAR file.
     */
    public static class Library {
        private String name; 
        private Downloads downloads; 
        private List<Rule> rules; 
        private Map<String, String> natives; // تم الإضافة: لتحديد مسار الملف التنفيذي الأصلي حسب OS
        private Extract extract;             // تم الإضافة: لتحديد الملفات التي يجب استثناؤها

        public String getName() { return name; }
        public List<Rule> getRules() { return rules; }
        public Map<String, String> getNatives() { return natives; } // Getter جديد

        /**
         * getNativeId(): للحصول على اسم Native JAR المناسب لنظام التشغيل الحالي.
         * (المنطق هنا بسيط، يفترض windows)
         */
        public String getNativeId() {
            if (natives == null) return null;
            
            // في اللانشرات الحقيقية يتم التحقق من نظام التشغيل (OS) بدقة
            if (natives.containsKey("windows")) {
                return natives.get("windows").replace("${arch}", "64"); // نفترض 64 بت
            }
            return null;
        }

        public boolean appliesToCurrentOS() {
            // Placeholder: Assume all libraries apply for now.
            return true; 
        }
    }
    
    public static class Extract {
        private List<String> exclude; // قائمة الملفات/المسارات المستبعدة من الاستخراج
    }
    
    public static class Downloads {
        private Artifact artifact;
        public Artifact getArtifact() { return artifact; }
    }
    
    public static class Artifact {
        private String path; 
        public String getPath() { return path; }
    }
    
    public static class Rule {
        private String action; 
        private Os os; 
    }
    
    public static class Os {
        private String name;
    }
}