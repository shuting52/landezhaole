// 迁移后版本面：AGP 8.13.2 / Kotlin 2.3.10 / Compose BOM 2025.12.00 / compileSdk 36
// 升链原因：Backdrop 1.0.6 要求 Kotlin 2.3 + Compose 1.10，旧面（Kotlin 1.9 / Compose 1.7）读不了它的元数据。
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
}
