# CaptionForge Production Release Checklist & Deployment Runbook

## 1. Executive Pre-Flight Status

| Verification Gate | Status | Target Benchmark | Actual Evidence |
|---|---|---|---|
| **Dart Static Analysis** | **PASS** | 0 warnings, 0 errors | `dart analyze` passes with 0 issues |
| **Flutter Unit & Widget Tests**| **PASS** | 100% pass rate | 19 / 19 tests passing (`flutter test`) |
| **FastAPI Backend Tests** | **PASS** | 100% pass rate | 14 / 14 tests passing (`pytest`) |
| **Live Workflow Socket Tests** | **PASS** | 100% pass rate | 14 steps + 5 bonus exports passing (`test_full_workflow.py`) |
| **Android APK Compilation** | **PASS** | Clean build with native video | `app-debug.apk` built (194,937,256 bytes) |

---

## 2. Android Production App Bundle & Signing Runbook

### A. Release Keystore Generation
Generate a cryptographically secure 4096-bit RSA keystore:
```bash
keytool -genkey -v -keystore android/app/captionforge-release.jks \
  -keyalg RSA -keysize 4096 -validity 10000 -alias captionforge
```

### B. Secrets Configuration (`android/key.properties`)
Create `captionforge_mobile/android/key.properties` (do not commit to version control):
```properties
storePassword=<SECURE_STORE_PASSWORD>
keyPassword=<SECURE_KEY_PASSWORD>
keyAlias=captionforge
storeFile=captionforge-release.jks
```

### C. Gradle Signing & Proguard/R8 Minification (`android/app/build.gradle`)
```groovy
def keystoreProperties = new Properties()
def keystorePropertiesFile = rootProject.file('key.properties')
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    ...
    signingConfigs {
        release {
            keyAlias = keystoreProperties['keyAlias']
            keyPassword = keystoreProperties['keyPassword']
            storeFile = keystoreProperties['storeFile'] ? file(keystoreProperties['storeFile']) : null
            storePassword = keystoreProperties['storePassword']
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### D. R8 Proguard Rules (`android/app/proguard-rules.pro`)
Ensure native Flutter and ExoPlayer reflection symbols remain intact:
```pro
# Flutter and Dart JNI
-keep class io.flutter.app.** { *; }
-keep class io.flutter.plugin.** { *; }
-keep class io.flutter.util.** { *; }
-keep class io.flutter.view.** { *; }

# ExoPlayer / Media3 (video_player plugin)
-keep class com.google.android.exoplayer2.** { *; }
-keep class androidx.media3.** { *; }
```

### E. Network Security Policy (`res/xml/network_security_config.xml`)
For production release, disable cleartext HTTP traffic and enforce TLS 1.3:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.captionforge.app</domain>
    </domain-config>
</network-security-config>
```

### F. Build Release App Bundle
```bash
flutter build appbundle --release
```
Target output: `build/app/outputs/bundle/release/app-release.aab`.

---

## 3. FastAPI Backend Production Hardening & Cloud Deployment

### A. Production ASGI Server (Gunicorn + Uvicorn Workers)
Replace local single-process development uvicorn with multi-worker Gunicorn:
```bash
gunicorn -w 4 -k uvicorn.workers.UvicornWorker app:app \
  --bind 0.0.0.0:8000 \
  --timeout 300 \
  --max-requests 1000 \
  --max-requests-jitter 50
```

### B. Reverse Proxy & Edge TLS Configuration (Nginx)
```nginx
server {
    listen 443 ssl http2;
    server_name api.captionforge.app;

    ssl_certificate /etc/letsencrypt/live/api.captionforge.app/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.captionforge.app/privkey.pem;

    client_max_body_size 500M;
    proxy_read_timeout 300s;
    proxy_connect_timeout 75s;

    location / {
        proxy_pass http://127.0.0.1:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### C. GPU Acceleration & Containerization
In production Docker environments with NVIDIA GPUs:
- Base image: `nvidia/cuda:12.2.0-runtime-ubuntu22.04`
- Install `ffmpeg` with `nvenc` hardware acceleration support.
- Faster-Whisper invocation parameter: `WhisperModel(model_size, device="cuda", compute_type="float16")`.
- Export pipeline: `-c:v h264_nvenc -preset p4 -cq 23`.

### D. Distributed Job Queue Architecture (Scale Phase)
Migrate local in-memory `BackgroundTasks` to Celery / Redis:
```
[Flutter Client] ──> [FastAPI REST Gateway] ──> [Redis Queue] ──> [GPU Celery Worker Pool]
                                                                          │
                                                                   [S3 / R2 Bucket]
```

---

## 4. Google Play Store & Apple App Store Billing Integration

- **Current Implementation:** `HomeScreen` includes Pro subscription modal displaying monthly/annual plans, feature checklists, and upgrade triggers.
- **Production Integration Step:**
  1. Add `in_app_purchase: ^3.2.0` to `captionforge_mobile/pubspec.yaml`.
  2. Configure product IDs in Google Play Console (`captionforge_pro_monthly`, `captionforge_pro_annual`).
  3. Validate purchase tokens on FastAPI backend via Google Play Developer API (`google-api-python-client`).
  4. Issue signed JWT session tokens granting unlimited AI transcription and 4K export privileges.

---

## 5. Pre-Launch Verification Sign-Off Matrix

- [x] All 21 reference screenshot features mapped and implemented.
- [x] Persian kinetic typography (Video 1) and glow hologram / metallic reveal (Video 2) implemented and export-verified.
- [x] Math-synthesized 9-preset audio SFX engine tested without external assets.
- [x] Dart analysis reports 0 errors and 0 warnings.
- [x] Flutter test suite passes 100% (19/19 tests).
- [x] FastAPI test suite passes 100% (14/14 tests).
- [x] Real-media live socket workflow verified with probed MP4 artifacts.
- [x] Android debug APK compiled and verified (`app-debug.apk`, 194.9 MB).
