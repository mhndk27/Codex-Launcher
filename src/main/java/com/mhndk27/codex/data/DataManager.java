package com.mhndk27.codex.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.SystemUtils; // لتحديد نظام التشغيل

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // مسار مجلد اللانشر الخاص: .codexlauncher
    private static final File CODEX_DIR = new File(System.getProperty("user.home"), ".codexlauncher");
    
    // مسارات ملفات JSON الخاصة بـ Codex Launcher
    private static final File PROFILES_FILE = new File(CODEX_DIR, "profiles.json");
    private static final File ACCOUNTS_FILE = new File(CODEX_DIR, "accounts.json");
    
    // مسار مجلد .minecraft (ملفات اللعبة الأساسية)
    private static final String MINECRAFT_ROOT_DIR = getMinecraftRootDir();
    // مسار مجلد الـ Instances الافتراضي
    private static final File INSTANCES_DIR = new File(MINECRAFT_ROOT_DIR, "instances");


    public DataManager() {
        initializeDataFiles();
        syncProfilesWithInstances(); // المزامنة التلقائية عند بدء التشغيل
    }
    
    // ------------------------------------
    // دوال مساعدة (Utility Methods)
    // ------------------------------------

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

    // ------------------------------------
    // تهيئة (Initialization)
    // ------------------------------------

    private void initializeDataFiles() {
        // إنشاء مجلدات اللانشر ومجلد الـ Instances ومجلد الـ Profiles
        if (!CODEX_DIR.exists() && !CODEX_DIR.mkdirs()) {
            System.err.println("FATAL: Could not create Codex Launcher directory.");
            return;
        }
        if (!INSTANCES_DIR.exists() && !INSTANCES_DIR.mkdirs()) {
             System.err.println("WARNING: Could not create default instances directory.");
        }
        
        // 1. إنشاء ملف الحسابات الافتراضي (إذا لم يكن موجودًا)
        if (!ACCOUNTS_FILE.exists()) {
            System.out.println("Creating default accounts.json...");
            try {
                // حساب افتراضي (Offline Mode)
                List<Account> defaultAccounts = new ArrayList<>();
                defaultAccounts.add(new Account(
                    UUID.randomUUID().toString(), 
                    "mhndk27_offline", 
                    "codex_token_temp", 
                    "Offline"
                ));
                saveAccounts(defaultAccounts); // تم إضافة الدالة saveAccounts
            } catch (IOException e) {
                System.err.println("Error saving default accounts file: " + e.getMessage());
            }
        }
    }
    
    // ------------------------------------
    // منطق المزامنة (Synchronization Logic)
    // ------------------------------------
    
    /**
     * syncProfilesWithInstances(): تفحص مجلد instances وتضيف البروفايلات الجديدة.
     */
    public void syncProfilesWithInstances() {
        if (!INSTANCES_DIR.exists()) return;
        
        List<Profile> existingProfiles = loadProfiles();
        File[] instanceFolders = INSTANCES_DIR.listFiles(File::isDirectory); // جلب كل المجلدات الفرعية
        
        if (instanceFolders != null) {
            boolean profileAdded = false;
            
            for (File folder : instanceFolders) {
                // مسار مجلد البروفايل (مثل: instances/ayano_fembric)
                String gameDirPath = folder.getAbsolutePath(); 
                
                // التحقق إذا كان البروفايل موجود مسبقًا في ملف profiles.json
                boolean exists = existingProfiles.stream()
                                    .anyMatch(p -> gameDirPath.equals(p.getGameDir()));
                                    
                if (!exists) {
                    // إذا كان مجلد Instance جديد، ننشئ له بروفايل Codex جديد
                    // اسم البروفايل يكون هو اسم المجلد
                    String profileName = folder.getName();
                    
                    // افتراضياً نربط بأحدث إصدار "1.21.10" (يجب أن يتم تعديله من قبل المستخدم لاحقًا)
                    Profile newProfile = new Profile(profileName, "1.21.10"); 
                    newProfile.setGameDir(gameDirPath); // تحديد مسار اللعبة
                    
                    existingProfiles.add(newProfile);
                    profileAdded = true;
                    System.out.println("SYNC: Added new profile from instance folder: " + profileName);
                }
            }
            
            // إذا تم إضافة أي بروفايل جديد، نحفظ ملف JSON المحدث
            if (profileAdded) {
                try {
                    saveProfiles(existingProfiles);
                    System.out.println("SYNC: Successfully saved updated profiles.json.");
                } catch (IOException e) {
                    System.err.println("Error saving synchronized profiles: " + e.getMessage());
                }
            }
        }
    }
    
    // ------------------------------------
    // دوال التحميل والحفظ
    // ------------------------------------
    
    public List<Profile> loadProfiles() {
        if (!PROFILES_FILE.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(PROFILES_FILE)) {
            Type listType = new TypeToken<ArrayList<Profile>>() {}.getType();
            List<Profile> profiles = GSON.fromJson(reader, listType);
            return profiles != null ? profiles : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error loading profiles from JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void saveProfiles(List<Profile> profiles) throws IOException {
        try (FileWriter writer = new FileWriter(PROFILES_FILE)) {
            GSON.toJson(profiles, writer);
        }
    }
    
    public void saveAccounts(List<Account> accounts) throws IOException {
        try (FileWriter writer = new FileWriter(ACCOUNTS_FILE)) {
            GSON.toJson(accounts, writer);
        }
    }
    
    public Account getActiveAccount() {
        List<Account> accounts = loadAccounts();
        return accounts.isEmpty() ? null : accounts.get(0);
    }
    
    public List<Account> loadAccounts() {
        if (!ACCOUNTS_FILE.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(ACCOUNTS_FILE)) {
            Type listType = new TypeToken<ArrayList<Account>>() {}.getType();
            List<Account> accounts = GSON.fromJson(reader, listType);
            return accounts != null ? accounts : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error loading accounts from JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}