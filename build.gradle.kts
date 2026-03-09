plugins {
    alias(libs.plugins.kotlinJvm) apply false
}

// 纯聚合项目，不需要 Kotlin 插件
val aggregateProjects = setOf("cfir", "raw-cfir")

subprojects {
    if (name in aggregateProjects) return@subprojects

    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "org.cangjie.cfir"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://download.jetbrains.com/teamcity-repository") {
            content {
                includeModule("org.jetbrains.teamcity", "serviceMessages")
                includeModule("org.jetbrains.teamcity.idea", "annotations")
            }
        }
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // 全局应用 projectDefault 源集布局（对齐 Kotlin 风格）
    sourceSets {
        "main" { projectDefault() }
        "test" { projectDefault() }
    }
}
