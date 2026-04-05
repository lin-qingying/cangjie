plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    application
}

sourceSets {
    "testFixtures" { projectDefault() }
}

dependencies {
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-cfir"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:analysis-api-standalone"))
    implementation(project(":analysis:cj-references"))
    implementation(project(":compiler:config"))
    implementation(libs.lsp4j)
    implementation(libs.lsp4j.jsonrpc)
    implementation(intellijCore()) // 修改：从 compileOnly 改为 implementation

    testImplementation(intellijCore())
    testImplementation(project(":analysis:analysis-api"))
    testImplementation(project(":analysis:analysis-api-cfir"))
    testImplementation(project(":analysis:analysis-api-impl-base"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(project(":analysis:cj-references"))
    testImplementation(project(":compiler:config"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesApi(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-cfir"))
    testFixturesApi(project(":analysis:analysis-api-impl-base"))
    testFixturesApi(project(":analysis:analysis-api-standalone"))
    testFixturesApi(project(":analysis:cj-references"))
    testFixturesApi(project(":compiler:config"))
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
