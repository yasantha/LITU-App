plugins {
    alias(libs.plugins.litu.android.library)
    alias(libs.plugins.litu.hilt)
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:config"))
    implementation(project(":core:billing"))
    implementation(project(":core:analytics"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
}
