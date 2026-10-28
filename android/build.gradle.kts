// 迁移后版本面：AGP 8.13.2 / Kotlin 2.4.10 / Compose BOM 2025.12.00 / compileSdk 36
// 升链原因：Backdrop 2.0.1 要求 Kotlin 2.4（元数据 2.4，旧编译器读不了）；Compose 1.12 由 backdrop 传递依赖带上来。
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
