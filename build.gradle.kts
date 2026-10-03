// Top-level build file. Module configuration lives in the convention plugins under build-logic/.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.android.asset.pack) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.play.publisher) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.detekt)
}

// Static analysis on every module (spec section 15: ci.yml runs lint and detekt).
allprojects {
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        parallel = true
        baseline = file("detekt-baseline.xml")
        source.setFrom("src/main/kotlin", "src/test/kotlin")
    }
    // detekt 1.23 cannot parse newer JVM targets; analysis does not depend on it.
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach { jvmTarget = "17" }
}
