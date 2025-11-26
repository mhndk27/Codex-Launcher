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
    private DownloadsContainer downloads;

    // --- Getters ---
    public String getMainClass() { return mainClass; }
    public List<Library> getLibraries() { return libraries; }
    public AssetsIndex getAssetsIndex() { return assetsIndex; } 
    public String getAssets() { return assets; } 
    public String getMinecraftArguments() { return minecraftArguments; } 
    public DownloadsContainer getDownloads() { return downloads; }

    /**
     * Inner class AssetsIndex: Contains information about the assets file.
     */
    public static class AssetsIndex {
        private String id; 
        public String getId() { return id; }
    }
    
    /**
     * Inner class DownloadsContainer: يحتوي على معلومات تحميل ملف الكلاينت الرئيسي (Client JAR).
     */
    public static class DownloadsContainer {
        private ClientDownload client;
        public ClientDownload getClient() { return client; }
    }
    
    /**
     * Inner class ClientDownload: يحتوي على رابط التحميل والـ SHA1 لملف الكلاينت.
     */
    public static class ClientDownload {
        private String url;
        private String sha1;
        public String getUrl() { return url; }
        public String getSha1() { return sha1; }
    }

    
    /**
     * Inner class Library: Represents a single dependency JAR file.
     */
    public static class Library {
        private String name; 
        private LibraryDownloads downloads;
        private List<Rule> rules; 
        private Map<String, String> natives; 
        private Extract extract;            

        public String getName() { return name; }
        public List<Rule> getRules() { return rules; }
        public Map<String, String> getNatives() { return natives; }
        public LibraryDownloads getDownloads() { return downloads; }
        public Extract getExtract() { return extract; }
        
        public String getNativeId() {
            if (natives == null) return null;
            if (natives.containsKey("windows")) {
                // يفترض هنا أننا دائما نستخدم نسخة 64 بت
                return natives.get("windows").replace("${arch}", "64"); 
            }
            return null;
        }

        public boolean appliesToCurrentOS() {
            // Placeholder: Assume all libraries apply for now (حتى نبرمج منطق القواعد الكامل).
            return true; 
        }
    }
    
    public static class LibraryDownloads {
        private Artifact artifact;
        private Artifact classifiers; // للمكتبات الخاصة مثل Natives
        public Artifact getArtifact() { return artifact; }
        public Artifact getClassifiers() { return classifiers; }
    }

    
    public static class Extract {
        private List<String> exclude; 
    }
    
    public static class Artifact {
        private String url;
        private String path; 
        private String sha1;
        public String getUrl() { return url; }
        public String getPath() { return path; }
        public String getSha1() { return sha1; } 
    }
    
    public static class Rule {
        private String action; 
        private Os os; 
    }
    
    public static class Os {
        private String name;
    }
}