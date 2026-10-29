plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

android {
    namespace = "com.landezhaole.glasstest"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.landezhaole.glasstest"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.animation)
    // 真液态玻璃：Kyant0/AndroidLiquidGlass 的 Backdrop 库，见上传的 2.0.1 安卓完整代码
    implementation("io.github.kyant0:backdrop-android:2.0.1")
    implementation("io.github.kyant0:shapes:1.2.1")
}
