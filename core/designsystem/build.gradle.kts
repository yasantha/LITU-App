plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)

    // Screenshot tests (spec 16): light, dark and 200% font. Record with ./gradlew recordRoborazziDebug.
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
}
