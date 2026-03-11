// 通用项目配置（对齐 Kotlin common-configuration.gradle.kts）
// 不使用 plugins {} 块：预编译脚本插件中不能带版本号的插件请求
// Kotlin 插件由各子项目自行应用，此处仅在插件已存在时进行配置

project.configureJvmDefaultToolchain()

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

plugins.withId("org.jetbrains.kotlin.jvm") {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    sourceSets {
        "main" { projectDefault() }
        "test" { projectDefault() }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
