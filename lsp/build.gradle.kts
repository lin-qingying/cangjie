plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    application
}

val intellijVersion = property("intellijSdkVersion") as String

sourceSets {
    "testFixtures" { projectDefault() }
}

dependencies {
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-cfir"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:analysis-api-standalone"))
    implementation(project(":analysis:cj-references"))
    implementation(project(":code-insight:formatting"))
    implementation(project(":code-insight:folding"))
    implementation(project(":code-insight:highlighting"))
    implementation(project(":compiler:config"))
    implementation(libs.lsp4j)
    implementation(libs.lsp4j.jsonrpc)
    implementation("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }
    implementation(intellijCore()) // 修改：从 compileOnly 改为 implementation

    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }
    testImplementation(project(":analysis:analysis-api"))
    testImplementation(project(":analysis:analysis-api-cfir"))
    testImplementation(project(":analysis:analysis-api-impl-base"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(project(":analysis:cj-references"))
    testImplementation(project(":code-insight:formatting"))
    testImplementation(project(":code-insight:folding"))
    testImplementation(project(":code-insight:highlighting"))
    testImplementation(project(":compiler:config"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesApi(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-cfir"))
    testFixturesApi(project(":analysis:analysis-api-impl-base"))
    testFixturesApi(project(":analysis:analysis-api-standalone"))
    testFixturesApi(project(":analysis:cj-references"))
    testFixturesApi(project(":code-insight:formatting"))
    testFixturesApi(project(":code-insight:highlighting"))
    testFixturesApi(project(":compiler:config"))
    testFixturesApi("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    testFixturesApi("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }
    testFixturesApi(intellijCore())
    testFixturesApi(libs.lsp4j)
    testFixturesApi(libs.lsp4j.jsonrpc)
    testFixturesApi(libs.junit.jupiter)
    testFixturesRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("org.cangnova.cangjie.lsp.CangjieLspServerLauncherKt")
    applicationDefaultJvmArgs = listOf(
        "-Xmx2g",
        "-XX:+UseG1GC",
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8"
    )
}
