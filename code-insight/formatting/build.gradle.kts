plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val intellijVersion = property("intellijSdkVersion") as String

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
