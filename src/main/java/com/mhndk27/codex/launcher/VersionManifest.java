package com.mhndk27.codex.launcher;

import java.util.List;

/**
 * VersionManifest: A model class to map the main parts of a Minecraft version JSON file (e.g., 1.21.10.json).
 * It extracts the main class and list of required libraries.
 */
public class VersionManifest {

    // الحقل الذي يحتوي على نقطة الدخول الرئيسية للعبة (Main Class)
    private String mainClass;
    
    // قائمة المكتبات (Dependencies) المطلوبة
    private List<Library> libraries;

    // --- Getters ---
    public String getMainClass() { return mainClass; }
    public List<Library> getLibraries() { return libraries; }

    /**
     * Inner class Library: Represents a single dependency JAR file.
     */
    public static class Library {
        private String name; // Library path (e.g., com.mojang:patchy:1.3.9)
        
        // نحتاج هذه الدوال لاحقًا لتنزيل الملفات (Artifact)
        private Downloads downloads; 
        
        // قواعد الاستثناء/التضمين حسب نظام التشغيل
        private List<Rule> rules; 

        public String getName() { return name; }
        public List<Rule> getRules() { return rules; }

        /**
         * AppliesToCurrentOS(): وظيفة مؤقتة للتحقق من أن المكتبة يجب أن تستخدم في نظام التشغيل الحالي.
         * المنطق الكامل لهذا معقد جداً، لكننا نتركه الآن ليتمم بناء المسار (Classpath).
         */
        public boolean appliesToCurrentOS() {
            // مؤقتاً: نفترض أن كل المكتبات تنطبق على نظام التشغيل الحالي
            return true; 
        }
    }
    
    // Classes for mapping complex parts of the JSON (Rules, Downloads, etc.)
    public static class Downloads {
        private Artifact artifact;
        public Artifact getArtifact() { return artifact; }
    }
    
    public static class Artifact {
        private String path; 
        public String getPath() { return path; }
    }
    
    public static class Rule {
        private String action; // "allow" or "disallow"
        private Os os; // The operating system this rule applies to
    }
    
    public static class Os {
        private String name;
    }
}