    import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// Pin the generated Res package: without this it falls back to a
// name-derived default that has already drifted once (stale outputs under
// a different package broke incremental builds).
compose.resources {
    packageOfResClass = "com.luum.michi.app.resources"
}

abstract class GenerateAniListBuildConfigTask : DefaultTask() {
    @get:Input
    abstract val clientId: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val packageDir = outputDir.get().asFile.resolve("com/luum/michi/app/core/auth/domain")
        packageDir.mkdirs()
        packageDir.resolve("AniListBuildConfig.kt").writeText(
            """
            package com.luum.michi.app.core.auth.domain

            /**
             * Generated at build time from `local.properties`.
             * Do not edit by hand. Configure `anilistClientId` in your local.properties.
             */
            internal object AniListBuildConfig {
                const val ClientId: String = "${clientId.get()}"
            }
            """.trimIndent() + "\n"
        )
    }
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val anilistClientIdValue: String =
    (localProperties["anilistClientId"] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("ANILIST_CLIENT_ID").orEmpty()

// Test-only AniList token for the opt-in live suite (see `live` test package).
// local.properties is gitignored; never commit or log this value.
val anilistTestTokenValue: String =
    (localProperties["anilistTestToken"] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("ANILIST_TEST_TOKEN").orEmpty()

// Fail fast when packaging a release without a client id, otherwise the shipped
// binary would bake in an empty ClientId and every user would hit the
// "Missing anilistClientId" screen. Debug builds are intentionally allowed to
// run without it so day-to-day development is not blocked.
val isReleaseBuild = gradle.startParameter.taskNames.any { taskName ->
    taskName.substringAfterLast(':').contains("release", ignoreCase = true)
}
if (isReleaseBuild && anilistClientIdValue.isBlank()) {
    throw GradleException(
        "Missing anilistClientId for a release build. Set `anilistClientId` in " +
            "local.properties or the ANILIST_CLIENT_ID environment variable before " +
            "assembling a release — otherwise shipped users would see the " +
            "\"Missing anilistClientId\" error."
    )
}

val generateAniListBuildConfig = tasks.register<GenerateAniListBuildConfigTask>("generateAniListBuildConfig") {
    clientId.set(anilistClientIdValue)
    outputDir.set(layout.buildDirectory.dir("generated/source/authConfig/commonMain/kotlin"))
}

kotlin {
    // NOTE: `Res.drawable.*` accessors are generated into
    // `commonMainResourceAccessors` by the compose-resources plugin, but under
    // androidMultiplatformLibrary that source set never reaches the Android
    // compilation (verified via build logs: compileAndroidMain only looks
    // for `androidMainResourceAccessors`, which is never generated). Do NOT
    // re-register that dir into another source set: K2 fragments reject a
    // file belonging to two modules. See Icons.kt for the workaround.
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
       namespace = "com.luum.michi.app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.browser)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain {
            kotlin.srcDir(generateAniListBuildConfig)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.materialIconsCore)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.material.kolor)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinxJson)
                implementation(libs.qrose)
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

tasks.withType<Test> {
    environment("ANILIST_TEST_TOKEN", anilistTestTokenValue)
}

