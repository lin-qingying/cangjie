plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

description = "宏语义展开的宿主无关编排：环境 SPI、服务与触发（Analysis 层一等能力）。"

dependencies {
    api(project(":analysis:low-level-api-cfir"))
    api(project(":macro:macro-common"))
    // 环境接口直接暴露 CompilerConfiguration，属于公开契约。
    api(project(":compiler:config"))
    compileOnly(intellijCore())

    testImplementation(intellijCore())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    // 测试宿主：fake environment / fake engine 供本模块与其他 analysis 模块的用例复用。
    testFixturesApi(project(":analysis:low-level-api-cfir"))
    testFixturesApi(project(":macro:macro-common"))
    testFixturesApi(project(":compiler:config"))
    testFixturesCompileOnly(intellijCore())
    testFixturesApi(libs.junit.jupiter)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "org.cangnova.cangjie.analysis.api.CaExperimentalApi",
            "org.cangnova.cangjie.analysis.api.CaPlatformInterface",
            "org.cangnova.cangjie.analysis.api.CaIdeApi",
        )
    }
}
