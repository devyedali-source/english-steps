plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.elyedali.englishsteps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.elyedali.englishsteps"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    // مفتاح التوقيع الثابت للمتجر: يأتي لاحقًا من أسرار GitHub (KEYSTORE_BASE64 و KEYSTORE_PASSWORD)
    val keystoreFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/english-upload-key.jks")
    val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
    val hasKeystore = keystoreFile.exists() && !keystorePassword.isNullOrEmpty()

    signingConfigs {
        // مفتاح التجربة الثابت (للتجربة فقط وليس للمتجر)
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasKeystore) {
            create("upload") {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = "upload"
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasKeystore) signingConfigs.getByName("upload") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
