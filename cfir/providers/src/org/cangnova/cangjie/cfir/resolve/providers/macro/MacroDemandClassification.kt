package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Macro surface 的语义位置。
 */
enum class MacroCallSite {
    DECLARATION,
    PARAMETER,
    EXPRESSION,
}

/**
 * stable splice 的目标槽位。
 */
enum class MacroReplacementSlotType {
    DECLARATION,
    PARAMETER,
    EXPRESSION,
    ANNOTATION,
}

/**
 * construction failure 的传播策略。
 */
enum class MacroFailurePolicy {
    STRICT,
    DEGRADED,
}

/**
 * artifact preparation 前的单个 surface snapshot。
 */
data class PreArtifactSurfaceSnapshot(
    val surface: MacroSurface,
    val callSite: MacroCallSite,
    val kind: MacroSurface.Kind,
    val qualifiedName: FqName?,
    val samePackageResult: MacroDefinitionEntry?,
    val builtinNonMacroResult: Name?,
    val builtinMacroResult: MacroDefinitionEntry?,
    val importCandidates: List<FqName>,
    val annotationCarrier: CfirAnnotationReplaceCarrier?,
    val rawMetadata: CfirAnnotationSlotSnapshot?,
    val externalPackageDemand: FqName?,
)

/**
 * raw build 后、artifact/source-package preparation 前冻结的 demand snapshot。
 */
data class PreArtifactDemandSnapshot(
    val decisions: List<PreArtifactSurfaceSnapshot>,
) {
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
 */
data class FinalMacroSurfaceDecision(
    val surface: MacroSurface,
    val callSite: MacroCallSite,
    val slotType: MacroReplacementSlotType,
    val annotationCarrier: CfirAnnotationReplaceCarrier?,
    val resolution: MacroResolution,
    val parserMode: MacroFragmentParser.Mode,
    val localConstruction: Boolean,
    val executorRequired: Boolean,
    val externalPackageDemand: FqName?,
    val failurePolicy: MacroFailurePolicy,
    val blockedDiagnostic: MacroConstructionDiagnostic?,
)

/**
 * 同一 session 内的 two-phase classification 状态。
 *
 * artifact preparation 只读取 [preArtifactSnapshot]；macro expand 只读取
 * [finalDecisions]，禁止重新扫 preFile.surfaces 或二次 resolve/routing。
 */
class MacroDemandClassification private constructor(
    val pre: PreMacroRawBuildResult,
    val preArtifactSnapshot: PreArtifactDemandSnapshot,
    private val defaultMacroImports: List<FqName>,
    private val builtinRegistries: MacroBuiltinRegistries,
) : CfirSessionComponent {
    private var frozenFinalDecisions: List<FinalMacroSurfaceDecision>? = null

    val isFinalFrozen: Boolean
        get() = frozenFinalDecisions != null

    val finalDecisions: List<FinalMacroSurfaceDecision>
        get() = frozenFinalDecisions
            ?: error("Final macro surface decisions are not frozen for this session.")

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

    fun freezeFailurePath(failurePolicy: MacroFailurePolicy = MacroFailurePolicy.STRICT): List<FinalMacroSurfaceDecision> {
        return freezeFinal(failurePolicy = failurePolicy)
    }

    companion object {
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

private fun MacroSurface.callSite(): MacroCallSite = when (this) {
    is MacroSurfaceExpr -> MacroCallSite.EXPRESSION
    is MacroSurfaceParam -> MacroCallSite.PARAMETER
    else -> MacroCallSite.DECLARATION
}

private fun MacroSurface.slotType(): MacroReplacementSlotType = when (this) {
    is MacroSurfaceExpr -> MacroReplacementSlotType.EXPRESSION
    is MacroSurfaceParam -> MacroReplacementSlotType.PARAMETER
    else -> MacroReplacementSlotType.DECLARATION
}

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
            .firstOrNull { it in sourceMacroPackages }
    }
    return qualifiedPackage ?: importCandidates.firstOrNull()
}

private fun MacroSurface.isMacroDefinitionSignatureSurfaceForClassification(): Boolean {
    val carrier = replaceHandle.carrier
    if (carrier is org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration) return true
    return carrier is org.cangnova.cangjie.cfir.declarations.CfirValueParameter &&
        carrier.containingDeclarationSymbol is org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
}
