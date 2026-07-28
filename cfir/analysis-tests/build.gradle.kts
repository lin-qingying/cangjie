plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

dependencies {
    compileOnly(intellijCore())

    testFixturesApi(project(":cfir:cfir-tree"))
    testFixturesApi(project(":cfir:resolve"))
    testFixturesApi(project(":cfir:entrypoint"))
    testFixturesApi(project(":cfir:checkers"))
    testFixturesApi(project(":cfir:cfir-serialization"))
    testFixturesApi(project(":common:diagnostics"))
    testFixturesApi(project(":cfir:raw-cfir:psi2cfir"))
    testFixturesApi(project(":cfir:raw-cfir:raw-cfir-common"))
    testFixturesApi(project(":psi"))
    testFixturesApi(project(":compiler:frontend"))
    testFixturesApi(project(":macro:macro-common"))
    testFixturesApi(project(":macro:macro-process"))
    testFixturesApi(testFixtures(project(":tests:test-infrastructure")))
    testFixturesImplementation(libs.kotlinx.serialization.json)

    testImplementation(testFixtures(project(":cfir:analysis-tests")))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testCompileOnly(intellijTestFramework()) { isTransitive = false }
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        // 全量 LLT 会在同一 test worker 中累计 8000 余项编译器分析状态；Gradle 默认测试堆
        // 已连续两次在结果 XML 全部写出后耗尽。显式配置模块级测试堆，保证全量验证能正常收尾。
        maxHeapSize = "2g"
        workingDir = rootDir
        val updateTestData = System.getProperty("update.test.data")
        if (updateTestData != null) {
            systemProperty("update.test.data", updateTestData)
        }
    }

    testGenerator("org.cangnova.cangjie.cfir.analysis.tests.TestGeneratorForCfirAnalysisTests")
}
