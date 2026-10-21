plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.peajecognitivo.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.peajecognitivo.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 19
        versionName = "3.0-MVP"
    }

    // Firma fija para debug: así cada build nuevo se instala ENCIMA del anterior
    // sin desinstalar. Es la clave de debug estándar (password "android"), no es secreta.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    // Firma de SUBIDA para Google Play (bundle .aab). Los datos vienen de variables de entorno
    // (en GitHub Actions, de los "secrets"); la clave NUNCA se guarda en el repo.
    val claveRelease = System.getenv("RELEASE_KEYSTORE_FILE")
    if (!claveRelease.isNullOrEmpty()) {
        signingConfigs {
            create("release") {
                storeFile = file(claveRelease)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Registro de diagnóstico: encendido en las pruebas (APK).
            buildConfigField("boolean", "LOG_DIAGNOSTICO", "true")
        }
        release {
            // Registro de diagnóstico: SIEMPRE apagado en lo que va a Google Play.
            buildConfigField("boolean", "LOG_DIAGNOSTICO", "false")
            if (!claveRelease.isNullOrEmpty()) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}