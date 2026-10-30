plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "ir.hajarrajaee.booking"
    compileSdk = 35
    defaultConfig {
        applicationId = "ir.hajarrajaee.booking"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.2.0-beta1"
        buildConfigField("String", "API_BASE_URL", "\"https://hajarrajaee.ir/api/v1\"")
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".beta"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.15.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
