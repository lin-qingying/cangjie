import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// DevEco Studio Plugin Build Script
// DevEco Studio is built on IntelliJ IDEA, so we use the IntelliJ Platform Gradle Plugin
// with localPath pointing to the local DevEco Studio installation.


plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellijPlatform)
    alias(libs.plugins.compose)
}

group = "org.cangnova.cangjie"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvmToolchain(17) // DevEco Studio typically bundles JBR 17; adjust to match your DevEco version
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // ─── Use local DevEco Studio installation instead of a remote SDK ───
        // Set the DEVECO_HOME environment variable to your DevEco Studio path, e.g.:
        //   macOS  : /Applications/DevEco-Studio.app/Contents
        //   Windows: C:/Program Files/Huawei/DevEco Studio/
        //   Linux  : /opt/DevEco-Studio/
        //
        // Alternatively, hard-code the path here for local development:
        //   local(file("/Applications/DevEco-Studio.app/Contents"))
        val devEcoHome = providers.environmentVariable("DEVECO_HOME")
            .orElse(providers.gradleProperty("devEcoHome"))
        local(file(devEcoHome.get()))

        // ─── Test framework ───
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // ─── Compose UI support ───
        composeUI()

        // ─── Declare the DevEco built-in Cangjie plugin as a dependency ───
        // This makes Cangjie plugin classes available at compile time and
        // prevents the platform from blocking the plugin from loading.
        // The plugin ID below is the expected ID for the Huawei Cangjie plugin;
        // verify by opening DevEco → Settings → Plugins and checking the plugin ID.
        bundledPlugin("com.huawei.deveco.language.cangjie")
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Cangjie Enhancer"

        ideaVersion {
            // sinceBuild is read from gradle.properties (pluginSinceBuild).
            // Set untilBuild to empty string to allow all future builds,
            // or pin it to the DevEco build range you have tested against.
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null } // remove upper bound – adjust if needed
        }

        changeNotes = """
            <ul>
              <li>1.0.0-SNAPSHOT – Initial release: enhanced Cangjie language support for DevEco Studio.</li>
            </ul>
        """.trimIndent()
    }

    // Disable the JetBrains Marketplace signing/publishing flow.
    // DevEco plugins are distributed via Huawei AppGallery Connect or internally.
    signing {
        // Configure signing credentials here if required by your distribution channel.
    }

    publishing {
        // Configure Huawei plugin repository token here if needed.
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    // Ensure the plugin zip is reproducible
    buildPlugin {
        archiveBaseName.set("cangjie-enhancer")
    }

    // Point runIde at the local DevEco installation so you can test with F5 / runIde task.
    // This is automatically inherited from intellijPlatform { local(...) } above,
    // but you can override it explicitly:
    //
    // runIde {
    //     autoReloadPlugins.set(true)
    // }
}