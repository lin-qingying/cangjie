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
    // 平台模块恢复直接消费内建库根协议，依赖由该 provider 所属模块明确提供。
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
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
    // 真实 CJO 协议直接核验平台 file-stub builder 的类型、包头与 PSI view，显式依赖其所属模块。
    testImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
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

tasks.named<Test>("test") {
    /*
     * 进程级用例要 fork 真实 JVM 启动服务端。child 的 classpath 不能依赖 test worker 的
     * `java.class.path`（Gradle 可能用 classpath jar 传递，直接透传会找不到测试类），
     * 但也不能当成 -D 属性传下去——Windows 命令行长度放不下完整 classpath。
     * 因此落盘到 build/ 下的文本文件，只把文件路径传给测试。
     */
    val runtimeClasspath = sourceSets["test"].runtimeClasspath
    val classpathFile = layout.buildDirectory.file("lsp-test-classpath.txt")
    inputs.files(runtimeClasspath)
        .withPropertyName("lspTestRuntimeClasspath")
        .withNormalizer(ClasspathNormalizer::class.java)
    outputs.file(classpathFile)
    doFirst {
        // 执行期由 Gradle 持项目锁解析依赖，配置期只登记 FileCollection。
        classpathFile.get().asFile.writeText(runtimeClasspath.asPath, Charsets.UTF_8)
    }
    systemProperty("lsp.test.classpath.file", classpathFile.get().asFile.absolutePath)

    /*
     * 本地默认保留「已知缺陷」红灯（`known-defect` 标签），这样缺陷不会因为测试全绿而消失。
     * 需要绿构建的 CI 加 `-PexcludeKnownDefects` 显式排除——排除是**显式动作**，不是默认。
     */
    if (providers.gradleProperty("excludeKnownDefects").isPresent) {
        useJUnitPlatform { excludeTags("known-defect") }
    }
}

distributions {
    main {
        contents {
            eachFile(org.cangnova.gradle.DisambiguateIntellijRuntimeJarNames())
        }
    }
}
