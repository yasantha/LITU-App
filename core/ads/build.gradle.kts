plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.android.compose)
    alias(libs.plugins.litu.hilt)
}

android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        // AdMob ad unit IDs for this app, from the AdMob console. Set LITU_ADMOB_BANNER_ID and
        // LITU_ADMOB_INTERSTITIAL_ID in gradle.properties or the environment. Debug builds always use
        // Google's test units; release builds without real IDs show no ads.
        fun prop(name: String) = providers.gradleProperty(name).orElse(providers.environmentVariable(name)).getOrElse("")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${prop("LITU_ADMOB_BANNER_ID")}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${prop("LITU_ADMOB_INTERSTITIAL_ID")}\"")
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:billing"))
    api(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
