import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val appVersionCode = providers.gradleProperty("app.versionCode").orNull
    ?.toIntOrNull()
    ?: throw GradleException("Gradle property app.versionCode must be a positive integer.")
if (appVersionCode !in 1..2_100_000_000) {
    throw GradleException("Gradle property app.versionCode must be between 1 and 2100000000.")
}

val releaseSigningValues = mapOf(
    "ASTORY_RELEASE_STORE_FILE" to providers.environmentVariable("ASTORY_RELEASE_STORE_FILE").orNull,
    "ASTORY_RELEASE_STORE_PASSWORD" to providers.environmentVariable("ASTORY_RELEASE_STORE_PASSWORD").orNull,
    "ASTORY_RELEASE_KEY_ALIAS" to providers.environmentVariable("ASTORY_RELEASE_KEY_ALIAS").orNull,
    "ASTORY_RELEASE_KEY_PASSWORD" to providers.environmentVariable("ASTORY_RELEASE_KEY_PASSWORD").orNull
)
val releaseSigningConfigured = releaseSigningValues.values.all { !it.isNullOrBlank() }
val requestedTaskNames = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
val releaseBuildRequested = requestedTaskNames.any { taskName ->
    taskName.contains("Release") || taskName in setOf("assemble", "build")
}

if (releaseBuildRequested && !releaseSigningConfigured) {
    throw GradleException(
        "Release signing is required. Set ASTORY_RELEASE_STORE_FILE, ASTORY_RELEASE_STORE_PASSWORD, " +
            "ASTORY_RELEASE_KEY_ALIAS and ASTORY_RELEASE_KEY_PASSWORD in the build process environment."
    )
}

val releaseStoreFile = releaseSigningValues["ASTORY_RELEASE_STORE_FILE"]
    ?.takeIf { it.isNotBlank() }
    ?.let { File(it) }

if (releaseBuildRequested) {
    val configuredStoreFile = releaseStoreFile
        ?: throw GradleException("ASTORY_RELEASE_STORE_FILE must be an absolute path outside the repository.")
    if (!configuredStoreFile.isAbsolute) {
        throw GradleException("ASTORY_RELEASE_STORE_FILE must be an absolute path outside the repository.")
    }
    val signingStore = configuredStoreFile.canonicalFile
    val projectDirectory = rootProject.projectDir.canonicalFile.toPath()
    if (signingStore.toPath().startsWith(projectDirectory)) {
        throw GradleException("Release keystore must be stored outside the Git repository.")
    }
    if (!signingStore.isFile) {
        throw GradleException("Configured release keystore does not exist or is not a file.")
    }
}

android {
    namespace = "com.hoangtu.aistorystudio"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hoangtu.aistorystudio"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseSigningValues.getValue("ASTORY_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigningValues.getValue("ASTORY_RELEASE_KEY_ALIAS")
                keyPassword = releaseSigningValues.getValue("ASTORY_RELEASE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp)
    testImplementation(libs.mockwebserver3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
