// 版本面：AGP 9.1.0 / Kotlin 2.4.10 / Compose BOM 2025.12.00 / compileSdk 37 / Gradle 9.1.0
// 升链原因：backdrop 2.0.1 + Compose 1.12 的 AAR 元数据要求 compileSdk>=37 且 AGP>=9.1.0
// 升链原因：Backdrop 2.0.1 要求 Kotlin 2.4（元数据 2.4，旧编译器读不了）；Compose 1.12 由 backdrop 传递依赖带上来。
plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
