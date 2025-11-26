package com.mhndk27.codex.launcher;

import java.io.File;               // للتعامل مع مسارات الملفات والمجلدات في نظام التشغيل
import java.util.ArrayList;        // قائمة عشان نخزن فيها أسماء الإصدارات
import java.util.List;
// مهم: هذه المكتبة (Library) تسهل علينا معرفة نظام التشغيل (Windows, Mac, Linux)
// لازم نتأكد إنها مضافة في pom.xml، بنضيفها في الخطوة الجاية لو ما كانت موجودة
import org.apache.commons.lang3.SystemUtils; 

public class VersionManager {
    
    // متغير ثابت (Constant) يحمل مسار مجلد .minecraft (ملفات اللعبة الأساسية)
    private static final String MINECRAFT_ROOT_DIR;
    
    // static block: هذا الجزء يتنفذ مرة وحدة فقط لما يتم استخدام الكلاس لأول مرة
    // وظيفته تحديد موقع ملفات ماين كرافت تلقائياً حسب نظام التشغيل (بدون ما تعطيني أي تفاصيل)
    static {
        // التحقق من نظام التشغيل (OS)
        if (SystemUtils.IS_OS_WINDOWS) {
            // ويندوز: المسار الافتراضي يكون داخل مجلد %APPDATA%
            // System.getenv("APPDATA"): يجلب مسار مجلد AppData
            MINECRAFT_ROOT_DIR = System.getenv("APPDATA") + File.separator + ".minecraft";
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            // ماك: المسار يكون داخل مجلد "Library/Application Support"
            MINECRAFT_ROOT_DIR = System.getProperty("user.home") + File.separator 
                                 + "Library" + File.separator + "Application Support" 
                                 + File.separator + "minecraft";
        } else {
            // لينكس/أنظمة أخرى: المسار الافتراضي يكون في مجلد المستخدم
            // System.getProperty("user.home"): يجلب مسار مجلد المستخدم
            MINECRAFT_ROOT_DIR = System.getProperty("user.home") + File.separator + ".minecraft";
        }
    }

    /**
     * getAvailableVersions(): وظيفتها قراءة مجلد "versions" وإرجاع أسماء الإصدارات المتوفرة.
     * @return قائمة (List) بأسماء الإصدارات (مثل "1.20.1", "fabric-loader-1.18.2").
     */
    public List<String> getAvailableVersions() {
        List<String> versions = new ArrayList<>(); // نجهز قائمة فارغة لتخزين أسماء الإصدارات
        
        // نحدد المسار الكامل لمجلد "versions" (الموجود داخل مجلد .minecraft)
        File versionsFolder = new File(MINECRAFT_ROOT_DIR, "versions");
        
        System.out.println("✅ البحث عن الإصدارات في المسار: " + versionsFolder.getAbsolutePath());
        
        // التحقق من أن المجلد موجود وأن المسار يشير إلى مجلد حقيقي
        if (versionsFolder.exists() && versionsFolder.isDirectory()) {
            // listFiles(): يجلب كل الملفات والمجلدات اللي داخل مجلد "versions"
            File[] files = versionsFolder.listFiles();
            
            if (files != null) {
                // نمر على كل ملف/مجلد
                for (File file : files) {
                    // كل إصدار هو مجلد فرعي باسمه، لذا نتأكد أنه مجلد وليس ملف
                    if (file.isDirectory()) {
                        // getName(): يجلب اسم المجلد (وهو اسم الإصدار)
                        versions.add(file.getName());
                    }
                }
            }
        } else {
            // لو المسار غلط أو ملفات ماين كرافت مو موجودة
            System.err.println("❌ خطأ: مجلد الإصدارات غير موجود. تأكد من أنك ثبت ماين كرافت!");
        }
        
        return versions; // نرجع القائمة النهائية
    }
}