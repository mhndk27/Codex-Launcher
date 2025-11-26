package com.mhndk27.codex.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mhndk27.codex.data.Profile;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MinecraftLauncher {

    // Gson object to parse version JSON files
    private static final Gson GSON = new GsonBuilder().create(); 
    
    private static final String MINECRAFT_ROOT_DIR = getMinecraftRootDir();
    private static final File VERSIONS_DIR = new File(MINECRAFT_ROOT_DIR, "versions");
    private static final File LIBRARIES_DIR = new File(MINECRAFT_ROOT_DIR, "libraries");

    // تحديد مسار .minecraft حسب نظام التشغيل (OS)
    private static String getMinecraftRootDir() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return System.getenv("APPDATA") + File.separator + ".minecraft";
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            return System.getProperty("user.home") + File.separator 
                   + "Library" + File.separator + "Application Support" 
                   + File.separator + "minecraft";
        } else {
            return System.getProperty("user.home") + File.separator + ".minecraft";
        }
    }

    /**
     * launch(): تجميع الوسائط المطلوبة وتشغيل عملية (Process) لعبة ماين كرافت.
     * @param profile البروفايل الذي تم اختياره، يحتوي على كل الإعدادات.
     * @param username اسم المستخدم (حالياً mhndk27_offline).
     */
    public void launch(Profile profile, String username) {
        String versionId = profile.getVersionId();
        
        System.out.println("\n--- Attempting to launch version: " + versionId + " ---");
        
        // 1. مسار ملف تعريف الإصدار (Version JSON file)
        File versionJsonFile = new File(VERSIONS_DIR, versionId + File.separator + versionId + ".json");
        
        if (!versionJsonFile.exists()) {
            System.err.println("Error: Version JSON file not found at: " + versionJsonFile.getAbsolutePath());
            System.err.println("Please download the version files or check the Version ID in your profiles.json.");
            return;
        }

        try (FileReader reader = new FileReader(versionJsonFile)) {
            // 2. قراءة وتحليل ملف JSON الخاص بالإصدار
            VersionManifest manifest = GSON.fromJson(reader, VersionManifest.class);
            
            if (manifest == null || manifest.getMainClass() == null) {
                System.err.println("Error parsing version JSON manifest or Main Class is missing.");
                return;
            }
            
            // 3. بناء الـ Classpath
            String classpath = buildClassPath(manifest, versionId);
            String mainClass = manifest.getMainClass();
            
            // 4. بناء أمر التشغيل النهائي
            List<String> command = buildLaunchCommand(profile, username, classpath, mainClass);

            System.out.println("Command Structure: java -cp <CLASSPATH> " + mainClass + " [ARGS]");
            
            // 5. بدء عملية التشغيل
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO(); 

            // تحديد مجلد العمل (Working Directory)
            File workingDir = profile.getGameDir() != null ? new File(profile.getGameDir()) : new File(MINECRAFT_ROOT_DIR);
            processBuilder.directory(workingDir);
            System.out.println("Working Directory set to: " + workingDir.getAbsolutePath());

            System.out.println("Starting Minecraft process...");
            processBuilder.start(); 
            
        } catch (IOException e) {
            System.err.println("FATAL: Error running process. Details: " + e.getMessage());
            e.printStackTrace();
        } 
    }
    
    /**
     * buildClassPath(): تجميع مسار كل ملفات الـ JAR (المكتبات) المطلوبة.
     */
    private String buildClassPath(VersionManifest manifest, String versionId) {
        
        // مسارات المكتبات المطلوبة 
        List<String> libraryPaths = manifest.getLibraries().stream()
            .filter(VersionManifest.Library::appliesToCurrentOS) 
            .map(lib -> {
                // تحويل اسم المكتبة (e.g., com.mojang:patchy:1.3.9) إلى مسار محلي
                String[] parts = lib.getName().split(":");
                String path = parts[0].replace('.', File.separatorChar) + File.separator 
                            + parts[1] + File.separator 
                            + parts[2] + File.separator 
                            + parts[1] + "-" + parts[2] + ".jar";
                return new File(LIBRARIES_DIR, path).getAbsolutePath();
            })
            .collect(Collectors.toList());

        // إضافة مسار ملف الإصدار الرئيسي (version JAR)
        File mainJar = new File(VERSIONS_DIR, versionId + File.separator + versionId + ".jar");
        if (mainJar.exists()) {
            libraryPaths.add(mainJar.getAbsolutePath());
        }
        
        // جمع كل المسارات في String واحد مفصول بـ File.pathSeparator (؛ في ويندوز، : في لينكس/ماك)
        return String.join(File.pathSeparator, libraryPaths);
    }
    
    /**
     * buildLaunchCommand(): بناء أمر تشغيل الجافا كاملاً.
     */
    private List<String> buildLaunchCommand(Profile profile, String username, String classpath, String mainClass) {
        List<String> command = new ArrayList<>();
        
        // 1. أمر تشغيل الجافا (Java executable)
        command.add("java"); 
        
        // 2. وسائط الـ JVM (الذاكرة والإعدادات المخصصة)
        command.add("-Xmx" + profile.getMemoryMax() + "M"); 
        if (profile.getJavaArgs() != null && !profile.getJavaArgs().isEmpty()) {
            for (String arg : profile.getJavaArgs().split(" ")) {
                command.add(arg);
            }
        }
        
        // 3. تحديد الـ Classpath (المكتبات) 
        command.add("-cp"); 
        command.add(classpath); 

        // 4. تحديد الـ Main Class
        command.add(mainClass); 
        
        // 5. وسائط اللعبة (Game Arguments)
        command.add("--username");
        command.add(username); 
        command.add("--version");
        command.add(profile.getVersionId()); 
        command.add("--gameDir");
        // نستخدم Game Dir من البروفايل، أو المسار الافتراضي
        command.add(profile.getGameDir() != null ? profile.getGameDir() : MINECRAFT_ROOT_DIR);

        return command;
    }
}