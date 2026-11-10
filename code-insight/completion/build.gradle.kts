plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val intellijVersion = property("intellijSdkVersion") as String

/*
 * 补全：契约实现、补全位置/插入/导入机制、Analysis 支撑的候选流水线。
 * 三者是一个功能的三个阶段，用包分层，不再拆成多个 Gradle 模块。
 */
dependencies {
    compileOnly(intellijCore())
    compileOnly("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:indexing:$intellijVersion") { isTransitive = false }
    /*
     * 统计接缝用平台的 OpenTelemetry provider 与 OTel API。
     * 与大仓其它模块一致用 `compileOnly`：两者都由 Analysis 平台制品在运行期提供
     * （`analysis-api-platform-interface` 自己也是这么声明的），这里只借用类型。
     */
    compileOnly(project(":analysis:analysis-api-platform-interface"))
    compileOnly(libs.opentelemetry.api)

    implementation(project(":code-insight:api"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":psi"))
    // 导入计划契约直接承载仓颉的 ImportPath（含组织名与别名），不另造平行字符串结构。
    implementation(project(":common"))

    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    testImplementation(project(":analysis:analysis-api-platform-interface"))
    testImplementation(libs.opentelemetry.api)
    // 补全区段测试需要真实 Analysis 会话：复用分析测试基座与 standalone 配置。
    testImplementation(project(":analysis:analysis-api-cfir"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
