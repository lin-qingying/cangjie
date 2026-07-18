package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Macro surface 的语义位置。
 */
enum class MacroCallSite {
    /** 宏附着在声明或声明级 annotation 位置。 */
    DECLARATION,
    /** 宏附着在形参或参数 annotation 位置。 */
    PARAMETER,
    /** 宏出现在表达式位置。 */
    EXPRESSION,
}

/**
 * stable splice 的目标槽位。
 */
enum class MacroReplacementSlotType {
    /** 替换声明槽位。 */
    DECLARATION,
    /** 替换形参槽位。 */
    PARAMETER,
    /** 替换表达式槽位。 */
    EXPRESSION,
    /** 替换 annotation 槽位。 */
    ANNOTATION,
}

/**
 * construction failure 的传播策略。
 */
enum class MacroFailurePolicy {
    /** strict 模式下错误直接阻断 construction。 */
    STRICT,
    /** degraded 模式下错误可被转换为 typed placeholder。 */
    DEGRADED,
}

/**
 * artifact preparation 前的单个 surface snapshot。
 *
 * @property surface 原始 macro surface。
 * @property callSite surface 所在语义位置。
 * @property kind macro 调用形态。
 * @property qualifiedName 调用限定名；语法缺失时为 null。
 * @property samePackageResult 同包 macro 定义命中结果，用于禁止同包 def/call。
 * @property builtinNonMacroResult builtin non-macro 命中名。
 * @property builtinMacroResult builtin macro 命中条目。
 * @property importCandidates import 与显式限定名推导出的候选包集合。
 * @property annotationCarrier annotation surface 的稳定槽位身份。
 * @property rawMetadata raw builder 记录的 annotation 槽位元数据。
 * @property externalPackageDemand 需要 artifact preparation 的外部 macro package。
 */
data class PreArtifactSurfaceSnapshot(
    /** 原始 macro surface。 */
    val surface: MacroSurface,
    /** surface 所在语义位置。 */
    val callSite: MacroCallSite,
    /** macro 调用形态。 */
    val kind: MacroSurface.Kind,
    /** 调用限定名；语法缺失时为 null。 */
    val qualifiedName: FqName?,
    /** 同包 macro 定义命中结果，用于禁止同包 def/call。 */
    val samePackageResult: MacroDefinitionEntry?,
    /** builtin non-macro 命中名。 */
    val builtinNonMacroResult: Name?,
    /** builtin macro 命中条目。 */
    val builtinMacroResult: MacroDefinitionEntry?,
    /** import 与显式限定名推导出的候选包集合。 */
    val importCandidates: List<FqName>,
    /** annotation surface 的稳定槽位身份。 */
    val annotationCarrier: CfirAnnotationReplaceCarrier?,
    /** raw builder 记录的 annotation 槽位元数据。 */
    val rawMetadata: CfirAnnotationSlotSnapshot?,
    /** 需要 artifact preparation 的外部 macro package。 */
    val externalPackageDemand: FqName?,
)

/**
 * raw build 后、artifact/source-package preparation 前冻结的 demand snapshot。
 *
 * @property decisions 每个 surface 在 artifact preparation 前的初步路由判定。
 */
data class PreArtifactDemandSnapshot(
    /** 每个 surface 在 artifact preparation 前的初步路由判定。 */
    val decisions: List<PreArtifactSurfaceSnapshot>,
) {
    /** 按外部包聚合的 surface demand，用于驱动 macro artifact preparation。 */
    val externalPackageDemandSurfaces: Map<FqName, List<MacroSurface>>
        get() = decisions
            .filter { it.externalPackageDemand != null }
            .groupBy(
                keySelector = { it.externalPackageDemand!! },
                valueTransform = { it.surface },
            )
}

/**
 * artifact definitions 回填后的最终 construction routing。
 *
 * @property surface 原始 macro surface。
 * @property callSite surface 所在语义位置。
 * @property slotType stable splice 的目标槽位。
 * @property annotationCarrier annotation surface 的稳定槽位身份。
 * @property resolution 最终解析结果。
 * @property parserMode fragment parser 应使用的解析模式。
 * @property localConstruction 是否允许在本地 construction 阶段处理该 surface。
 * @property executorRequired 是否必须调用 external executor。
 * @property externalPackageDemand 该 surface 依赖的外部 macro package。
 * @property failurePolicy 该 surface 的失败传播策略。
 * @property blockedDiagnostic construction 被阻塞时预先生成的诊断。
 */
