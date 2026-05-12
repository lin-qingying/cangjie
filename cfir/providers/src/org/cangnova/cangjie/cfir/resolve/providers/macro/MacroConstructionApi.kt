package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * Raw CFIR 构建产物中、尚未注册到 source provider 的单个文件包装。
 *
 * Batch 1 阶段仅作为 [cfirFile] 的薄包装；
 * Batch 4 引入完整 PreMacro / MacroSurface 模型后，
 * 该类型会承载只含 surface 节点的中间 CFIR。
 *
 * @param surfaces  本文件中收集到的 macro surface 列表（baseline 第 7 节）。
 *                  Batch 4a 阶段由 [buildPreMacroRawFiles] 默认填空；
 *                  4b 起 PSI / LightTree builder 双覆盖时填入实际数据。
 */
class PreMacroCfirFile internal constructor(
    val cfirFile: CfirFile,
    val surfaces: List<MacroSurface> = emptyList(),
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

    /** 所有文件中收集到的 macro surface（聚合）。 */
    val allSurfaces: List<MacroSurface>
        get() = files.flatMap { it.surfaces }
}

/**
 * 构造 [PreMacroRawBuildResult]。
 *
 * Raw builder（PSI 或 LightTree）完成构建后，
 * 必须经此入口包装产物，**不得**直接调用 source provider 的注册接口。
 *
 * @param fileSurfaces  与 [rawCfirFiles] 顺序对齐的 per-file surface 列表；
 *                      默认全空（Batch 4b 起由 PSI / LightTree builder 填入）。
 */
fun buildPreMacroRawFiles(
    session: CfirSession,
    rawCfirFiles: List<CfirFile>,
    fileSurfaces: List<List<MacroSurface>> = List(rawCfirFiles.size) { emptyList() },
): PreMacroRawBuildResult {
    require(fileSurfaces.size == rawCfirFiles.size) {
        "fileSurfaces size (${fileSurfaces.size}) must match rawCfirFiles size (${rawCfirFiles.size})"
    }
    return PreMacroRawBuildResult(
        session = session,
        files = rawCfirFiles.mapIndexed { index, cfirFile ->
            PreMacroCfirFile(cfirFile = cfirFile, surfaces = fileSurfaces[index])
        },
    )
}

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
 * 单条 macro 构造诊断（baseline 第 1 节 + 第 9 节）。
 */
data class MacroConstructionDiagnostic(
    val severity: Severity,
    val message: String,
    /**
     * 关联 surface id；可为 null（例如 alias 冲突诊断 binding 整条 import）。
     * Batch 9 的 diagnostic renderer 通过该 id 反查 [MacroExpansionRegistry.originSurfaceById]。
     */
    val originSurfaceId: Long? = null,
    /** Baseline 第 9 节 typed factory：未展开 / 展开失败 / 同包 / 无 executor / 等。 */
    val kind: Kind = Kind.GENERIC,
) {
    enum class Severity { INFO, WARNING, ERROR }

    enum class Kind {
        GENERIC,
        MACRO_NOT_EXPANDED,
        MACRO_EXPANSION_FAILED,
        MACRO_SAME_PACKAGE_DEF_CALL,
        MACRO_ALIAS_CONFLICT,
        MACRO_EXECUTOR_UNAVAILABLE,
        MACRO_CANNOT_OPEN_LIB,
        MACRO_REEVALUATION_FAILED,
        MACRO_UNRESOLVED,
        MACRO_CYCLE,
    }
}

/**
 * Macro 展开过程信息载体：session/analysis 级长生命周期对象，
 * 记录 surface tree、call forest、construction 诊断、原始位点映射等。
 *
 * Batch 9 扩展（baseline 第 10 节）：
 * - `originSurfaceById` 维护 `surfaceId -> MacroSurface` 的反查表，
 *   ordinary checker 上报的诊断可通过 `originSurfaceId` 字段映射回原 macro site；
 * - `placeholderOriginById` 维护 degraded mode 下生成的 typed error
 *   placeholder 与原 surface 的关系，便于 LSP / IDE 渲染。
 *
 * `MacroExpansionRegistry` 作为 [org.cangnova.cangjie.cfir.session.CfirSessionComponent]
 * 候选，可通过 [org.cangnova.cangjie.cfir.session.CfirSession.register] 挂到 session 上；
 * 多模块 build 时每个 source session 各持一份。
 */
class MacroExpansionRegistry : org.cangnova.cangjie.cfir.session.CfirSessionComponent {
    private val _diagnostics: MutableList<MacroConstructionDiagnostic> = mutableListOf()
    private val _originSurfaceById: MutableMap<Long, MacroSurface> = mutableMapOf()
    private val _placeholderOriginById: MutableMap<Long, Long> = mutableMapOf()
    private val _generatedDisplayText: MutableMap<Long, String> = mutableMapOf()

    val diagnostics: List<MacroConstructionDiagnostic>
        get() = _diagnostics.toList()

    val hasErrors: Boolean
        get() = _diagnostics.any { it.severity == MacroConstructionDiagnostic.Severity.ERROR }

