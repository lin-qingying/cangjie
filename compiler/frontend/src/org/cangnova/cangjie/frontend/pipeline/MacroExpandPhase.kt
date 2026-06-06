package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildErrorFunction
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroDesugarer
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinMacroRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.FinalMacroSurfaceDecision
import org.cangnova.cangjie.cfir.resolve.providers.macro.IdentityMacroStableSplicer
import org.cangnova.cangjie.cfir.resolve.providers.macro.IfAvailableSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallForestBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionCycle
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroForestEvaluator
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentInput
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionCacheKey
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroBuiltinRegistries
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroPayloadChannel
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroReplaceSlot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolution
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroStableSplicer
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.cfir.symbols.CfirErrorFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroDiagnosticSeverity
import org.cangnova.cangjie.macro.MacroExpansionFailureKind
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.MacroLibraryLoadFailure
import org.cangnova.cangjie.macro.MacroLibraryLoadFailureKind
import org.cangnova.cangjie.macro.MacroLibraryLoadResult
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.macro.protocol.MacroMsgCodec
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.presentableDescription
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.text
import java.util.IdentityHashMap

fun interface MacroExecutorFactory {
    fun create(session: CfirSession): MacroExecutor
}

fun interface MacroFragmentParserFactory {
    fun create(session: CfirSession): MacroFragmentParser
}

/**
 * 前端宏展开配置键。
 */
object FrontendMacroConfigurationKeys {
    @JvmField
    val MACRO_EXECUTOR_FACTORY = CompilerConfigurationKey.create<MacroExecutorFactory>("MACRO_EXECUTOR_FACTORY")

    @JvmField
    val MACRO_EXPAND_MAX_ITERATIONS = CompilerConfigurationKey.create<Int>("MACRO_EXPAND_MAX_ITERATIONS")

    @JvmField
    val MACRO_FRAGMENT_PARSER_FACTORY =
        CompilerConfigurationKey.create<MacroFragmentParserFactory>("MACRO_FRAGMENT_PARSER_FACTORY")

    @JvmField
    val MACRO_ARTIFACT_PACKAGES =
        CompilerConfigurationKey.create<List<MacroArtifactPackage>>("MACRO_ARTIFACT_PACKAGES")

    @JvmField
    val MACRO_EXECUTOR_ABI_VERSION =
        CompilerConfigurationKey.create<String>("MACRO_EXECUTOR_ABI_VERSION")

    @JvmField
    val MACRO_SOURCE_PACKAGE_COMPILATION_REQUESTS =
        CompilerConfigurationKey.create<List<MacroSourcePackageCompilationRequest>>(
            "MACRO_SOURCE_PACKAGE_COMPILATION_REQUESTS",
        )

    @JvmField
    val MACRO_PACKAGE_COMPILATION_ORCHESTRATOR =
        CompilerConfigurationKey.create<MacroPackageCompilationOrchestrator>("MACRO_PACKAGE_COMPILATION_ORCHESTRATOR")

    @JvmField
    val MACRO_COMPILATION_CACHE_CONTEXT =
        CompilerConfigurationKey.create<MacroCompilationCacheContext>("MACRO_COMPILATION_CACHE_CONTEXT")

    @JvmField
    val MACRO_BACKGROUND_AUTO_COMPILATION_ENABLED =
        CompilerConfigurationKey.create<Boolean>("MACRO_BACKGROUND_AUTO_COMPILATION_ENABLED")

    @JvmField
    val MACRO_EXPANSION_DEMAND_AUTO_COMPILATION_ENABLED =
        CompilerConfigurationKey.create<Boolean>("MACRO_EXPANSION_DEMAND_AUTO_COMPILATION_ENABLED")

    @JvmField
    val MACRO_SDK_HOME = CompilerConfigurationKey.create<String>("MACRO_SDK_HOME")

    /**
     * Macro construction 模式。CLI 生产路径恒为 `STRICT`；
     * IDE / analysis 路径以及部分 testdata 通过设置 `DEGRADED` 让构造期诊断
     * 也能流过 ordinary checker（`MacroConstructionDiagnosticCollectorComponent`），
     * 而不是仅作为 compiler message 报告。
     */
    @JvmField
    val MACRO_CONSTRUCTION_MODE =
        CompilerConfigurationKey.create<MacroConstructionService.Mode>("MACRO_CONSTRUCTION_MODE")
}

var CompilerConfiguration.macroExecutorFactory: MacroExecutorFactory?
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_FACTORY)
    set(value) {
        putIfNotNull(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_FACTORY, value)
    }

var CompilerConfiguration.macroExpandMaxIterations: Int
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXPAND_MAX_ITERATIONS, 16)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_EXPAND_MAX_ITERATIONS, value)
    }

var CompilerConfiguration.macroFragmentParserFactory: MacroFragmentParserFactory?
    get() = get(FrontendMacroConfigurationKeys.MACRO_FRAGMENT_PARSER_FACTORY)
    set(value) {
        putIfNotNull(FrontendMacroConfigurationKeys.MACRO_FRAGMENT_PARSER_FACTORY, value)
    }

var CompilerConfiguration.macroArtifactPackages: List<MacroArtifactPackage>
    get() = get(FrontendMacroConfigurationKeys.MACRO_ARTIFACT_PACKAGES, emptyList())
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_ARTIFACT_PACKAGES, value)
    }

var CompilerConfiguration.macroConstructionMode: MacroConstructionService.Mode
    get() = get(FrontendMacroConfigurationKeys.MACRO_CONSTRUCTION_MODE, MacroConstructionService.Mode.STRICT)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_CONSTRUCTION_MODE, value)
    }

var CompilerConfiguration.macroExecutorAbiVersion: String
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_ABI_VERSION, MacroExecutor.DEFAULT_ABI_VERSION)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_ABI_VERSION, value)
    }

var CompilerConfiguration.macroSourcePackageCompilationRequests: List<MacroSourcePackageCompilationRequest>
    get() = get(FrontendMacroConfigurationKeys.MACRO_SOURCE_PACKAGE_COMPILATION_REQUESTS, emptyList())
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_SOURCE_PACKAGE_COMPILATION_REQUESTS, value)
    }

var CompilerConfiguration.macroPackageCompilationOrchestrator: MacroPackageCompilationOrchestrator?
    get() = get(FrontendMacroConfigurationKeys.MACRO_PACKAGE_COMPILATION_ORCHESTRATOR)
    set(value) {
        putIfNotNull(FrontendMacroConfigurationKeys.MACRO_PACKAGE_COMPILATION_ORCHESTRATOR, value)
    }

var CompilerConfiguration.macroCompilationCacheContext: MacroCompilationCacheContext
    get() = get(FrontendMacroConfigurationKeys.MACRO_COMPILATION_CACHE_CONTEXT)
        ?: MacroCompilationCacheContext(
            targetPlatform = targetPlatform ?: CangJiePlatforms.defaultCangJiePlatform,
        )
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_COMPILATION_CACHE_CONTEXT, value)
    }

var CompilerConfiguration.macroBackgroundAutoCompilationEnabled: Boolean
    get() = get(FrontendMacroConfigurationKeys.MACRO_BACKGROUND_AUTO_COMPILATION_ENABLED, false)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_BACKGROUND_AUTO_COMPILATION_ENABLED, value)
    }

var CompilerConfiguration.macroExpansionDemandAutoCompilationEnabled: Boolean
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXPANSION_DEMAND_AUTO_COMPILATION_ENABLED, true)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_EXPANSION_DEMAND_AUTO_COMPILATION_ENABLED, value)
    }

var CompilerConfiguration.macroSdkHome: String
    get() = get(FrontendMacroConfigurationKeys.MACRO_SDK_HOME, DEFAULT_MACRO_SDK_HOME)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_SDK_HOME, value)
    }

const val DEFAULT_MACRO_SDK_HOME: String = "C:\\Users\\lin17\\.cangjie\\sdks\\cangjie-1.0.5"

/**
 * Compiler frontend 的 macro construction 实现。
 *
 * 该实现严格遵守 baseline 第 2 节：text patch / full-file rebuild 不参与语义路径。
 * 在 stable splice 接入前，含 macro surface 的源码只能产 construction diagnostic
 * 并阻止 ordinary resolve；无宏源码走 identity [MacroConstructionResult.Success]。
 */