data class FinalMacroSurfaceDecision(
    /** 原始 macro surface。 */
    val surface: MacroSurface,
    /** surface 所在语义位置。 */
    val callSite: MacroCallSite,
    /** stable splice 的目标槽位。 */
    val slotType: MacroReplacementSlotType,
    /** annotation surface 的稳定槽位身份。 */
    val annotationCarrier: CfirAnnotationReplaceCarrier?,
    /** 最终解析结果。 */
    val resolution: MacroResolution,
    /** fragment parser 应使用的解析模式。 */
    val parserMode: MacroFragmentParser.Mode,
    /** 是否允许在本地 construction 阶段处理该 surface。 */
    val localConstruction: Boolean,
    /** 是否必须调用 external executor。 */
    val executorRequired: Boolean,
    /** 该 surface 依赖的外部 macro package。 */
    val externalPackageDemand: FqName?,
    /** 该 surface 的失败传播策略。 */
    val failurePolicy: MacroFailurePolicy,
    /** construction 被阻塞时预先生成的诊断。 */
    val blockedDiagnostic: MacroConstructionDiagnostic?,
)

/**
 * 同一 session 内的 two-phase classification 状态。
 *
 * artifact preparation 只读取 [preArtifactSnapshot]；macro expand 只读取
 * [finalDecisions]，禁止重新扫 preFile.surfaces 或二次 resolve/routing。
 *
 * @property pre 当前 session 的 pre-macro raw build 结果。
 * @property preArtifactSnapshot artifact preparation 前冻结的 demand snapshot。
 * @property defaultMacroImports 默认隐式 macro import 包列表。
 * @property builtinRegistries builtin macro / annotation / non-macro 注册表。
 */
class MacroDemandClassification private constructor(
    /** 当前 session 的 pre-macro raw build 结果。 */
    val pre: PreMacroRawBuildResult,
    /** artifact preparation 前冻结的 demand snapshot。 */
    val preArtifactSnapshot: PreArtifactDemandSnapshot,
    /**
     * 默认隐式 macro import 包列表，参与 artifact 前后的宏名解析。
     */
    private val defaultMacroImports: List<FqName>,
    /**
     * builtin macro、annotation 与 non-macro 注册表。
     */
    private val builtinRegistries: MacroBuiltinRegistries,
) : CfirSessionComponent {
    /** artifact definitions 回填后的最终决策快照；null 表示尚未冻结。 */
    private var frozenFinalDecisions: List<FinalMacroSurfaceDecision>? = null

    /** 最终 routing 是否已经冻结。 */
    val isFinalFrozen: Boolean
        get() = frozenFinalDecisions != null

    /** 已冻结的最终 routing 决策；未冻结时访问会报错以避免二次推导。 */
    val finalDecisions: List<FinalMacroSurfaceDecision>
        get() = frozenFinalDecisions
            ?: error("Final macro surface decisions are not frozen for this session.")

    /**
     * 回填库、shared builtin 与 artifact 宏定义后冻结最终 routing 决策。
     *
     * 重复调用返回第一次冻结的结果，保证同一 session 内 macro construction
     * 不会因为外部定义列表顺序或调用时机改变而二次改写路由。
     */
    fun freezeFinal(
        libraryDefinitions: List<MacroDefinitionEntry> = emptyList(),
        sharedBuiltinDefinitions: List<MacroDefinitionEntry> = emptyList(),
        macroArtifactDefinitions: List<MacroDefinitionEntry> = emptyList(),
        failurePolicy: MacroFailurePolicy = MacroFailurePolicy.STRICT,
    ): List<FinalMacroSurfaceDecision> {
        frozenFinalDecisions?.let { return it }
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
            builtinRegistries = builtinRegistries,
        )
        val decisions = preArtifactSnapshot.decisions.map { snapshot ->
            snapshot.toFinalDecision(context, failurePolicy)
        }
        frozenFinalDecisions = decisions
        return decisions
    }

    /** 在 artifact preparation 失败路径上冻结最终决策，仅保留已有 snapshot 与失败策略。 */
    fun freezeFailurePath(failurePolicy: MacroFailurePolicy = MacroFailurePolicy.STRICT): List<FinalMacroSurfaceDecision> {
        return freezeFinal(failurePolicy = failurePolicy)
    }

    /** [MacroDemandClassification] 的构造入口。 */
    companion object {
        /**
         * 从 [pre] 构造两阶段 demand classification。
         *
         * 本函数只读取 pre-macro 文件、annotation metadata registry 与离线 symbol index，
         * 不触碰 final source provider。
         */
        fun create(
            pre: PreMacroRawBuildResult,
            defaultMacroImports: List<FqName> = emptyList(),
            builtinRegistries: MacroBuiltinRegistries = MacroBuiltinRegistries.DEFAULT,
        ): MacroDemandClassification {
            val symbolIndex = buildMacroSymbolIndex(pre)
            val context = bindMacroImports(
                pre = pre,
                symbolIndex = symbolIndex,
                defaultMacroImports = defaultMacroImports,
                builtinRegistries = builtinRegistries,
            )
            val snapshots = pre.files.flatMap { preFile ->
                val sourceMacroPackages = pre.files
                    .filter { it.isMacroPackage }
                    .mapTo(linkedSetOf()) { it.cfirFile.packageDirective.packageFqName }
                preFile.surfaces.map { surface ->
                    surface.toPreArtifactSnapshot(
                        context,
                        preFile,
                        pre.session.annotationMetadataRegistryOrNull,
                        sourceMacroPackages,
                    )
                }
            }
            return MacroDemandClassification(
                pre = pre,
                preArtifactSnapshot = PreArtifactDemandSnapshot(snapshots),
                defaultMacroImports = defaultMacroImports,
                builtinRegistries = builtinRegistries,
            )
        }
    }
}

