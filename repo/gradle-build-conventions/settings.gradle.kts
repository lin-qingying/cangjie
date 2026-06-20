pluginManagement {
    includeBuild("../gradle-settings-conventions")

    repositories {
        mavenCentral()
        gradlePluginPortal()
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
include(":analysis-coverage-convention")
include(":buildsrc-compat")
include(":gradle-plugins-common")
include(":project-tests-convention")
include(":generators")
include(":native-compile-plugin")
