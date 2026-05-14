package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.LanguageVersionSettings

/**
 * 表示一组仓颉源码声明的 [CaModule]。
 *
 * - 与平台层"模块"概念不一定一对一:例如 IDE 中 main / test source set
 *   会被建模为两个独立的 [CaSourceModule],test 通过 friend 依赖访问 main 的 `internal`;
 * - 持有源码相关的语言版本设置([languageVersionSettings])与 PSI 源码根集合。
 *
 * 对齐 Kotlin Analysis API 的 `KaSourceModule`。
 */
interface CaSourceModule : CaModule {
    /**
     * 模块名称,具体格式由平台实现决定。
     */
    val name: String

    /**
     * 仓颉语言设置(API 版本、特性开关、编译标志等)。
     */
    val languageVersionSettings: LanguageVersionSettings

    /**
     * PSI 视角下的源码根集合。
     */
    val psiRoots: List<PsiFileSystemItem>
        get() = emptyList()

    /**
     * 人类可读的模块描述。
     */
    override val moduleDescription: String
        get() = "Sources of $name"

    /**
     * 稳定二进制名称,默认沿用 [name]。
     */
    override val stableModuleName: String?
        get() = name
}
