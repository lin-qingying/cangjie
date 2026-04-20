package org.cangnova.cangjie.analysis.api.projectStructure

import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile

/**
 * 游离文件模块。
 *
 * 对齐 Kotlin `KaDanglingFileModule`：主契约以 [files] 为中心，`file` 仅保留兼容入口，
 * `psiRoots` 不再充当 dangling file 的事实源。
 */
interface CaDanglingFileModule : CaSourceModule {
    /**
     * 单文件游离模块的兼容入口。
     */
    @Deprecated(
        "Use 'files' instead.",
        ReplaceWith("files.single()", imports = ["kotlin.collections.single"]),
    )
    val file: CjFile
        get() = files.first()

    /**
     * 同一模块中一起参与分析的所有游离文件。
     *
     * 当游离文件失效时，该属性应抛错；调用方应先通过 [isValid] 判断。
     */
    val files: List<CjFile>

    val contextModule: CaModule

    /**
     * 控制游离文件在解析非局部声明时优先走自身还是上下文模块。
     */
    val resolutionMode: CaDanglingFileResolutionMode

    /**
     * 当前模块中是否至少有一个文件是代码片段。
     */
    val isCodeFragment: Boolean

    /**
     * 当前游离文件模块的 [files] 是否仍然有效。
     */
    val isValid: Boolean

    override val moduleDescription: String
        get() = "Temporary file"
}

/**
 * 是否支持在 PSI 修改后继续复用 dangling file session。
 */
val CaDanglingFileModule.isStable: Boolean
    get() = files.all { it.isPhysical && it.viewProvider.isEventSystemEnabled }
