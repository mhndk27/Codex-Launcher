CodexLauncher — Small refactor and project structure

ملخص التغييرات:
- نظمت الكلاسات إلى حزم (packages) أكثر ترتيبًا:
  - `com.mhndk27.codex` — يحتوي على `MainApp` وملفات غلاف (wrappers) للحفاظ على التوافق.
  - `com.mhndk27.codex.ui` — واجهة المستخدم: `Controller` (FXML controller).
  - `com.mhndk27.codex.downloader` — مسؤوليات تنزيل الملفات: `MetadataDownloader`, `LibraryDownloader`, `AssetsDownloader`.
  - `com.mhndk27.codex.launch` — منطق تشغيل اللعبة: `MinecraftLauncher`.
  - `com.mhndk27.codex.util` — أدوات مساعدة: `ZipUtils`, `GsonFactory`.

ملاحظات تقنية:
- أضفت ملفات "wrapper" بسيطة في الحزمة الأصلية `com.mhndk27.codex` لتسهيل التوافق مع أي رمز قد يعتمد على المسارات القديمة.
- قمت بتحديث ملف FXML (`src/main/resources/com/mhndk27/codex/main-view.fxml`) ليشير إلى `com.mhndk27.codex.ui.Controller`.

كيفية التشغيل (Windows PowerShell):

1) بناء المشروع وتشغيل الواجهة عبر Maven (يتطلب JDK + Maven مثبتين):

```powershell
mvn clean install javafx:run
```

2) الواجهة بسيطة — اضغط `Launch Minecraft` لتجربة سير التحضير (سيستخدم إعدادات وهمية في `Controller`).

ملاحظات إضافية:
- المسارات والحسابات والإعدادات الحالية في `Controller` هي بيانات ثابتة (mock). يمكنك فصل إعدادات المستخدم إلى نموذج (`model`) أو واجهة إعداد لاحقًا.
- المكتبات والأصول تُحمّل وتُخزن محليًا تحت `libraries/` و`assets/` كما هو موضح في الكود.

إذا تبي أكمّل لك التالي:
- فصل إعدادات المستخدم (حفظ / اختيار حسابات).
- واجهة لاختيار نسخة ومجلد اللعبة.
- تحسين معالج التنزيل لعرض تقدم تنزيل لكل ملف.

أخبرني ايش تبغى أعمل بعدين، وأقدر أطبّق التغييرات أو أشغل البناء وأراجع أي أخطاء تظهر.
