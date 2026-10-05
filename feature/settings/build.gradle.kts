plugins {
    alias(libs.plugins.litu.android.feature)
}

dependencies {
    implementation(project(":core:billing"))
    implementation(project(":core:sync"))
    implementation(project(":core:analytics"))
    implementation(project(":core:ads"))
}
