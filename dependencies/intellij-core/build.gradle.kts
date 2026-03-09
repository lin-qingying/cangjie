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
    api("com.jetbrains.intellij.platform:core:$intellijVersion") { isTransitive = false }
    api("com.jetbrains.intellij.platform:core-impl:$intellijVersion") { isTransitive = false }
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
