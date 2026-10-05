import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * Release signing is optional. The keys come from keystore.properties in the
 * root of the project, which is not in the repository. Without that file the
 * release build still runs and makes an unsigned APK, so a fresh clone always
 * builds.
 */
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasSigning = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "dev.equwal.inkupdate"
    compileSdk = 36

    // No list of dependencies, encrypted for Google alone, in the signed APK.
    // F-Droid does not accept it, and it would stop the reproducible build check.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "dev.equwal.inkupdate"
        minSdk = 31
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.2"
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 removes unused code and resources. F-Droid asks for it.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// The APK has no dependency. The app uses java.net.HttpURLConnection for the
// requests, org.json from the platform for the answers, and JobScheduler for
// the daily check.
dependencies {
    // Tests only, never in the APK. android.jar gives the unit tests an
    // org.json that throws, so the tests use the real one.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
