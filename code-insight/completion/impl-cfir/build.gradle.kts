plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    compileOnly(intellijCore())
    compileOnly("com.jetbrains.intellij.platform:analysis:${property("intellijSdkVersion")}") { isTransitive = false }
    compileOnly("com.jetbrains.intellij.platform:indexing:${property("intellijSdkVersion")}") { isTransitive = false }

    implementation(project(":code-insight:completion:contracts"))
    implementation(project(":code-insight:completion:impl-shared"))
    implementation(project(":analysis:analysis-api"))
    // 声明建议候选消费 override/implement 生成能力的**窄接口**（`:code-insight:api`），
    // 不依赖 `:code-insight:override-implement` 模块本身，更不触碰它的 internal 实现。
    implementation(project(":code-insight:api"))
    implementation(project(":psi"))
    implementation(project(":common"))

    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:analysis:${property("intellijSdkVersion")}") { isTransitive = false }
    // 补全区段测试需要真实 Analysis 会话：复用分析测试基座与 standalone 配置。
    testImplementation(project(":analysis:analysis-api-cfir"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
