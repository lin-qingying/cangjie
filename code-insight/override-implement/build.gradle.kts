plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    compileOnly(intellijCore())
    compileOnly("com.jetbrains.intellij.platform:core-ui:${property("intellijSdkVersion")}") { isTransitive = false }

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-cfir"))
    implementation(project(":code-insight:api"))
    implementation(project(":psi"))

    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:core-ui:${property("intellijSdkVersion")}") { isTransitive = false }
    // 验证「成员生成结果」需要真实 Analysis 会话：复用分析测试基座与 standalone 配置。
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
