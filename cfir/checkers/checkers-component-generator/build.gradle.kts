plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":generators"))
    implementation(project(":cfir:cfir-tree"))
    implementation(project(":cfir:cfir-cones"))

    implementation(project(":cfir:cfir-tree:tree-generator"))
    implementation(project(":common:diagnostics"))
    implementation(project(":psi"))

    implementation(kotlin("reflect"))

    compileOnly(intellijCore())
    implementation(libs.guava)
}

application {
    mainClass.set("org.cangnova.cangjie.cfir.checkers.generator.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
