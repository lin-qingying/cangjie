pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

}

plugins {
    // Should be synced with the version in gradle/libs.versions.toml
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "gradle-settings-conventions"

include(":jvm-toolchain-provisioning")
