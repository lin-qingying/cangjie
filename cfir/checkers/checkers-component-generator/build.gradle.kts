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

val targetCheckersGenDir = layout.projectDirectory.dir("../gen")

val generateCfirDiagnostics by tasks.registering(JavaExec::class) {
    group = "generation"
    description = "Generate Cfir diagnostics artifacts into :cfir:checkers/gen."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.cangnova.cangjie.cfir.checkers.generator.MainKt")
    workingDir = rootProject.projectDir
    args("all", targetCheckersGenDir.asFile.absolutePath)
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
