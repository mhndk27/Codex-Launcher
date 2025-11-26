package com.mhndk27.codex;

import javafx.application.Application; // هذا الكلاس الأساسي اللي لازم نرث (Inherit) منه عشان نسوي تطبيق واجهة
import javafx.fxml.FXMLLoader;     // كلاس يستخدم لتحميل ملفات تصميم الواجهة (main-view.fxml)
import javafx.scene.Scene;         // المشهد (Scene) اللي يحمل الواجهة الرسومية داخل النافذة
import javafx.stage.Stage;         // النافذة الرئيسية للتطبيق (مثل شاشة اللانشر)

import java.io.IOException;        // لمعالجة الأخطاء المحتملة أثناء قراءة الملفات

// MainApp يرث من Application: هذا يعني أن JavaFX يعرف كيف يبدأ تشغيل هذا الكلاس كنافذة
public class MainApp extends Application {
    
    // @Override: تعني أننا نغير طريقة عمل دالة start() الموجودة أصلاً في الكلاس الأب
    // start(Stage stage): هي الدالة اللي يناديها نظام JavaFX أول ما يبدأ التطبيق
    @Override
    public void start(Stage stage) throws IOException {
        // FXMLLoader: كائن يستخدم لتحميل (قراءة) ملف تصميم الواجهة main-view.fxml
        // getResource: يجلب ملف الواجهة من مجلد resources
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("main-view.fxml"));
        
        // Scene: ننشئ مشهد جديد بحجم 800 بكسل عرض و 500 بكسل طول
        // fxmlLoader.load(): يقوم ببناء عناصر الواجهة من ملف FXML
        Scene scene = new Scene(fxmlLoader.load(), 800, 500);
        
        // stage: هو النافذة الرئيسية اللي تظهر للمستخدم
        stage.setTitle("Codex Launcher - مهند"); // وضع عنوان للنافذة
        stage.setScene(scene); // إدخال المشهد (الواجهة) داخل النافذة
        stage.show(); // عرض النافذة على الشاشة
    }

    // main(String[] args): الدالة الرئيسية اللي تبدأ تشغيل أي برنامج جافا تقليدي
    public static void main(String[] args) {
        // launch(): هذا الأمر يبدأ عملية JavaFX الداخلية، وفي النهاية ينادي دالة start(Stage)
        launch();
    }
}