// RAW_CFIR PSI2CFIR: PSI → Raw CFIR 转换（对齐 Kotlin raw-fir/psi2fir）
plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

sourceSets {
    "testFixtures" { projectDefault() }
    "test" { generatedTestDir() }
}

val intellijVersion = property("intellijSdkVersion") as String

dependencies {
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:raw-cfir:raw-cfir-common"))
    implementation(project(":psi"))

    compileOnly(intellijCore())

    // testFixtures 对外暴露的基类依赖
    testFixturesApi(project(":cfir:cfir-tree"))
    testFixturesApi(project(":cfir:raw-cfir:raw-cfir-common"))
    testFixturesApi(project(":psi"))
    testFixturesApi(testFixtures(project(":tests:test-infrastructure")))
    testFixturesApi(libs.junit4)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // IntelliJ 平台（测试时需要完整传递依赖，不能 isTransitive=false）
    testFixturesApi("com.jetbrains.intellij.platform:core:$intellijVersion")
    testFixturesApi("com.jetbrains.intellij.platform:core-impl:$intellijVersion")
    testFixturesApi("com.jetbrains.intellij.platform:extensions:$intellijVersion")
    testFixturesImplementation("com.jetbrains.intellij.platform:util:$intellijVersion")

    testImplementation(testFixtures(project(":tests:test-infrastructure")))

    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    // TestDataPath 注解仅编译期使用
    testCompileOnly(intellijTestFramework()) { isTransitive = false }
}


sourceSets {
    "main" { projectDefault() }
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

    testGenerator("org.cangjie.cfir.builder.TestGeneratorForPsi2Cfir")
}
