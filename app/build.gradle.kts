plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.goodrawru.myposev"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "ru.goodrawru.myposev"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "1.2.9"
    }

    // Release signing is enabled only when GitHub Actions secrets are supplied.
    // Never store the keystore or passwords in the repository.
    val signingStoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
    val signingStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val signingKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
    val signingKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")

    if (!signingStoreFile.isNullOrBlank() &&
        !signingStorePassword.isNullOrBlank() &&
        !signingKeyAlias.isNullOrBlank() &&
        !signingKeyPassword.isNullOrBlank()
    ) {
        signingConfigs {
            create("release") {
                storeFile = file(signingStoreFile)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = false
            }
        }
    } else {
        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // Media3 1.5.1 is compatible with this project's compileSdk 35 / AGP 8.7.3.
    val media3Version = "1.5.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("com.caverock:androidsvg-aar:1.4")
}
