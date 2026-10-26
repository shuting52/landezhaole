plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
// 签名密钥文件路径（CI 上由 Secrets 还原，本地没有时为 null，不挂签名）
val keystoreFile = System.getenv("KEYSTORE_PATH")?.let { file(it) }

android {
    namespace = "com.landezhaole.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.landezhaole.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 20
        versionName = "1.3.1"
        vectorDrawables { useSupportLibrary = true }
    }
    signingConfigs {
        create("release") {
            // 统一签名：密钥只存在 GitHub Actions 密文里，从不进仓库、不进代码。
            // 本地没配 KEYSTORE_PATH 时不挂签名配置，CI 上一定有。
            storeFile = keystoreFile
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }
    packaging {
        resources {
            pickFirsts += "META-INF/kotlin-project-structure-metadata.json"
            excludes += "META-INF/*.kotlin_module"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true")
    }
}
dependencies {
    constraints {
        implementation("androidx.collection:collection:1.4.4")
        implementation("androidx.collection:collection-ktx:1.4.4")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    }
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
