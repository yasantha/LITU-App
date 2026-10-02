plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.android.room)
    alias(libs.plugins.litu.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.serialization.json)
}
