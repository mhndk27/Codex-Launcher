package com.mhndk27.codex.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.UUID; // <--- تم تصحيح خطأ الاستيراد
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.jar.JarFile;

public class MinecraftLauncher {

    private static final Gson GSON = new GsonBuilder().create(); 
    
    private static final String MINECRAFT_ROOT_DIR = getMinecraftRootDir();
    private static final File VERSIONS_DIR = new File(MINECRAFT_ROOT_DIR, "versions");
    private static final File LIBRARIES_DIR = new File(MINECRAFT_ROOT_DIR, "libraries");
    private static final File ASSETS_DIR = new File(MINECRAFT_ROOT_DIR, "assets"); 
    
    private final DataManager dataManager = new DataManager(); 
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

    public void launch(Profile profile, String username) {
        String versionId = profile.getVersionId();
        
        if (versionId == null || versionId.isEmpty()) {
            System.err.println("Error: Cannot launch. The selected profile '" + profile.getName() + "' has no Version ID specified.");
            return;
        }

        System.out.println("\n--- Attempting to launch version: " + versionId + " ---");
        
        File versionJsonFile = new File(VERSIONS_DIR, versionId + File.separator + versionId + ".json");
        
        // 1. التحقق وتنزيل ملف الـ JSON
        // **مؤقت:** نستخدم رابط وهاش ثابت لإصدار 1.20.1 لغرض التجربة
        String knownVersionJsonUrl = "https://piston-data.mojang.com/v1/objects/1c261947b744474724a0d8e8736a5b672a9e34a2/1.20.1.json"; 
        String dummySha1 = "d4807a505165c40467b7f2f11467406a6669910d";

        if (!downloadManager.downloadFile(knownVersionJsonUrl, versionJsonFile, dummySha1)) {
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
            
            // 2. تنزيل جميع المكتبات وملف الكلاينت JAR
            if (!downloadRequiredFiles(manifest, versionId)) {
                System.err.println("FATAL: Failed to download all required libraries and client JAR. Launch aborted.");
                return;
            }
            
            nativesDir = extractNatives(manifest);
            
            String classpath = buildClassPath(manifest, versionId);
            String mainClass = manifest.getMainClass();
            
            String assetsIndex = (manifest.getAssetsIndex() != null) 
                                 ? manifest.getAssetsIndex().getId() 
                                 : manifest.getAssets(); 
            
            List<String> gameArguments = parseGameArguments(profile, manifest);

            List<String> command = buildLaunchCommand(profile, mainClass, classpath, assetsIndex, gameArguments, nativesDir);

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
     * downloadRequiredFiles(): تنزيل ملف الكلاينت وجميع المكتبات المطلوبة.
     */
    private boolean downloadRequiredFiles(VersionManifest manifest, String versionId) {
        System.out.println("--- Starting Resource Download Check ---");
        
        // التحقق من وجود معلومات التحميل
        if (manifest.getDownloads() == null || manifest.getDownloads().getClient() == null) {
            System.err.println("FATAL: Missing Client download information in version manifest.");
            return false;
        }

        // 1. تنزيل ملف Client JAR الرئيسي
        VersionManifest.ClientDownload clientDownload = manifest.getDownloads().getClient();
        File mainJar = new File(VERSIONS_DIR, versionId + File.separator + versionId + ".jar");
        
        if (!downloadManager.downloadFile(clientDownload.getUrl(), mainJar, clientDownload.getSha1())) {
            return false;
        }

        // 2. تنزيل جميع المكتبات (Libraries) والـ Natives
        for (VersionManifest.Library lib : manifest.getLibraries()) {
            if (!lib.appliesToCurrentOS() || lib.getDownloads() == null) {
                continue; 
            }

            // أ. تنزيل المكتبات العادية (Artifact)
            VersionManifest.Artifact artifact = lib.getDownloads().getArtifact();
            if (artifact != null && artifact.getUrl() != null) {
                 File libFile = new File(LIBRARIES_DIR, artifact.getPath());
                 if (!downloadManager.downloadFile(artifact.getUrl(), libFile, artifact.getSha1())) {
                     return false;
                 }
            }

            // ب. تنزيل Natives (Classifiers)
            String nativeId = lib.getNativeId();
            if (nativeId != null) {
                VersionManifest.Artifact nativeArtifact = lib.getDownloads().getClassifiers();
                
                if (nativeArtifact != null && nativeArtifact.getUrl() != null) {
                    // بناء مسار ملف Native JAR بناءً على الـ nativeId
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

        System.out.println("--- All core resources are ready. 🔥 ---");
        return true;
    }

    /**
     * deleteDirectory(): دالة مساعدة لحذف مجلد والمحتوى بداخله بشكل متكرر.
     */
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


    /**
     * getTemporaryNativesDir(): إنشاء مجلد مؤقت لاستخراج Natives إليه.
     */
    private File getTemporaryNativesDir() throws IOException {
        String tempDirName = "natives-" + UUID.randomUUID().toString(); 
        File tempDir = new File(VERSIONS_DIR, tempDirName); 
        
        if (!tempDir.mkdirs()) {
            throw new IOException("Failed to create temporary natives directory: " + tempDir.getAbsolutePath());
        }
        return tempDir;
    }

    /**
     * extractNatives(): استخراج الملفات التنفيذية الأصلية إلى المجلد المؤقت.
     */
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
                VersionManifest.Artifact artifact = lib.getDownloads().getArtifact();
                if (artifact != null && artifact.getPath() != null) {
                    return new File(LIBRARIES_DIR, artifact.getPath()).getAbsolutePath();
                } else {
                    // في حال عدم وجود معلومات تحميل مباشرة (وهو نادر)، نستخدم المسار القديم
                    String[] parts = lib.getName().split(":");
                    String path = parts[0].replace('.', File.separatorChar) + File.separator 
                                + parts[1] + File.separator 
                                + parts[2] + File.separator 
                                + parts[1] + "-" + parts[2] + ".jar";
                    return new File(LIBRARIES_DIR, path).getAbsolutePath();
                }
            })
            .collect(Collectors.toList());

        File mainJar = new File(VERSIONS_DIR, versionId + File.separator + versionId + ".jar");
        if (mainJar.exists()) {
            libraryPaths.add(mainJar.getAbsolutePath());
        }
        
        return String.join(File.pathSeparator, libraryPaths);
    }
    
    private List<String> parseGameArguments(Profile profile, VersionManifest manifest) {
        String argsString = manifest.getMinecraftArguments();
        
        if (argsString == null || argsString.isEmpty()) {
            System.err.println("Warning: 'minecraftArguments' is missing. Launch command may be incomplete.");
            return new ArrayList<>();
        }
        
        Account account = dataManager.getActiveAccount();
        String username = account != null ? account.getUsername() : "Player";
        // إذا كان الـ accessToken فارغاً، اللعبة ستحاول التشغيل في وضع الأوفلاين (Offline Mode)
        String uuid = account != null ? account.getUuid() : "00000000-0000-0000-0000-000000000000"; 
        String accessToken = account != null ? account.getAccessToken() : "0"; 
        
        Map<String, String> replacements = new HashMap<>();
        replacements.put("${auth_player_name}", username);
        replacements.put("${version_name}", profile.getVersionId());
        replacements.put("${game_directory}", profile.getGameDir() != null ? profile.getGameDir() : MINECRAFT_ROOT_DIR);
        replacements.put("${assets_root}", ASSETS_DIR.getAbsolutePath());
        replacements.put("${assets_index}", (manifest.getAssetsIndex() != null ? manifest.getAssetsIndex().getId() : manifest.getAssets()));
        replacements.put("${auth_uuid}", uuid);
        replacements.put("${auth_access_token}", accessToken);
        replacements.put("${user_type}", "mojang"); 
        replacements.put("${version_type}", "release"); 
        
        String resolvedArgs = argsString;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            resolvedArgs = resolvedArgs.replace(entry.getKey(), entry.getValue());
        }
        
        return Arrays.asList(resolvedArgs.split(" "));
    }

    private List<String> buildLaunchCommand(Profile profile, String mainClass, String classpath, String assetsIndex, List<String> gameArguments, File nativesDir) {
        List<String> command = new ArrayList<>();
        
        String javaExecutable = "java"; 
        if (profile.getJavaDir() != null && !profile.getJavaDir().isEmpty()) {
            javaExecutable = profile.getJavaDir();
        }
        command.add(javaExecutable); 
        
        if (nativesDir != null) {
            command.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        }
        
        // JVM Arguments
        command.add("-Xmx" + profile.getMemoryMax() + "M"); 
        if (profile.getJavaArgs() != null && !profile.getJavaArgs().isEmpty()) {
            for (String arg : profile.getJavaArgs().split(" ")) {
                command.add(arg);
            }
        }
        
        // Classpath
        command.add("-cp"); 
        command.add(classpath); 

        // Main Class
        command.add(mainClass); 
        
        // Game Arguments
        command.addAll(gameArguments);

        return command;
    }
}