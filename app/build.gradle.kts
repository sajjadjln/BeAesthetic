import java.util.Properties
import com.android.build.api.artifact.SingleArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.beaesthetic"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.beaesthetic"
        minSdk = 26
        targetSdk = 36
        // versionName = 0.<slice>.<fix> until the MVP is complete; versionCode = major × 10000 + minor × 100 + patch.
        versionCode = 501
        versionName = "0.5.1"
    }

    // The release key lives outside the repo (ARCHITECTURE §13). Its passwords never go in a file the
    // repo can see: scripts/release.sh asks for them and passes them as environment variables.
    // (A git-ignored keystore.properties with storeFile, storePassword, keyAlias, keyPassword also works.)
    val keystoreFile = rootProject.file("keystore.properties")
    val props = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use { load(it) } }
    fun signingValue(key: String, env: String): String? = props.getProperty(key) ?: providers.environmentVariable(env).orNull
    val storePath = signingValue("storeFile", "PEYKAR_KEYSTORE")
    val storePass = signingValue("storePassword", "PEYKAR_STORE_PASSWORD")
    if (storePath != null && storePass != null) {
        signingConfigs {
            create("release") {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = signingValue("keyAlias", "PEYKAR_KEY_ALIAS") ?: "peykar"
                keyPassword = signingValue("keyPassword", "PEYKAR_KEY_PASSWORD") ?: storePass
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Persian only (D87): keep no other languages from libraries in the APK.
    androidResources {
        localeFilters += listOf("fa")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Robolectric migration tests read the exported schemas from the tested variant's assets
    // (ARCHITECTURE §8). Debug builds only; the release APK doesn't carry them.
    sourceSets["debug"].assets.srcDir("$projectDir/schemas")

    lint {
        abortOnError = true
        warningsAsErrors = false
        error += listOf("HardcodedText")
        // Installed as an APK, never an app bundle, so per-language splits never happen.
        disable += listOf("AppBundleLocaleChanges")
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
}

// ---------------------------------------------------------------------------------------------
// Guards (ARCHITECTURE §13, D60): no INTERNET permission, no Google Play Services, Firebase or
// analytics/crash SDKs anywhere in the dependency graph. Both run as part of `check` and before
// every APK is assembled.
// ---------------------------------------------------------------------------------------------

val bannedGroups = listOf(
    "com.google.android.gms",
    "com.google.firebase",
    "com.google.android.play",
    "com.google.android.datatransport",
    "com.crashlytics",
    "io.sentry",
    "com.bugsnag",
    "com.amplitude",
    "com.mixpanel",
    "com.appsflyer",
    "com.flurry",
    "io.branch",
)

abstract class CheckManifestTask : DefaultTask() {
    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @TaskAction
    fun check() {
        val text = mergedManifest.get().asFile.readText()
        if (text.contains("android.permission.INTERNET")) {
            throw GradleException("android.permission.INTERNET is in the merged manifest (D60). Find the library that adds it.")
        }
    }
}

abstract class CheckDependenciesTask : DefaultTask() {
    @get:Input
    abstract val modules: ListProperty<String>

    @get:Input
    abstract val banned: ListProperty<String>

    @TaskAction
    fun check() {
        val found = modules.get().filter { module -> banned.get().any { module.startsWith("$it:") || module.startsWith("$it.") } }
        if (found.isNotEmpty()) {
            throw GradleException("Banned dependencies (ARCHITECTURE §2): ${found.joinToString()}")
        }
    }
}

androidComponents {
    // Unit tests run once, on the debug variant (it carries the Room schemas the migration tests read).
    beforeVariants(selector().withBuildType("release")) { it.enableUnitTest = false }

    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val manifestCheck = tasks.register<CheckManifestTask>("check${name}NoInternet") {
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
        val runtimeClasspath = configurations.named("${variant.name}RuntimeClasspath")
        val dependencyCheck = tasks.register<CheckDependenciesTask>("check${name}BannedDependencies") {
            banned.set(bannedGroups)
            modules.set(
                runtimeClasspath.flatMap { config ->
                    config.incoming.resolutionResult.rootComponent.map { root ->
                        val seen = mutableSetOf<String>()
                        fun walk(component: org.gradle.api.artifacts.result.ResolvedComponentResult) {
                            component.dependencies
                                .filterIsInstance<org.gradle.api.artifacts.result.ResolvedDependencyResult>()
                                .forEach { dep ->
                                    val id = dep.selected.moduleVersion?.let { "${it.group}:${it.name}" } ?: return@forEach
                                    if (seen.add(id)) walk(dep.selected)
                                }
                        }
                        walk(root)
                        seen.sorted()
                    }
                },
            )
        }
        tasks.matching { it.name == "check" }.configureEach { dependsOn(manifestCheck, dependencyCheck) }
        tasks.matching { it.name == "assemble$name" }.configureEach { dependsOn(manifestCheck, dependencyCheck) }
    }
}
