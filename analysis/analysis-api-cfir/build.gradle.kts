import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("analysis-coverage-convention")
    id("java-test-fixtures")
    id("generated-sources")

    id("project-tests-convention")
}

description = "CFIR-backed implementation of the Cangjie frontend analysis API."

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

allprojects {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.optIn.addAll(
            listOf(
                "org.cangnova.cangjie.analysis.api.CaImplementationDetail",
                "org.cangnova.cangjie.analysis.api.CaExperimentalApi",
                "org.cangnova.cangjie.analysis.api.CaNonPublicApi",
                "org.cangnova.cangjie.analysis.api.CaIdeApi",
                "org.cangnova.cangjie.analysis.api.CaPlatformInterface",
                "org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponentImplementationDetail",
            )
        )
    }
}

/**
 * Analysis API 的 CFIR 后端实现模块。
 *
 * 本模块负责把公开 Analysis API 组件映射到 low-level CFIR 能力面，
 * 不直接承载 Raw CFIR 构建、resolve 流水线与诊断缓存实现。
 */
dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:low-level-api-cfir"))
    implementation(project(":analysis:decompiled"))
    implementation(project(":analysis:stubs"))
    implementation(project(":analysis:cj-references"))

    implementation(project(":analysis:symbol-light-declarations"))
    implementation(project(":cfir:entrypoint"))
    implementation(project(":cfir:cfir-tree"))
    implementation(project(":cfir:resolve"))
    implementation(project(":cfir:checkers"))
    api(project(":common:diagnostics"))
    implementation(project(":psi"))


    compileOnly(intellijCore())

    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(project(":analysis:light-declarations"))
    testImplementation(project(":analysis:analysis-tools"))
    testImplementation(project(":analysis:cj-references"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesImplementation(testFixtures(project(":analysis:analysis-test-framework")))
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
        val updateTestData = System.getProperty("update.test.data")
        if (updateTestData != null) {
            systemProperty("update.test.data", updateTestData)
        }
    }

    testGenerator("org.cangnova.cangjie.analysis.api.cfir.test.TestGeneratorKt")
}

generatedSourcesTask(
    taskName = "generateDiagnostics",
    generatorProject = ":analysis:analysis-api-cfir:analysis-api-cfir-generator",
    generatorMainClass = "org.cangnova.cangjie.analysis.api.cfir.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            "org.cangnova.cangjie.analysis.api.cfir.diagnostics",
            generationRoot.toString(),
        )
    }
)
