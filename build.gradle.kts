plugins {
    id("com.android.application") version "9.5.1" apply false
    // AGP 9 compiles Kotlin itself (android.builtInKotlin); Compose and serialization are compiler plugins.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
    // :shared is Kotlin Multiplatform, so an iOS app can reuse models and calculations later.
    id("org.jetbrains.kotlin.multiplatform") version "2.4.10" apply false
    id("com.android.kotlin.multiplatform.library") version "9.5.1" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}
