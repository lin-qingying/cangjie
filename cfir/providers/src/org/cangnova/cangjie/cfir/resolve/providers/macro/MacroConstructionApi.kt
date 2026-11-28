package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.parsingFileFacts
import org.cangnova.cangjie.parsing.ParsingFileFacts
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.parsing.CjParserSettings
import org.cangnova.cangjie.cfir.declarations.publishParsingDiagnostics
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.parsing.CjParserSourceContext

/**
 * Raw CFIR 构建产物中、尚未注册到 source provider 的单个文件包装。
 *
 * 文件、macro surfaces 与元数据事务是同次 raw 构建的完整产品，不能分别重建或按名称重新配对。
 * @property cfirFile raw builder 构造出的原始 CFIR 文件。
 * @property surfaces 当前文件中采集到的 construction-only macro surface 列表。
 */
class PreMacroCfirFile(
    /** raw builder 构造出的原始 CFIR 文件。 */
    val cfirFile: CfirFile,
    /** 当前文件中采集到的 construction-only macro surface 列表。 */
    val surfaces: List<MacroSurface> = emptyList(),
    /** raw 与该文件的所有 fragment 共同使用的私有元数据事务。 */
    val annotationMetadata: CfirAnnotationMetadataBuilder,
    /** 与文件树同代的完整 parser 报告；宏和条件编译过滤不能丢弃或重新分配顺序。 */
    val parsingDiagnosticPublication: CjParsingDiagnosticPublication = CjParsingDiagnosticPublication.EMPTY,
    val parserSettings: CjParserSettings = CjParserSettings(),
) {
    init {
        cfirFile.publishParsingDiagnostics(parsingDiagnosticPublication, parserSettings)
    }
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

    /**
     * 在宏分类前替换 raw 文件集合；保留 construction 边界，不允许调用方直接注册
     * 被裁剪结果。条件编译、增量过滤等 raw owner 使用该入口发布新的 pre 状态。
     */
    public fun mapFiles(transform: (PreMacroCfirFile) -> PreMacroCfirFile): PreMacroRawBuildResult =
        PreMacroRawBuildResult(session, files.map(transform))
}

/** 替换单文件 raw CFIR，同时保留该文件尚未消费的 macro surfaces。 */
public fun PreMacroCfirFile.withCfirFile(cfirFile: CfirFile): PreMacroCfirFile =
    PreMacroCfirFile(cfirFile, surfaces, annotationMetadata, parsingDiagnosticPublication, parserSettings)

/** 在 raw 裁剪 owner 中同步移除已删除声明对应的 macro surfaces。 */
public fun PreMacroCfirFile.withSurfaces(predicate: (MacroSurface) -> Boolean): PreMacroCfirFile =
    PreMacroCfirFile(cfirFile, surfaces.filter(predicate), annotationMetadata, parsingDiagnosticPublication, parserSettings)

/** construction 的最终文件保持输入顺序；封闭元数据不是另一次 raw 构建。 */
private fun PreMacroRawBuildResult.sealAnnotationMetadata(finalFiles: List<CfirFile>): List<CfirAnnotationMetadataSnapshot> {
    require(finalFiles.size == files.size) { "Construction must retain its per-file metadata ownership." }
    finalFiles.forEach { it.checkPrimaryConstructorParameterFieldsMaterialized() }
    finalFiles.forEachIndexed { index, file ->
        prepareDefaultParameterFunctions(file, files[index].annotationMetadata, files[index].parserSettings.backend)
    }
    return files.mapIndexed { index, file -> file.annotationMetadata.seal(listOf(finalFiles[index])) }
}

/** 失败结果只丢弃本次私有事务，不影响 session 已发布的其他文件。 */
fun PreMacroRawBuildResult.abortAnnotationMetadata() {
    files.forEach { it.annotationMetadata.abortIfOpen() }
}

/**
 * 构造 [PreMacroRawBuildResult]。
 *
 * Raw builder（PSI 或 LightTree）完成构建后，
 * 必须经此入口包装产物，**不得**直接调用 source provider 的注册接口。
 *
 * @param session 当前 raw build 所属的 CFIR session。
 * @param files raw builder 已构造但尚未注册的完整单文件产品。
 * @return 带 construction 边界约束的 pre-macro raw build 结果。
 */
