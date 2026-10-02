plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.hilt)
}

dependencies {
    implementation(project(":core:config"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