class FrontendMacroConstructionService(
    private val configuration: CompilerConfiguration,
    private val stableSplicer: MacroStableSplicer = CfirExpressionMacroStableSplicer,
    private val builtinNonMacroDesugarer: BuiltinNonMacroDesugarer = CangJieBuiltinNonMacroDesugarer,
) : MacroConstructionService {
    override fun expand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        classification: MacroDemandClassification,
        mode: MacroConstructionService.Mode,
        preConstructionDiagnostics: List<MacroConstructionDiagnostic>,
    ): MacroConstructionResult {
        val session = pre.session
        val registry = MacroExpansionRegistry()
        val rawFiles = pre.files.map { it.cfirFile }
        var builtDegradedPlaceholders = false

        // Baseline 第 9 节："session/analysis 长生命周期 registry"。
        // 把所有 surface 注册到 registry，供 diagnostic / LSP 反查原 macro 位点。
        for (surface in pre.allSurfaces) {
            registry.registerOriginSurface(surface)
        }
        registry.addAll(preConstructionDiagnostics)

        // baseline 第 12 节 Batch 5："alias conflict / macro package / ..."。
        reportAliasConflicts(context, registry)

        val expandedFiles = expandMacroSurfaces(pre, context, classification, registry) ?: rawFiles
        if (mode == MacroConstructionService.Mode.DEGRADED &&
            pre.allSurfaces.isNotEmpty() &&
            registry.hasErrors &&
            registry.diagnostics.all(::isDegradableDiagnostic)
        ) {
            replaceSurfaceOnlyDegradedPlaceholders(pre, expandedFiles, registry)
            builtDegradedPlaceholders = true
        }

        // baseline 第 11 节 cache key：splice 完成后逐文件计算 13 维 key，
        // 写入 registry 供上游 IDE / build cache 失效判定。
        registerCacheKeys(pre, context, expandedFiles, registry)
        pre.session.annotationMetadataRegistryOrNull?.freeze()

        // baseline 第 10 节："session/analysis 长生命周期 registry"挂到 session
        // 上，供 ordinary checker / IDE / LSP 通过 `session.macroExpansionRegistry` 读取。
        session.register(MacroExpansionRegistry::class, registry)

        if (registry.hasErrors) {
            if (mode == MacroConstructionService.Mode.DEGRADED &&
                builtDegradedPlaceholders &&
                registry.diagnostics.all(::isDegradableDiagnostic)
            ) {
                return MacroConstructionService.degradedOf(pre, expandedFiles, registry)
            }
            return MacroConstructionResult.Failed(registry)
        }

        return MacroConstructionService.successOf(pre, expandedFiles, registry)
    }

    /**
     * 通过 macro forest 串起 executor / builtin evaluator / fragment parser / stable splice。
     */
    private fun expandMacroSurfaces(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        classification: MacroDemandClassification,
        registry: MacroExpansionRegistry,
    ): List<CfirFile>? {
        for (decision in classification.finalDecisions) {
            decision.blockedDiagnostic?.let(registry::addDiagnostic)
        }
        val expansionDecisions = pre.files.flatMap { preFile ->
            preFile.surfaces.mapNotNull { surface ->
                val decision =
                    classification.finalDecisions.firstOrNull { it.surface === surface } ?: return@mapNotNull null
                if (!decision.localConstruction) return@mapNotNull null
                if ((preFile.isMacroPackage && surface !is MacroSurfaceExpr) || surface.isMacroDefinitionSignatureSurface()) {
                    return@mapNotNull null
                }
                decision
            }
        }
        if (expansionDecisions.isEmpty()) return pre.files.map { it.cfirFile }
        val expansionSurfaces = expansionDecisions.map { it.surface }
        val decisionsBySurfaceId = expansionDecisions.associateBy { it.surface.surfaceId }

        val parserFactory = configuration.macroFragmentParserFactory
        if (parserFactory == null) {
            for (surface in expansionSurfaces) {
                reportError(
                    registry = registry,
                    message = "Macro surface `${surface.capturedRawSyntax.orEmpty()}` cannot be expanded: fragment parser is not configured.",
                    kind = MacroConstructionDiagnostic.Kind.MACRO_NOT_EXPANDED,
                    originSurfaceId = surface.surfaceId,
                )
            }
            return null
        }

        val parser = parserFactory.create(pre.session)
        val executor = configuration.macroExecutorFactory?.create(pre.session)
        val slots = mutableListOf<MacroReplaceSlot>()
        val evaluator = MacroForestEvaluator(configuration.macroExpandMaxIterations)
        val forest = MacroCallForestBuilder.build(expansionSurfaces)
        val expansionSurfaceIds = expansionSurfaces.mapTo(mutableSetOf()) { it.surfaceId }
        val surfaceFiles = pre.files
            .flatMap { preFile ->
                preFile.surfaces
                    .filter { surface -> surface.surfaceId in expansionSurfaceIds }
                    .map { surface -> surface.surfaceId to preFile }
            }
            .toMap()

        evaluator.evaluate(
            forest = forest,
            expand = { node, childResults ->
                val surface = node.surface
                val decision = decisionsBySurfaceId[surface.surfaceId]
                    ?: return@evaluate null
                val name = surface.qualifiedName?.shortName()
                if (surface.hasBlockingPreConstructionDiagnostic(registry)) {
                    return@evaluate null
                }
                if (name == null) {
                    reportError(
                        registry = registry,
                        message = "Macro surface `${surface.capturedRawSyntax.orEmpty()}` has no resolvable macro name.",
                        kind = MacroConstructionDiagnostic.Kind.MACRO_UNRESOLVED,
                        originSurfaceId = surface.surfaceId,
                    )
                    return@evaluate null
                }

                if (node.hasUnresolvedChildPayloadChannel(childResults)) {
                    reportError(
                        registry = registry,
                        message = "Nested macro surface inside `${
                            surface.qualifiedName?.asString().orEmpty()
                        }` cannot be mapped to attr or input token payload.",
                        kind = MacroConstructionDiagnostic.Kind.MACRO_REEVALUATION_FAILED,
                        originSurfaceId = surface.surfaceId,
                    )
                    return@evaluate null
                }
                val refreshedTokens = node.refreshTokensWithChildResults(childResults)
                val expandedTokens = expandResolvedSurface(
                    pre = pre,
                    surface = surface,
                    decision = decision,
                    node = node,
                    childResults = childResults,
                    refreshedTokens = refreshedTokens,
                    executor = executor,
                    registry = registry,
                    preFile = surfaceFiles[surface.surfaceId],
                ) ?: return@evaluate null

                val fragment = parseAndDesugarFragment(
                    surface = surface,
                    decision = decision,
                    annotationSnapshot = decision.annotationCarrier
                        ?.let { pre.session.annotationMetadataRegistryOrNull?.snapshot(it) },
                    node = node,
                    parser = parser,
                    tokens = expandedTokens,
                    registry = registry,
                ) ?: return@evaluate null
                (fragment as? MacroFragmentResult.Success)
                    ?.payload
                    ?.let { it as? CfirElement }
                    ?.let { registry.registerGeneratedCfirElement(it, surface.surfaceId) }
                val parentVisibleTokens = fragment.parentVisibleTokens()

                if (surface is BuiltinNonMacroSurface) {
                    registry.registerGeneratedDisplayText(
                        surfaceId = surface.surfaceId,
                        text = parentVisibleTokens.joinToString(separator = "") { it.text },
                    )
                    return@evaluate parentVisibleTokens
                }

                slots += MacroReplaceSlot(
                    handle = surface.replaceHandle,
                    origin = surface,
                    fragment = fragment,
                )
                parentVisibleTokens
            },
            onCycle = { cycle -> reportMacroCycle(cycle, registry) },
        )
        executor?.let { runCatching { it.close() } }

        if (slots.isEmpty()) return null
        if (stableSplicer === IdentityMacroStableSplicer) {
            for (slot in slots) {
                reportError(
                    registry = registry,
                    message = "Macro call `${
                        slot.origin.qualifiedName?.asString().orEmpty()
                    }` produced a fragment, but stable splicer is not configured.",
                    kind = MacroConstructionDiagnostic.Kind.MACRO_NOT_EXPANDED,
                    originSurfaceId = slot.origin.surfaceId,
                )
            }
            return null
        }

        return runCatching {
            stableSplicer.applySlices(pre.files.map { it.cfirFile }, slots).also {
                val metadataRegistry = pre.session.annotationMetadataRegistryOrNull
                if (metadataRegistry != null) {
                    for (slot in slots) {
                        val carrier = slot.handle.annotationCarrier ?: continue
                        val replacement = (slot.fragment as? MacroFragmentResult.CustomAnnotation)?.payload ?: continue
                        metadataRegistry.migrate(carrier, replacement)
                    }
                }
            }
        }.getOrElse { error ->
            for (slot in slots) {
                reportError(
                    registry = registry,
                    message = error.message ?: "Stable macro splice failed.",
                    kind = MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED,
                    originSurfaceId = slot.origin.surfaceId,
                )
            }
            null
        }
    }

    private fun expandResolvedSurface(
        pre: PreMacroRawBuildResult,
        surface: MacroSurface,
        decision: FinalMacroSurfaceDecision,
        node: MacroCallNode,
        childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
        refreshedTokens: RefreshedMacroSurfaceTokens,
        executor: MacroExecutor?,
        registry: MacroExpansionRegistry,
        preFile: PreMacroCfirFile?,
    ): List<MacroSurfaceToken>? {
        return when (val resolution = decision.resolution) {
            is MacroResolution.SamePackage -> {
                reportSamePackageMacroDefinition(surface, resolution.sourceEntry, registry)
                null
            }

            is MacroResolution.Unresolved -> {
                val name = resolution.name
                reportError(
                    registry = registry,
                    message = "Macro call `@${name.asString()}` is unresolved in macro construction.",
                    kind = MacroConstructionDiagnostic.Kind.MACRO_UNRESOLVED,
                    originSurfaceId = surface.surfaceId,
                )
                null
            }

            is MacroResolution.KindMismatch -> {
                val (kind, reason) = resolution.toConstructionDiagnostic()
                reportError(
                    registry = registry,
                    message = reason,
                    kind = kind,
                    originSurfaceId = surface.surfaceId,
                )
                null
            }

            is MacroResolution.BuiltinNonMacro -> tokensForBuiltinNonMacro(surface, refreshedTokens)
            is MacroResolution.CustomAnnotation -> {
                val snapshot = decision.annotationCarrier
                    ?.let { pre.session.annotationMetadataRegistryOrNull?.snapshot(it) }
                    ?: pre.session.annotationMetadataRegistryOrNull?.snapshotForSurface(surface)
                snapshot?.tokens ?: run {
                    reportError(
                        registry = registry,
                        message = "Custom annotation surface `${surface.capturedRawSyntax.orEmpty()}` is missing its annotation slot snapshot.",
                        kind = MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED,
                        originSurfaceId = surface.surfaceId,
                    )
                    null
                }
            }

            is MacroResolution.Builtin -> evaluateBuiltinMacro(surface, resolution.entry, preFile, registry)
            is MacroResolution.Resolved -> evaluateExternalMacro(
                surface = surface,
                entry = resolution.entry,
                node = node,
                childResults = childResults,
                refreshedTokens = refreshedTokens,
                executor = executor,
                registry = registry,
                preFile = preFile,
            )
        }
    }

    private fun evaluateBuiltinMacro(
        surface: MacroSurface,
        entry: MacroDefinitionEntry,
        preFile: PreMacroCfirFile?,
        registry: MacroExpansionRegistry,
    ): List<MacroSurfaceToken>? {
        val text = when (entry.name) {
            BuiltinMacroRegistry.sourcePackage -> stringLiteral(surface.scopeContext.packageFqName.asString())
            BuiltinMacroRegistry.sourceFile -> stringLiteral(
                preFile?.cfirFile?.sourceFile?.name ?: preFile?.cfirFile?.name.orEmpty()
            )

            BuiltinMacroRegistry.sourceLine -> {
                val offset = surface.sourceRange?.startOffset ?: 0
                val mappedLine = preFile?.cfirFile?.sourceFileLinesMapping?.getLineByOffset(offset)
                val line = mappedLine?.takeIf { it >= 0 }?.plus(1) ?: 0
                line.toString()
            }

            else -> {
                reportError(
                    registry = registry,
                    message = "Unknown builtin macro `${entry.name.asString()}`.",
                    kind = MacroConstructionDiagnostic.Kind.MACRO_UNRESOLVED,
                    originSurfaceId = surface.surfaceId,
                )
                return null
            }
        }
        return listOf(MacroSurfaceToken(text = text, startOffset = 0, endOffset = text.length))
    }

    private fun evaluateExternalMacro(
        surface: MacroSurface,
        entry: MacroDefinitionEntry,
        node: MacroCallNode,
        childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
        refreshedTokens: RefreshedMacroSurfaceTokens,
        executor: MacroExecutor?,
        registry: MacroExpansionRegistry,
        preFile: PreMacroCfirFile?,
    ): List<MacroSurfaceToken>? {
        if (executor == null || !executor.isAvailable()) {
            reportError(
                registry = registry,
                message = "Macro executor is unavailable for `@${entry.name.asString()}`.",
                kind = MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_UNAVAILABLE,
                originSurfaceId = surface.surfaceId,
            )
            return null
        }

        entry.libPath?.takeIf { it.isNotBlank() }?.let { libPath ->
            when (val loadResult = executor.loadLibraries(listOf(libPath))) {
                is MacroLibraryLoadResult.Success -> Unit
                is MacroLibraryLoadResult.Failure -> {
                    loadResult.failures.forEach { failure ->
                        reportLibraryLoadFailure(surface, failure, registry)
                    }
                    return null
                }
            }
        }
        val callInfo = surface.toMacroCallInfo(entry, node.parentNames, refreshedTokens, preFile)
        val result = executor.execute(listOf(callInfo)).singleOrNull()
            ?: MacroExpansionResult.Failure(
                message = "Macro executor returned no result for `${entry.name.asString()}`.",
                kind = MacroExpansionFailureKind.PROTOCOL_ERROR,
            )

        return when (result) {
            is MacroExpansionResult.Success -> {
                recordMacroDiagReports(surface, result.diagnostics, registry)
                result.tokens.toMacroSurfaceTokens()
            }

            is MacroExpansionResult.Failure -> {
                recordMacroDiagReports(surface, result.diagnostics, registry)
                reportError(
                    registry = registry,
                    message = result.message,
                    kind = result.kind.toConstructionDiagnosticKind(),
                    originSurfaceId = surface.surfaceId,
                    diagnosticOrigin = MacroConstructionDiagnostic.Origin.EXECUTOR,
                )
                null
            }
        }
    }

    private fun recordMacroDiagReports(
        surface: MacroSurface,
        diagnostics: List<org.cangnova.cangjie.macro.MacroDiagnosticInfo>,
        registry: MacroExpansionRegistry,
    ) {
        diagnostics.forEach { diagnostic ->
            registry.addDiagnostic(
                MacroConstructionDiagnostic(
                    severity = diagnostic.severity.toConstructionSeverity(),
                    message = diagnostic.message,
                    originSurfaceId = surface.surfaceId,
                    diagnosticOrigin = MacroConstructionDiagnostic.Origin.DIAG_REPORT,
                    hint = diagnostic.hint.takeIf(String::isNotBlank),
                    tokenRangeBeginLine = diagnostic.begin.line.takeIf { it > 0 },
                    tokenRangeBeginColumn = diagnostic.begin.column.takeIf { it > 0 },
                    tokenRangeEndLine = diagnostic.end.line.takeIf { it > 0 },
                    tokenRangeEndColumn = diagnostic.end.column.takeIf { it > 0 },
                )
            )
        }
    }

    private fun reportLibraryLoadFailure(
        surface: MacroSurface,
        failure: MacroLibraryLoadFailure,
        registry: MacroExpansionRegistry,
    ) {
        reportError(
            registry = registry,
            message = failure.message,
            kind = failure.kind.toConstructionDiagnosticKind(),
            originSurfaceId = surface.surfaceId,
            macroLibraryPath = failure.libPath,
            diagnosticOrigin = MacroConstructionDiagnostic.Origin.EXECUTOR,
        )
    }

    private fun MacroResolution.KindMismatch.toConstructionDiagnostic():
            Pair<MacroConstructionDiagnostic.Kind, String> {
        val macroName = entry.name.asString()
        return when (reason) {
            MacroResolution.KindMismatch.Reason.FORCED_KIND_NOT_SUPPORTED ->
                MacroConstructionDiagnostic.Kind.MACRO_EXPAND_ATEXCL to
                        "Macro call `@$macroName` does not support `@!` forced invocation."

            MacroResolution.KindMismatch.Reason.PLAIN_ATTR_OVERLOAD_NOT_SUPPORTED ->
                MacroConstructionDiagnostic.Kind.MACRO_EXPECT_PLAIN_MACRO to
                        "Macro call `@$macroName` requires parenthesized plain macro invocation."
        }
    }

    private fun parseAndDesugarFragment(
        surface: MacroSurface,
        decision: FinalMacroSurfaceDecision,
        annotationSnapshot: org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot?,
        node: MacroCallNode,
        parser: MacroFragmentParser,
        tokens: List<MacroSurfaceToken>,
        registry: MacroExpansionRegistry,
    ): MacroFragmentResult? {
        val parsed = parser.parse(
            MacroFragmentInput(
                node = node,
                tokens = tokens,
                decision = decision,
                annotationSnapshot = annotationSnapshot,
            )
        )
        val fragment = if (surface is BuiltinNonMacroSurface && parsed is MacroFragmentResult.Success) {
            builtinNonMacroDesugarer.desugar(surface, parsed) ?: parsed
        } else {
            parsed
        }
        return when (fragment) {
            is MacroFragmentResult.Failure -> {
                reportError(
                    registry = registry,
                    message = fragment.reason,
                    kind = MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED,
                    originSurfaceId = surface.surfaceId,
                )
                null
            }

            is MacroFragmentResult.Success,
            is MacroFragmentResult.CustomAnnotation -> fragment
        }
    }

    private fun tokensForBuiltinNonMacro(
        surface: MacroSurface,
        refreshedTokens: RefreshedMacroSurfaceTokens,
    ): List<MacroSurfaceToken> {
        return if (surface is IfAvailableSurface) {
            surface.branchTokens.ifEmpty { refreshedTokens.inputTokens }
        } else {
            refreshedTokens.inputTokens.ifEmpty { refreshedTokens.attrTokens }
        }
    }

    private fun reportMacroCycle(
        cycle: MacroExpansionCycle,
        registry: MacroExpansionRegistry,
    ) {
        for (node in cycle.nodes) {
            val name = node.surface.qualifiedName?.shortName()
            reportError(
                registry = registry,
                message = "Macro expansion cycle detected for `${name?.asString().orEmpty()}`.",
                kind = MacroConstructionDiagnostic.Kind.MACRO_CYCLE,
                originSurfaceId = node.surface.surfaceId,
            )
        }
    }

    private fun reportSamePackageMacroDefinition(
        surface: MacroSurface,
        sameDef: MacroDefinitionEntry,
        registry: MacroExpansionRegistry,
    ) {
        val callPackage = surface.scopeContext.packageFqName
        reportError(
            registry = registry,
            message = buildString {
                append("Macro call `@")
                append(sameDef.name.asString())
                append("` cannot resolve to a macro definition declared in the same package `")
                append(if (callPackage.isRoot) "<root>" else callPackage.asString())
                append("`; macros must be provided by a separate macro package, artifact, or builtin.")
            },
            kind = MacroConstructionDiagnostic.Kind.MACRO_SAME_PACKAGE_DEF_CALL,
            originSurfaceId = surface.surfaceId,
        )
    }

    /**
     * 报告 alias 冲突（同一短名绑到多个 fqn）。
     */
    private fun reportAliasConflicts(
        context: MacroResolutionContext,
        registry: MacroExpansionRegistry,
    ) {
        for (conflict in context.aliasConflicts) {
            val message = buildString {
                append("Macro import alias `")
                append(conflict.alias.asString())
                append("` is bound to multiple targets: ")
                append(conflict.targets.joinToString(", ") { it.asString() })
            }
            reportError(
                registry = registry,
                message = message,
                kind = MacroConstructionDiagnostic.Kind.MACRO_ALIAS_CONFLICT,
                relatedName = conflict.alias,
                relatedTargets = conflict.targets,
                originSource = conflict.source,
            )
        }
    }

    private fun isDegradableDiagnostic(diagnostic: MacroConstructionDiagnostic): Boolean {
        if (diagnostic.originSurfaceId == null) return false
        return when (diagnostic.kind) {
            MacroConstructionDiagnostic.Kind.MACRO_ALIAS_CONFLICT,
            MacroConstructionDiagnostic.Kind.MACRO_SAME_PACKAGE_DEF_CALL,
            MacroConstructionDiagnostic.Kind.MACRO_CANNOT_OPEN_LIB,
            MacroConstructionDiagnostic.Kind.MACRO_CYCLE -> false

            MacroConstructionDiagnostic.Kind.GENERIC,
            MacroConstructionDiagnostic.Kind.MACRO_NOT_EXPANDED,
            MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED,
            MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_UNAVAILABLE,
            MacroConstructionDiagnostic.Kind.MACRO_DEPENDENCY_COMPILE_FAILED,
            MacroConstructionDiagnostic.Kind.MACRO_REEVALUATION_FAILED,
            MacroConstructionDiagnostic.Kind.MACRO_UNRESOLVED -> true
            // 由 baseline §9 / artifact-resolver 引入的新增 kind 默认保守归类为不可降级，
            // 避免在 STRICT 模式下因新增诊断悄悄走 DEGRADED 占位路径。
            // 后续按需把可降级的 kind 显式列入上面的白名单。
            else -> false
        }
    }

    private fun MacroSurface.hasBlockingPreConstructionDiagnostic(registry: MacroExpansionRegistry): Boolean {
        return registry.diagnostics.any { diagnostic ->
            diagnostic.originSurfaceId == surfaceId &&
                    diagnostic.severity == MacroConstructionDiagnostic.Severity.ERROR &&
                    diagnostic.diagnosticOrigin in setOf(
                MacroConstructionDiagnostic.Origin.ARTIFACT_RESOLVER,
                MacroConstructionDiagnostic.Origin.ORCHESTRATION,
            )
        }
    }

    /**
     * 声明 / 参数 / builtin non-macro surface 不会以 macro construction surface 形态进入 final CFIR。
     * DEGRADED 模式下用 registry 记录原始 surface 与占位 id 的映射，由 IDE/LSP
     * 按 original macro site 渲染诊断；不创建 `CfirMacroErrorPlaceholder`，
     * 也不把 construction surface 放进 final tree。
     */
    private fun replaceSurfaceOnlyDegradedPlaceholders(
        pre: PreMacroRawBuildResult,
        files: List<CfirFile>,
        registry: MacroExpansionRegistry,
    ) {
        val declarationPlaceholders = IdentityHashMap<CfirDeclaration, CfirDeclaration>()
        val parameterPlaceholders = IdentityHashMap<CfirValueParameter, CfirValueParameter>()
        val expressionPlaceholders = IdentityHashMap<CfirExpression, CfirExpression>()
        val diagnosticSurfaceIds = registry.diagnostics
            .mapNotNullTo(linkedSetOf()) { it.originSurfaceId }
        for (surface in pre.allSurfaces) {
            if (surface.surfaceId !in diagnosticSurfaceIds) continue
            registry.registerPlaceholder(
                placeholderId = surface.surfaceId,
                originSurfaceId = surface.surfaceId,
            )
            when (val carrier = surface.replaceHandle.carrier) {
                is CfirValueParameter -> parameterPlaceholders[carrier] =
                    buildParameterMacroErrorPlaceholder(surface, carrier)

                is CfirDeclaration -> declarationPlaceholders[carrier] =
                    buildDeclarationMacroErrorPlaceholder(surface, carrier)

                is CfirExpression -> expressionPlaceholders[carrier] =
                    buildExpressionMacroErrorPlaceholder(surface, carrier)
            }
        }
        for (file in files) {
            replaceDegradedDeclarationPlaceholders(file.declarations, declarationPlaceholders, parameterPlaceholders)
            replaceDegradedExpressionPlaceholders(file, expressionPlaceholders)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun replaceDegradedDeclarationPlaceholders(
        declarations: List<CfirDeclaration>,
        declarationPlaceholders: IdentityHashMap<CfirDeclaration, CfirDeclaration>,
        parameterPlaceholders: IdentityHashMap<CfirValueParameter, CfirValueParameter>,
    ) {
        val mutableDeclarations = declarations as? MutableList<CfirDeclaration> ?: return
        val iterator = mutableDeclarations.listIterator()
        while (iterator.hasNext()) {
            val declaration = iterator.next()
            val replacement = declarationPlaceholders.remove(declaration)
            val current = replacement ?: declaration
            if (replacement != null) {
                iterator.set(replacement)
            }
            replaceDegradedParameterPlaceholders(current, parameterPlaceholders)
            when (current) {
                is CfirClassLikeDeclaration ->
                    replaceDegradedDeclarationPlaceholders(
                        current.declarations,
                        declarationPlaceholders,
                        parameterPlaceholders
                    )

                is CfirExtend ->
                    replaceDegradedDeclarationPlaceholders(
                        current.declarations,
                        declarationPlaceholders,
                        parameterPlaceholders
                    )

                else -> Unit
            }
        }
    }

    private fun replaceDegradedParameterPlaceholders(
        declaration: CfirDeclaration,
        parameterPlaceholders: IdentityHashMap<CfirValueParameter, CfirValueParameter>,
    ) {
        if (declaration !is CfirFunction || declaration.valueParameters.isEmpty()) return
        val newParameters = declaration.valueParameters.map { parameter ->
            parameterPlaceholders.remove(parameter) ?: parameter
        }
        declaration.replaceValueParameters(newParameters)
    }

    private fun buildDeclarationMacroErrorPlaceholder(
        surface: MacroSurface,
        declaration: CfirDeclaration,
    ): CfirDeclaration {
        val diagnostic = macroPlaceholderDiagnostic(surface)
        val source = surface.macroOriginSource(declaration.source)
        if (declaration is CfirFunction) {
            return buildErrorFunction {
                this.source = source
                moduleData = declaration.moduleData
                resolvePhase = CfirResolvePhase.RAW_CFIR
                annotations.addAll(declaration.annotations)
                origin = declaration.origin
                attributes = declaration.attributes
                deprecationsProvider = declaration.deprecationsProvider
                dispatchReceiverType = declaration.dispatchReceiverType
                status = declaration.status
                typeParameters.addAll(declaration.typeParameters)
                valueParameters.addAll(declaration.valueParameters)
                body = null
                this.diagnostic = diagnostic
                symbol = CfirErrorFunctionSymbol()
            }
        }
        return org.cangnova.cangjie.cfir.declarations.builder.buildInvalidDeclaration {
            this.source = source
            moduleData = declaration.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            annotations.addAll(declaration.annotations)
            symbol = org.cangnova.cangjie.cfir.symbols.CfirInvalidDeclarationSymbol()
            origin = declaration.origin
            attributes = declaration.attributes
            reason = diagnostic.reason
        }
    }

    private fun replaceDegradedExpressionPlaceholders(
        file: CfirFile,
        expressionPlaceholders: IdentityHashMap<CfirExpression, CfirExpression>,
    ) {
        if (expressionPlaceholders.isEmpty()) return
        val transformer = object : CfirDefaultTransformer<Unit>() {
            override fun transformErrorExpression(errorExpression: CfirErrorExpression, data: Unit): CfirExpression {
                expressionPlaceholders.remove(errorExpression)?.let { return it }
                return super.transformErrorExpression(errorExpression, data)
            }
        }
        file.transform<CfirFile, Unit>(transformer, Unit)
    }

    private fun buildExpressionMacroErrorPlaceholder(
        surface: MacroSurface,
        expression: CfirExpression,
    ): CfirExpression {
        val diagnostic = macroPlaceholderDiagnostic(surface)
        val source = surface.macroOriginSource(expression.source)
        return buildErrorExpression {
            this.source = source
            this.diagnostic = diagnostic
        }
    }

    private fun buildParameterMacroErrorPlaceholder(
        surface: MacroSurface,
        parameter: CfirValueParameter,
    ): CfirValueParameter {
        val diagnostic = macroPlaceholderDiagnostic(surface)
        val source = surface.macroOriginSource(parameter.source)
        return buildValueParameter {
            this.source = source
            moduleData = parameter.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            annotations.addAll(parameter.annotations)
            origin = parameter.origin
            attributes = parameter.attributes
            isLocal = parameter.isLocal
            deprecationsProvider = parameter.deprecationsProvider
            dispatchReceiverType = parameter.dispatchReceiverType
            symbol = CfirValueParameterSymbol(
                CallableId(parameter.symbol.callableId.packageName, parameter.symbol.callableId.callableName),
            )
            containingDeclarationSymbol = parameter.containingDeclarationSymbol
            isNamed = parameter.isNamed
            status = parameter.status
            typeParameters.addAll(parameter.typeParameters)
            returnTypeRef = parameter.returnTypeRef
            name = parameter.name
            defaultValue = buildErrorExpression {
                this.source = source
                this.diagnostic = diagnostic
            }
        }
    }

    private fun MacroSurface.macroOriginSource(
        fallback: org.cangnova.cangjie.source.CjSourceElement?,
    ): org.cangnova.cangjie.source.CjSourceElement? {
        return sourceRange?.source ?: fallback
    }

    private fun macroPlaceholderDiagnostic(surface: MacroSurface): ConeSimpleDiagnostic {
        return ConeSimpleDiagnostic(
            "Macro call `${surface.capturedRawSyntax ?: surface.qualifiedName?.asString().orEmpty()}` " +
                    "was not expanded during macro construction.",
        )
    }

    private fun reportError(
        registry: MacroExpansionRegistry,
        message: String,
        kind: MacroConstructionDiagnostic.Kind,
        originSurfaceId: Long? = null,
        originSource: org.cangnova.cangjie.source.CjSourceElement? = null,
        relatedName: org.cangnova.cangjie.name.Name? = null,
        relatedTargets: List<org.cangnova.cangjie.name.FqName> = emptyList(),
        macroLibraryPath: String? = null,
        diagnosticOrigin: MacroConstructionDiagnostic.Origin = MacroConstructionDiagnostic.Origin.CONSTRUCTION,
    ) {
        registry.addDiagnostic(
            MacroConstructionDiagnostic(
                severity = MacroConstructionDiagnostic.Severity.ERROR,
                message = message,
                originSurfaceId = originSurfaceId,
                originSource = originSource,
                kind = kind,
                macroLibraryPath = macroLibraryPath,
                diagnosticOrigin = diagnosticOrigin,
                relatedName = relatedName,
                relatedTargets = relatedTargets,
            )
        )
    }

    /**
     * 按文件计算 13 维 [MacroExpansionCacheKey] 并写入 [registry]（baseline §11）。
     *
     * 每个维度的取值入口：
     * -  1. sourceContentHash         ← `CfirFile.sourceFile.getContentsAsStream()` 的 SHA-256
     * -  2. fileIdentity              ← `sourceFile.path ?: cfirFile.name`
     * -  3. macroSurfaceRangesHash    ← surfaces 的 `(fqn, range, snapshot)` 序列，不包含 session-local surfaceId
     * -  4. importsHash               ← `imports` + `defaultMacroImports` + 该文件相关 `importBindings`
     * -  5. modulePackageIdentity     ← `moduleData.name` + `packageFqName`
     * -  6. sdkSignature              ← `configuration.languageVersionSettings.toString()`
     * -  7. macroDependencySignature  ← 非源包 [MacroDefinitionEntry]（lib/shared/artifact/builtin）+ artifact/dylib/BCHIR hash
     * -  8. compilerOptionsHash       ← `useLightTree` + `moduleName` + iteration limit + compiler/debug/parallel/target/env
     * -  9. executorAbi               ← `executor.abiVersion ?: "none"`
     * - 10. constructionAlgorithmVersion ← `MacroConstructionService.ALGORITHM_VERSION`
     * - 11. tokenScannerVersion       ← `MacroPayloadTokenizer.VERSION`
     * - 12. fragmentParserVersion     ← `MacroFragmentParser.VERSION`
     * - 13. runtimeFingerprint        ← `MacroBuiltinRegistries.VERSION` + maxIterations + 展开产物快照 hash
     */
    private fun registerCacheKeys(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        expandedFiles: List<CfirFile>,
        registry: MacroExpansionRegistry,
    ) {
        val sdkSignature = org.cangnova.cangjie.utils.StableHash.sha256(
            configuration.languageVersionSettings.toString(),
        )
        val macroDependencySignature = computeMacroDependencySignature(context)
        val compilerOptionsHash = org.cangnova.cangjie.utils.StableHash.sha256Of(
            "lightTree=${configuration.useLightTree}",
            "moduleName=${configuration.moduleName.orEmpty()}",
            "maxIterations=${configuration.macroExpandMaxIterations}",
            "macroCompilerOptions=${configuration.macroCompilationCacheContext.compilerOptionsFingerprint}",
            "macroDebugFlags=${configuration.macroCompilationCacheContext.debugFlagsFingerprint}",
            "macroParallelFlags=${configuration.macroCompilationCacheContext.parallelFlagsFingerprint}",
            "macroTargetPlatform=${configuration.macroCompilationCacheContext.targetPlatform.presentableDescription}",
            "macroRuntimeLoaderEnv=${configuration.macroCompilationCacheContext.runtimeLoaderEnvironmentFingerprint}",
        )
        val executorAbi = configuration.macroExecutorFactory
            ?.create(pre.session)
            ?.abiVersion
            ?: "none"
        val maxIterations = configuration.macroExpandMaxIterations
        val expandedByName: Map<String, CfirFile> = expandedFiles.associateBy { it.name }

        for (preFile in pre.files) {
            val cfirFile = preFile.cfirFile
            val expanded = expandedByName[cfirFile.name] ?: cfirFile
            val identity = cfirFile.sourceFile?.path ?: cfirFile.name
            val key = MacroExpansionCacheKey(
                sourceContentHash = hashSourceContent(cfirFile),
                fileIdentity = identity,
                macroSurfaceRangesHash = hashSurfaces(preFile.surfaces, pre.session.annotationMetadataRegistryOrNull),
                importsHash = hashImports(cfirFile, context),
                modulePackageIdentity = "${cfirFile.moduleData.name}::${cfirFile.packageDirective.packageFqName.asString()}",
                sdkSignature = sdkSignature,
                macroDependencySignature = macroDependencySignature,
                compilerOptionsHash = compilerOptionsHash,
                executorAbi = executorAbi,
                constructionAlgorithmVersion = MacroConstructionService.ALGORITHM_VERSION,
                tokenScannerVersion = MacroPayloadTokenizer.VERSION,
                fragmentParserVersion = MacroFragmentParser.VERSION,
                runtimeFingerprint = org.cangnova.cangjie.utils.StableHash.sha256Of(
                    "builtinRegistry=${MacroBuiltinRegistries.VERSION}",
                    "iterationLimit=$maxIterations",
                    "result=${hashResultSnapshot(expanded)}",
                ),
            )
            registry.registerCacheKey(identity, key)
        }
    }

    private fun hashSourceContent(file: CfirFile): String {
        val text = runCatching {
            file.sourceFile?.getContentsAsStream()?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull() ?: file.name
        return org.cangnova.cangjie.utils.StableHash.sha256(text)
    }

    private fun hashSurfaces(
        surfaces: List<MacroSurface>,
        annotationMetadataRegistry: org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataRegistry?,
    ): String {
        if (surfaces.isEmpty()) return "surfaces:empty"
        val parts = surfaces.map { surface ->
            val range = surface.sourceRange
            val snapshot = surface.replaceHandle.annotationCarrier
                ?.let { annotationMetadataRegistry?.snapshot(it) }
            "${surface.qualifiedName?.asString().orEmpty()}|" +
                    "${range?.startOffset ?: -1}|${range?.endOffset ?: -1}|${surface.kind}|" +
                    "slot=${snapshot?.stableCacheText().orEmpty()}"
        }
        return org.cangnova.cangjie.utils.StableHash.sha256Of(parts)
    }

    private fun hashImports(file: CfirFile, context: MacroResolutionContext): String {
        val importParts = file.imports.map { import ->
            "${import.importedFqName?.asString().orEmpty()}|" +
                    "wildcard=${import.isAllUnder}|alias=${import.aliasName?.asString().orEmpty()}"
        }
        val defaultParts = context.defaultMacroImports.map { it.asString() }
        val bindingParts = context.importBindings.map { binding ->
            "${binding.importedFqName.asString()}->${binding.resolvedTargets.joinToString(",") { it.fqName.asString() }}"
        }
        return org.cangnova.cangjie.utils.StableHash.sha256Of(importParts + defaultParts + bindingParts)
    }

    private fun computeMacroDependencySignature(context: MacroResolutionContext): String {
        val sourceTargets = context.symbolIndex.sources.map { it.fqName.asString() }
        val foreignTargets = context.symbolIndex.foreigns.map { entry ->
            "${entry.fqName.asString()}|exec=${entry.executableFqName.asString()}|${entry.source}|lib=${entry.libPath.orEmpty()}|abi=${entry.executorAbi.orEmpty()}|" +
                    "artifact=${entry.artifactSignature.orEmpty()}|cjo=${entry.cjoHash.orEmpty()}|" +
                    "dylib=${entry.dynamicLibHash.orEmpty()}|bchir=${entry.dependenciesBchirHash.orEmpty()}|" +
                    "resolver=${entry.resolverAlgorithmVersion ?: -1}"
        }
        return org.cangnova.cangjie.utils.StableHash.sha256Of(sourceTargets + foreignTargets)
    }

    private fun hashResultSnapshot(file: CfirFile): String {
        if (file.declarations.isEmpty()) return "result:empty"
        val parts = buildList {
            file.declarations.forEach { declaration ->
                collectDeclarationSnapshot(declaration, this)
            }
        }
        return org.cangnova.cangjie.utils.StableHash.sha256Of(parts)
    }

    private fun collectDeclarationSnapshot(declaration: CfirDeclaration, parts: MutableList<String>) {
        parts += "${declaration::class.simpleName.orEmpty()}|${declaration.stableName()}|ann=${declaration.annotations.stableAnnotationSnapshot()}"
        if (declaration is CfirClassLikeDeclaration) {
            declaration.declarations.forEach { collectDeclarationSnapshot(it, parts) }
        }
        if (declaration is CfirExtend) {
            declaration.declarations.forEach { collectDeclarationSnapshot(it, parts) }
        }
        if (declaration is CfirFunction) {
            declaration.valueParameters.forEach { collectDeclarationSnapshot(it, parts) }
        }
        if (declaration is CfirEnumConstructor) {
            declaration.valueParameters.forEach { collectDeclarationSnapshot(it, parts) }
        }
    }

    private fun CfirDeclaration.stableName(): String = when (this) {
        is CfirClassLikeDeclaration -> name.asString()
        is CfirNamedFunction -> name.asString()
        is CfirMacroDeclaration -> name.asString()
        is CfirProperty -> name.asString()
        is CfirFieldVariable -> name.asString()
        is CfirValueParameter -> name.asString()
        is CfirTypeParameter -> name.asString()
        is CfirEnumConstructor -> name.asString()
        else -> ""
    }

    private fun List<CfirAnnotation>.stableAnnotationSnapshot(): String =
        joinToString(separator = ";") { annotation ->
            when (annotation) {
                is CfirAnnotationCall -> {
                    val args = annotation.argumentList.arguments.joinToString(separator = ",") { argument ->
                        argument.source?.text?.toString().orEmpty()
                    }
                    "${annotation.typeRef.source?.text?.toString().orEmpty()}[$args]"
                }

                else -> annotation.source?.text?.toString().orEmpty()
            }
        }

    private fun org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot.stableCacheText(): String =
        listOf(
            "index=$annotationIndex",
            "raw=$rawSyntax",
            "forced=$forcedCustom",
            "fqn=${qualifiedName?.asString().orEmpty()}",
            "args=${argumentText.orEmpty()}",
            "tokens=${tokens.joinToString(separator = "") { it.text }}",
            "callSite=$callSite",
        ).joinToString("|")

    private fun Int.toConstructionSeverity(): MacroConstructionDiagnostic.Severity {
        return when (this) {
            MacroDiagnosticSeverity.INFO -> MacroConstructionDiagnostic.Severity.INFO
            MacroDiagnosticSeverity.WARNING -> MacroConstructionDiagnostic.Severity.WARNING
            MacroDiagnosticSeverity.ERROR -> MacroConstructionDiagnostic.Severity.ERROR
            else -> MacroConstructionDiagnostic.Severity.ERROR
        }
    }

    private fun MacroExpansionFailureKind.toConstructionDiagnosticKind(): MacroConstructionDiagnostic.Kind {
        return when (this) {
            MacroExpansionFailureKind.CANNOT_FIND_METHOD -> MacroConstructionDiagnostic.Kind.MACRO_CANNOT_FIND_METHOD
            MacroExpansionFailureKind.EVALUATE_FAILED -> MacroConstructionDiagnostic.Kind.MACRO_EVALUATE_FAILED
            MacroExpansionFailureKind.EXPAND_FAILED -> MacroConstructionDiagnostic.Kind.MACRO_EXPAND_FAILED
            MacroExpansionFailureKind.PROTOCOL_ERROR -> MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_PROTOCOL_ERROR
            MacroExpansionFailureKind.SERVER_DISCONNECTED -> MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_SERVER_DISCONNECTED
            MacroExpansionFailureKind.TIMEOUT -> MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_TIMEOUT
            MacroExpansionFailureKind.SERVER_CRASH -> MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_SERVER_CRASH
        }
    }

    private fun MacroLibraryLoadFailureKind.toConstructionDiagnosticKind(): MacroConstructionDiagnostic.Kind {
        return when (this) {
            MacroLibraryLoadFailureKind.CANNOT_OPEN_LIB -> MacroConstructionDiagnostic.Kind.MACRO_CANNOT_OPEN_LIB
            MacroLibraryLoadFailureKind.PROTOCOL_ERROR -> MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_PROTOCOL_ERROR
            MacroLibraryLoadFailureKind.SERVER_DISCONNECTED -> MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_SERVER_DISCONNECTED
            MacroLibraryLoadFailureKind.TIMEOUT -> MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_TIMEOUT
            MacroLibraryLoadFailureKind.SERVER_CRASH -> MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_SERVER_CRASH
        }
    }
}

/**
 * 仓颉 builtin non-macro desugar。
 *
 * `@IfAvailable` 不送 macro executor，也不进入 final CFIR；construction
 * 阶段只保留其可用分支 fragment，随后由上层消费该 construction-only
 * fragment，避免把 builtin non-macro surface 交给 ordinary resolve。
 */
private object CangJieBuiltinNonMacroDesugarer : BuiltinNonMacroDesugarer {
    override fun desugar(
        surface: BuiltinNonMacroSurface,
        fragment: MacroFragmentResult.Success,
    ): MacroFragmentResult? {
        return when (surface) {
            is IfAvailableSurface -> fragment.copy(
                tokens = surface.branchTokens.ifEmpty { fragment.tokens },
            )
        }
    }
}

/**
 * 基于 raw builder 保留的 typed expression carrier 执行稳定替换。
 *
 * 该实现只处理 expression macro：fragment parser 必须把 payload 解析成
 * [CfirExpression]，然后这里按 construction surface 顺序替换对应
 * typed carrier。声明 / 参数 / builtin non-macro 等其它 surface 没有
 * expression carrier 时会硬失败，避免 silent identity splice。
 */
private object CfirExpressionMacroStableSplicer : MacroStableSplicer {
    override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): List<CfirFile> {
        val expressionSlotsWithCarrier = IdentityHashMap<CfirExpression, MacroReplaceSlot>()
        val declarationSlotsWithCarrier = IdentityHashMap<CfirDeclaration, MacroReplaceSlot>()
        val parameterSlotsWithCarrier = IdentityHashMap<CfirValueParameter, MacroReplaceSlot>()
        val annotationSlotsWithCarrier =
            linkedMapOf<org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationReplaceCarrier, MacroReplaceSlot>()
        val unsupportedSlots = mutableListOf<MacroReplaceSlot>()

        for (slot in slots) {
            val annotationCarrier = slot.handle.annotationCarrier
            if (annotationCarrier != null && slot.fragment is MacroFragmentResult.CustomAnnotation) {
                require(annotationSlotsWithCarrier.put(annotationCarrier, slot) == null) {
                    "Stable splice received duplicate annotation slot for index ${annotationCarrier.annotationIndex}."
                }
                continue
            }
            when (val origin = slot.origin) {
                is MacroSurfaceExpr -> {
                    val carrier = slot.handle.carrier
                    if (carrier is CfirExpression) {
                        expressionSlotsWithCarrier[carrier] = slot
                    } else {
                        unsupportedSlots += slot
                    }
                }

                is MacroSurfaceParam -> {
                    val carrier = slot.handle.carrier
                    if (carrier is CfirValueParameter) {
                        parameterSlotsWithCarrier[carrier] = slot
                    } else {
                        unsupportedSlots += slot
                    }
                }

                is MacroSurfaceDecl, is MacroSurfaceNode, is BuiltinNonMacroSurface -> {
                    val carrier = origin.replaceHandle.carrier
                    if (carrier is CfirDeclaration) {
                        declarationSlotsWithCarrier[carrier] = slot
                    } else {
                        unsupportedSlots += slot
                    }
                }
            }
        }

        val annotationOwners = annotationSlotsWithCarrier.keys.mapTo(linkedSetOf()) { it.owner }
        val ownerConflicts = buildList {
            addAll(declarationSlotsWithCarrier.keys.filter { it in annotationOwners })
            addAll(parameterSlotsWithCarrier.keys.filter { it in annotationOwners })
        }
        require(ownerConflicts.isEmpty()) {
            "Stable splice cannot replace an owner/parameter and one of its annotation slots in the same batch: " +
                    ownerConflicts.joinToString { it.symbol.toString() }
        }

        require(unsupportedSlots.isEmpty()) {
            "Stable splice is not implemented for macro surface(s) without typed carrier: " +
                    unsupportedSlots.joinToString { it.origin.qualifiedName?.asString().orEmpty() }
        }

        for (file in files) {
            replaceDeclarationSlots(file.declarations, declarationSlotsWithCarrier, parameterSlotsWithCarrier)
        }
        replaceAnnotationSlots(annotationSlotsWithCarrier)

        val transformer = object : CfirDefaultTransformer<Unit>() {
            override fun transformErrorExpression(errorExpression: CfirErrorExpression, data: Unit): CfirExpression {
                val slot = expressionSlotsWithCarrier.remove(errorExpression)
                    ?: return super.transformErrorExpression(errorExpression, data) as CfirExpression
                return slot.toExpressionPayload()
            }
        }

        for (file in files) {
            file.transform<CfirFile, Unit>(transformer, Unit)
        }

        require(expressionSlotsWithCarrier.isEmpty()) {
            "Stable splice could not find expression carrier for macro surface(s): " +
                    expressionSlotsWithCarrier.values.joinToString { it.origin.qualifiedName?.asString().orEmpty() }
        }
        require(declarationSlotsWithCarrier.isEmpty()) {
            "Stable splice could not find declaration carrier for macro surface(s): " +
                    declarationSlotsWithCarrier.values.joinToString { it.origin.qualifiedName?.asString().orEmpty() }
        }
        require(parameterSlotsWithCarrier.isEmpty()) {
            "Stable splice could not find parameter carrier for macro surface(s): " +
                    parameterSlotsWithCarrier.values.joinToString { it.origin.qualifiedName?.asString().orEmpty() }
        }
        require(annotationSlotsWithCarrier.isEmpty()) {
            "Stable splice could not find annotation carrier for macro surface(s): " +
                    annotationSlotsWithCarrier.values.joinToString { it.origin.qualifiedName?.asString().orEmpty() }
        }
        return files
    }

    private fun replaceAnnotationSlots(
        annotationSlots: MutableMap<org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationReplaceCarrier, MacroReplaceSlot>,
    ) {
        val entries = annotationSlots.entries.toList()
        for ((carrier, slot) in entries) {
            val annotations = carrier.owner.annotations
            require(annotations.getOrNull(carrier.annotationIndex) === carrier.originalAnnotation) {
                "Stable splice annotation carrier no longer matches owner/index ${carrier.annotationIndex}."
            }
            val replacement = (slot.fragment as? MacroFragmentResult.CustomAnnotation)?.payload
                ?: error(
                    "Annotation macro surface `${
                        slot.origin.qualifiedName?.asString().orEmpty()
                    }` did not produce a CfirAnnotationCall payload."
                )
            val mutableAnnotations = annotations.toMutableList()
            mutableAnnotations[carrier.annotationIndex] = replacement
            carrier.owner.replaceAnnotations(mutableAnnotations)
            annotationSlots.remove(carrier)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun replaceDeclarationSlots(
        declarations: List<CfirDeclaration>,
        declarationSlots: IdentityHashMap<CfirDeclaration, MacroReplaceSlot>,
        parameterSlots: IdentityHashMap<CfirValueParameter, MacroReplaceSlot>,
    ) {
        val mutableDeclarations = declarations as? MutableList<CfirDeclaration>
            ?: error("Stable splice requires mutable declaration carrier list.")
        val iterator = mutableDeclarations.listIterator()
        while (iterator.hasNext()) {
            val declaration = iterator.next()
            val replacement = declarationSlots.remove(declaration)?.toDeclarationPayload()
            val current = replacement ?: declaration
            if (replacement != null) {
                iterator.set(replacement)
            }
            replaceParameterSlots(current, parameterSlots)
            replaceNestedDeclarationSlots(current, declarationSlots, parameterSlots)
        }
    }

    private fun replaceNestedDeclarationSlots(
        declaration: CfirDeclaration,
        declarationSlots: IdentityHashMap<CfirDeclaration, MacroReplaceSlot>,
        parameterSlots: IdentityHashMap<CfirValueParameter, MacroReplaceSlot>,
    ) {
        when (declaration) {
            is CfirClassLikeDeclaration -> replaceDeclarationSlots(
                declaration.declarations,
                declarationSlots,
                parameterSlots
            )

            is CfirExtend -> replaceDeclarationSlots(declaration.declarations, declarationSlots, parameterSlots)
            else -> Unit
        }
    }

    private fun replaceParameterSlots(
        declaration: CfirDeclaration,
        parameterSlots: IdentityHashMap<CfirValueParameter, MacroReplaceSlot>,
    ) {
        if (declaration !is CfirFunction || declaration.valueParameters.isEmpty()) return
        val newParameters = declaration.valueParameters.map { parameter ->
            parameterSlots.remove(parameter)?.toValueParameterPayload() ?: parameter
        }
        declaration.replaceValueParameters(newParameters)
    }

    private fun MacroReplaceSlot.toExpressionPayload(): CfirExpression {
        val success = fragment as? MacroFragmentResult.Success
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not parse successfully.")
        return success.payload as? CfirExpression
            ?: error(
                "Macro fragment for `${
                    origin.qualifiedName?.asString().orEmpty()
                }` did not produce a CfirExpression payload."
            )
    }

    private fun MacroReplaceSlot.toDeclarationPayload(): CfirDeclaration {
        val success = fragment as? MacroFragmentResult.Success
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not parse successfully.")
        return success.payload as? CfirDeclaration
            ?: error(
                "Macro fragment for `${
                    origin.qualifiedName?.asString().orEmpty()
                }` did not produce a CfirDeclaration payload."
            )
    }

    private fun MacroReplaceSlot.toValueParameterPayload(): CfirValueParameter {
        val success = fragment as? MacroFragmentResult.Success
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not parse successfully.")
        return success.payload as? CfirValueParameter
            ?: error(
                "Macro fragment for `${
                    origin.qualifiedName?.asString().orEmpty()
                }` did not produce a CfirValueParameter payload."
            )
    }
}

private fun MacroFragmentResult.parentVisibleTokens(): List<MacroSurfaceToken> {
    return when (this) {
        is MacroFragmentResult.Success -> tokens
        is MacroFragmentResult.CustomAnnotation -> tokens
        is MacroFragmentResult.Failure -> emptyList()
    }
}

private fun MacroSurface.toMacroCallInfo(
    entry: MacroDefinitionEntry,
    parentNames: List<String>,
    refreshedTokens: RefreshedMacroSurfaceTokens,
    preFile: PreMacroCfirFile?,
): MacroCallInfo {
    val linesMapping = preFile?.cfirFile?.sourceFileLinesMapping
    val start = sourceRange?.startOffset.toSourcePosition(linesMapping)
    val endOffset = sourceRange?.endOffset?.minus(1)?.coerceAtLeast(sourceRange?.startOffset ?: 0)
    val end = endOffset.toSourcePosition(linesMapping)
    val hasAttrs = refreshedTokens.attrTokens.isNotEmpty()
    val packageName = entry.executablePackageFqName.asString().takeUnless { it == "<root>" }.orEmpty()
    return MacroCallInfo(
        idName = entry.name.asString(),
        methodName = macroWrapperFunctionName(packageName, hasAttrs, entry.executableName.asString()),
        packageName = packageName,
        libPath = entry.libPath.orEmpty(),
        hasAttrs = hasAttrs,
        argTokens = refreshedTokens.inputTokens.toTokenInfo(start, end, linesMapping),
        attrTokens = refreshedTokens.attrTokens.toTokenInfo(start, end, linesMapping),
        parentNames = parentNames,
        position = start,
        endPosition = end,
    )
}

/**
 * `macro package` 中的 `public macro` 定义是 artifact 发现/编译的输入，
 * 不是本轮 macro construction 要送 executor 的调用 surface。
 *
 * 这里仅排除宏定义自身以及宏定义形参上由 raw builder 暂存的 surface；
 * 宏定义体或同包源码中真实的 `@Macro(...)` 表达式仍会进入后续解析，并按
 * same-package def/call 规则报错。
 */
private fun MacroSurface.isMacroDefinitionSignatureSurface(): Boolean {
    val carrier = replaceHandle.carrier
    if (carrier is CfirMacroDeclaration) return true
    return carrier is CfirValueParameter &&
            carrier.containingDeclarationSymbol is CfirMacroDeclarationSymbol
}

/**
 * 对齐官方 `Utils::GetMacroFuncName`：宏定义编译后导出的 wrapper 名称不是源码宏名，
 * 而是按 plain/attr 前缀、宏名和完整包名组成，并将包名中的 `.` 替换为 `_`。
 */
private fun macroWrapperFunctionName(
    packageName: String,
    hasAttrs: Boolean,
    macroName: String,
): String {
    val prefix = if (hasAttrs) "macroCall_a_" else "macroCall_c_"
    return (prefix + macroName + "_" + packageName).replace('.', '_')
}

private data class RefreshedMacroSurfaceTokens(
    val inputTokens: List<MacroSurfaceToken>,
    val attrTokens: List<MacroSurfaceToken>,
)

private fun MacroCallNode.hasUnresolvedChildPayloadChannel(
    childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
): Boolean {
    return childEdges.any { edge ->
        edge.child in childResults && edge.channel == MacroPayloadChannel.UNRESOLVED
    }
}

private fun MacroCallNode.refreshTokensWithChildResults(
    childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
): RefreshedMacroSurfaceTokens {
    if (childResults.isEmpty()) {
        return RefreshedMacroSurfaceTokens(inputTokens = surface.inputTokens, attrTokens = surface.attrTokens)
    }

    val input = surface.inputTokens.replaceChildMacroRanges(childResults, MacroPayloadChannel.INPUT)
    val attr = surface.attrTokens.replaceChildMacroRanges(childResults, MacroPayloadChannel.ATTR)
    return RefreshedMacroSurfaceTokens(inputTokens = input, attrTokens = attr)
}

private fun List<MacroSurfaceToken>.replaceChildMacroRanges(
    childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
    channel: MacroPayloadChannel,
): List<MacroSurfaceToken> {
    if (isEmpty() || childResults.isEmpty()) return this

    var current = this
    val replacements = childResults.entries.asSequence()
        .filter { (child, _) -> child.parent?.childEdges.orEmpty().any { it.child === child && it.channel == channel } }
        .sortedWith(
            compareBy(
                { it.key.surface.sourceRange?.startOffset ?: Int.MAX_VALUE },
                { it.key.surface.sourceRange?.endOffset ?: Int.MAX_VALUE },
                { it.key.surface.surfaceId },
            ),
        ).toList()

    for ((child, replacement) in replacements) {
        val range = child.surface.sourceRange ?: continue
        val startIndex = current.indexOfFirst { it.isInside(range) }
        if (startIndex < 0) continue
        val endIndex = current.indexOfLast { it.isInside(range) }
        current = buildList(current.size - (endIndex - startIndex + 1) + replacement.size) {
            addAll(current.subList(0, startIndex))
            addAll(replacement)
            addAll(current.subList(endIndex + 1, current.size))
        }
    }
    return current
}

private fun MacroSurfaceToken.isInside(range: org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceSourceRange): Boolean {
    return startOffset >= range.startOffset && endOffset <= range.endOffset
}

private fun List<MacroSurfaceToken>.toTokenInfo(
    defaultBegin: SourcePosition,
    defaultEnd: SourcePosition,
): List<TokenInfo> = toTokenInfo(defaultBegin, defaultEnd, null)

private fun List<MacroSurfaceToken>.toTokenInfo(
    defaultBegin: SourcePosition,
    defaultEnd: SourcePosition,
    linesMapping: CjSourceFileLinesMapping?,
): List<TokenInfo> = map { token ->
    TokenInfo(
        kind = 0u.toUByte(),
        value = token.text,
        begin = token.startOffset.toSourcePosition(linesMapping).takeIf { token.startOffset >= 0 } ?: defaultBegin,
        end = token.endOffset.toSourcePosition(linesMapping).takeIf { token.endOffset >= 0 } ?: defaultEnd,
    )
}

private fun Int?.toSourcePosition(linesMapping: CjSourceFileLinesMapping?): SourcePosition {
    val offset = this ?: return SourcePosition()
    if (offset < 0 || linesMapping == null) return SourcePosition()
    val (line, column) = linesMapping.getLineAndColumnByOffset(offset)
    if (line < 0 || column < 0) return SourcePosition()
    return SourcePosition(line = line + 1, column = column + 1)
}

private fun List<TokenInfo>.toMacroSurfaceTokens(): List<MacroSurfaceToken> {
    if (isEmpty()) return emptyList()
    val text = MacroMsgCodec.rebuildExpandedText(this)
    return listOf(
        MacroSurfaceToken(
            text = text,
            startOffset = 0,
            endOffset = text.length,
        )
    )
}

private fun stringLiteral(value: String): String = buildString {
    append('"')
    for (ch in value) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
    append('"')
}
