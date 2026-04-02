plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

dependencies {
    testFixturesApi(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-impl-base"))
    testFixturesApi(project(":analysis:analysis-api-cfir"))
    testFixturesApi(project(":psi"))
    testFixturesApi(project(":cfir:cfir-tree"))

    testFixturesApi(testFixtures(project(":tests:test-infrastructure")))

    testFixturesApi(intellijCore())
    testFixturesApi(libs.junit.jupiter)
    testFixturesRuntimeOnly(libs.junit.platform.launcher)
}
