package com.mhndk27.codex.data;

import java.util.UUID;

// هذا الكلاس يمثل هيكلية بروفايل التشغيل الخاص بـ Codex Launcher
public class Profile {
    
    private String id;       
    private String name;     
    private String versionId;
    private String gameDir;  
    private int memoryMax;   
    private String javaArgs; 
    private String javaDir; // مسار ملف تشغيل الجافا (java.exe)

    // مُنشئ (Constructor) كامل
    public Profile(String name, String versionId) {
        this.id = UUID.randomUUID().toString(); 
        this.name = name;
        this.versionId = versionId;
        this.gameDir = null; 
        this.memoryMax = 4096; 
        this.javaArgs = "";    
        this.javaDir = null; 
    }
    
    // مُنشئ فارغ (Constructor) - ضروري لمكتبة Gson
    public Profile() {
    }

    // دوال Getter (للحصول على البيانات)
    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersionId() { return versionId; }
    public String getGameDir() { return gameDir; }
    public int getMemoryMax() { return memoryMax; }
    public String getJavaArgs() { return javaArgs; }
    public String getJavaDir() { return javaDir; } // تم إضافة Getter

    // دوال Setter (لتعديل البيانات)
    public void setMemoryMax(int memoryMax) { this.memoryMax = memoryMax; }
    public void setJavaArgs(String javaArgs) { this.javaArgs = javaArgs; }
    public void setGameDir(String gameDir) { this.gameDir = gameDir; }
    public void setJavaDir(String javaDir) { this.javaDir = javaDir; } // تم إضافة Setter
    public void setId(String id) {this.id = id;}
    @Override
    public String toString() {
        return name;
    }
}