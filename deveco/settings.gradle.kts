import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.2.20"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
        id("org.jetbrains.intellij.platform") version "2.10.5"
        id("org.jetbrains.intellij.platform.module") version "2.10.5"
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.10.5"
}

includeBuild("../") {
    dependencySubstitution {
        substitute(module("org.cangnova.cangjie:cangjie-frontend-common-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-common-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-psi-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-psi-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-cfir-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-cfir-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-analysis-api-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-analysis-api-cfir-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-cfir-for-ide-module"))
        substitute(module("org.cangnova.cangjie:cangjie-frontend-analysis-api-standalone-for-ide"))
            .using(project(":prepare:ide-plugin-dependencies-module:cangjie-frontend-analysis-api-standalone-for-ide-module"))
    }
}

rootProject.name = "deveco-cangjie"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS

    repositories {
        // 优先使用主仓库本地发布目录与 Maven Local，便于与编译器源码联调。
        maven {
            url = uri("../build/repo")
        }
        mavenLocal()
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
            intellijDependencies()
            localPlatformArtifacts()
            marketplace()
        }
    }
}

include(":product")
include(":modules:core")
include(":modules:deveco-bridge")
include(":modules:test-support")
