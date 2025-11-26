package com.mhndk27.codex;

import com.mhndk27.codex.data.DataManager; 
import com.mhndk27.codex.data.Profile;     
import com.mhndk27.codex.launcher.MinecraftLauncher; 

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
    
    @FXML
    private ComboBox<Profile> versionSelector; 
    @FXML
    private Label statusLabel;               
    
    private final DataManager dataManager = new DataManager(); 
    private final MinecraftLauncher launcher = new MinecraftLauncher();
    
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 500);
        
        stage.setTitle("Codex Launcher - مهند 🔥"); 
        stage.setScene(scene); 
        stage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("GUI loaded. Loading profiles and syncing instances...");
        
        List<Profile> availableProfiles = dataManager.loadProfiles();
        
        if (!availableProfiles.isEmpty()) {
            versionSelector.getItems().addAll(availableProfiles);
            versionSelector.setPromptText("اختر بروفايل...");
            versionSelector.getSelectionModel().selectFirst(); 
            
            statusLabel.setText("الحالة: " + availableProfiles.size() + " بروفايل متوفر. 😎");
        } else {
            versionSelector.setPromptText("💀 لا توجد بروفايلات!");
            statusLabel.setText("الحالة: لا توجد بروفايلات (قم بتثبيت إصدار أو instance).");
        }
    }
    
    /**
     * onLaunchButtonClick(): تم إصلاح الاستدعاء لتمرير كائن Profile كاملاً.
     */
    @FXML
    protected void onLaunchButtonClick() {
        Profile selectedProfile = versionSelector.getSelectionModel().getSelectedItem();
        
        if (selectedProfile == null) {
            statusLabel.setText("💀 خطأ: يجب اختيار بروفايل أولاً.");
            return;
        }
        
        // --- التعديل هنا ---
        // 1. استخراج البيانات الضرورية
        String activeUsername = dataManager.getActiveAccount() != null ? dataManager.getActiveAccount().getUsername() : "Player";
        
        statusLabel.setText("الحالة: جاري تحضير " + selectedProfile.getName() + "...");
        System.out.println("🚀 Selected Profile: " + selectedProfile.getName());
        System.out.println("Version ID: " + selectedProfile.getVersionId() + ", Game Dir: " + selectedProfile.getGameDir());
        
        // 2. تمرير كائن Profile مباشرةً لدالة launch
        launcher.launch(selectedProfile, activeUsername); 
        // ------------------
    }

    public static void main(String[] args) {
        launch();
    }
}