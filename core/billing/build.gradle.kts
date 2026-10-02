plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.hilt)
}

android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        // RevenueCat public SDK key (Google). Set LITU_REVENUECAT_KEY in gradle.properties or the
        // environment; without it debug builds use a local test store and release shows billing unavailable.
        val key = providers.gradleProperty("LITU_REVENUECAT_KEY").orElse(providers.environmentVariable("LITU_REVENUECAT_KEY")).getOrElse("")
        buildConfigField("String", "REVENUECAT_API_KEY", "\"$key\"")
    }
}

dependencies {
    implementation(libs.revenuecat.purchases)
}
