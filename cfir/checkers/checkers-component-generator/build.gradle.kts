plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":generators"))
    implementation(project(":cfir:cfir-tree"))
    implementation(project(":cfir:cfir-tree:tree-generator"))

    implementation(kotlin("reflect"))

    implementation(intellijCore())
    implementation(libs.guava)
}

val targetCheckersGenDir = layout.projectDirectory.dir("../gen")

val generateCfirDiagnostics by tasks.registering(JavaExec::class) {
    group = "generation"
    description = "Generate Cfir diagnostics artifacts into :cfir:checkers/gen."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.cangjie.cfir.checkers.generator.MainKt")
    workingDir = rootProject.projectDir
    args("diagnostics", targetCheckersGenDir.asFile.absolutePath)
}

application {
    mainClass.set("org.cangjie.cfir.checkers.generator.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
