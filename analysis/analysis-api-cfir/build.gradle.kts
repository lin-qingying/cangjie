import org.gradle.api.tasks.JavaExec

plugins {
    kotlin("jvm")
    id("analysis-coverage-convention")
    id("java-test-fixtures")
    id("project-tests-convention")
}

description = "CFIR-backed implementation of the Cangjie frontend analysis API."

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { none() }
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
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
        val updateTestData = System.getProperty("update.test.data")
        if (updateTestData != null) {
            systemProperty("update.test.data", updateTestData)
        }
    }

    /**
     * Analysis API 的 generated tests 采用 Kotlin analysis 同构的三层结构：
     * 1. testData 位于 `analysis-api`
     * 2. 抽象用例与生成器位于 `analysis-api-impl-base:testFixtures`
     * 3. CFIR runner 位于当前模块的 `test`
     *
     * 因此生成器任务的运行时类路径必须绑定到当前模块的 `test` source set，
     * 才能通过 `testImplementation(testFixtures(...))` 正确拿到 impl-base 的 testFixtures 产物。
     */
    testGenerator(
        "org.cangnova.cangjie.analysis.api.impl.base.test.TestGeneratorForAnalysisApi",
        generatorClasspathSourceSetName = "test",
        excludeGeneratorSourceSetOutput = true,
    )
}

val checkGeneratedAnalysisApiTests by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "校验 analysis-api-cfir 的 generated tests 与注册表、testData 一致。"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("org.cangnova.cangjie.analysis.api.impl.base.test.GeneratedAnalysisApiTestConsistencyChecker")
    args(rootDir.absolutePath)
    dependsOn(tasks.named("testClasses"))
}

val checkGeneratedAnalysisApiMatrix by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "校验 Analysis API generated test 矩阵的结构完整性。"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("org.cangnova.cangjie.analysis.api.impl.base.test.GeneratedAnalysisApiMatrixConsistencyChecker")
    args(rootDir.absolutePath)
    dependsOn(tasks.named("testClasses"))
}

tasks.named("check") {
    dependsOn(checkGeneratedAnalysisApiMatrix)
    dependsOn(checkGeneratedAnalysisApiTests)
}

/**
 * 在仓库根任务图中注册 analysis 框架级统一入口。
 *
 * 这些任务聚合 API surface、generated tests 和核心编译校验，
 * 避免后续继续手工拼接一长串 analysis 模块任务。
 */
if (path == ":analysis:analysis-api-cfir") {
    rootProject.tasks.register("generateAnalysisFrameworkBaselines") {
        group = "verification"
        description = "生成 analysis 模块族的 API surface 与 generated tests 基线。"
        dependsOn(
            ":analysis:analysis-api:generateApiSurfaceDump",
            ":analysis:analysis-api-platform-interface:generateApiSurfaceDump",
            ":analysis:analysis-api-standalone:generateApiSurfaceDump",
            ":analysis:analysis-api-impl-base:generateApiSurfaceDump",
            ":analysis:analysis-api-cfir:generateTestGeneratorForAnalysisApiTests",
        )
    }

    rootProject.tasks.register("checkAnalysisFramework") {
        group = "verification"
        description = "统一校验 analysis 模块族的 framework-level surface、generated tests 与核心编译链。"
        dependsOn(
            ":analysis:analysis-api:checkApiSurfaceDump",
            ":analysis:analysis-api-platform-interface:checkApiSurfaceDump",
            ":analysis:analysis-api-standalone:checkApiSurfaceDump",
            ":analysis:analysis-api-impl-base:checkApiSurfaceDump",
            ":analysis:analysis-api:compileKotlin",
            ":analysis:analysis-api-platform-interface:compileKotlin",
            ":analysis:analysis-api-impl-base:compileKotlin",
            ":analysis:analysis-api-standalone:compileKotlin",
            ":analysis:analysis-internal-utils:compileKotlin",
            ":analysis:low-level-api-cfir:compileKotlin",
            ":analysis:stubs:compileKotlin",
            ":analysis:decompiled:decompiler-to-file-stubs:compileKotlin",
            ":analysis:decompiled:decompiler-to-stubs:compileKotlin",
            ":analysis:decompiled:decompiler-to-psi:compileKotlin",
            ":analysis:decompiled:light-declarations-for-decompiled:compileKotlin",
            ":analysis:decompiled:compileKotlin",
            ":analysis:light-declarations:compileKotlin",
            ":analysis:symbol-light-declarations:compileKotlin",
            ":analysis:analysis-tools:compileKotlin",
            ":analysis:analysis-api-cfir:compileKotlin",
            ":analysis:cj-references:compileKotlin",
            ":analysis:analysis-test-framework:compileTestFixturesKotlin",
            ":analysis:analysis-api-impl-base:compileTestFixturesKotlin",
            ":analysis:analysis-api-cfir:checkGeneratedAnalysisApiMatrix",
            ":analysis:analysis-api-cfir:checkGeneratedAnalysisApiTests",
        )
    }
}
