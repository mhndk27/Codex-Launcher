package com.mhndk27.codex.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.mhndk27.codex.data.Account;
import com.mhndk27.codex.data.DataManager;
import com.mhndk27.codex.data.Profile;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.jar.JarFile;

public class MinecraftLauncher {

    private static final Gson GSON = new GsonBuilder().create();
    
    private static final String VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    
    private static final String MINECRAFT_ROOT_DIR = getMinecraftRootDir();
    private static final File VERSIONS_DIR = new File(MINECRAFT_ROOT_DIR, "versions");
    private static final File LIBRARIES_DIR = new File(MINECRAFT_ROOT_DIR, "libraries");
    private static final File ASSETS_DIR = new File(MINECRAFT_ROOT_DIR, "assets");
    private static final File ASSETS_OBJECTS_DIR = new File(ASSETS_DIR, "objects");
    
    private static final String ASSETS_BASE_URL = "https://resources.download.minecraft.net/";
    
    private final DataManager dataManager = new DataManager();
    // يجب أن تكون هذه الكلاس موجودة في com.mhndk27.codex.launcher
    private final DownloadManager downloadManager = new DownloadManager(); 

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

    private VersionManifestIndex.Version getVersionInfo(String versionId) {
        File indexFile = new File(MINECRAFT_ROOT_DIR, "version_manifest_v2.json");

        if (!downloadManager.downloadFile(VERSION_MANIFEST_URL, indexFile, null)) {
            System.err.println("FATAL: Could not download the main version manifest index.");
            return null;
        }

        try (FileReader reader = new FileReader(indexFile)) {
            VersionManifestIndex index = GSON.fromJson(reader, VersionManifestIndex.class);

            if (index == null || index.getVersions() == null) {
                System.err.println("Error parsing main version manifest index.");
                return null;
            }

            return index.getVersions().stream()
                    .filter(v -> versionId.equals(v.getId()))
                    .findFirst()
                    .orElse(null);

        } catch (IOException e) {
            System.err.println("Error reading version index file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Launch the game using a Profile ID (Recommended for dynamic launching)
     * إطلاق اللعبة باستخدام معرف البروفايل (موصى به للتشغيل الديناميكي)
     */
    public void launch(String profileId) {
        Profile profile = dataManager.getProfileById(profileId); // <--- تحتاج تطبيق هذه الدالة في DataManager
        
        if (profile == null) {
            System.err.println("FATAL: Profile with ID " + profileId + " not found. Launch aborted.");
            return;
        }

        // الحصول على اسم المستخدم من الحساب النشط
        Account account = dataManager.getActiveAccount();
        String username = account != null ? account.getUsername() : "Player";
        
        // استدعاء دالة الإطلاق الأصلية
        launch(profile, username); 
    }

    /**
     * Launch the game using a pre-loaded Profile object
     * إطلاق اللعبة باستخدام كائن Profile محمل مسبقًا
     */
    public void launch(Profile profile, String username) {
        String versionId = profile.getVersionId();
        
        if (versionId == null || versionId.isEmpty()) {
            System.err.println("Error: Cannot launch. The selected profile '" + profile.getName() + "' has no Version ID specified.");
            return;
        }

        System.out.println("\n--- Attempting to launch version: " + versionId + " ---");
        
        VersionManifestIndex.Version versionInfo = getVersionInfo(versionId);
        if (versionInfo == null) {
            System.err.println("FATAL: Could not find download URL for version " + versionId + ". Launch aborted.");
            return;
        }

        File versionJsonFile = new File(VERSIONS_DIR, versionId + File.separator + versionId + ".json");
        
        if (!downloadManager.downloadFile(versionInfo.getUrl(), versionJsonFile, versionInfo.getSha1())) {
            System.err.println("FATAL: Failed to ensure version JSON file. Cannot proceed.");
            return;
        }
        
        File nativesDir = null;

        try (FileReader reader = new FileReader(versionJsonFile)) {
            VersionManifest manifest = GSON.fromJson(reader, VersionManifest.class);
            
            if (manifest == null || manifest.getMainClass() == null) {
                System.err.println("Error parsing version JSON manifest or Main Class is missing.");
                return;
            }
            
            if (!downloadRequiredFiles(manifest, versionId)) {
                System.err.println("FATAL: Failed to download all required libraries and client JAR. Launch aborted.");
                return;
            }

            // 3. تنزيل جميع ملفات الأصول (Assets)
            if (!downloadAssets(manifest)) {
                System.err.println("FATAL: Failed to download all required assets. Launch aborted.");
                return;
            }
            
            nativesDir = extractNatives(manifest);
            
            String classpath = buildClassPath(manifest, versionId);
            String mainClass = manifest.getMainClass();
            
            // تم التعديل لاستخدام getAssetIndex()
            String assetsIndex = (manifest.getAssetIndex() != null) 
                                 ? manifest.getAssetIndex().getId() 
                                 : manifest.getAssets(); 
            
            List<String> gameArguments = parseGameArguments(profile, manifest);

            List<String> command = buildLaunchCommand(profile, manifest, mainClass, classpath, assetsIndex, gameArguments, nativesDir);

            System.out.println("Command Structure: java -cp <CLASSPATH> " + mainClass + " [ARGS]");
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO(); 

            File workingDir = profile.getGameDir() != null ? new File(profile.getGameDir()) : new File(MINECRAFT_ROOT_DIR);
            processBuilder.directory(workingDir);
            System.out.println("Working Directory set to: " + workingDir.getAbsolutePath());

            System.out.println("Starting Minecraft process... 🚀");
            
            Process process = processBuilder.start(); 
            
            process.waitFor(); 
            
            int exitCode = process.exitValue();
            System.out.println("Minecraft exited with code: " + exitCode);
            
        } catch (IOException e) {
            System.err.println("FATAL: Error running process. Details: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             System.err.println("Process interrupted.");
        } finally {
            if (nativesDir != null) {
                 System.out.println("Cleaning up natives directory: " + nativesDir.getAbsolutePath());
                 try {
                     deleteDirectory(nativesDir);
                     System.out.println("Natives directory cleaned up successfully. 😎");
                 } catch (IOException e) {
                     System.err.println("Error during natives cleanup: " + e.getMessage());
                 }
            }
        }
    }

    /**
     * downloadAssets(): تنزيل ملفات الأصول (Assets) بناءً على ملف الـ Index.
     */
    private boolean downloadAssets(VersionManifest manifest) {
        System.out.println("--- Starting Assets Download Check ---");

        // تم التعديل لاستخدام getAssetIndex()
        if (manifest.getAssetIndex() == null) {
            System.err.println("Warning: Assets index information missing in manifest.");
            return true; 
        }

        VersionManifest.AssetsIndex assetsIndex = manifest.getAssetIndex();
        String indexId = assetsIndex.getId(); 
        String indexUrl = assetsIndex.getUrl(); 
        String indexSha1 = assetsIndex.getSha1(); 

        if (indexUrl == null || indexSha1 == null) {
            System.err.println("FATAL: Assets Index URL/SHA1 missing from Manifest. Cannot proceed with asset download.");
            return false;
        }


        File assetIndexFile = new File(ASSETS_DIR, "indexes" + File.separator + indexId + ".json");

        // 1. تنزيل ملف Assets Index JSON
        if (!downloadManager.downloadFile(indexUrl, assetIndexFile, indexSha1)) {
            System.err.println("FATAL: Failed to download asset index file.");
            return false;
        }

        try (FileReader reader = new FileReader(assetIndexFile)) {
            AssetIndex assetIndex = GSON.fromJson(reader, AssetIndex.class);
            
            if (assetIndex == null || assetIndex.getObjects() == null) {
                System.err.println("Error parsing asset index JSON.");
                return false;
            }

            int totalAssets = assetIndex.getObjects().size();
            int downloadedCount = 0;
            
            // 2. تكرار وتنزيل كل أصل
            for (AssetIndex.AssetObject assetObject : assetIndex.getObjects().values()) {
                String hash = assetObject.getHash();
                String firstTwo = hash.substring(0, 2); 
                String assetUrl = ASSETS_BASE_URL + firstTwo + "/" + hash; 

                File targetFile = new File(ASSETS_OBJECTS_DIR, firstTwo + File.separator + hash);
                
                if (downloadManager.downloadFile(assetUrl, targetFile, hash)) {
                    downloadedCount++;
                    if (downloadedCount % 100 == 0 || downloadedCount == totalAssets) {
                        System.out.print("\rProgress: " + downloadedCount + "/" + totalAssets + " assets downloaded. ");
                    }
                } else {
                    System.err.println("\nFailed to download asset: " + hash);
                    return false;
                }
            }

            System.out.println("\n--- All " + totalAssets + " assets are ready! 🔥 ---");
            return true;

        } catch (IOException e) {
            System.err.println("FATAL: Error reading asset index file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * downloadRequiredFiles(): تنزيل ملف الكلاينت وجميع المكتبات المطلوبة.
     */
    private boolean downloadRequiredFiles(VersionManifest manifest, String versionId) {
        System.out.println("--- Starting Libraries Download Check ---");
        
        if (manifest.getDownloads() == null || manifest.getDownloads().getClient() == null) {
            System.err.println("FATAL: Missing Client download information in version manifest.");
            return false;
        }

        VersionManifest.ClientDownload clientDownload = manifest.getDownloads().getClient();
        File mainJar = new File(VERSIONS_DIR, versionId + File.separator + versionId + ".jar");
        
        if (!downloadManager.downloadFile(clientDownload.getUrl(), mainJar, clientDownload.getSha1())) {
            return false;
        }

        for (VersionManifest.Library lib : manifest.getLibraries()) {
            // تحقق من تطبيق المكتبة على نظام التشغيل
            if (!lib.appliesToCurrentOS() || lib.getDownloads() == null) { 
                continue; 
            }

            // تنزيل ملف المكتبة الأساسي (Artifact)
            VersionManifest.Artifact artifact = lib.getDownloads().getArtifact();
            if (artifact != null && artifact.getUrl() != null) {
                 File libFile = new File(LIBRARIES_DIR, artifact.getPath());
                 if (!downloadManager.downloadFile(artifact.getUrl(), libFile, artifact.getSha1())) {
                     return false;
                 }
            }

            // تنزيل ملف الـ Native JAR إذا كان موجوداً
            String nativeId = lib.getNativeId();
            if (nativeId != null) {
                // نستخدم getClassifiers الذي يرجع الـ Artifact الصحيح بناءً على نظام التشغيل
                VersionManifest.Artifact nativeArtifact = lib.getDownloads().getClassifiers(); 
                
                if (nativeArtifact != null && nativeArtifact.getUrl() != null) {
                    // بناء مسار حفظ الـ Native JAR
                    String[] parts = lib.getName().split(":");
                    String nativePath = parts[0].replace('.', File.separatorChar) + File.separator 
                                        + parts[1] + File.separator 
                                        + parts[2] + File.separator 
                                        + parts[1] + "-" + parts[2] + "-" + nativeId + ".jar"; 
                    
                    File nativeFile = new File(LIBRARIES_DIR, nativePath);
                    
                    if (!downloadManager.downloadFile(nativeArtifact.getUrl(), nativeFile, nativeArtifact.getSha1())) {
                         return false;
                    }
                }
            }
        }

        System.out.println("--- All core resources (Client JAR & Libraries) are ready. 🔥 ---");
        return true;
    }

    private void deleteDirectory(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException("Failed to delete file/directory: " + dir.getAbsolutePath());
        }
    }


    private File getTemporaryNativesDir() throws IOException {
        String tempDirName = "natives-" + UUID.randomUUID().toString(); 
        File tempDir = new File(VERSIONS_DIR, tempDirName); 
        
        if (!tempDir.mkdirs()) {
            throw new IOException("Failed to create temporary natives directory: " + tempDir.getAbsolutePath());
        }
        return tempDir;
    }

    private File extractNatives(VersionManifest manifest) throws IOException {
        File nativesDir = getTemporaryNativesDir();
        
        System.out.println("Extracting Natives to: " + nativesDir.getAbsolutePath());

        for (VersionManifest.Library lib : manifest.getLibraries()) {
            String nativeId = lib.getNativeId();
            if (nativeId != null) {
                
                String[] parts = lib.getName().split(":");
                String path = parts[0].replace('.', File.separatorChar) + File.separator 
                              + parts[1] + File.separator 
                              + parts[2] + File.separator 
                              + parts[1] + "-" + parts[2] + "-" + nativeId + ".jar"; 
                
                File nativeJar = new File(LIBRARIES_DIR, path);
                
                if (nativeJar.exists()) {
                    try (JarFile jar = new JarFile(nativeJar)) {
                        Enumeration<? extends ZipEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            
                            if (entry.getName().contains("META-INF")) {
                                continue;
                            }
                            
                            File destFile = new File(nativesDir, entry.getName());
                            if (entry.isDirectory()) {
                                destFile.mkdirs();
                                continue;
                            }
                            
                            // ضمان وجود المجلد الأب
                            destFile.getParentFile().mkdirs(); 
                            
                            try (InputStream is = jar.getInputStream(entry);
                                 FileOutputStream fos = new FileOutputStream(destFile)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = is.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error extracting natives from " + nativeJar.getName() + ": " + e.getMessage());
                    }
                } else {
                    System.err.println("Warning: Native JAR not found: " + nativeJar.getAbsolutePath());
                }
            }
        }
        return nativesDir;
    }
    
    private String buildClassPath(VersionManifest manifest, String versionId) {
        List<String> libraryPaths = manifest.getLibraries().stream()
            .filter(VersionManifest.Library::appliesToCurrentOS) 
            .map(lib -> {
                // حاول الحصول على المسار من الـ Artifact
                VersionManifest.Artifact artifact = lib.getDownloads().getArtifact();
                if (artifact != null && artifact.getPath() != null) {
                    return new File(LIBRARIES_DIR, artifact.getPath()).getAbsolutePath();
                } 
                
                // fallback: بناء المسار بالطريقة التقليدية
                String[] parts = lib.getName().split(":");
                String path = parts[0].replace('.', File.separatorChar) + File.separator 
                              + parts[1] + File.separator 
                              + parts[2] + File.separator 
                              + parts[1] + "-" + parts[2] + ".jar";
                return new File(LIBRARIES_DIR, path).getAbsolutePath();
            })
            .collect(Collectors.toList());

        File mainJar = new File(VERSIONS_DIR, versionId + File.separator + versionId + ".jar");
        if (mainJar.exists()) {
            libraryPaths.add(mainJar.getAbsolutePath());
        }
        
        return String.join(File.pathSeparator, libraryPaths);
    }
    
    private List<String> parseGameArguments(Profile profile, VersionManifest manifest) {
        
        List<String> rawArguments = new ArrayList<>();
        
        // 1. التعامل مع Arguments الحديثة (List)
        if (manifest.getArguments() != null && manifest.getArguments().getGame() != null) {
            
            for (Object arg : manifest.getArguments().getGame()) {
                if (arg instanceof String) {
                    rawArguments.add((String) arg);
                } else if (arg instanceof LinkedTreeMap) {
                    // تجاهل قواعد الـ Rules المعقدة مؤقتاً
                }
            }
            
            if (rawArguments.isEmpty() && manifest.getArguments().getGame().size() > 0) {
                 System.err.println("Warning: Modern game arguments contain only complex rules (not currently supported).");
            }
        
        // 2. التعامل مع Arguments القديمة (String)
        } else if (manifest.getMinecraftArguments() != null && !manifest.getMinecraftArguments().isEmpty()) {
            System.out.println("Using legacy 'minecraftArguments' format.");
            rawArguments.addAll(Arrays.asList(manifest.getMinecraftArguments().split(" ")));
        } else {
            System.err.println("Warning: Neither 'arguments' nor 'minecraftArguments' were found in the manifest.");
            return new ArrayList<>();
        }
        
        // --- تعويض الـ Placeholders ---
        
        Account account = dataManager.getActiveAccount();
        String username = account != null ? account.getUsername() : "Player";
        String uuid = account != null ? account.getUuid() : "00000000-0000-0000-0000-000000000000"; 
        String accessToken = account != null ? account.getAccessToken() : "0"; 
        
        String versionType = (manifest.getArguments() != null) ? "release" : "Legacy"; 
        
        Map<String, String> replacements = new HashMap<>();
        replacements.put("${auth_player_name}", username);
        replacements.put("${version_name}", profile.getVersionId());
        replacements.put("${game_directory}", profile.getGameDir() != null ? profile.getGameDir() : MINECRAFT_ROOT_DIR);
        replacements.put("${assets_root}", ASSETS_DIR.getAbsolutePath());
        
        // حل مشكلة الـ Assets Index
        String assetIndexId = (manifest.getAssetIndex() != null ? manifest.getAssetIndex().getId() : manifest.getAssets());
        replacements.put("${assets_index}", assetIndexId); 
        replacements.put("${assets_index_name}", assetIndexId); 
        
        replacements.put("${auth_uuid}", uuid);
        replacements.put("${auth_access_token}", accessToken); 
        replacements.put("${user_type}", "mojang"); 
        replacements.put("${version_type}", versionType);
        
        replacements.put("${clientid}", "launcher"); 
        replacements.put("${auth_xuid}", "0"); 
        
        List<String> resolvedArguments = new ArrayList<>();
        for (String arg : rawArguments) {
            String resolvedArg = arg;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                resolvedArg = resolvedArg.replace(entry.getKey(), entry.getValue());
            }
            resolvedArguments.add(resolvedArg);
        }
        
        return resolvedArguments;
    }

    private List<String> getJvmArguments(VersionManifest manifest, String classpath) {
        if (manifest.getArguments() == null || manifest.getArguments().getJvm() == null) {
            return new ArrayList<>();
        }
        
        Map<String, String> jvmReplacements = new HashMap<>();
        jvmReplacements.put("${classpath}", classpath);
        jvmReplacements.put("${library_directory}", LIBRARIES_DIR.getAbsolutePath()); 
        jvmReplacements.put("${user_home}", System.getProperty("user.home")); 
        
        List<String> jvmArgs = new ArrayList<>();
        for (Object arg : manifest.getArguments().getJvm()) {
            if (arg instanceof String) {
                String argString = (String) arg;
                for (Map.Entry<String, String> entry : jvmReplacements.entrySet()) {
                    argString = argString.replace(entry.getKey(), entry.getValue());
                }
                jvmArgs.add(argString);
            } else if (arg instanceof LinkedTreeMap) {
                // تجاهل قواعد الـ JVM المعقدة مؤقتاً
            }
        }
        return jvmArgs;
    }


    private List<String> buildLaunchCommand(Profile profile, VersionManifest manifest, String mainClass, String classpath, String assetsIndex, List<String> gameArguments, File nativesDir) {
        List<String> command = new ArrayList<>();
        
        String javaExecutable = "java"; 
        if (profile.getJavaDir() != null && !profile.getJavaDir().isEmpty()) {
            javaExecutable = profile.getJavaDir();
            // في ويندوز، نضيف java.exe إذا لم يكن موجوداً
            if (SystemUtils.IS_OS_WINDOWS && !javaExecutable.toLowerCase().endsWith("java.exe")) {
                javaExecutable += File.separator + "java.exe";
            }
        }
        command.add(javaExecutable); 
        
        // JVM Arguments (الخاصة بالإصدار - الصيغة الجديدة)
        List<String> versionJvmArgs = getJvmArguments(manifest, classpath);
        command.addAll(versionJvmArgs);
        
        // 1. إضافة مسار Natives (مطلوب دائماً)
        if (nativesDir != null) {
            command.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        }
        
        // 2. JVM Arguments (الخاصة بالمستخدم: الذاكرة والـ Args الإضافية)
        command.add("-Xmx" + profile.getMemoryMax() + "M"); 
        command.add("-Xms" + profile.getMemoryMax() / 2 + "M"); // أضفنا الحد الأدنى للذاكرة
        
        if (profile.getJavaArgs() != null && !profile.getJavaArgs().isEmpty()) {
            for (String arg : profile.getJavaArgs().split(" ")) {
                command.add(arg);
            }
        }
        
        // 3. Classpath - يتم إضافته فقط إذا كان الإصدار قديماً (Legacy)
        if (manifest.getArguments() == null) { 
            command.add("-cp"); 
            command.add(classpath); 
        }

        // 4. Main Class
        command.add(mainClass); 
        
        // 5. Game Arguments
        command.addAll(gameArguments);

        return command;
    }
}