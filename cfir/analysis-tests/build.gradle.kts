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
    testFixturesApi(project(":cfir:diagnostics"))
    testFixturesApi(project(":cfir:raw-cfir:psi2cfir"))
    testFixturesApi(project(":cfir:raw-cfir:raw-cfir-common"))
    testFixturesApi(project(":psi"))
    testFixturesApi(testFixtures(project(":tests:test-infrastructure")))
    testFixturesApi(libs.junit4)

    testImplementation(testFixtures(project(":cfir:analysis-tests")))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testCompileOnly(intellijTestFramework()) { isTransitive = false }
    testRuntimeOnly(libs.junit.vintage.engine)
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
    testTask(jUnitMode = JUnitMode.JUnit4) {
        workingDir = rootDir
        val updateTestData = System.getProperty("update.test.data")
        if (updateTestData != null) {
            systemProperty("update.test.data", updateTestData)
        }
    }

    testGenerator("org.cangnova.cangjie.cfir.analysis.tests.TestGeneratorForCfirAnalysisTests")
}
