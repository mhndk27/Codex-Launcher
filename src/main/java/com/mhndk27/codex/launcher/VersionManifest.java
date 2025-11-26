package com.mhndk27.codex.launcher;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap; 

/**
 * VersionManifest: A model class to map the main parts of a Minecraft version JSON file.
 */
public class VersionManifest {

    private String mainClass;
    private List<Library> libraries;
    private AssetsIndex assetIndex; // <--- تم التعديل هنا: assetIndex
    private String assets; 
    private String minecraftArguments; 
    private Arguments arguments; 
    private DownloadsContainer downloads;

    // --- Getters ---
    public String getMainClass() { return mainClass; }
    public List<Library> getLibraries() { return libraries; }
    public AssetsIndex getAssetIndex() { return assetIndex; } // <--- تم التعديل هنا: getAssetIndex()
    public String getAssets() { return assets; } 
    public String getMinecraftArguments() { return minecraftArguments; } 
    public Arguments getArguments() { return arguments; }
    public DownloadsContainer getDownloads() { return downloads; }

    
    // --------------------------------------------------------------------------------------------------
    // كلاسات خاصة بالوسائط (Arguments) الحديثة
    // --------------------------------------------------------------------------------------------------
    
    public static class Arguments {
        private List<Object> game; 
        private List<Object> jvm;

        public List<Object> getGame() { return game; }
        public List<Object> getJvm() { return jvm; }
    }

    public static class ArgumentRule {
        private List<Rule> rules;
        private List<String> value;
    }

    // --------------------------------------------------------------------------------------------------
    // الكلاسات المتبقية
    // --------------------------------------------------------------------------------------------------
    
    /**
     * Inner class AssetsIndex: Contains information about the assets file.
     */
    public static class AssetsIndex {
        private String id; 
        private String url; 
        private String sha1; 
        
        public String getId() { return id; }
        public String getUrl() { return url; } 
        public String getSha1() { return sha1; } 
    }
    
    public static class DownloadsContainer {
        private ClientDownload client;
        public ClientDownload getClient() { return client; }
    }
    
    public static class ClientDownload {
        private String url;
        private String sha1;
        public String getUrl() { return url; }
        public String getSha1() { return sha1; }
    }

    
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
                return natives.get("windows").replace("${arch}", "64"); 
            }
            return null;
        }

        public boolean appliesToCurrentOS() {
            return true; 
        }
    }
    
    public static class LibraryDownloads {
        private Artifact artifact;
        private Artifact classifiers; 
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