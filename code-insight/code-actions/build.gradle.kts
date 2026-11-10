plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val intellijVersion = property("intellijSdkVersion") as String

/*
 * Code action：诊断驱动的代码修改入口。
 * quick fixes 与 override/implement 共用同一交互通道（intention / LSP codeAction），
 * 因此放在一个模块；成员生成对外仍只通过 `:code-insight:api` 的窄接口暴露。
 */
dependencies {
    compileOnly(intellijCore())
    compileOnly("com.jetbrains.intellij.platform:core-ui:$intellijVersion") { isTransitive = false }

    implementation(project(":code-insight:api"))
    // import 族 quick fix 复用补全链路的导入计划契约与写入应用器。
    implementation(project(":code-insight:completion"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-cfir"))
    implementation(project(":psi"))
    implementation(project(":common"))

    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:core-ui:$intellijVersion") { isTransitive = false }
    // 验证「成员生成结果」与 import 修复需要真实 Analysis 会话。
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
