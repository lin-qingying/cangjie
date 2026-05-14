package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.psi.PsiFileSystemItem

/**
 * [CaLibraryModule] 对应的源码侧模块。
 *
 * - 在 IDE 中查看库文件时,若存在源码归档(例如附带的 `-sources` 包),
 *   会优先呈现源码而非二进制反编译结果,这一源码视图就用 [CaLibrarySourceModule] 表示;
 * - 其依赖图应与 [binaryLibraryModule] 完全一致;
 *   若库依赖未知,同样需要 [CaLibraryFallbackDependenciesModule] 兜底。
 *
 * 对齐 Kotlin Analysis API 的 `KaLibrarySourceModule`。
 */
interface CaLibrarySourceModule : CaModule {
    /**
     * 库源码的稳定名称,通常与对应二进制 [CaLibraryModule.libraryName] 相同。
     */
    val libraryName: String

    /**
     * 与之配对的二进制库模块。
     */
    val binaryLibraryModule: CaLibraryModule

    /**
     * 源码根集合(PSI 视角),例如源码 `.zip`、目录等。
     */
    val sourceRoots: List<PsiFileSystemItem>
        get() = emptyList()

    /**
     * 人类可读的模块描述。
     */
    override val moduleDescription: String
        get() = "Library sources of $libraryName"

    /**
     * 稳定二进制名称,默认沿用 [libraryName]。
     */
    override val stableModuleName: String?
        get() = libraryName
}
