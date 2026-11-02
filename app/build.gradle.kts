import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

val appVersion = "1.0.0"
group = "dev.legendaryredfox.mgpatch"
version = appVersion

kotlin {
    jvmToolchain(21)
    compilerOptions { allWarningsAsErrors = true }

    // Compose Desktop runs on Linux, macOS and Windows from the same JVM target.
    // The patch engine in commonMain uses no JVM API, so more targets can be added later.
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(libs.compose.ui.test)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.legendaryredfox.mgpatch.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MetalGearPatcher"
            packageVersion = appVersion
            description = "Real-hardware fixes for the Metal Gear Mega Drive port (v1.1 and v1.2)"
            vendor = "legendaryredfox"
            modules("java.desktop")
            linux {
                debMaintainer = "legendaryredfox.dev@gmail.com"
                iconFile.set(project.file("icons/icon.png"))
            }
            macOS {
                bundleID = "dev.legendaryredfox.mgpatch"
                iconFile.set(project.file("icons/icon.icns"))
            }
            windows {
                menuGroup = "Metal Gear Patcher"
                upgradeUuid = "6f1b0f3e-5f0a-4a64-9b7c-3d2b5d6a1c11"
                iconFile.set(project.file("icons/icon.ico"))
            }
        }
    }
}

// Test ROMs are never committed. Point MG_ROM_DIR at a folder with your dumps to enable the
// real-ROM tests (they are skipped otherwise).
tasks.withType<Test>().configureEach {
    environment("MG_ROM_DIR", System.getenv("MG_ROM_DIR") ?: "")
}
