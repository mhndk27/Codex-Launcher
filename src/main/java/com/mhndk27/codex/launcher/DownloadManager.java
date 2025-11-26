package com.mhndk27.codex.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException; // <--- تم إضافة الاستيرادات اللازمة للـ SHA1

public class DownloadManager {

    /**
     * downloadFile(): تنزيل ملف من URL معين والتحقق من الـ SHA1.
     */
    public boolean downloadFile(String downloadUrl, File targetFile, String expectedSha1) {
        if (targetFile.exists()) {
            if (checkSha1(targetFile, expectedSha1)) {
                System.out.println("File exists and SHA1 matches: " + targetFile.getName());
                return true;
            }
            System.out.println("File exists but SHA1 mismatch. Re-downloading: " + targetFile.getName());
            targetFile.delete(); // حذف الملف التالف لضمان إعادة التنزيل
        }

        System.out.println("Downloading: " + targetFile.getName() + " from " + downloadUrl);

        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        try {
            // استخدام URI.toURL() لتجنب تحذير الدالة القديمة (Deprecated)
            URL url = new URI(downloadUrl).toURL();
            
            try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                 FileOutputStream fos = new FileOutputStream(targetFile)) {
                
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                System.out.println("Download successful: " + targetFile.getAbsolutePath());
                
                if (checkSha1(targetFile, expectedSha1)) {
                    return true;
                } else {
                    System.err.println("FATAL: SHA1 verification failed after download for " + targetFile.getName());
                    targetFile.delete(); // حذف الملف التالف
                    return false;
                }

            }

        } catch (URISyntaxException e) {
            System.err.println("Download failed for " + targetFile.getName() + ". Invalid URL syntax: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("Download failed for " + targetFile.getName() + ". I/O Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * checkSha1(): دالة لحساب هاش SHA1 للملف والتحقق منه.
     */
    private boolean checkSha1(File file, String expectedSha1) {
        if (expectedSha1 == null || expectedSha1.isEmpty()) {
            // إذا لم يكن هناك SHA1 متوقع (وهو نادر في موارد ماينكرافت)، نفترض أن الملف صحيح
            return true;
        }
        
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            
            byte[] sha1Hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : sha1Hash) {
                // تحويل البايت إلى تنسيق سداسي عشري (Hex) من خانتين
                sb.append(String.format("%02x", b));
            }
            
            String actualSha1 = sb.toString();
            if (actualSha1.equals(expectedSha1)) {
                 return true;
            } else {
                 System.err.println("SHA1 Mismatch! Expected: " + expectedSha1 + ", Actual: " + actualSha1);
                 return false;
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error checking SHA1 for " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }
}