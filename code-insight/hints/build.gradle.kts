plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

/*
 * Inlay hints：被动显示类能力，收集器只读快照、不改代码，
 * 因此与 code action / 补全分离，拥有独立开关与独立收集入口。
 */
dependencies {
    compileOnly(intellijCore())

    implementation(project(":analysis:analysis-api"))
    implementation(project(":common"))
    implementation(project(":psi"))

    testImplementation(intellijCore())
    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
