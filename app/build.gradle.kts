plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val apiUrl = providers.gradleProperty("sigelabApiUrl")
    .orElse("https://sigelab-api-umg-e8e6g9chenbchzh2.canadacentral-01.azurewebsites.net")
    .get()
android {
    namespace = "gt.edu.sigelab.mobile"
    compileSdk = 37
    defaultConfig {
        applicationId = "gt.edu.sigelab.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.3.0"
        buildConfigField("String", "API_URL", "\"${apiUrl.trimEnd('/')}/\"")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
