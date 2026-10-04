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

    implementation(project(":code-insight:completion:contracts"))
    implementation(project(":psi"))
    implementation(project(":common"))
    implementation(project(":analysis:analysis-api"))

    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:analysis:${property("intellijSdkVersion")}") { isTransitive = false }
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
