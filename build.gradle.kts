import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    idea
    alias(libs.plugins.kotlinJvm) apply false
    id("common-configuration") apply false
    id("project-tests-convention") apply false
}

allprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
            freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
        }
    }
    pluginManager.apply("common-configuration")
}
