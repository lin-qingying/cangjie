
plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}
// RAW_CFIR LightTree2CFIR: LightTree → Raw CFIR 转换（对齐 Kotlin raw-fir/light-tree2fir）

sourceSets {
    "main" { projectDefault() }
    "testFixtures" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

val intellijVersion = property("intellijSdkVersion") as String

dependencies {
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:raw-cfir:raw-cfir-common"))
    api(project(":cfir:semantics"))
    api(project(":psi"))

    compileOnly(intellijCore())

    // testFixtures 对外暴露的基类依赖
    testFixturesApi(project(":cfir:cfir-tree"))
    testFixturesApi(project(":cfir:raw-cfir:raw-cfir-common"))
    testFixturesApi(project(":cfir:raw-cfir:psi2cfir"))
    testFixturesApi(project(":cfir:symbols"))
    testFixturesApi(project(":psi"))
    testFixturesApi(testFixtures(project(":cfir:raw-cfir:psi2cfir")))
    testFixturesApi(testFixtures(project(":tests:test-infrastructure")))
    testFixturesApi(libs.junit4)

    // IntelliJ 平台（测试时需要完整传递依赖）
    testFixturesApi("com.jetbrains.intellij.platform:core:$intellijVersion")
    testFixturesApi("com.jetbrains.intellij.platform:core-impl:$intellijVersion")
    testFixturesApi("com.jetbrains.intellij.platform:extensions:$intellijVersion")
    testFixturesImplementation("com.jetbrains.intellij.platform:util:$intellijVersion")

    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(testFixtures(project(":cfir:raw-cfir:psi2cfir")))
    testImplementation(testFixtures(project(":cfir:raw-cfir:light-tree2cfir")))
    testImplementation(project(":psi"))
    testImplementation(project(":cfir:symbols"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testCompileOnly(intellijTestFramework()) { isTransitive = false }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        workingDir = rootDir
        val updateTestData = System.getProperty("update.test.data")
        if (updateTestData != null) {
            systemProperty("update.test.data", updateTestData)
        }
    }

    testGenerator("org.cangnova.cangjie.cfir.lightTree.TestGeneratorForLightTree2Cfir")
}
