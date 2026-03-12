plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-tree"))
    implementation(project(":cfir:diagnostic-renderers"))
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
