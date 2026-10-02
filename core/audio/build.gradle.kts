plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.hilt)
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.media3.exoplayer)
    implementation(libs.play.asset.delivery)
    implementation(libs.kotlinx.coroutines.play.services)
}
