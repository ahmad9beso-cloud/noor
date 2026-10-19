# نور — Noor

تطبيق إسلامي لأندرويد: **المصحف** (بتخطيط مصحف المدينة، تقليب أو تمرير) و**الأذكار** و**القبلة** و**مواقيت الصلاة**
مع إشعارات هادئة بلا أذان ولا صوت. واجهة عربية فقط، وضع نهاري وليلي، يعمل دون إنترنت ولا يجمع أي بيانات.

An offline, Arabic-only Islamic app for Android: Mushaf reader (Madinah layout, page-flip or scroll, bookmarks per
surah/ayah), adhkar with counters, qibla compass, prayer times with silent notifications. Native Kotlin + Jetpack
Compose, arm64-v8a only, no INTERNET permission.

> ⚠ اقرأ [NOTICE.md](NOTICE.md) قبل النشر العلني: مصدر نص المصحف وترخيصه بحاجة إلى حسم، ولم تتم مراجعته بشرياً بعد.

## البناء على GitHub

1. ارفع المشروع إلى مستودع GitHub (الفرع `main`).
2. تبويب **Actions ← Build APK** يبني تلقائياً مع كل push. الملف الناتج في **Artifacts** باسم `noor-arm64-release`.
3. وسم بصيغة `v1.0.0` ينشئ إصداراً (Release) ويرفق الـ APK به.

### التوقيع (مرة واحدة)

بلا إعداد، يُوقَّع الـ APK بمفتاح تجريبي: يثبَّت عادي، لكن التحديث فوق نسخة موقّعة بمفتاح آخر يتطلب حذفها أولاً.
لتوقيع ثابت أنشئ مفتاحاً **واحتفظ به في مكان آمن** (إن ضاع لن تستطيع تحديث التطبيق):

```bash
keytool -genkeypair -v -keystore noor.jks -alias noor -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 noor.jks   # الناتج يوضع في secret باسم KEYSTORE_BASE64
```

ثم أضف في **Settings ← Secrets and variables ← Actions**: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## البناء محلياً

JDK 17 وأندرويد SDK 35:

```bash
./gradlew assembleRelease          # app/build/outputs/apk/release/
```

لدعم معماريات أخرى عدّل `abiFilters` في `app/build.gradle.kts`.

## إعادة توليد البيانات

ملفات `assets/` و`res/font/` مولَّدة وموجودة في المستودع. لإعادة توليدها (يحتاج Python 3 وnpm وإنترنت):

```bash
python3 tools/build_assets.py
```

## هيكل المشروع

| المسار | المحتوى |
|---|---|
| `logic/` | حساب المواقيت (فلكي، بلا إنترنت)، اتجاه القبلة، التاريخ الهجري — Kotlin صرف |
| `data/` | تحميل المصحف والأذكار، الإعدادات، قائمة المدن |
| `notify/` | تنبيه صامت قبل كل صلاة (AlarmManager) ويُعاد ضبطه بعد الإقلاع والتحديث وتغيّر الوقت |
| `ui/` | شاشات Compose: الرئيسية، المصحف، الأذكار، القبلة، الإعدادات |

## ملاحظات تصميم

* **المصحف**: 604 صفحة × 15 سطراً كما في مصحف المدينة. كل كلمة عنصر مستقل، والسطر يُضبط بتوزيع الفراغات
  (خط أميري قرآن)، وتُقاس عروض الكلمات وقت التشغيل ليتّسع كل سطر دون قصّ.
* **المواقيت**: 8 طرق حساب، العصر جمهور/حنفي، وتعديل التاريخ الهجري ±2 يوم.
* **الموقع**: بلا خدمات Google؛ إما GPS/شبكة من الجهاز أو اختيار مدينة من قائمة مدمجة بلا أي إذن.
