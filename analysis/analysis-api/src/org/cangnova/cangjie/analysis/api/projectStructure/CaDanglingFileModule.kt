package org.cangnova.cangjie.analysis.api.projectStructure

import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile

/**
 * 游离文件模块(dangling file module)。
 *
 * - 表示一组临时的、不属于任何正式 source/library 模块的文件,
 *   常用于代码片段(code fragment)求值、IDE 的"修改沙箱"以及 in-memory 文件分析;
 * - 主契约以 [files] 为中心,`file` 仅保留兼容入口;
 * - `psiRoots` 不再作为游离文件的事实源,通过 [files] 显式提供。
 *
 * 对齐 Kotlin Analysis API 的 `KaDanglingFileModule`。
 */
interface CaDanglingFileModule : CaSourceModule {
    /**
     * 单文件游离模块的兼容入口。
     *
     * 多文件场景应改用 [files];本属性保留用于历史调用方平滑迁移。
     */
    @Deprecated(
        "Use 'files' instead.",
        ReplaceWith("files.single()", imports = ["kotlin.collections.single"]),
    )
    /**
     * 单文件 dangling module 的历史兼容入口。
     */
    val file: CjFile
        get() = files.first()

    /**
     * 同一模块中一起参与分析的所有游离文件。
     *
     * 当游离文件失效时,该属性应抛错;调用方应先通过 [isValid] 判断。
     */
    val files: List<CjFile>

    /**
     * 游离文件分析时所参照的上下文模块。
     *
     * 非局部引用(类型、跨文件符号等)默认会回退到 [contextModule] 解析。
     */
    val contextModule: CaModule

    /**
     * 控制游离文件在解析非局部声明时优先走自身还是上下文模块。
     */
    val resolutionMode: CaDanglingFileResolutionMode

    /**
     * 当前模块中是否至少有一个文件是代码片段([CjCodeFragment])。
     */
    val isCodeFragment: Boolean

    /**
     * 当前游离文件模块的 [files] 是否仍然有效。
     *
     * 一旦失效便不可恢复,任何对失效模块的访问都应抛错。
     */
    val isValid: Boolean

    /**
     * 人类可读的模块描述。
     */
    override val moduleDescription: String
        get() = "Temporary file"
}

/**
 * 是否支持在 PSI 修改后继续复用 dangling file session。
 *
 * 仅当所有文件都是物理文件且其 view provider 启用了事件系统时,
 * 才视为稳定;此时同一 session 可以跨修改重用,避免反复重建。
 */
val CaDanglingFileModule.isStable: Boolean
    get() = files.all { it.isPhysical && it.viewProvider.isEventSystemEnabled }
