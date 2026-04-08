import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// CFIR Diagnostics: 诊断框架核心（DiagnosticFactory、DiagnosticReporter、Severity、Collector、PositioningStrategy）

plugins {
    kotlin("jvm")
}

description = "Cangjie frontend diagnostics model, factories, collectors and renderers."

dependencies {
    api(project(":psi"))

    api(project(":common"))
    implementation(project(":util"))
    compileOnly(intellijCore())
    implementation(libs.guava)
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.add("-Xcontext-parameters")
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" { none() }
}
