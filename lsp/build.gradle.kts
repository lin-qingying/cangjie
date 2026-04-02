plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

sourceSets {
    "testFixtures" { projectDefault() }
}

dependencies {
    implementation(project(":analysis:analysis-api"))
    implementation(project(":compiler:config"))
    implementation(libs.lsp4j)
    implementation(libs.lsp4j.jsonrpc)
    compileOnly(intellijCore())

    testImplementation(intellijCore())
    testImplementation(project(":analysis:analysis-api"))
    testImplementation(project(":compiler:config"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesApi(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-impl-base"))
    testFixturesApi(project(":compiler:config"))
    testFixturesApi(intellijCore())
    testFixturesApi(libs.lsp4j)
    testFixturesApi(libs.lsp4j.jsonrpc)
    testFixturesApi(libs.junit.jupiter)
    testFixturesRuntimeOnly(libs.junit.platform.launcher)
}