    /**
     * `surfaceId -> MacroSurface` 反查。
     *
     * 由 [registerOriginSurface] 累积；ordinary checker / IDE 通过
     * `MacroConstructionDiagnostic.originSurfaceId` 解析回 macro 位点。
     */
    val originSurfaceById: Map<Long, MacroSurface>
        get() = _originSurfaceById.toMap()

    /**
     * `placeholderId -> originSurfaceId` 反查。
     *
     * Degraded mode 下生成的 `CfirErrorExpression` / `CfirInvalidDeclaration`
     * 等 typed error placeholder 通过 `macroOriginId` metadata 写入；
     * IDE / LSP 用此映射在 placeholder 之上回显原 macro 调用。
     */
    val placeholderOriginById: Map<Long, Long>
        get() = _placeholderOriginById.toMap()

    /** 可选：surface 展开后用于 IDE 展示的文本（不参与 semantic）。 */
    val generatedDisplayText: Map<Long, String>
        get() = _generatedDisplayText.toMap()

    fun addDiagnostic(diagnostic: MacroConstructionDiagnostic) {
        _diagnostics += diagnostic
    }

    fun addAll(diagnostics: Iterable<MacroConstructionDiagnostic>) {
        _diagnostics += diagnostics
    }

    fun registerOriginSurface(surface: MacroSurface) {
        _originSurfaceById[surface.surfaceId] = surface
    }

    fun registerPlaceholder(placeholderId: Long, originSurfaceId: Long) {
        _placeholderOriginById[placeholderId] = originSurfaceId
    }

    fun registerGeneratedDisplayText(surfaceId: Long, text: String) {
        _generatedDisplayText[surfaceId] = text
    }

    /** LSP / debug pass: 已知 placeholder id 反查原 surface。 */
    fun originSurfaceForPlaceholder(placeholderId: Long): MacroSurface? {
        val originId = _placeholderOriginById[placeholderId] ?: return null
        return _originSurfaceById[originId]
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
 * - 消费 [PreMacroRawBuildResult] + [MacroResolutionContext]
 * - 与 macro symbol index / executor / fragment parser 协作完成展开
 * - 输出 [MacroConstructionResult]
 *
 * 该接口禁止调用方直接接触 source [org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl]，
 * 唯一可注册路径是返回 [MacroConstructionResult.Success] / [MacroConstructionResult.Degraded] 后
 * 由上层调用 [recordExpandedRawFilesOnce]。
 */
interface MacroConstructionService {
    /**
     * 完整 construction 入口（baseline 第 1 节主流程）：
     *
     * ```
     * symbolIndex = buildMacroSymbolIndex(pre, libraries, macroArtifacts)
     * context     = bindMacroImports(pre, symbolIndex, ...)
     * result      = service.expand(pre, context, mode)
     * ```
     */
    fun expand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        mode: Mode,
    ): MacroConstructionResult

    enum class Mode {
        /** CLI：任何 [MacroConstructionResult.Degraded] / [MacroConstructionResult.ExecutorUnavailable] 都被视为失败。 */
        STRICT,

        /** IDE / analysis：允许 degraded placeholder。 */
        DEGRADED,
    }

    companion object {
        /**
         * 无宏 identity 实现：把 raw 文件原样打包为可注册输入，
         * 不调用 executor、不做任何展开。
         *
         * 即便 identity，也会基于 [MacroResolutionContext] 报告 same-package def/call 诊断
         * （baseline 第 4 节规则）。
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

/**
 * 便利入口：自动构造默认的 [MacroSymbolIndex] + [MacroResolutionContext]，
 * 再委托给 [MacroConstructionService.expand]。
 *
 * 调用方仅传入 [pre] 与 [mode] 即可；library / artifact / shared / builtin 入口
 * 通过命名参数注入以保持向后兼容。
 *
 * 主流程（baseline 第 1 节）建议显式分三步：
 *   1. [buildMacroSymbolIndex]
 *   2. [bindMacroImports]
 *   3. [MacroConstructionService.expand]
 * 仅当不需要 inspect index / context 时使用本便利入口。
 */
fun MacroConstructionService.expandWithDefaultContext(
    pre: PreMacroRawBuildResult,
    mode: MacroConstructionService.Mode,
    libraryDefinitions: List<MacroDefinitionEntry> = emptyList(),
    sharedBuiltinDefinitions: List<MacroDefinitionEntry> = emptyList(),
    macroArtifactDefinitions: List<MacroDefinitionEntry> = emptyList(),
    defaultMacroImports: List<org.cangnova.cangjie.name.FqName> = emptyList(),
): MacroConstructionResult {
    val symbolIndex = buildMacroSymbolIndex(
        pre = pre,
        libraryDefinitions = libraryDefinitions,
        sharedBuiltinDefinitions = sharedBuiltinDefinitions,
        macroArtifactDefinitions = macroArtifactDefinitions,
    )
    val context = bindMacroImports(
        pre = pre,
        symbolIndex = symbolIndex,
        defaultMacroImports = defaultMacroImports,
    )
    return expand(pre, context, mode)
}

private object IdentityMacroConstructionService : MacroConstructionService {
    override fun expand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
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
