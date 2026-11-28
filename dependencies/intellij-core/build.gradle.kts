plugins {
    `java-library`
}

val intellijVersion = property("intellijSdkVersion") as String

dependencies {
    // SDK 公开签名使用 CoroutineScope；其 POM 的 runtime 依赖不会自动进入编译 API。
    api(libs.intellij.coroutines.core)
    api("com.jetbrains.intellij.platform:util-rt:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-class-loader:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-base:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
    // 平台公共 API 的状态类型包含 org.jdom.Element，调用方编译需要同版本 JDOM。
    api("com.jetbrains.intellij.platform:util-jdom:$intellijVersion") { isTransitive = false }
    // 253 起插件 XML 解析模型拆分到独立的 plugins-parser-impl。
    // analysis-api-impl-base 的 headless 插件装配需要直接访问其中的 RawPluginDescriptor /
    // PluginDescriptorReaderContext / PluginDescriptorBuilder 等类型。
    api("com.jetbrains.intellij.platform:plugins-parser-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:core:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:core-impl:$intellijVersion") { isTransitive = false }
    // Required by AppUIExecutor/AsyncExecutionService in stub-backed PSI tests.
    // Keep transitive to pull threading/runtime pieces used by ide-impl (e.g. rwmutex-idea, concurrency).
    api("com.jetbrains.intellij.platform:ide-impl:$intellijVersion")
    api("com.jetbrains.intellij.platform:concurrency:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:extensions:$intellijVersion") { isTransitive = false }
    // Language 支持（ParserDefinition, Language, FileType, PsiBuilder 等）
    api("com.jetbrains.intellij.platform:lang:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:lang-impl:$intellijVersion") { isTransitive = false }
    // 可修改 PSI 的标准服务直接使用公开 CodeStyleManager/Postprocess 实现，必须进入编译 API。
    api("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:code-style-impl:$intellijVersion") { isTransitive = false }
    // Analysis & Indexing
    api("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:analysis-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:indexing:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:indexing-impl:$intellijVersion") { isTransitive = false }
    // 项目模型
    api("com.jetbrains.intellij.platform:project-model:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:project-model-impl:$intellijVersion") { isTransitive = false }
    // 对齐 Kotlin dependencies/intellij-core：平台运行时只保留一份 OpenTelemetry API，
    // 供 analysis-api 统计后端（CangJieGlobalOpenTelemetryProvider）在 IDE 运行时解析；不参与编译传递。
    runtimeOnly(libs.opentelemetry.api) { isTransitive = false }
}
