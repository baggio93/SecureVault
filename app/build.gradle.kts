plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt") // <-- AGGIUNGI QUESTA RIGA QUI
}

android {
    namespace = "com.baggioak.securevault"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.baggioak.securevault"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/io.netty.versions.properties" // <-- AGGIUNGI QUESTA RIGA
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.appdistribution.gradle)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Retrofit per le chiamate API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Convertitore JSON (Gson) per Retrofit
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp per gestire la connessione e i Cookie (Sessioni PHP)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Gestione integrata dei Cookie
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0") // Aggiungi questa se manca
    implementation("androidx.biometric:biometric:1.1.0")
}