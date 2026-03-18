plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "pictures.tristagram.lina.bodytune"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "pictures.tristagram.lina.bodytune"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Работа с БД Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-common:${roomVersion}")

    // Графики (аналог Chart.js)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Material Design компоненты
    implementation("com.google.android.material:material:1.11.0")
}