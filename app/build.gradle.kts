plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

android {
    namespace = "com.example.myapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.myapplication.watch"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
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
    buildFeatures {
        compose = true
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.unit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")

    implementation("androidx.wear.compose:compose-material:1.3.1")
    implementation("androidx.wear.compose:compose-foundation:1.3.1")


    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation ("androidx.compose.ui:ui-tooling")
    implementation ("androidx.compose.ui:ui-tooling-preview")
}
