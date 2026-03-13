import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// CFIR Diagnostics: 诊断框架核心（DiagnosticFactory、DiagnosticReporter、Severity、Collector、PositioningStrategy）

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-common"))
    api(project(":common"))
    api(project(":compiler:config"))
    api(project(":util"))
    compileOnly(intellijCore())
    implementation(libs.guava)
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xcontext-parameters"))
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" { none() }
}
