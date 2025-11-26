package com.mhndk27.codex.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken; 
import org.apache.commons.lang3.SystemUtils; 

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// DataManager: مسؤول عن إدارة ملفات profiles.json و accounts.json
public class DataManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final String MINECRAFT_ROOT_DIR = getMinecraftRootDir();
    private static final File CODEX_DIR = new File(System.getProperty("user.home"), ".codexlauncher");
    private static final File PROFILES_FILE = new File(CODEX_DIR, "profiles.json");
    private static final File ACCOUNTS_FILE = new File(CODEX_DIR, "accounts.json");
    private static final File INSTANCES_DIR = new File(MINECRAFT_ROOT_DIR, "instances");


    public DataManager() {
        initializeDataFiles();
        syncProfilesWithInstances(); 
    }
    
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

    private void initializeDataFiles() {
        // إنشاء مجلدات اللانشر ومجلد الـ Instances 
        if (!CODEX_DIR.exists() && !CODEX_DIR.mkdirs()) {
            System.err.println("FATAL: Could not create Codex Launcher directory.");
            return;
        }
        if (!INSTANCES_DIR.exists() && !INSTANCES_DIR.mkdirs()) {
             System.err.println("WARNING: Could not create default instances directory.");
        }
        
        // إنشاء ملف الحسابات الافتراضي 
        if (!ACCOUNTS_FILE.exists()) {
            System.out.println("Creating default accounts.json...");
            try {
                List<Account> defaultAccounts = new ArrayList<>();
                defaultAccounts.add(new Account(
                    UUID.randomUUID().toString(), 
                    "mhndk27_offline", 
                    "codex_token_temp", 
                    "Offline"
                ));
                saveAccounts(defaultAccounts); 
            } catch (IOException e) {
                System.err.println("Error saving default accounts file: " + e.getMessage());
            }
        }
    }
    
    // دالة محاولة استخراج Version ID من مجلد Instance
    private String guessVersionId(File instanceFolder) {
        File instanceVersionsDir = new File(instanceFolder, "versions");
        if (instanceVersionsDir.exists() && instanceVersionsDir.isDirectory()) {
            File[] versionFolders = instanceVersionsDir.listFiles(File::isDirectory);
            if (versionFolders != null && versionFolders.length > 0) {
                // نأخذ اسم أول مجلد إصدار نجده 
                return versionFolders[0].getName();
            }
        }
        return null;
    }


    public void syncProfilesWithInstances() {
        if (!INSTANCES_DIR.exists()) return;
        
        List<Profile> existingProfiles = loadProfiles();
        File[] instanceFolders = INSTANCES_DIR.listFiles(File::isDirectory); 
        
        if (instanceFolders != null) {
            boolean profileAdded = false;
            
            for (File folder : instanceFolders) {
                String gameDirPath = folder.getAbsolutePath(); 
                
                boolean exists = existingProfiles.stream()
                                    .anyMatch(p -> gameDirPath.equals(p.getGameDir()));
                                    
                if (!exists) {
                    String profileName = folder.getName();
                    String detectedVersion = guessVersionId(folder);

                    Profile newProfile = new Profile(profileName, detectedVersion); 
                    newProfile.setGameDir(gameDirPath); 
                    
                    existingProfiles.add(newProfile);
                    profileAdded = true;
                    System.out.println("SYNC: Added new profile: " + profileName + 
                                       (detectedVersion == null ? " (Version TBD)" : " (Version: " + detectedVersion + ")"));
                }
            }
            
            if (profileAdded || existingProfiles.isEmpty()) { // Ensure at least one default is saved if needed
                 try {
                    saveProfiles(existingProfiles);
                    System.out.println("SYNC: Successfully saved updated profiles.json. 😎");
                } catch (IOException e) {
                    System.err.println("Error saving synchronized profiles: " + e.getMessage());
                }
            }
        }
    }
    
    // دوال التحميل والحفظ
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