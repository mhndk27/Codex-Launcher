package com.mhndk27.codex.launcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;

public class MinecraftLauncher {

    // مسار مجلد .minecraft (تم تحديده سابقًا في VersionManager، لكن نكرره هنا للوضوح)
    private static final String GAME_DIR;
    
    // static block: تحديد مسار مجلد اللعبة الافتراضي (.minecraft) حسب نظام التشغيل (OS)
    static {
        if (SystemUtils.IS_OS_WINDOWS) {
            GAME_DIR = System.getenv("APPDATA") + File.separator + ".minecraft";
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            GAME_DIR = System.getProperty("user.home") + File.separator 
                                 + "Library" + File.separator + "Application Support" 
                                 + File.separator + "minecraft";
        } else {
            GAME_DIR = System.getProperty("user.home") + File.separator + ".minecraft";
        }
    }

    /**
     * launch(): وظيفتها تجميع الوسائط المطلوبة وتشغيل عملية (Process) لعبة ماين كرافت.
     * @param versionId الإصدار الذي تم اختياره من القائمة (مثل "1.20.1").
     * @param username اسم المستخدم (حالياً ثابت "mhndk27"، لاحقاً نجلب الاسم من SQLite).
     */
    public void launch(String versionId, String username) {
        // رسائل للمطورين (باللغة الإنجليزية، بناءً على طلبك)
        System.out.println("\n--- Attempting to launch version: " + versionId + " ---");
        
        // مسار ملف الـ JAR الخاص بالإصدار
        File versionJar = new File(GAME_DIR, "versions" + File.separator + versionId + File.separator + versionId + ".jar");
        
        // التحقق من وجود ملف الإصدار (هذا التحقق مبدئي)
        if (!versionJar.exists()) {
            System.err.println("Error: Version JAR file for " + versionId + " not found at: " + versionJar.getAbsolutePath());
            return;
        }

        // بناء الأمر (Command) - هذا الأمر مبسط جداً حالياً
        List<String> command = new ArrayList<>();
        
        // 1. أمر تشغيل الجافا (Java executable)
        command.add("java"); 
        
        // 2. وسائط الـ JVM (Java Virtual Machine) - إعدادات بسيطة للذاكرة
        command.add("-Xmx2G"); 
        command.add("-Duser.language=en"); 

        // 3. الوسائط الخاصة بتشغيل اللعبة
        command.add("-jar"); 
        command.add(versionJar.getAbsolutePath()); 
        
        // 4. وسائط اللعبة (Game Arguments) - الحد الأدنى للتشغيل
        command.add("--username");
        command.add(username); 
        command.add("--version");
        command.add(versionId); 
        command.add("--gameDir");
        command.add(GAME_DIR); 

        System.out.println("Command (Partial): " + String.join(" ", command));
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            
            // توجيه إخراج اللعبة إلى الطرفية
            processBuilder.inheritIO(); 

            System.out.println("Starting Minecraft process...");
            // حل الخطأ: لا نحتاج لتعريف متغير process إذا لم نستخدمه لاحقًا
            processBuilder.start(); 
            
        } catch (IOException e) {
            System.err.println("FATAL: Error running process. Is 'java' in your PATH?");
            e.printStackTrace();
        } 
    }
}