plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val intellijVersion = property("intellijSdkVersion") as String

/*
 * 重构：独立的能力与独立入口（rename / safe delete / …）。
 *
 * 这里不属于「编辑力学」：重构的正确性由符号同一性与引用范围决定，
 * 目标形态是符号级 rename；PSI 引用搜索只是当前实现手段。
 */
dependencies {
    compileOnly(intellijCore())
    compileOnly("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:indexing:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:refactoring:$intellijVersion") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:usage-view:$intellijVersion") { isTransitive = false }

    implementation(project(":common"))
    implementation(project(":psi"))
    implementation(project(":util"))

    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:indexing:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:refactoring:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:usage-view:$intellijVersion") { isTransitive = false }
    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
