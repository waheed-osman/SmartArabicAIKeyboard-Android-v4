# بناء ملف APK

## الطريقة الأسهل: عبر GitHub Actions (بدون تثبيت أي شيء)
1. أنشئ مستودعاً جديداً على GitHub وارفع إليه **محتويات** هذا المجلد (وليس المجلد نفسه).
   - من المتصفح: *Add file ← Upload files*، واسحب كل الملفات والمجلدات (تأكد من ظهور المجلد المخفي `.github`).
   - أو من الطرفية:
     ```bash
     git init -b main
     git add .
     git commit -m "Initial commit"
     git remote add origin https://github.com/<USER>/<REPO>.git
     git push -u origin main
     ```
2. افتح تبويب **Actions** ← سيبدأ البناء تلقائياً (أو شغّل *Build APK* يدوياً).
3. بعد نجاحه (3-6 دقائق) افتح التشغيل ونزّل الملف من **Artifacts**: `qalam-keyboard-debug` (يحتوي ملف `app-debug.apk`).
4. انقل الملف إلى هاتفك وثبّته (فعّل «التثبيت من مصادر غير معروفة» عند الطلب).

## نسخة Release موقّعة (للنشر)
```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias qalam_key
base64 -w0 release.jks   # انسخ الناتج
```
أضف في GitHub: *Settings ← Secrets and variables ← Actions* الأسرار التالية:
`KEYSTORE_BASE64` و `KEYSTORE_PASSWORD` و `KEY_ALIAS` و `KEY_PASSWORD`.
بعدها سيُنتج كل تشغيل أيضاً Artifact باسم `qalam-keyboard-release`. **احتفظ بملف release.jks وكلمات المرور في مكان آمن؛ فقدانها يمنع تحديث التطبيق لاحقاً.**

## البناء محلياً
المتطلبات: Android Studio Ladybug أو أحدث، JDK 17، Android SDK 35.
```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # يستخدم keystore.properties إن وُجد (انظر app/build.gradle.kts)
```
مثال `keystore.properties` (مُستثنى من git):
```
storeFile=release.jks
storePassword=...
keyAlias=qalam_key
keyPassword=...
```

## التفعيل على الهاتف
1. افتح تطبيق **لوحة القلم الذكية** ← **تفعيل من الإعدادات** وفعّل اللوحة.
2. اضغط **اختيار اللوحة** وحددها.
3. أدخل مفتاح Gemini API (مجاني من Google AI Studio) في شاشة الإعدادات.
