plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.android.room)
    alias(libs.plugins.litu.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
