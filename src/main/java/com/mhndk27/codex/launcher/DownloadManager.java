package com.mhndk27.codex.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class DownloadManager {

    /**
     * downloadFile(): دالة بسيطة لتنزيل ملف من URL معين.
     * لا تشمل التحقق من التجزئة (Hash/SHA1) بعد.
     */
    public boolean downloadFile(String downloadUrl, File targetFile) {
        if (targetFile.exists()) {
            // يمكن هنا إضافة منطق التحقق من الـ SHA1
            System.out.println("File already exists: " + targetFile.getName());
            return true;
        }

        System.out.println("Downloading: " + targetFile.getName() + " from " + downloadUrl);

        // التأكد من وجود مجلد الأب
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        try (ReadableByteChannel rbc = Channels.newChannel(new URL(downloadUrl).openStream());
             FileOutputStream fos = new FileOutputStream(targetFile)) {
            
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            System.out.println("Download successful: " + targetFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            System.err.println("Download failed for " + targetFile.getName() + ". Error: " + e.getMessage());
            return false;
        }
    }
}