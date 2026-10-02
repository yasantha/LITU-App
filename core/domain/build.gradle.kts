plugins {
    alias(libs.plugins.litu.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
    testImplementation(libs.turbine)
}
