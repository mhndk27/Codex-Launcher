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
        
        if (!versionJsonFile.exists()) {
            System.err.println("Error: Version JSON file not found at: " + versionJsonFile.getAbsolutePath());
            System.err.println("Please download the version files or check the Version ID in your profiles.json.");
            return;
        }

        File nativesDir = null;

        try (FileReader reader = new FileReader(versionJsonFile)) {
            VersionManifest manifest = GSON.fromJson(reader, VersionManifest.class);
            
            if (manifest == null || manifest.getMainClass() == null) {
                System.err.println("Error parsing version JSON manifest or Main Class is missing.");
                return;
            }
            
            // 1. استخراج Natives إلى مجلد مؤقت
            nativesDir = extractNatives(manifest);
            
            String classpath = buildClassPath(manifest, versionId);
            String mainClass = manifest.getMainClass();
            
            String assetsIndex = (manifest.getAssetsIndex() != null) 
                                 ? manifest.getAssetsIndex().getId() 
                                 : manifest.getAssets(); 
            
            List<String> gameArguments = parseGameArguments(profile, manifest);

            // 2. بناء الأمر مع مسار Natives
            List<String> command = buildLaunchCommand(profile, mainClass, classpath, assetsIndex, gameArguments, nativesDir);

            System.out.println("Command Structure: java -cp <CLASSPATH> " + mainClass + " [ARGS]");
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO(); 

            File workingDir = profile.getGameDir() != null ? new File(profile.getGameDir()) : new File(MINECRAFT_ROOT_DIR);
            processBuilder.directory(workingDir);
            System.out.println("Working Directory set to: " + workingDir.getAbsolutePath());

            System.out.println("Starting Minecraft process...");
            
            // 3. تشغيل العملية 
            Process process = processBuilder.start(); 
            
            // يمكنك هنا إضافة منطق لمراقبة العملية (مثلاً: حذف ملفات Natives عند الإغلاق)
            // process.waitFor(); 
            
        } catch (IOException e) {
            System.err.println("FATAL: Error running process. Details: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // يجب عليك إضافة منطق لتنظيف مجلد Natives عند انتهاء اللعبة! (تجاهلناه مؤقتاً)
        }
    }
    
    /**
     * getTemporaryNativesDir(): إنشاء مجلد مؤقت لاستخراج Natives إليه.
     */
    private File getTemporaryNativesDir() throws IOException {
        String tempDirName = "natives-" + System.currentTimeMillis();
        File tempDir = new File(VERSIONS_DIR, tempDirName); 
        
        if (tempDir.exists()) {
            // يمكن إضافة منطق حذف المجلد الموجود مسبقًا هنا
        }
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
                
                // تحويل اسم المكتبة إلى مسار محلي JAR
                String[] parts = lib.getName().split(":");
                String path = parts[0].replace('.', File.separatorChar) + File.separator 
                            + parts[1] + File.separator 
                            + parts[2] + File.separator 
                            + parts[1] + "-" + parts[2] + "-" + nativeId + ".jar"; // إضافة nativeId هنا
                
                File nativeJar = new File(LIBRARIES_DIR, path);
                
                if (nativeJar.exists()) {
                    try (JarFile jar = new JarFile(nativeJar)) {
                        Enumeration<? extends ZipEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            
                            // استثناء ملفات الميتا (Meta files)
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
        String argsString = manifest.getMinecraftArguments();
        
        if (argsString == null || argsString.isEmpty()) {
            System.err.println("Warning: 'minecraftArguments' is missing. Launch command may be incomplete.");
            return new ArrayList<>();
        }
        
        Account account = dataManager.getActiveAccount();
        String username = account != null ? account.getUsername() : "Player";
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
    
    /**
     * buildLaunchCommand(): بناء أمر تشغيل الجافا كاملاً.
     */
    private List<String> buildLaunchCommand(Profile profile, String mainClass, String classpath, String assetsIndex, List<String> gameArguments, File nativesDir) {
        List<String> command = new ArrayList<>();
        
        String javaExecutable = "java"; 
        if (profile.getJavaDir() != null && !profile.getJavaDir().isEmpty()) {
            javaExecutable = profile.getJavaDir();
        }
        command.add(javaExecutable); 
        
        // --- إضافة مسار Natives كـ JVM Argument ---
        if (nativesDir != null) {
            command.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        }
        // ---------------------------------------------
        
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