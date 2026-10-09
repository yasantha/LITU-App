plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.hilt)
}

android {
    buildFeatures { buildConfig = true }
    buildTypes {
        // RevenueCat public SDK key (Google Play, "goog_…"). Set LITU_REVENUECAT_KEY in gradle.properties
        // or the environment. Only release builds use it: debug builds always get the local test store,
        // and release without a key shows billing unavailable.
        debug { buildConfigField("String", "REVENUECAT_API_KEY", "\"\"") }
        release {
            val key = providers.gradleProperty("LITU_REVENUECAT_KEY").orElse(providers.environmentVariable("LITU_REVENUECAT_KEY")).getOrElse("")
            buildConfigField("String", "REVENUECAT_API_KEY", "\"$key\"")
        }
    }
}

dependencies {
    implementation(libs.revenuecat.purchases)
}
