plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.android.room)
    alias(libs.plugins.litu.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
    // MigrationTestHelper reads the exported schemas; run it under Robolectric so CI needs no emulator.
    sourceSets { named("test") { assets.directories.add("$projectDir/schemas") } }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
}
