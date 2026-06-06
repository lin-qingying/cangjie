plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    compileOnly(intellijCore())
    compileOnly("com.jetbrains.intellij.platform:analysis:${property("intellijSdkVersion")}") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:indexing:${property("intellijSdkVersion")}") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:refactoring:${property("intellijSdkVersion")}") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:usage-view:${property("intellijSdkVersion")}") { isTransitive = false }

    implementation(project(":common"))
    implementation(project(":psi"))
    implementation(project(":util"))

    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:analysis:${property("intellijSdkVersion")}") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:indexing:${property("intellijSdkVersion")}") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:refactoring:${property("intellijSdkVersion")}") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:usage-view:${property("intellijSdkVersion")}") { isTransitive = false }
    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
