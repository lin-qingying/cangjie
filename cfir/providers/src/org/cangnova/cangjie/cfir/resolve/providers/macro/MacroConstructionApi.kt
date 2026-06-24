package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement

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
 * @property cfirFile raw builder 构造出的原始 CFIR 文件。
 * @property surfaces 当前文件中采集到的 construction-only macro surface 列表。
 */
class PreMacroCfirFile internal constructor(
    /** raw builder 构造出的原始 CFIR 文件。 */
    val cfirFile: CfirFile,
    /** 当前文件中采集到的 construction-only macro surface 列表。 */
    val surfaces: List<MacroSurface> = emptyList(),
) {
    /** 当前文件是否声明为 `macro package`。 */
    val isMacroPackage: Boolean
        get() = cfirFile.packageDirective.isMacroPackage
}

/**
 * `buildPreMacroRawFilesNoRecord(...)` 的产物。
 *
 * 它**不**返回 `List<CfirFile>`，强制调用方必须经过
 * [MacroConstructionService.expand] 与 [recordExpandedRawFilesOnce] 才能
 * 把文件注册进 source provider。
 *
 * 该不变量是 baseline 第 2 节"硬性边界"的代码级表达。
 *
 * @property session 当前 raw build 所属的 CFIR session。
 * @property files 尚未注册进 source provider 的 pre-macro 文件集合。
 */
