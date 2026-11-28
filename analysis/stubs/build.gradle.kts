plugins {
    kotlin("jvm")
    id("analysis-coverage-convention")
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":psi"))
    implementation(project(":common"))

    testImplementation(intellijCore())
    testImplementation(project(":compiler:config"))
    testImplementation(project(":analysis:analysis-api-impl-base"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(project(":analysis:cj-references"))
    testImplementation(project(":analysis:decompiled"))
    // 真实 CJO 读取集成测试直接使用 LL producer、加载记录及 Kotlin 断言。
    testImplementation(project(":analysis:low-level-api-cfir"))
    testImplementation(project(":analysis:decompiled:decompiler-to-stubs"))
    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-api-cfir")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    val updateTestData = System.getProperty("update.test.data")
    if (updateTestData != null) {
        systemProperty("update.test.data", updateTestData)
    }
    val builtinsTestFile = System.getProperty("cangjie.builtins.test.file")
    if (builtinsTestFile != null) {
        systemProperty("cangjie.builtins.test.file", builtinsTestFile)
    }
}
