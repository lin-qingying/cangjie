package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * Raw CFIR 构建产物中、尚未注册到 source provider 的单个文件包装。
 *
 * Batch 1 阶段仅作为 [cfirFile] 的薄包装；
 * Batch 4 引入完整 PreMacro / MacroSurface 模型后，
 * 该类型会承载只含 surface 节点的中间 CFIR。
 */
class PreMacroCfirFile internal constructor(
    val cfirFile: CfirFile,
)

/**
 * `buildPreMacroRawFilesNoRecord(...)` 的产物。
 *
 * 它**不**返回 `List<CfirFile>`，强制调用方必须经过
 * [MacroConstructionService.expand] 与 [recordExpandedRawFilesOnce] 才能
 * 把文件注册进 source provider。
 *
 * 该不变量是 baseline 第 2 节"硬性边界"的代码级表达。
 */
class PreMacroRawBuildResult internal constructor(
    val session: CfirSession,
    val files: List<PreMacroCfirFile>,
) {
    val isEmpty: Boolean get() = files.isEmpty()
    val size: Int get() = files.size
}

/**
 * 构造 [PreMacroRawBuildResult]。
 *
 * Raw builder（PSI 或 LightTree）完成构建后，
 * 必须经此入口包装产物，**不得**直接调用 source provider 的注册接口。
 */
fun buildPreMacroRawFiles(
    session: CfirSession,
    rawCfirFiles: List<CfirFile>,
): PreMacroRawBuildResult = PreMacroRawBuildResult(
    session = session,
    files = rawCfirFiles.map(::PreMacroCfirFile),
)

/**
 * 经过 [MacroConstructionService] 处理后、可被 [recordExpandedRawFilesOnce] 接受的文件包装。
 *
 * 该类型只能通过 [MacroConstructionService] 的工厂方法（[MacroConstructionService.successOf] /
 * [MacroConstructionService.degradedOf]）产出，
 * 跨模块代码因此无法绕过 construction step 直接构造可注册输入。
 */
class RecordableRawCfirFiles internal constructor(
    val session: CfirSession,
    val files: List<CfirFile>,
) {
    val size: Int get() = files.size
    val isEmpty: Boolean get() = files.isEmpty()
}

/**
 * 宏构造期诊断条目（Batch 1 阶段最小集；Batch 9 扩展）。
 */
data class MacroConstructionDiagnostic(
    val severity: Severity,
    val message: String,
) {
    enum class Severity { INFO, WARNING, ERROR }
}

/**
 * Macro 展开过程信息载体：session/analysis 级长生命周期对象，
 * 记录 surface tree、call forest、construction 诊断、原始位点映射等。
 *
 * Batch 1 阶段仅承载构造期诊断；Batch 7-9 扩展为完整 registry。
 */
class MacroExpansionRegistry {
    private val _diagnostics: MutableList<MacroConstructionDiagnostic> = mutableListOf()

    val diagnostics: List<MacroConstructionDiagnostic>
        get() = _diagnostics.toList()

    val hasErrors: Boolean
        get() = _diagnostics.any { it.severity == MacroConstructionDiagnostic.Severity.ERROR }

    fun addDiagnostic(diagnostic: MacroConstructionDiagnostic) {
        _diagnostics += diagnostic
    }

    fun addAll(diagnostics: Iterable<MacroConstructionDiagnostic>) {
        _diagnostics += diagnostics
    }

    companion object {
        val EMPTY: MacroExpansionRegistry = MacroExpansionRegistry()
    }
}

/**
 * Macro 构造步骤的输出。
 *
 * - [Success]：CLI strict 模式唯一可接受的成功状态。
 * - [Degraded]：IDE / analysis 模式可接受；含 typed error placeholder。
 * - [Failed]：构造遇到无法降级的错误。
 * - [ExecutorUnavailable]：CLI 必须 Failed；IDE 可走 Degraded。
 * - [Blocked]：例如同包 def/call、cannot-open-lib，需要上层决策。
 */
sealed class MacroConstructionResult {
    abstract val registry: MacroExpansionRegistry

    data class Success(
        val recordableFiles: RecordableRawCfirFiles,
        override val registry: MacroExpansionRegistry,
    ) : MacroConstructionResult()

    data class Degraded(
        val recordableFiles: RecordableRawCfirFiles,
        override val registry: MacroExpansionRegistry,
    ) : MacroConstructionResult()

    data class Failed(
        override val registry: MacroExpansionRegistry,
    ) : MacroConstructionResult()

    data class ExecutorUnavailable(
        override val registry: MacroExpansionRegistry,
    ) : MacroConstructionResult()

    data class Blocked(
        override val registry: MacroExpansionRegistry,
    ) : MacroConstructionResult()
}

/**
 * Macro 构造步骤抽象。
 *
 * 实现职责：
 * - 消费 [PreMacroRawBuildResult]
 * - 与 macro symbol index / executor / fragment parser 协作完成展开
 * - 输出 [MacroConstructionResult]
 *
 * 该接口禁止调用方直接接触 source [org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl]，
 * 唯一可注册路径是返回 [MacroConstructionResult.Success] / [MacroConstructionResult.Degraded] 后
 * 由上层调用 [recordExpandedRawFilesOnce]。
 */
interface MacroConstructionService {
    fun expand(pre: PreMacroRawBuildResult, mode: Mode): MacroConstructionResult

    enum class Mode {
        /** CLI：任何 [Degraded] / [ExecutorUnavailable] 都被视为失败。 */
        STRICT,

        /** IDE / analysis：允许 degraded placeholder。 */
        DEGRADED,
    }

    companion object {
        /**
         * 无宏 identity 实现：把 raw 文件原样打包为可注册输入，
         * 不调用 executor、不做任何展开。
         */
        val Identity: MacroConstructionService = IdentityMacroConstructionService

        /**
         * 给具体 service 实现使用的成功结果工厂。
         */
        fun successOf(
            pre: PreMacroRawBuildResult,
            files: List<CfirFile>,
            registry: MacroExpansionRegistry,
        ): MacroConstructionResult.Success = MacroConstructionResult.Success(
            recordableFiles = RecordableRawCfirFiles(pre.session, files),
            registry = registry,
        )

        /**
         * 给具体 service 实现使用的 degraded 结果工厂。
         */
        fun degradedOf(
            pre: PreMacroRawBuildResult,
            files: List<CfirFile>,
            registry: MacroExpansionRegistry,
        ): MacroConstructionResult.Degraded = MacroConstructionResult.Degraded(
            recordableFiles = RecordableRawCfirFiles(pre.session, files),
            registry = registry,
        )
    }
}

private object IdentityMacroConstructionService : MacroConstructionService {
    override fun expand(
        pre: PreMacroRawBuildResult,
        mode: MacroConstructionService.Mode,
    ): MacroConstructionResult {
        val files = pre.files.map(PreMacroCfirFile::cfirFile)
        return MacroConstructionService.successOf(
            pre = pre,
            files = files,
            registry = MacroExpansionRegistry.EMPTY,
        )
    }
}
