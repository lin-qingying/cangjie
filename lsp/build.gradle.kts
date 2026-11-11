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
    implementation(project(":analysis:analysis-api-platform-interface"))
    // 宏语义展开的宿主无关编排；LSP 通过它注册环境并触发快照构造。
    implementation(project(":analysis:macro-expansion"))
    implementation(project(":analysis:cj-references"))
    /*
     * 补全：LSP 与 IDE **共用同一份候选生成实现**（规格 L1）。
     * 分支 B（见 tasks.md 15.1 的结论）下 LSP 直接调用 `CangJieCompletionService`，
     * 不注册 `completion.contributor`，因此这里只加依赖，不 prime 平台补全扩展点。
     */
    implementation(project(":code-insight:completion"))
    implementation(project(":code-insight:editor"))
    implementation(project(":code-insight:code-actions"))
    implementation(project(":code-insight:hints"))
    implementation(project(":code-insight:refactoring"))
    // 请求级统计与 analysis 侧共用同一个 OpenTelemetry 后端实例，因此只需要 API。
    implementation(libs.opentelemetry.api)
    implementation(project(":code-insight:highlighting"))
    // codeAction：quick-fix 注册表（协议侧标题与存在性）与成员生成（headless 应用）。
    // api 模块必须显式声明：analysis-features 用 implementation 引它，不透传。
    implementation(project(":code-insight:api"))
    implementation(project(":compiler:config"))
    // LSP 的进程内宏展开：完整宏引擎只在 compiler:frontend；执行器在 macro:macro-process。
    implementation(project(":compiler:frontend"))
    implementation(project(":macro:macro-process"))
    implementation(libs.lsp4j)
    implementation(libs.lsp4j.jsonrpc)
    implementation("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.platform:indexing:$intellijVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.platform:refactoring:$intellijVersion") { isTransitive = false }
    implementation("com.jetbrains.intellij.platform:usage-view:$intellijVersion") { isTransitive = false }
    implementation(intellijCore()) // 修改：从 compileOnly 改为 implementation

    testImplementation(intellijCore())
    testImplementation("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:indexing:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:refactoring:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:usage-view:$intellijVersion") { isTransitive = false }
    testImplementation(project(":analysis:analysis-api"))
    testImplementation(project(":analysis:analysis-api-cfir"))
    testImplementation(project(":analysis:analysis-api-impl-base"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(project(":analysis:cj-references"))
    testImplementation(project(":code-insight:completion"))
    testImplementation(project(":code-insight:editor"))
    testImplementation(project(":code-insight:code-actions"))
    testImplementation(project(":code-insight:hints"))
    testImplementation(project(":code-insight:refactoring"))
    testImplementation(project(":code-insight:highlighting"))
    testImplementation(project(":compiler:config"))
    // Phase 2 容器探针与后续宏展开用例复用 fake 环境 / 引擎。
    testImplementation(testFixtures(project(":analysis:macro-expansion")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    // 请求级接缝用例需要真实 SDK 才能读到指标：InMemoryMetricReader 断言采样确实进了后端。
    testImplementation(libs.opentelemetry.sdk)
    testImplementation(libs.opentelemetry.sdk.metrics)
    testImplementation(libs.opentelemetry.sdk.testing)

    testFixturesApi(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-cfir"))
    testFixturesApi(project(":analysis:analysis-api-impl-base"))
    testFixturesApi(project(":analysis:analysis-api-standalone"))
    testFixturesApi(project(":analysis:cj-references"))
    testFixturesApi(project(":code-insight:editor"))
    testFixturesApi(project(":code-insight:highlighting"))
    testFixturesApi(project(":compiler:config"))
    testFixturesApi("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    testFixturesApi("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    testFixturesApi("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }
    testFixturesApi("com.jetbrains.intellij.platform:indexing:$intellijVersion") { isTransitive = false }
    testFixturesApi("com.jetbrains.intellij.platform:refactoring:$intellijVersion") { isTransitive = false }
    testFixturesApi("com.jetbrains.intellij.platform:usage-view:$intellijVersion") { isTransitive = false }
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

distributions {
    main {
        contents {
            eachFile(org.cangnova.gradle.DisambiguateIntellijRuntimeJarNames())
        }
    }
}
