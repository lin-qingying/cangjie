package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.psi.PsiFileSystemItem

/**
 * 表示一个二进制库的 [CaModule]。
 *
 * - 对应仓颉编译产出的 `.cjo`/`.so`/`.a` 等二进制工件;
 * - 作为依赖出现在其他模块的 `directRegularDependencies` 中,
 *   也可以本身作为 use-site module(例如查看反编译源码);
 * - 在作为依赖被消费时,自身的依赖图通常无关紧要;
 *   作为 use-site 解析时,则需要依赖图完整,否则需要一个 [CaLibraryFallbackDependenciesModule] 兜底。
 *
 * 对齐 Kotlin Analysis API 的 `KaLibraryModule`。
 */
interface CaLibraryModule : CaModule {
    /**
     * 库的稳定名称,具体格式由平台实现确定。
     */
    val libraryName: String

    /**
     * 库的二进制根集合(PSI 视角),例如 `.cjo` 所在文件、目录等。
     *
     * 与 [contentScope] 应保持一致:scope 中的文件即可在 [binaryRoots] 递归覆盖范围内找到。
     */
    val binaryRoots: List<PsiFileSystemItem>
        get() = emptyList()

    /**
     * 人类可读的模块描述。
     */
    override val moduleDescription: String
        get() = "Library binaries of $libraryName"

    /**
     * 稳定二进制名称,默认沿用 [libraryName]。
     */
    override val stableModuleName: String?
        get() = libraryName
}
