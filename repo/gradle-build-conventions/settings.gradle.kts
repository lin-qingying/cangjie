pluginManagement {
    includeBuild("../gradle-settings-conventions")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        // Should be synced with the version in gradle/libs.versions.toml
        id("org.jetbrains.kotlin.jvm") version "2.2.0"
    }
}

plugins {
    id("jvm-toolchain-provisioning")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "gradle-build-conventions"

include(":utilities")
include(":buildsrc-compat")
include(":gradle-plugins-common")
include(":project-tests-convention")
