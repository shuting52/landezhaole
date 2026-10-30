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
        versionCode = 6
        versionName = "2.1.0"
    }

    // 固定签名：CI 里用 GitHub Secrets 的 keystore 编 release 包，V1 + V2 + V3 全开。
    // 本地没有 KEYSTORE_PATH 时不设 release 签名，编出来就是 debug 包，不影响 CI。
    signingConfigs {
        if (System.getenv("KEYSTORE_PATH") != null) {
            create("release") {
                storeFile = file(System.getenv("KEYSTORE_PATH"))
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.material3:material3")
}
