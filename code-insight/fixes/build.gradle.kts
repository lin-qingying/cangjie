plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":analysis:analysis-api-cfir"))
    implementation(project(":code-insight:api"))
    implementation(project(":code-insight:override-implement"))
    // import 族 quick fix 复用补全链路的导入计划契约与写入应用器。
    implementation(project(":code-insight:completion:contracts"))
    implementation(project(":code-insight:completion:impl-shared"))
    implementation(project(":psi"))

    testImplementation(intellijCore())
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    // import 族 factory 回归需要真实 Analysis 会话：复用分析测试基座与 standalone 配置。
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testRuntimeOnly(libs.junit.platform.launcher)
}
