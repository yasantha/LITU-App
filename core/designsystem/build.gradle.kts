plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.android.compose)
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
}
