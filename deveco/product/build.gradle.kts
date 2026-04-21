import org.gradle.api.GradleException
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

val devEcoHome = providers.environmentVariable("DEVECO_HOME")
    .orElse(providers.gradleProperty("devEcoHome"))
val syncPlatformVersion = providers.gradleProperty("devecoSyncPlatformVersion")
val bundledCangjiePluginId = providers.gradleProperty("devecoBundledCangjiePluginId")

dependencies {
    implementation(project(":modules:core"))
    implementation(project(":modules:deveco-bridge"))

    runtimeOnly("org.cangnova.cangjie:cangjie-frontend-common-for-ide:${providers.gradleProperty("cangjieIdeVersion").get()}")
    runtimeOnly("org.cangnova.cangjie:cangjie-frontend-psi-for-ide:${providers.gradleProperty("cangjieIdeVersion").get()}")
    runtimeOnly("org.cangnova.cangjie:cangjie-frontend-cfir-for-ide:${providers.gradleProperty("cangjieIdeVersion").get()}")
    runtimeOnly("org.cangnova.cangjie:cangjie-frontend-analysis-api-for-ide:${providers.gradleProperty("cangjieIdeVersion").get()}")
    runtimeOnly("org.cangnova.cangjie:cangjie-frontend-analysis-api-cfir-for-ide:${providers.gradleProperty("cangjieIdeVersion").get()}")
    runtimeOnly("org.cangnova.cangjie:cangjie-frontend-analysis-api-standalone-for-ide:${providers.gradleProperty("cangjieIdeVersion").get()}")

    intellijPlatform {
        if (devEcoHome.isPresent) {
            local(file(devEcoHome.get()))
            if (bundledCangjiePluginId.isPresent) {
                bundledPlugin(bundledCangjiePluginId.get())
            }
        } else {
            intellijIdea(syncPlatformVersion.get())
        }
        testFramework(TestFrameworkType.Platform)
        composeUI()
    }
}

intellijPlatform {
    autoReload = true

    pluginConfiguration {
        name = "Cangjie DevEco"

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
}

tasks {
    buildPlugin {
        archiveBaseName.set("cangjie-deveco")
    }

    runIde {
        if (!devEcoHome.isPresent) {
            enabled = false
        }
        doFirst {
            if (!devEcoHome.isPresent) {
                throw GradleException("runIde 需要先配置 DEVECO_HOME 或 gradle.properties 中的 devEcoHome。")
            }
        }
    }
}
