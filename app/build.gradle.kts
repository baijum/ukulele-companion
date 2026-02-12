import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.baijum.ukufretboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.baijum.ukufretboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 39
        versionName = "5.9"
    }

    if (keystorePropertiesFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile", ""))
                storePassword = keystoreProperties.getProperty("storePassword", "")
                keyAlias = keystoreProperties.getProperty("keyAlias", "")
                keyPassword = keystoreProperties.getProperty("keyPassword", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.core.ktx)
    implementation(libs.work.runtime)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.reorderable)
    debugImplementation(libs.ui.tooling)
}
