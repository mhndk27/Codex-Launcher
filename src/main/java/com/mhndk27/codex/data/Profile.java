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

    // مُنشئ (Constructor) المستخدم في DataManager.java (لإضافة بروفايل جديد من instance)
    public Profile(String name, String versionId) {
        this.id = UUID.randomUUID().toString(); 
        this.name = name;
        this.versionId = versionId;
        this.gameDir = null; 
        this.memoryMax = 4096; 
        this.javaArgs = "";    
    }
    
    // مُنشئ فارغ (Constructor) - ضروري لمكتبة Gson لقراءة الملفات
    public Profile() {
    }

    // دوال Getter (للحصول على البيانات)
    public String getId() { return id; }
    public String getName() { return name; }
    public String getVersionId() { return versionId; }
    public String getGameDir() { return gameDir; }
    public int getMemoryMax() { return memoryMax; }
    public String getJavaArgs() { return javaArgs; }
    
    // دوال Setter (لتعديل البيانات)
    public void setMemoryMax(int memoryMax) { this.memoryMax = memoryMax; }
    public void setJavaArgs(String javaArgs) { this.javaArgs = javaArgs; }
    
    // تم إضافة هذه الدالة: (حل الخطأ: The method setGameDir(String) is undefined)
    public void setGameDir(String gameDir) { this.gameDir = gameDir; }

    // دالة toString: لعرض الاسم فقط في القائمة المنسدلة
    @Override
    public String toString() {
        return name;
    }
}