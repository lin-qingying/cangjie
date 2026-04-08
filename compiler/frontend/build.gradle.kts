plugins {
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:phaser"))
    implementation(project(":compiler:config"))
    implementation(project(":cfir:entrypoint"))
    implementation(project(":macro:macro-common"))

    compileOnly(intellijCore())
    testImplementation(intellijCore())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
generatedSourcesTask(
    taskName = "generateFrontendArguments",
    generatorProject = ":compiler:frontend-arguments-generator",
    generatorMainClass = "org.cangnova.cangjie.frontend.arguments.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            "CommonToolArguments",
            "CommonCompilerArguments",
        )
    }
)

tasks.matching { it.name == "kotlinSourcesJar" || it.name == "sourcesJar" }.configureEach {
    dependsOn("generateFrontendArguments")
}
