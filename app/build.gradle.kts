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
        versionCode = 19
        versionName = "1.3.7"
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
    implementation("com.caverock:androidsvg-aar:1.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
