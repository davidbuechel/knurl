// Models, the exercise catalog and every calculation (1RM, volume, progression, records, plates).
// No Android or Apple APIs in commonMain: time comes from kotlin.time and kotlinx-datetime.
// The iOS targets compile on a Mac; on Windows they are skipped.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    android {
        namespace = "de.knurl.shared"
        compileSdk = 37
        minSdk = 29
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
