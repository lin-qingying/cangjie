plugins {
    kotlin("jvm")
    id("generated-sources")

}
// CFIR Tree: IR 声明、表达式、类型引用、访问者
dependencies {
    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-cones"))
    api(project(":common"))
    api(project(":util"))

    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter)

    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testRuntimeOnly(libs.junit.platform.launcher)
}
sourceSets {
    "main" { projectDefault() }
}


generatedSourcesTask(
    taskName = "generateTree",
    generatorProject = ":cfir:cfir-tree:tree-generator",
    generatorMainClass = "org.cangjie.cfir.tree.generator.MainKt",
)
