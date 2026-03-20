plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:symbols"))
    api(project(":cfir:semantics"))

    api(project(":cfir:diagnostic-renderers"))
    api(project(":cfir:cfir-cones"))
    api(project(":common:diagnostics"))
//    api(project(":cfir:resolve"))
    compileOnly(intellijCore())
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xcontext-parameters")
}

val generateCfirErrors by tasks.registering {
    group = "generation"
    description = "Generate Cfir diagnostics artifacts from Cangjie diagnostics list."
    dependsOn(":cfir:checkers:checkers-component-generator:generateCfirDiagnostics")
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" { none() }
}