/** 将单个 [MacroSurface] 转换为 artifact preparation 前的冻结 snapshot。 */
private fun MacroSurface.toPreArtifactSnapshot(
    context: MacroResolutionContext,
    preFile: PreMacroCfirFile,
    annotationMetadataRegistry: CfirAnnotationMetadataRegistry?,
    sourceMacroPackages: Set<FqName>,
): PreArtifactSurfaceSnapshot {
    val callSite = callSite()
    val annotationCarrier = replaceHandle.annotationCarrier
    val metadata = annotationCarrier?.let { annotationMetadataRegistry?.snapshot(it) }
    val name = qualifiedName?.shortName()
    val samePackage = name?.let { context.symbolIndex.samePackageMacroDef(scopeContext.packageFqName, it) }
    val builtinNonMacro = name?.takeIf { samePackage == null && it in context.builtinNonMacroRegistry }
    val builtinMacro = name
        ?.takeIf { samePackage == null && it in context.builtinMacroRegistry }
        ?.let { context.symbolIndex.lookupByFqName(FqName.topLevel(it)) }
        ?.takeIf { it.source == MacroDefinitionEntry.Source.BUILTIN_MACRO }
    val importCandidates = collectImportCandidatePackages(preFile)
    val demand = externalDemandPackage(
        preFile = preFile,
        samePackage = samePackage,
        builtinNonMacro = builtinNonMacro,
        builtinMacro = builtinMacro,
        importCandidates = importCandidates,
        sourceMacroPackages = sourceMacroPackages,
    )
    return PreArtifactSurfaceSnapshot(
        surface = this,
        callSite = callSite,
        kind = kind,
        qualifiedName = qualifiedName,
        samePackageResult = samePackage,
        builtinNonMacroResult = builtinNonMacro,
        builtinMacroResult = builtinMacro,
        importCandidates = importCandidates,
        annotationCarrier = annotationCarrier,
        rawMetadata = metadata,
        externalPackageDemand = demand,
    )
}

/** 将 pre-artifact snapshot 回填最终解析结果，形成 construction routing 决策。 */
private fun PreArtifactSurfaceSnapshot.toFinalDecision(
    context: MacroResolutionContext,
    failurePolicy: MacroFailurePolicy,
): FinalMacroSurfaceDecision {
    val name = qualifiedName?.shortName()
    val resolution = when {
        name == null -> MacroResolution.Unresolved(Name.special("<missing>"))
        samePackageResult != null -> MacroResolution.SamePackage(samePackageResult)
        annotationCarrier != null && kind == MacroSurface.Kind.FORCED -> MacroResolution.CustomAnnotation(name)
        else -> context.resolveMacroCall(
            callPackage = surface.scopeContext.packageFqName,
            qualifier = qualifiedName?.parent()?.takeUnless { it == surface.scopeContext.packageFqName },
            name = name,
            kind = surface.kind,
            hasParenthesis = surface.hasParenthesis,
            allowsDeclarationInputParenthesisOmission = surface is MacroSurfaceDecl,
        ).let { resolved ->
            if (annotationCarrier != null && resolved is MacroResolution.Unresolved) {
                MacroResolution.CustomAnnotation(name)
            } else {
                resolved
            }
        }
    }
    val parserMode = when {
        resolution is MacroResolution.CustomAnnotation -> MacroFragmentParser.Mode.CUSTOM_ANNOTATION
        surface is MacroSurfaceExpr -> MacroFragmentParser.Mode.EXPRESSION
        else -> MacroFragmentParser.Mode.DECLARATION
    }
    val slotType = when {
        resolution is MacroResolution.CustomAnnotation -> MacroReplacementSlotType.ANNOTATION
        else -> surface.slotType()
    }
    val blockedDiagnostic = (resolution as? MacroResolution.SamePackage)?.let { samePackage ->
        MacroConstructionDiagnostic(
            severity = MacroConstructionDiagnostic.Severity.ERROR,
            message = "Macro call `@${samePackage.sourceEntry.name.asString()}` cannot resolve to a macro definition declared in the same package `${surface.scopeContext.packageFqName.asString()}`.",
            originSurfaceId = surface.surfaceId,
            kind = MacroConstructionDiagnostic.Kind.MACRO_SAME_PACKAGE_DEF_CALL,
        )
    }
    return FinalMacroSurfaceDecision(
        surface = surface,
        callSite = callSite,
        slotType = slotType,
        annotationCarrier = annotationCarrier,
        resolution = resolution,
        parserMode = parserMode,
        localConstruction = resolution !is MacroResolution.SamePackage,
        executorRequired = resolution is MacroResolution.Resolved,
        externalPackageDemand = externalPackageDemand,
        failurePolicy = failurePolicy,
        blockedDiagnostic = blockedDiagnostic,
    )
}

