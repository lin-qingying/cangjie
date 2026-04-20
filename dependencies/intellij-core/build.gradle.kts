plugins {
    `java-library`
}

val intellijVersion = property("intellijSdkVersion") as String

dependencies {
    api("com.jetbrains.intellij.platform:util-rt:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-class-loader:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-base:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:util-xml-dom:$intellijVersion") { isTransitive = false }
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
    // Analysis & Indexing
    api("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:analysis-impl:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:indexing:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:indexing-impl:$intellijVersion") { isTransitive = false }
    // 项目模型
    api("com.jetbrains.intellij.platform:project-model:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:project-model-impl:$intellijVersion") { isTransitive = false }
}
