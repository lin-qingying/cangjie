plugins {
    kotlin("jvm")
    id("generated-sources")

}

dependencies {
    api(project(":compiler:phaser"))
    implementation(project(":compiler:config"))
    implementation(project(":compiler:chir"))
    implementation(project(":compiler:codegen"))
    implementation(project(":cfir:entrypoint"))
    compileOnly(intellijCore())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
generatedSourcesTask(
    taskName = "generateCliArguments",
    generatorProject = ":compiler:cli-arguments-generator",
    generatorMainClass = "org.cangnova.cangjie.cli.arguments.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            "CommonToolArguments",
            "CommonCompilerArguments",
        )
    }
)

