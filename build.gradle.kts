plugins {
    base
    idea
    alias(libs.plugins.kotlinJvm) apply false
    id("common-configuration") apply false
    id("project-tests-convention") apply false
}

allprojects {
    pluginManager.apply("common-configuration")
}
