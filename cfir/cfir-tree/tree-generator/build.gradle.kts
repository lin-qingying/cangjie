plugins {
    kotlin("jvm")
    application
}

val runtimeOnly by configurations
val compileOnly by configurations
runtimeOnly.extendsFrom(compileOnly)

dependencies {
    implementation(project(":cfir:cfir-common"))
    implementation(project(":cfir:cfir-cones"))

    implementation(project(":generators"))
    implementation(project(":util"))

    compileOnly(intellijCore())

    runtimeOnly(intellijJDom())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

application {
    mainClass.set("org.cangnova.cangjie.cfir.tree.generator.MainKt")
}
