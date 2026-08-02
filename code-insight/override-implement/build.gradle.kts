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
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
