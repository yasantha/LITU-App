
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

// Macrobenchmark for cold start and question scrolling, and the Baseline Profile generator
// (spec sections 6 and 16). Run on a device:
//   ./gradlew :benchmark:connectedDevBenchmarkReleaseAndroidTest
//   ./gradlew :app:generateProdReleaseBaselineProfile
android {
    namespace = "com.myday.litu.benchmark"
    compileSdk = 37
    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            buildConfigField("String", "TARGET_PACKAGE", "\"com.myday.litu.dev\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "TARGET_PACKAGE", "\"com.myday.litu\"")
        }
    }
    targetProjectPath = ":app"
    buildFeatures { buildConfig = true }
    // Emulators give noisy timings; real numbers come from the reference phones (spec 14).
    defaultConfig.testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro)
}