class PreMacroRawBuildResult internal constructor(
    /** 当前 raw build 所属的 CFIR session。 */
    val session: CfirSession,
    /** 尚未注册进 source provider 的 pre-macro 文件集合。 */
    val files: List<PreMacroCfirFile>,
) {
    /** 是否没有任何 pre-macro 文件。 */
    val isEmpty: Boolean get() = files.isEmpty()

    /** pre-macro 文件数量。 */
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
 * @param session 当前 raw build 所属的 CFIR session。
 * @param rawCfirFiles raw builder 已经构造完成但尚未注册的 CFIR 文件列表。
 * @return 带 construction 边界约束的 pre-macro raw build 结果。
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
 *
 * @property session 当前可注册文件所属的 CFIR session。
 * @property files 已经过 macro construction 处理、允许进入 source provider 的文件列表。
 */
class RecordableRawCfirFiles internal constructor(
    /** 当前可注册文件所属的 CFIR session。 */
    val session: CfirSession,
    /** 已经过 macro construction 处理、允许进入 source provider 的文件列表。 */
    val files: List<CfirFile>,
) {
    /** 可注册文件数量。 */
    val size: Int get() = files.size

    /** 是否没有任何可注册文件。 */
    val isEmpty: Boolean get() = files.isEmpty()
}

/**
 * 单条 macro 构造诊断（baseline 第 1 节 + 第 9 节）。
 *
 * @property severity 诊断严重级别。
 * @property message 面向用户或调试输出的诊断文本。
 * @property originSurfaceId 关联的 surface id；非 surface 诊断可为 null。
 * @property originSource 非 surface 诊断的原始源码位点。
 * @property kind 结构化诊断种类，供 renderer 与上层工具稳定分派。
 * @property artifactPackage package-level artifact 相关诊断的包名。
 * @property artifactPath `.cjo` 或 artifact 本体路径。
 * @property macroLibraryPath 宏动态库路径。
 * @property diagnosticOrigin 诊断来源，用于区分 construction、executor 与宏库上报。
 * @property compileInvocationId 宏包源码编译 invocation id。
 * @property sourceDiagnosticsRef 宏包源码编译原始诊断引用。
 * @property hint 宏库 diagReport 提供的补充提示。
 * @property tokenRangeBeginLine protocol 诊断 token range 起始行。
 * @property tokenRangeBeginColumn protocol 诊断 token range 起始列。
 * @property tokenRangeEndLine protocol 诊断 token range 结束行。
 * @property tokenRangeEndColumn protocol 诊断 token range 结束列。
 * @property relatedName 结构化相关名称，避免从 message 中反解析。
 * @property relatedTargets 结构化相关目标集合。
 */
data class MacroConstructionDiagnostic(
    /** 诊断严重级别。 */
    val severity: Severity,
    /** 面向用户或调试输出的诊断文本。 */
    val message: String,
    /**
     * 关联 surface id；可为 null（例如 alias 冲突诊断 binding 整条 import）。
     * Batch 9 的 diagnostic renderer 通过该 id 反查 [MacroExpansionRegistry.originSurfaceById]。
     */
    val originSurfaceId: Long? = null,
    /** 非 surface 诊断的原始源码位点，例如 macro import alias 冲突。 */
    val originSource: CjSourceElement? = null,
    /** Baseline 第 9 节 typed factory：未展开 / 展开失败 / 同包 / 无 executor / 等。 */
    val kind: Kind = Kind.GENERIC,
    /** package-level artifact 相关诊断的包名。 */
    val artifactPackage: FqName? = null,
    /** `.cjo` 或 artifact 本体路径。 */
    val artifactPath: String? = null,
    /** 宏动态库路径。 */
    val macroLibraryPath: String? = null,
    /** 诊断来源；`DIAG_REPORT` 表示宏库主动上报的用户诊断。 */
    val diagnosticOrigin: Origin = Origin.CONSTRUCTION,
    /** 宏包源码编译 invocation id；用于把使用方诊断关联回独立 `--compile-macro` invocation。 */
    val compileInvocationId: String? = null,
    /** 宏包源码编译原始诊断引用；由外层 CLI / build orchestration 维护其生命周期。 */
    val sourceDiagnosticsRef: String? = null,
    /** 宏库 diagReport 的 hint。 */
    val hint: String? = null,
    /** diagReport / protocol 诊断的 token range 起点。 */
    val tokenRangeBeginLine: Int? = null,
    /** protocol 诊断 token range 起始列。 */
    val tokenRangeBeginColumn: Int? = null,
    /** diagReport / protocol 诊断的 token range 终点。 */
    val tokenRangeEndLine: Int? = null,
    /** protocol 诊断 token range 结束列。 */
    val tokenRangeEndColumn: Int? = null,
    /** 结构化诊断名，用于 alias / unresolved 等非 surface 诊断，避免从 message 反解析。 */
    val relatedName: Name? = null,
    /** 结构化诊断目标，用于 alias conflict 等需要完整目标集合的诊断。 */
    val relatedTargets: List<FqName> = emptyList(),
) {
    /** Macro construction 诊断严重级别。 */
    enum class Severity {
        /** 信息级诊断，不阻断 construction。 */
        INFO,
        /** 警告级诊断，不阻断 strict 模式但需要展示给用户。 */
        WARNING,
        /** 错误级诊断；strict 模式下会导致 construction 失败。 */
        ERROR,
    }

    /** Macro construction 诊断来源。 */
    enum class Origin {
        /** 本地 construction 流水线直接产生的诊断。 */
        CONSTRUCTION,
        /** artifact resolver 发现的依赖、路径或元数据问题。 */
        ARTIFACT_RESOLVER,
        /** 外层 CLI / build orchestration 产生的诊断。 */
        ORCHESTRATION,
        /** executor 启动、通信或调用阶段产生的诊断。 */
        EXECUTOR,
        /** 宏库通过 diagReport 主动上报的用户诊断。 */
        DIAG_REPORT,
    }

    /** Macro construction 诊断的结构化分类。 */
    enum class Kind {
        /** 未细分的通用诊断。 */
        GENERIC,
        /** macro 调用未被展开。 */
        MACRO_NOT_EXPANDED,
        /** macro 展开失败。 */
        MACRO_EXPANSION_FAILED,
        /** macro package 未定义或不可见。 */
        MACRO_UNDEFINED_PACKAGE,
        /** macro 标识符未声明。 */
        MACRO_UNDECLARED_IDENTIFIER,
        /** 目标应为 macro 定义但实际不是。 */
        MACRO_EXPECT_MACRO_DEFINITION,
        /** macro 依赖源码编译失败。 */
        MACRO_DEPENDENCY_COMPILE_FAILED,
        /** import / lookup 得到多个候选，无法唯一匹配。 */
        MACRO_AMBIGUOUS_MATCH,
        /** 找不到依赖 BCHIR 产物。 */
        MACRO_CANNOT_FIND_DEPENDENCY_BCHIR,
        /** 调用点要求普通 macro，但目标形态不匹配。 */
        MACRO_EXPECT_PLAIN_MACRO,
        /** 调用点要求 attributed macro，但目标形态不匹配。 */
        MACRO_EXPECT_ATTRIBUTED_MACRO,
        /** 使用了 `@!` 强制展开但目标不支持。 */
        MACRO_EXPAND_ATEXCL,
        /** attr token 序列非法。 */
        MACRO_INVALID_ATTR_TOKENS,
        /** input token 序列非法。 */
        MACRO_INVALID_INPUT_TOKENS,
        /** macro payload 中存在非法转义。 */
        MACRO_INVALID_ESCAPE,
        /** 同包内定义与调用 macro，违反 construction 边界。 */
        MACRO_SAME_PACKAGE_DEF_CALL,
        /** macro import alias 发生冲突。 */
        MACRO_ALIAS_CONFLICT,
        /** 当前环境没有可用 executor。 */
        MACRO_EXECUTOR_UNAVAILABLE,
        /** 无法打开宏动态库。 */
        MACRO_CANNOT_OPEN_LIB,
        /** 动态库中找不到目标方法。 */
        MACRO_CANNOT_FIND_METHOD,
        /** executor evaluate 调用失败。 */
        MACRO_EVALUATE_FAILED,
        /** executor 返回展开失败。 */
        MACRO_EXPAND_FAILED,
        /** 展开结果不应继续包含 macro call。 */
        MACRO_EXPAND_CODE_SHOULD_NOT_HAVE_MACROCALL,
        /** 保存 macro 调用文件失败。 */
        MACRO_CALL_SAVE_FILE_FAILED,
        /** executor 协议错误。 */
        MACRO_EXECUTOR_PROTOCOL_ERROR,
        /** executor server 断开连接。 */
        MACRO_EXECUTOR_SERVER_DISCONNECTED,
        /** executor 调用超时。 */
        MACRO_EXECUTOR_TIMEOUT,
        /** executor server 崩溃。 */
        MACRO_EXECUTOR_SERVER_CRASH,
        /** 展开结果 re-evaluation 或 fragment reparse 失败。 */
        MACRO_REEVALUATION_FAILED,
        /** macro 调用无法解析。 */
        MACRO_UNRESOLVED,
        /** macro forest 展开检测到循环。 */
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
 * - `generatedSourceOriginById` 维护 successful splice 后生成 CFIR 的 source
 *   与原始 macro surface 的关系；ordinary checker 诊断提交前通过该表
 *   重定位到原 macro 调用位点。
 *
 * `MacroExpansionRegistry` 作为 [org.cangnova.cangjie.cfir.session.CfirSessionComponent]
 * 候选，可通过 [org.cangnova.cangjie.cfir.session.CfirSession.register] 挂到 session 上；
 * 多模块 build 时每个 source session 各持一份。
 */
class MacroExpansionRegistry : org.cangnova.cangjie.cfir.session.CfirSessionComponent {
    /** construction 期间累积的宏诊断列表。 */
    private val _diagnostics: MutableList<MacroConstructionDiagnostic> = mutableListOf()
    /** `surfaceId -> MacroSurface` 反查表。 */
    private val _originSurfaceById: MutableMap<Long, MacroSurface> = mutableMapOf()
    /** degraded placeholder id 到原始 surface id 的映射。 */
    private val _placeholderOriginById: MutableMap<Long, Long> = mutableMapOf()
    /** 展开产物 source 到原始 surface id 的映射。 */
    private val _generatedSourceOriginById: MutableMap<AbstractCjSourceElement, Long> = mutableMapOf()
    /** surface id 到 IDE / debug 展示文本的可选映射。 */
    private val _generatedDisplayText: MutableMap<Long, String> = mutableMapOf()
    /** file identity 到宏展开 cache key 的映射。 */
    private val _cacheKeys: MutableMap<String, MacroExpansionCacheKey> = linkedMapOf()

    /** construction 期间产生的诊断快照。 */
    val diagnostics: List<MacroConstructionDiagnostic>
        get() = _diagnostics.toList()

    /** 是否存在错误级 construction 诊断。 */
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

    /**
     * `generated source -> originSurfaceId` 反查。
     *
     * Successful splice 后，普通 checker 仍只遍历 final CFIR；当 checker 在
     * 展开产物 source 上报错时，diagnostic reporter 通过此表定位回原 macro
     * 调用 source。
     */
    val generatedSourceOriginById: Map<AbstractCjSourceElement, Long>
        get() = _generatedSourceOriginById.toMap()

    /** 可选：surface 展开后用于 IDE 展示的文本（不参与 semantic）。 */
    val generatedDisplayText: Map<Long, String>
        get() = _generatedDisplayText.toMap()

    /** 追加一条 macro construction 诊断。 */
    fun addDiagnostic(diagnostic: MacroConstructionDiagnostic) {
        _diagnostics += diagnostic
    }

    /** 批量追加 macro construction 诊断，保持输入顺序。 */
    fun addAll(diagnostics: Iterable<MacroConstructionDiagnostic>) {
        _diagnostics += diagnostics
    }

    /** 注册一个原始 macro surface，建立 surface id 到 surface 的反查。 */
    fun registerOriginSurface(surface: MacroSurface) {
        _originSurfaceById[surface.surfaceId] = surface
    }

    /** 注册 degraded placeholder 与原始 surface 的对应关系。 */
    fun registerPlaceholder(placeholderId: Long, originSurfaceId: Long) {
        _placeholderOriginById[placeholderId] = originSurfaceId
    }

    /** 注册展开产物 source 到原始 surface 的对应关系；source 为空时忽略。 */
    fun registerGeneratedSource(source: AbstractCjSourceElement?, originSurfaceId: Long) {
        if (source != null) {
            _generatedSourceOriginById[source] = originSurfaceId
        }
    }

    /**
     * 递归注册展开产物 CFIR 子树中所有 source 与原始 surface 的对应关系。
     *
     * 普通 checker 只看到 final CFIR；此映射使诊断 reporter 能把 generated source
     * 上的错误重定位回 macro 调用位点。
     */
    fun registerGeneratedCfirElement(element: CfirElement, originSurfaceId: Long) {
        element.accept(
            object : CfirDefaultVisitor<Unit, Unit>() {
                override fun visitElement(element: CfirElement, data: Unit) {
                    registerGeneratedSource(element.source, originSurfaceId)
                    element.acceptChildren(this, data)
                }
            },
            Unit,
        )
    }

    /** 记录 surface 展开后的展示文本，供 IDE / debug pass 使用。 */
    fun registerGeneratedDisplayText(surfaceId: Long, text: String) {
        _generatedDisplayText[surfaceId] = text
    }

    /**
     * 注册某 `CfirFile` 的 cache key（baseline 第 11 节）。
     *
     * key 由 [FrontendMacroConstructionService] 在 splice 完成后逐文件计算，
     * 上游 IDE / build cache 通过 [cacheKeys] 与 [moduleSignature] 决定何时失效。
     */
    fun registerCacheKey(fileIdentity: String, key: MacroExpansionCacheKey) {
        _cacheKeys[fileIdentity] = key
    }

    /** 按 fileIdentity 索引的 cache key 表。 */
    val cacheKeys: Map<String, MacroExpansionCacheKey>
        get() = _cacheKeys.toMap()

    /**
     * 模块级签名：把所有文件 cache key 的 stableHash 按 fileIdentity 排序后聚合成一个 hash。
     *
     * 任何文件的任一 cache 维度变化都会改变本签名；
     * baseline 第 11 节 "macro artifact / import / builtin registry 改变时模块级失效"。
     */
    fun moduleSignature(): String {
        if (_cacheKeys.isEmpty()) return EMPTY_MODULE_SIGNATURE
        val parts = _cacheKeys.entries
            .sortedBy { it.key }
            .flatMap { (id, key) -> listOf(id, key.stableHash()) }
        return org.cangnova.cangjie.utils.StableHash.sha256Of(parts)
    }

    /** LSP / debug pass: 已知 placeholder id 反查原 surface。 */
    fun originSurfaceForPlaceholder(placeholderId: Long): MacroSurface? {
        val originId = _placeholderOriginById[placeholderId] ?: return null
        return _originSurfaceById[originId]
    }

    /** 普通 checker 在 generated source 上报错时，反查原 macro 调用 source。 */
    fun originSourceForGeneratedSource(source: AbstractCjSourceElement): CjSourceElement? {
        val originId = _generatedSourceOriginById[source] ?: return null
        return _originSurfaceById[originId]?.sourceRange?.source
    }

    /** Macro expansion registry 的共享常量。 */
    companion object {
        /** 无任何 construction 状态的空 registry。 */
        val EMPTY: MacroExpansionRegistry = MacroExpansionRegistry()

        /** 没有文件级 cache key 时使用的模块签名。 */
        const val EMPTY_MODULE_SIGNATURE: String = "macro-module:empty"
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
    /** construction 过程的 registry，无论成功、降级还是失败都必须返回。 */
    abstract val registry: MacroExpansionRegistry

    /**
     * construction 完全成功，文件可直接注册进 source provider。
     *
     * @property recordableFiles 可注册的 expanded raw CFIR 文件。
     * @property registry construction 过程信息与诊断 registry。
     */
    data class Success(
        /** 可注册的 expanded raw CFIR 文件。 */
        val recordableFiles: RecordableRawCfirFiles,
        /** construction 过程信息与诊断 registry。 */
        override val registry: MacroExpansionRegistry,
    ) : MacroConstructionResult()

    /**
     * construction 降级成功，文件含 typed error placeholder，可供 IDE / analysis 继续工作。
     *
     * @property recordableFiles 含 degraded placeholder 的可注册文件。
     * @property registry construction 过程信息与诊断 registry。
     */
    data class Degraded(
        /** 含 degraded placeholder 的可注册文件。 */
        val recordableFiles: RecordableRawCfirFiles,
        /** construction 过程信息与诊断 registry。 */
        override val registry: MacroExpansionRegistry,
    ) : MacroConstructionResult()

    /**
     * construction 失败且没有可注册文件。
     *
     * @property registry construction 过程信息与诊断 registry。
     */
    data class Failed(
        /** construction 过程信息与诊断 registry。 */
        override val registry: MacroExpansionRegistry,
    ) : MacroConstructionResult()

    /**
     * executor 不可用导致无法完成真实展开。
     *
     * @property registry construction 过程信息与诊断 registry。
     */
    data class ExecutorUnavailable(
        /** construction 过程信息与诊断 registry。 */
        override val registry: MacroExpansionRegistry,
    ) : MacroConstructionResult()

    /**
     * construction 被语义或环境条件阻塞，需要上层 orchestration 决策。
     *
     * @property registry construction 过程信息与诊断 registry。
     */
    data class Blocked(
        /** construction 过程信息与诊断 registry。 */
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
        classification: MacroDemandClassification,
        mode: Mode,
        preConstructionDiagnostics: List<MacroConstructionDiagnostic> = emptyList(),
    ): MacroConstructionResult

    /** Macro construction 的运行模式，决定失败是否可以降级为 placeholder。 */
    enum class Mode {
        /** CLI：任何 [MacroConstructionResult.Degraded] / [MacroConstructionResult.ExecutorUnavailable] 都被视为失败。 */
        STRICT,

        /** IDE / analysis：允许 degraded placeholder。 */
        DEGRADED,
    }

    /** [MacroConstructionService] 的默认实现和结果工厂集合。 */
    companion object {
        /**
         * Macro construction 算法版本（baseline §11 cache key 第 10 维）。
         *
         * 主流程顺序、forest 推进策略、splice 语义、degraded placeholder 形态
         * 任一变更都必须递增；上游 cache 据此整体失效。
         */
        const val ALGORITHM_VERSION: Int = 1

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
         *
         * @param pre 原始 pre-macro build 结果，用于保持 session 身份。
         * @param files 成功展开后可注册的 CFIR 文件列表。
         * @param registry construction registry。
         * @return 包装后的成功 construction 结果。
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
         *
         * @param pre 原始 pre-macro build 结果，用于保持 session 身份。
         * @param files 降级后仍可注册的 CFIR 文件列表。
         * @param registry construction registry。
         * @return 包装后的 degraded construction 结果。
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
 *
 * @receiver 执行实际 construction 的服务实现。
 * @param pre pre-macro raw build 结果。
 * @param mode construction 运行模式。
 * @param libraryDefinitions 从普通库反序列化得到的宏定义。
 * @param sharedBuiltinDefinitions 共享基础库提供的宏定义。
 * @param macroArtifactDefinitions 独立 macro artifact 提供的宏定义。
 * @param defaultMacroImports 默认隐式 macro import 包列表。
 * @return construction 结果。
 */
fun MacroConstructionService.expandWithDefaultContext(
    pre: PreMacroRawBuildResult,
    mode: MacroConstructionService.Mode,
    libraryDefinitions: List<MacroDefinitionEntry> = emptyList(),
    sharedBuiltinDefinitions: List<MacroDefinitionEntry> = emptyList(),
    macroArtifactDefinitions: List<MacroDefinitionEntry> = emptyList(),
    defaultMacroImports: List<FqName> = emptyList(),
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
    val classification = MacroDemandClassification.create(pre, defaultMacroImports = defaultMacroImports)
    classification.freezeFinal(
        libraryDefinitions = libraryDefinitions,
        sharedBuiltinDefinitions = sharedBuiltinDefinitions,
        macroArtifactDefinitions = macroArtifactDefinitions,
    )
    return expand(pre, context, classification, mode)
}

/** 不做真实 macro 展开的 identity service，用于无宏或测试场景维持边界约束。 */
private object IdentityMacroConstructionService : MacroConstructionService {
    /**
     * 将 pre-macro 文件原样包装为可注册文件；若已有 construction 诊断，则按 [mode]
     * 决定返回 failed 还是 degraded。
     */
    override fun expand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        classification: MacroDemandClassification,
        mode: MacroConstructionService.Mode,
        preConstructionDiagnostics: List<MacroConstructionDiagnostic>,
    ): MacroConstructionResult {
        val files = pre.files.map(PreMacroCfirFile::cfirFile)
        if (preConstructionDiagnostics.isNotEmpty()) {
            val registry = MacroExpansionRegistry().apply { addAll(preConstructionDiagnostics) }
            return if (registry.hasErrors && mode == MacroConstructionService.Mode.STRICT) {
                MacroConstructionResult.Failed(registry)
            } else {
                MacroConstructionService.degradedOf(pre, files, registry)
            }
        }
        return MacroConstructionService.successOf(
            pre = pre,
            files = files,
            registry = MacroExpansionRegistry.EMPTY,
        )
    }
}
