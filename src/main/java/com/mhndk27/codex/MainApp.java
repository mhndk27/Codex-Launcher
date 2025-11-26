package com.mhndk27.codex;

import com.mhndk27.codex.data.DataManager; // استيراد كلاس إدارة البيانات
import com.mhndk27.codex.data.Profile;     // استيراد كلاس البروفايل
import com.mhndk27.codex.launcher.MinecraftLauncher; // استيراد كلاس المشغل

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainApp extends Application implements Initializable {
    
    // تم تغيير نوع القائمة المنسدلة من <String> إلى <Profile>
    @FXML
    private ComboBox<Profile> versionSelector; 
    @FXML
    private Label statusLabel;               
    
    // كائن (Object) من كلاس DataManager للتحكم في ملفات JSON
    private final DataManager dataManager = new DataManager(); 
    // كائن (Object) من كلاس MinecraftLauncher لبدء التشغيل
    private final MinecraftLauncher launcher = new MinecraftLauncher();
    
    // start(Stage stage): الدالة اللي يناديها JavaFX أول ما يبدأ التطبيق
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 500);
        
        stage.setTitle("Codex Launcher - مهند 🔥"); 
        stage.setScene(scene); 
        stage.show();
    }

    // initialize(): يتم تنفيذها بعد تحميل كل عناصر الواجهة.
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("GUI loaded. Loading profiles and syncing instances...");
        
        // 1. جلب قائمة البروفايلات المحدثة (بعد المزامنة)
        List<Profile> availableProfiles = dataManager.loadProfiles();
        
        // 2. تحديث قائمة الـ ComboBox
        if (!availableProfiles.isEmpty()) {
            // إضافة كائنات Profile إلى القائمة المنسدلة
            versionSelector.getItems().addAll(availableProfiles);
            
            versionSelector.setPromptText("اختر بروفايل...");
            // تحديد أول بروفايل كافتراضي
            versionSelector.getSelectionModel().selectFirst(); 
            
            statusLabel.setText("الحالة: " + availableProfiles.size() + " بروفايل متوفر. 😎");
        } else {
            versionSelector.setPromptText("💀 لا توجد بروفايلات!");
            statusLabel.setText("الحالة: لا توجد بروفايلات (قم بتثبيت إصدار أو instance).");
        }
    }
    
    /**
     * onLaunchButtonClick(): دالة يتم تنفيذها عند الضغط على زر "تشغيل ماين كرافت 🔥"
     */
    @FXML
    protected void onLaunchButtonClick() {
        // getItem(): يجلب كائن البروفايل (Profile object) المختار
        Profile selectedProfile = versionSelector.getSelectionModel().getSelectedItem();
        
        if (selectedProfile == null) {
            statusLabel.setText("💀 خطأ: يجب اختيار بروفايل أولاً.");
            return;
        }

        // 1. استخراج البيانات من كائن البروفايل
        String versionId = selectedProfile.getVersionId();
        String gameDir = selectedProfile.getGameDir();
        String activeUsername = dataManager.getActiveAccount() != null ? dataManager.getActiveAccount().getUsername() : "Player";
        
        statusLabel.setText("الحالة: جاري تحضير " + selectedProfile.getName() + "...");
        System.out.println("🚀 Selected Profile: " + selectedProfile.getName());
        System.out.println("Version ID: " + versionId + ", Game Dir: " + gameDir);
        
        // 2. استدعاء مشغل اللعبة
        // ملاحظة: سنمرر كل إعدادات البروفايل اللازمة في الخطوات القادمة
        launcher.launch(versionId, activeUsername); 
    }

    // ... دالة main تبقى كما هي ...
    public static void main(String[] args) {
        launch();
    }
}