import java.util.Properties

plugins {
    alias(libs.plugins.litu.android.application)
    alias(libs.plugins.litu.android.compose)
    alias(libs.plugins.litu.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.play.publisher)
    alias(libs.plugins.baselineprofile)
}

// Firebase is configured per flavour: app/src/dev/google-services.json (litu-dev) and
// app/src/prod/google-services.json (litu-prod). Without them the app runs fully offline with
// backup, Remote Config, Crashlytics and Analytics turned off.
val hasFirebaseConfig = listOf("dev", "prod").any { file("src/$it/google-services.json").exists() }
if (hasFirebaseConfig) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

// Upload key: keystore.properties locally, or LITU_* environment variables in CI (spec section 15).
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? = keystoreProperties.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.myday.litu"

    defaultConfig {
        // Package name is open decision 1 in the spec.
        applicationId = "com.myday.litu"
        versionCode = 1
        versionName = "1.0.0"
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Life in the UK Test Prep (Dev)")
            resValue("string", "launcher_name", "UK Test Dev")
        }
        create("prod") {
            dimension = "env"
            // Contains the phrase learners search for; "Prep" keeps it clearly unofficial (spec 24).
            resValue("string", "app_name", "Life in the UK Test Prep")
            resValue("string", "launcher_name", "UK Test Prep")
        }
    }

    signingConfigs {
        create("upload") {
            val store = signingValue("storeFile", "LITU_UPLOAD_STORE_FILE")
            if (store != null) {
                storeFile = rootProject.file(store)
                storePassword = signingValue("storePassword", "LITU_UPLOAD_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "LITU_UPLOAD_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "LITU_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingConfigs.getByName("upload").storeFile != null) signingConfig = signingConfigs.getByName("upload")
        }
    }

    // Audio clips ship in a fast-follow Play Asset Delivery pack (spec section 11).
    assetPacks += listOf(":audio_pack")

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions.unitTests.isIncludeAndroidResources = true
}

// Baseline Profile from :benchmark (spec section 6). One profile serves both flavours.
baselineProfile {
    mergeIntoMain = true
}

// Gradle Play Publisher: release.yml uploads prodRelease to the internal track.
play {
    serviceAccountCredentials.set(rootProject.file("play-service-account.json"))
    track.set("internal")
    defaultToAppBundles.set(true)
    enabled.set(rootProject.file("play-service-account.json").exists())
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:content"))
    implementation(project(":core:progress"))
    implementation(project(":core:sync"))
    implementation(project(":core:billing"))
    implementation(project(":core:config"))
    implementation(project(":core:audio"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:analytics"))

    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:practice"))
    implementation(project(":feature:review"))
    implementation(project(":feature:mock"))
    implementation(project(":feature:notes"))
    implementation(project(":feature:progress"))
    implementation(project(":feature:timer"))
    implementation(project(":feature:paywall"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":benchmark"))
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
