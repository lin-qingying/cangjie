plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
    id("org.jetbrains.intellij.platform") apply false
    id("org.jetbrains.intellij.platform.module") apply false
}

group = "org.cangnova.cangjie"
version = providers.gradleProperty("pluginVersion").get()

subprojects {
    group = rootProject.group
    version = rootProject.version
}
