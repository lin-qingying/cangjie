plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val intellijVersion = property("intellijSdkVersion") as String

/*
 * 编辑力学：折叠与格式化。
 *
 * 该能力的输入只有 token、CST 与代码风格设置，没有跨声明的语义，
 * 因此模块只依赖 psi/common/util；调用方（IDE / LSP）复用同一份实现。
 */
dependencies {
    compileOnly(intellijCore())
    compileOnly(intellijJDom())
    compileOnly("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }

    implementation(project(":common"))
    implementation(project(":psi"))
    implementation(project(":util"))

    testImplementation(intellijCore())
    testImplementation(intellijJDom())
    testImplementation("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }
    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