/** 推导 surface 的语义调用位置。 */
private fun MacroSurface.callSite(): MacroCallSite = when (this) {
    is MacroSurfaceExpr -> MacroCallSite.EXPRESSION
    is MacroSurfaceParam -> MacroCallSite.PARAMETER
    else -> MacroCallSite.DECLARATION
}

/** 推导 surface 对应的 stable splice 槽位类型。 */
private fun MacroSurface.slotType(): MacroReplacementSlotType = when (this) {
    is MacroSurfaceExpr -> MacroReplacementSlotType.EXPRESSION
    is MacroSurfaceParam -> MacroReplacementSlotType.PARAMETER
    else -> MacroReplacementSlotType.DECLARATION
}

/** 从显式限定名和 import 列表中收集可能需要 artifact preparation 的 macro package。 */
private fun MacroSurface.collectImportCandidatePackages(preFile: PreMacroCfirFile): List<FqName> {
    val name = qualifiedName?.shortName() ?: return emptyList()
    val result = linkedSetOf<FqName>()
    qualifiedName
        ?.parent()
        ?.takeUnless { it.isRoot || it == scopeContext.packageFqName }
        ?.let(result::add)
    for (import in preFile.cfirFile.imports) {
        val importedFqName = import.importedFqName ?: continue
        val packageFqName = if (import.isAllUnder) importedFqName else importedFqName.parent()
        if (packageFqName.isRoot || packageFqName == scopeContext.packageFqName) continue
        val canBind = import.isAllUnder ||
            import.aliasName == name ||
            importedFqName.shortName() == name
        if (canBind) result += packageFqName
    }
    return result.toList()
}

/** 推导该 surface 是否需要某个外部 macro package 的 artifact preparation。 */
private fun MacroSurface.externalDemandPackage(
    preFile: PreMacroCfirFile,
    samePackage: MacroDefinitionEntry?,
    builtinNonMacro: Name?,
    builtinMacro: MacroDefinitionEntry?,
    importCandidates: List<FqName>,
    sourceMacroPackages: Set<FqName>,
): FqName? {
    if (preFile.isMacroPackage || isMacroDefinitionSignatureSurfaceForClassification()) return null
    if (samePackage != null || builtinNonMacro != null || builtinMacro != null) return null
    val qualifiedPackage = qualifiedName
        ?.parent()
        ?.takeUnless {
            it.isRoot ||
                it == scopeContext.packageFqName ||
                it == preFile.cfirFile.packageDirective.packageFqName
        }
    if (replaceHandle.annotationCarrier != null) {
        if (kind == MacroSurface.Kind.FORCED) return null
        return listOfNotNull(qualifiedPackage)
            .plus(importCandidates)
            .firstOrNull { it in sourceMacroPackages || it in OFFICIAL_STDLIB_MACRO_PACKAGES }
    }
    return qualifiedPackage ?: importCandidates.firstOrNull()
}

/**
 * 官方编译器在 macro call resolve / evaluation 阶段会把这些标准库包作为内建宏包处理。
 *
 * annotation surface 不能无差别对所有 import 发起 artifact demand，否则普通 annotation
 * import 会被误判为宏包缺失；这里只对官方标准宏包和同项目 macro package 开放。
 */
private val OFFICIAL_STDLIB_MACRO_PACKAGES: Set<FqName> = setOf(
    StandardNames.STD_DERIVING_PACKAGE_FQ_NAME,
    StandardNames.STD_UNITTEST_TESTMACRO_PACKAGE_FQ_NAME,
    StandardNames.STD_UNITTEST_MOCK_MOCKMACRO_PACKAGE_FQ_NAME,
)

/** 判定当前 surface 是否只是 macro 定义签名的一部分，而不是 macro 调用需求。 */
private fun MacroSurface.isMacroDefinitionSignatureSurfaceForClassification(): Boolean {
    val carrier = replaceHandle.carrier
    if (carrier is org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration) return true
    return carrier is org.cangnova.cangjie.cfir.declarations.CfirValueParameter &&
        carrier.containingDeclarationSymbol is org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
}