fun buildPreMacroRawFiles(
    session: CfirSession,
    files: List<PreMacroCfirFile>,
): PreMacroRawBuildResult {
    require(files.map { it.annotationMetadata }.distinct().size == files.size) { "Each raw file requires its own metadata transaction." }
    require(files.all { it.cfirFile.moduleData.session === session }) { "Raw files must belong to their construction session." }
    return PreMacroRawBuildResult(
        session = session,
        files = files.toList(),
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
    /** 按最终文件封闭的 metadata，provider 发布文件前一并交付索引。 */
    val annotationMetadata: List<CfirAnnotationMetadataSnapshot>,
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
        /** 宏重解析结果不符合声明槽位要求。 */
        MACRO_EXPECT_DECLARATION,
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
/** 实际 parser 调用的完整报告；宿主输入、source map 与报告共同冻结，不随保留的 CFIR 文件原地换代。 */
data class MacroParsingDiagnosticRecord(
    val originNode: MacroCallNode,
    /** 调用当时的完整 raw owner；同一 surface 后续重新登记不能改写已提交代次的来源。 */
    val originFile: CfirFile,
    /** 调用前的宿主 parser 产品；文件副本可共享该身份，新源码代次不能重用。 */
    val originDiagnosticPublication: CjParsingDiagnosticPublication,
    val originSourceContext: CjParserSourceContext,
    val diagnosticPublication: CjParsingDiagnosticPublication,
    val sourceMap: MacroFragmentSourceMap?,
    /** 在真实 parser 调用前分配；seal/失败状态和映射后的显示位置不决定调用顺序。 */
    val invocationSequence: Long,
    val syntaxFailure: Boolean = false,
)

class MacroExpansionRegistry : org.cangnova.cangjie.cfir.session.CfirSessionComponent {
    private data class ParsingInvocation(
        val sequence: Long,
        val file: CfirFile,
        val publication: CjParsingDiagnosticPublication,
        val sourceContext: CjParserSourceContext,
    )
    private data class CompletedParsingReport(val record: MacroParsingDiagnosticRecord, val constructionCommitted: Boolean)
    private data class GeneratedSourceOrigin(
        val surfaceId: Long,
        val fragmentSourceMap: MacroFragmentSourceMap?,
        val fallbackToSurface: Boolean = true,
    )

    /** construction 期间累积的宏诊断列表。 */
    private val _diagnostics: MutableList<MacroConstructionDiagnostic> = mutableListOf()
    /** 独立的 parser 错误通道，禁止 ordinary CFIR collector 将其映射为语义工厂。 */
    private val _parsingFailures: MutableList<MacroFragmentResult.SyntaxFailure> = mutableListOf()
    private val parsingInvocations = java.util.IdentityHashMap<MacroCallNode, ParsingInvocation>()
    private var nextParsingInvocationSequence = 0L
    private val parsingFailureRecords = mutableListOf<MacroParsingDiagnosticRecord>()
    private val pendingParsingReports = linkedMapOf<Long, MacroParsingDiagnosticRecord>()
    private val completedParsingReports = linkedMapOf<Long, CompletedParsingReport>()
    private val _originFileBySurfaceId: MutableMap<Long, CfirFile> = mutableMapOf()
    /** 原始文件快照不变；依源码构造顺序更新每个文件的 parser 角色事务。 */
    private val _parsingFactsByFile: MutableMap<CfirFile, ParsingFileFacts> = java.util.IdentityHashMap()
    /** `surfaceId -> MacroSurface` 反查表。 */
    private val _originSurfaceById: MutableMap<Long, MacroSurface> = mutableMapOf()
    /** `fileIdentity -> 原始 macro surface 列表`。 */
    private val _originSurfacesByFileIdentity: MutableMap<String, MutableList<MacroSurface>> = linkedMapOf()
    /** `fileIdentity -> construction 阶段已消费的 macro 简单名集合`。 */
    private val _usedMacroNamesByFileIdentity: MutableMap<String, MutableSet<Name>> = linkedMapOf()
    /** `fileIdentity -> macro package -> construction 阶段已消费的 macro 简单名集合`。 */
    private val _usedMacroNamesByPackageByFileIdentity: MutableMap<String, MutableMap<FqName, MutableSet<Name>>> = linkedMapOf()
    /** `fileIdentity -> construction 阶段已消费的 macro package 集合`。 */
    private val _usedMacroPackagesByFileIdentity: MutableMap<String, MutableSet<FqName>> = linkedMapOf()
    /** 编译单元内声明的 macro package 身份集合（官方语义：macro package import 是宏注册，永不报 unused）。 */
    private val _declaredMacroPackages: MutableSet<FqName> = linkedSetOf()
    /** degraded placeholder id 到原始 surface id 的映射。 */
    private val _placeholderOriginById: MutableMap<Long, Long> = mutableMapOf()
    /** 展开产物 source 到原始 surface 与可选逐 token source map 的映射。 */
    private val _generatedSourceOriginById: MutableMap<AbstractCjSourceElement, GeneratedSourceOrigin> = mutableMapOf()
    /** file identity 到宏展开 cache key 的映射。 */
    private val _cacheKeys: MutableMap<String, MacroExpansionCacheKey> = linkedMapOf()

    /** construction 期间产生的诊断快照。 */
    val diagnostics: List<MacroConstructionDiagnostic>
        get() = _diagnostics.toList()

    /** 是否存在错误级 construction 诊断。 */
    val hasErrors: Boolean
        get() = _parsingFailures.any { it.diagnostics.any { diagnostic -> diagnostic.isError } } ||
            _diagnostics.any { it.severity == MacroConstructionDiagnostic.Severity.ERROR }

    val parsingFailures: List<MacroFragmentResult.SyntaxFailure> get() = _parsingFailures.toList()
    /** metadata seal 后才存在可注册 CFIR 产品；失败单元的 parser 报告另由完整诊断时间线读取。 */
    val parsingReports: List<MacroParsingDiagnosticRecord>
        get() = completedParsingReports.values.filter { it.constructionCommitted }.map { it.record }

    /** 成功与失败共用 producer 时间线，不能先按提交状态分桶再决定独立根首报。 */
    val parsingDiagnosticRecords: List<MacroParsingDiagnosticRecord>
        get() = (completedParsingReports.values.map { it.record } + parsingFailureRecords).sortedBy { it.invocationSequence }

    fun beginParsingInvocation(node: MacroCallNode) {
        val file = checkNotNull(originFileForSurface(node.surface.surfaceId)) { "A parser invocation requires its actual source owner" }
        val publication = checkNotNull(file.parsingDiagnosticPublication) { "A parser invocation requires its frozen host diagnostic product" }
        val sourceContext = checkNotNull(file.parsingSourceContext) { "A parser invocation requires its frozen host source input" }
        parsingInvocations[node] = ParsingInvocation(nextParsingInvocationSequence++, file, publication, sourceContext)
    }

    /** Splicer 正常返回后暂存实际接受的批次；同一 surface 的淘汰代次由新结果替换。 */
    fun stageParsingReports(slots: List<MacroReplaceSlot>) {
        val records = slots.map { slot ->
            val fragment = slot.fragment
            val publication = fragment.diagnosticPublication
            val sourceMap = when (fragment) {
                is MacroFragmentResult.Success -> fragment.sourceMap
                is MacroFragmentResult.CustomAnnotation -> fragment.sourceMap
                else -> error("Only an accepted fragment can stage successful parser reports")
            }
            val invocation = checkNotNull(parsingInvocations[fragment.originNode]) { "A successful parser report must retain its actual invocation" }
            check(originFileForSurface(slot.origin.surfaceId) === invocation.file) { "A staged parser report cannot change its invocation owner" }
            slot.origin.surfaceId to MacroParsingDiagnosticRecord(
                fragment.originNode, invocation.file, invocation.publication, invocation.sourceContext,
                publication, sourceMap, invocation.sequence,
            )
        }
        records.forEach { (surface, record) -> pendingParsingReports[surface] = record }
    }

    /**
     * construction 正常返回后保留完整 parser 证据。metadata seal 的成功只决定可注册 CFIR 产品，
     * PARSE 失败不能额外抑制成功兄弟根的 WARNING；取消与内部异常不会完成本批次。
     */
    internal fun completeParsingReports(constructionCommitted: Boolean) {
        pendingParsingReports.forEach { (surface, record) ->
            completedParsingReports[surface] = CompletedParsingReport(record, constructionCommitted)
        }
        pendingParsingReports.clear()
    }

    /** 保留原始 parser 输入和错误范围，CLI 与 IDE 使用同一份语法证据。 */
    fun addParsingFailure(failure: MacroFragmentResult.SyntaxFailure) {
        val invocation = checkNotNull(parsingInvocations[failure.originNode]) { "A failed parser report must retain its actual invocation" }
        val record = MacroParsingDiagnosticRecord(
            failure.originNode, invocation.file, invocation.publication, invocation.sourceContext,
            failure.diagnosticPublication, failure.sourceMap, invocation.sequence, syntaxFailure = true,
        )
        _parsingFailures += failure
        parsingFailureRecords += record
    }

    /** 宏的原始文件承担宿主位置映射，不从展开后 CFIR 或 package 名推测文件。 */
    fun originFileForSurface(surfaceId: Long): CfirFile? = _originFileBySurfaceId[surfaceId]

    /** 返回该宏所属原文件当前的文法事实，不从展开声明或文件名推断。 */
    fun parsingFactsForSurface(surfaceId: Long): ParsingFileFacts? =
        _originFileBySurfaceId[surfaceId]?.let { _parsingFactsByFile[it] }

    /** parser 在发现后续语法错误前也可能已改变文件角色；完成结果均需接入事务。 */
    fun acceptParsingFacts(surfaceId: Long, facts: ParsingFileFacts) {
        val file = checkNotNull(_originFileBySurfaceId[surfaceId]) { "Macro surface has no original file." }
        val previous = checkNotNull(_parsingFactsByFile[file]) { "Original file has no published parsing facts." }
        check(previous.sourceKind == facts.sourceKind) { "Macro reparse changed the original source kind." }
        check(!previous.cjmpRole.isCommon || facts.cjmpRole.isCommon) { "Macro reparse lost the original common file role." }
        check(!previous.cjmpRole.isSpecific || facts.cjmpRole.isSpecific) { "Macro reparse lost the original specific file role." }
        _parsingFactsByFile[file] = facts
    }

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
        get() = _generatedSourceOriginById.mapValues { (_, origin) -> origin.surfaceId }

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

    /**
     * 登记某个源文件中 raw builder 捕获的全部原始 macro surface。
     *
     * 官方 `CheckAnnoBeforeMacro` 读取的是文件级 `originalMacroCallNodes`；
     * CFIR 对应 owner 是 construction registry 中按源文件保存的 surface forest，
     * 不能从 final CFIR 的 declaration annotation 列表反推。
     */
    fun registerFileSurfaces(file: CfirFile, surfaces: List<MacroSurface>) {
        if (surfaces.isEmpty()) return
        file.parsingFileFacts?.let { facts -> _parsingFactsByFile.putIfAbsent(file, facts) }
        for (surface in surfaces) {
            registerOriginSurface(surface)
            _originFileBySurfaceId[surface.surfaceId] = file
        }
        for (fileIdentity in file.macroExpansionFileIdentities()) {
            val existing = _originSurfacesByFileIdentity.getOrPut(fileIdentity) { mutableListOf() }
            val knownSurfaceIds = existing.mapTo(linkedSetOf()) { it.surfaceId }
            for (surface in surfaces) {
                if (knownSurfaceIds.add(surface.surfaceId)) {
                    existing += surface
                }
            }
        }
    }

    /** 查询某源文件 raw builder 捕获的全部原始 macro surface。 */
    fun originSurfaces(file: CfirFile): List<MacroSurface> {
        val result = linkedMapOf<Long, MacroSurface>()
        for (fileIdentity in file.macroExpansionFileIdentities()) {
            for (surface in _originSurfacesByFileIdentity[fileIdentity].orEmpty()) {
                result.putIfAbsent(surface.surfaceId, surface)
            }
        }
        return result.values.toList()
    }

    /**
     * 登记某个源文件中已经被 construction routing 消费的 macro surface。
     *
     * ordinary checker 看到的是展开后的 final CFIR，原始 macro 调用可能已经被 splice
     * 移除；unused-import 必须通过这份文件级事实恢复官方 `GetUsedMacroDecls(file)` 语义。
     */
    fun registerUsedMacroSurface(file: CfirFile, surface: MacroSurface) {
        val qualifiedName = surface.qualifiedName ?: return
        val name = qualifiedName.shortName()
        val packageFqName = qualifiedName.parent()
        for (fileIdentity in file.macroExpansionFileIdentities()) {
            _usedMacroNamesByFileIdentity.getOrPut(fileIdentity) { linkedSetOf() } += name
            if (!packageFqName.isRoot) {
                _usedMacroNamesByPackageByFileIdentity
                    .getOrPut(fileIdentity) { linkedMapOf() }
                    .getOrPut(packageFqName) { linkedSetOf() } += name
            }
        }
    }

    /**
     * 登记某个源文件中已经被 construction routing 消费的已解析 macro 定义。
     *
     * 与 [registerUsedMacroSurface] 的语法名兜底不同，该入口记录定义所在包，
     * 供 `import pkg.*` 的 unused-import 判定按官方 `usedMacroInFile[packageName]`
     * 语义识别 all-under macro import 使用。
     */
    fun registerUsedMacroDefinition(file: CfirFile, entry: MacroDefinitionEntry) {
        for (fileIdentity in file.macroExpansionFileIdentities()) {
            _usedMacroNamesByFileIdentity.getOrPut(fileIdentity) { linkedSetOf() } += entry.name
            _usedMacroNamesByPackageByFileIdentity
                .getOrPut(fileIdentity) { linkedMapOf() }
                .getOrPut(entry.packageFqName) { linkedSetOf() } += entry.name
            _usedMacroPackagesByFileIdentity.getOrPut(fileIdentity) { linkedSetOf() } += entry.packageFqName
        }
    }

    /**
     * 登记 construction routing 已确认的 macro package 使用。
     *
     * `MacroResolution.CustomAnnotation` 或 degraded 路径可能没有可用的
     * [MacroDefinitionEntry]，但 classification 已经依据源码 macro package/import
     * 候选确认了该 package 是当前 surface 的 construction demand。该事实不能丢失，
     * 否则 final CFIR 中保留的 annotation surface 会让 imports checker 误报 unresolved。
     */
    fun registerUsedMacroPackage(file: CfirFile, packageFqName: FqName) {
        require(!packageFqName.isRoot) { "A used macro package must not be the root package." }
        for (fileIdentity in file.macroExpansionFileIdentities()) {
            _usedMacroPackagesByFileIdentity.getOrPut(fileIdentity) { linkedSetOf() } += packageFqName
        }
    }

    /**
     * 查询某源文件 construction 阶段已消费的 macro 简单名集合。
     */
    fun usedMacroNames(file: CfirFile): Set<Name> {
        return file.macroExpansionFileIdentities()
            .flatMap { identity -> _usedMacroNamesByFileIdentity[identity].orEmpty() }
            .toSet()
    }

    /**
     * 查询某源文件 construction 阶段在指定 macro 包中已消费的 macro 简单名集合。
     */
    fun usedMacroNames(file: CfirFile, packageFqName: FqName): Set<Name> {
        return file.macroExpansionFileIdentities()
            .flatMap { identity ->
                _usedMacroNamesByPackageByFileIdentity[identity]
                    ?.get(packageFqName)
                    .orEmpty()
            }
            .toSet()
    }

    /** 查询当前文件 construction 阶段确认使用过的 macro package。 */
    fun usedMacroPackages(file: CfirFile): Set<FqName> = file.macroExpansionFileIdentities()
        .flatMap { identity -> _usedMacroPackagesByFileIdentity[identity].orEmpty() }
        .toSet()

    /** 注册编译单元内声明的 macro package 身份。 */
    fun registerDeclaredMacroPackage(fqName: FqName) {
        if (!fqName.isRoot) {
            _declaredMacroPackages.add(fqName)
        }
    }

    /** 查询给定 FqName 是否为本编译单元声明的 macro package。 */
    fun isDeclaredMacroPackage(fqName: FqName): Boolean = fqName in _declaredMacroPackages

    /** 注册 degraded placeholder 与原始 surface 的对应关系。 */
    fun registerPlaceholder(placeholderId: Long, originSurfaceId: Long) {
        _placeholderOriginById[placeholderId] = originSurfaceId
    }

    /** 注册展开产物 source 到原始 surface 的对应关系；source 为空时忽略。 */
    fun registerGeneratedSource(source: AbstractCjSourceElement?, originSurfaceId: Long) {
        if (source != null) {
            _generatedSourceOriginById[source] = GeneratedSourceOrigin(originSurfaceId, fragmentSourceMap = null)
        }
    }

    /**
     * 递归注册展开产物 CFIR 子树中所有 source 与原始 surface 的对应关系。
     *
     * 普通 checker 只看到 final CFIR；此映射使诊断 reporter 能把 generated source
     * 上的错误重定位回原始 macro 调用位点。自定义注解重解析保留 root 的宿主 source，
     * 并要求未能映射的子节点维持原坐标，不能扩大成整个 macro surface。
     *
     * @param fragmentSourceMap 重解析 fragment 坐标到原始 token 来源的映射。
     * @param registerRootSource 是否将 root 的 source 视为片段生成坐标。
     * @param fallbackToSurfaceOnUnmappedSource 未命中 source map 时是否回退到 macro surface。
     */
    fun registerGeneratedCfirElement(
        element: CfirElement,
        originSurfaceId: Long,
        fragmentSourceMap: MacroFragmentSourceMap? = null,
        registerRootSource: Boolean = true,
        fallbackToSurfaceOnUnmappedSource: Boolean = true,
    ) {
        val root = element
        val boundSourceMap = fragmentSourceMap?.bindToFragmentTree(root.source)
        root.accept(
            object : CfirDefaultVisitor<Unit, Unit>() {
                /**
                 * 访问展开产物中的每个 CFIR 节点，记录其 source 到原始 macro surface 的映射。
                */
                override fun visitElement(element: CfirElement, data: Unit) {
                    val source = element.source
                    if (source != null && (element !== root || registerRootSource)) {
                        _generatedSourceOriginById[source] = GeneratedSourceOrigin(
                            originSurfaceId,
                            boundSourceMap,
                            fallbackToSurfaceOnUnmappedSource,
                        )
                    }
                    element.acceptChildren(this, data)
                }
            },
            Unit,
        )
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
        val sourceAnchor = (source as? CjOffsetsOnlySourceElement)?.sourceAnchor ?: source
        val origin = _generatedSourceOriginById[source]
            ?: _generatedSourceOriginById[sourceAnchor]
            ?: run {
            val matchingOrigins = _generatedSourceOriginById.values.filter { origin ->
                origin.fragmentSourceMap?.owns(sourceAnchor) == true
            }
            if (matchingOrigins.map { it.surfaceId }.distinct().size != 1) return null
            matchingOrigins.firstOrNull() ?: return null
        }
        val surface = _originSurfaceById[origin.surfaceId] ?: return null
        return origin.fragmentSourceMap?.mapRange(source.startOffset, source.endOffset)
            ?: if (origin.fallbackToSurface) surface.sourceRange?.source else null
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
 * 把 construction routing 已确认消费的 surface 按宿主文件登记到 registry。
 *
 * unused-import checker 读取的是 construction 后的 final CFIR；原始 macro/custom
 * annotation surface 可能已被展开、降级或重分类，因此必须在 construction service
 * 层统一记录“该源码文件使用过这个 surface 名称/定义包”的事实。
 */
fun MacroExpansionRegistry.registerConstructionSurfaceUsage(
    pre: PreMacroRawBuildResult,
    classification: MacroDemandClassification,
) {
    val snapshotsBySurfaceId = classification.preArtifactSnapshot.decisions
        .associateBy { it.surface.surfaceId }
    val usedDecisionsBySurfaceId = classification.finalDecisions
        .asSequence()
        .filter { it.localConstruction }
        .filterNot { it.surface.isMacroDefinitionSignatureSurfaceForUsage() }
        .associateBy { it.surface.surfaceId }
    if (usedDecisionsBySurfaceId.isEmpty()) return

    for (preFile in pre.files) {
        for (surface in preFile.surfaces) {
            val decision = usedDecisionsBySurfaceId[surface.surfaceId] ?: continue
            registerUsedMacroSurface(preFile.cfirFile, surface)
            decision.externalPackageDemand?.let { packageFqName ->
                registerUsedMacroPackage(preFile.cfirFile, packageFqName)
            }
            if (decision.resolution is MacroResolution.CustomAnnotation && surface.replaceHandle.annotationCarrier != null) {
                // A custom/degraded annotation has no resolved MacroDefinitionEntry, but
                // its import candidates are still the construction routing facts that
                // made this annotation surface reachable. Preserve those package-level
                // uses so the final imports checker does not report a false unresolved
                // import after the original surface has been retained.
                snapshotsBySurfaceId[surface.surfaceId]
                    ?.importCandidates
                    ?.filterNot { it.isRoot }
                    ?.forEach { packageFqName ->
                        registerUsedMacroPackage(preFile.cfirFile, packageFqName)
                    }
            }
            val resolvedEntry = (decision.resolution as? MacroResolution.Resolved)?.entry
            if (resolvedEntry != null) {
                registerUsedMacroDefinition(preFile.cfirFile, resolvedEntry)
            }
        }
    }
}

/** 宏定义签名 surface 是 macro package 编译输入，不是使用方 import usage。 */
private fun MacroSurface.isMacroDefinitionSignatureSurfaceForUsage(): Boolean {
    val carrier = replaceHandle.carrier
    if (carrier is CfirMacroDeclaration) return true
    return carrier is CfirValueParameter &&
        carrier.containingDeclarationSymbol is CfirMacroDeclarationSymbol
}

/**
 * Macro registry 内使用的稳定文件身份。
 *
 * 同一源码文件在 PSI、LightTree 与 resolve 副本中可能只保留 path、source name 或 CFIR name
 * 的一部分；registry 同时写入这些稳定键，避免 checker 依赖某个入口的 source 表示细节。
 */
private fun CfirFile.macroExpansionFileIdentities(): Set<String> = buildSet {
    addMacroExpansionFileIdentity(sourceFile?.path)
    addMacroExpansionFileIdentity(sourceFile?.name)
    addMacroExpansionFileIdentity(name)
}

/**
 * 记录 registry 查询使用的文件身份。
 *
 * PSI、LightTree 与 resolve 后的 CFIR 副本在 Windows 上可能分别保留绝对路径、
 * 反斜杠路径、标准化斜杠路径或仅文件名；registry 必须把这些稳定形态一并登记，
 * 才能让 construction 阶段记录的文件级 macro 使用事实被后续 checker 找回。
 */
private fun MutableSet<String>.addMacroExpansionFileIdentity(value: String?) {
    val identity = value?.takeIf(String::isNotBlank) ?: return
    add(identity)
    val normalized = identity.replace('\\', '/')
    add(normalized)
    normalized.substringAfterLast('/').takeIf(String::isNotBlank)?.let(::add)
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
        const val ALGORITHM_VERSION: Int = 5

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
        ): MacroConstructionResult.Success {
            val metadata = pre.sealAnnotationMetadata(files)
            registry.completeParsingReports(constructionCommitted = true)
            return MacroConstructionResult.Success(RecordableRawCfirFiles(pre.session, files, metadata), registry)
        }

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
        ): MacroConstructionResult.Degraded {
            val metadata = pre.sealAnnotationMetadata(files)
            registry.completeParsingReports(constructionCommitted = true)
            return MacroConstructionResult.Degraded(RecordableRawCfirFiles(pre.session, files, metadata), registry)
        }

        /** 失败只终止 CFIR 元数据事务，全部实际解析报告仍交付编译单元的统一 LEX/PARSE 发布门。 */
        fun failedOf(pre: PreMacroRawBuildResult, registry: MacroExpansionRegistry): MacroConstructionResult.Failed {
            pre.abortAnnotationMetadata()
            registry.completeParsingReports(constructionCommitted = false)
            return MacroConstructionResult.Failed(registry)
        }
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
        val registry = MacroExpansionRegistry()
        for (preFile in pre.files) {
            registry.registerFileSurfaces(preFile.cfirFile, preFile.surfaces)
        }
        registry.registerConstructionSurfaceUsage(pre, classification)
        registry.addAll(preConstructionDiagnostics)
        pre.session.register(MacroExpansionRegistry::class, registry)

        if (!(registry.hasErrors && mode == MacroConstructionService.Mode.STRICT)) {
            pre.files.forEach {
                finalizeImplicitConstructorDeclarations(it.cfirFile, it.annotationMetadata)
                materializePrimaryConstructorParameterFields(it.cfirFile, it.annotationMetadata)
            }
        }

        if (preConstructionDiagnostics.isNotEmpty()) {
            return if (registry.hasErrors && mode == MacroConstructionService.Mode.STRICT) {
                MacroConstructionService.failedOf(pre, registry)
            } else {
                MacroConstructionService.degradedOf(pre, files, registry)
            }
        }
        return MacroConstructionService.successOf(
            pre = pre,
            files = files,
            registry = registry,
        )
    }
}
