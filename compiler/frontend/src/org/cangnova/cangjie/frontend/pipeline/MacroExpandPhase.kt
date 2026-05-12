package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildErrorFunction
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroDesugarer
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinMacroRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroSurface
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
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
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
import org.cangnova.cangjie.cfir.symbols.CfirErrorFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
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
        mode: MacroConstructionService.Mode,
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

        // baseline 第 12 节 Batch 5："alias conflict / macro package / ..."。
        reportAliasConflicts(context, registry)

        val expandedFiles = expandMacroSurfaces(pre, context, registry) ?: rawFiles
        if (mode == MacroConstructionService.Mode.DEGRADED &&
            pre.allSurfaces.isNotEmpty() &&
            registry.hasErrors &&
            registry.diagnostics.all(::isDegradableDiagnostic)
        ) {
            replaceSurfaceOnlyDegradedPlaceholders(pre, expandedFiles, registry)
            builtDegradedPlaceholders = true
        }

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
        registry: MacroExpansionRegistry,
    ): List<CfirFile>? {
        if (pre.allSurfaces.isEmpty()) return pre.files.map { it.cfirFile }

        val parserFactory = configuration.macroFragmentParserFactory
        if (parserFactory == null) {
            for (surface in pre.allSurfaces) {
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
        val forest = MacroCallForestBuilder.build(pre.allSurfaces)
        val surfaceFiles = pre.files
            .flatMap { preFile -> preFile.surfaces.map { surface -> surface.surfaceId to preFile } }
            .toMap()

        evaluator.evaluate(
            forest = forest,
            expand = { node, childResults ->
                val surface = node.surface
                val name = surface.qualifiedName?.shortName()
                if (name == null) {
                    reportError(
                        registry = registry,
                        message = "Macro surface `${surface.capturedRawSyntax.orEmpty()}` has no resolvable macro name.",
                        kind = MacroConstructionDiagnostic.Kind.MACRO_UNRESOLVED,
                        originSurfaceId = surface.surfaceId,
                    )
                    return@evaluate null
                }

                val resolution = context.resolveMacroCall(
                    callPackage = surface.scopeContext.packageFqName,
                    qualifier = surface.qualifiedName?.parent()?.takeUnless { it == surface.scopeContext.packageFqName },
                    name = name,
                    kind = surface.kind,
                )
                if (node.hasUnresolvedChildPayloadChannel(childResults)) {
                    reportError(
                        registry = registry,
                        message = "Nested macro surface inside `${surface.qualifiedName?.asString().orEmpty()}` cannot be mapped to attr or input token payload.",
                        kind = MacroConstructionDiagnostic.Kind.MACRO_REEVALUATION_FAILED,
                        originSurfaceId = surface.surfaceId,
                    )
                    return@evaluate null
                }
                val refreshedTokens = node.refreshTokensWithChildResults(childResults)
                val expandedTokens = expandResolvedSurface(
                    surface = surface,
                    resolution = resolution,
                    node = node,
                    childResults = childResults,
                    refreshedTokens = refreshedTokens,
                    executor = executor,
                    registry = registry,
                    preFile = surfaceFiles[surface.surfaceId],
                ) ?: return@evaluate null

                val fragment = parseAndDesugarFragment(
                    surface = surface,
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
                    message = "Macro call `${slot.origin.qualifiedName?.asString().orEmpty()}` produced a fragment, but stable splicer is not configured.",
                    kind = MacroConstructionDiagnostic.Kind.MACRO_NOT_EXPANDED,
                    originSurfaceId = slot.origin.surfaceId,
                )
            }
            return null
        }

        return runCatching {
            stableSplicer.applySlices(pre.files.map { it.cfirFile }, slots)
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
        surface: MacroSurface,
        resolution: MacroResolution,
        node: MacroCallNode,
        childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
        refreshedTokens: RefreshedMacroSurfaceTokens,
        executor: MacroExecutor?,
        registry: MacroExpansionRegistry,
        preFile: PreMacroCfirFile?,
    ): List<MacroSurfaceToken>? {
        return when (resolution) {
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
                reportError(
                    registry = registry,
                    message = "Macro call `@${resolution.entry.name.asString()}` does not support `${surface.kind}` invocation.",
                    kind = MacroConstructionDiagnostic.Kind.MACRO_UNRESOLVED,
                    originSurfaceId = surface.surfaceId,
                )
                null
            }
            is MacroResolution.BuiltinNonMacro -> tokensForBuiltinNonMacro(surface, refreshedTokens)
            is MacroResolution.CustomAnnotation -> refreshedTokens.inputTokens.ifEmpty { refreshedTokens.attrTokens }
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
            BuiltinMacroRegistry.sourceFile -> stringLiteral(preFile?.cfirFile?.sourceFile?.name ?: preFile?.cfirFile?.name.orEmpty())
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

        entry.libPath?.takeIf { it.isNotBlank() }?.let { executor.loadLibraries(listOf(it)) }
        val callInfo = surface.toMacroCallInfo(entry, node.parentNames, refreshedTokens, preFile)
        val result = executor.execute(listOf(callInfo)).singleOrNull()
            ?: MacroExpansionResult.Failure("Macro executor returned no result for `${entry.name.asString()}`.")

        return when (result) {
            is MacroExpansionResult.Success -> {
                result.diagnostics.forEach { diagnostic ->
                    registry.addDiagnostic(
                        MacroConstructionDiagnostic(
                            severity = if (diagnostic.severity > 0) {
                                MacroConstructionDiagnostic.Severity.ERROR
                            } else {
                                MacroConstructionDiagnostic.Severity.INFO
                            },
                            message = diagnostic.message,
                            originSurfaceId = surface.surfaceId,
                        )
                    )
                }
                result.tokens.toMacroSurfaceTokens()
            }
            is MacroExpansionResult.Failure -> {
                reportError(
                    registry = registry,
                    message = result.message,
                    kind = MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED,
                    originSurfaceId = surface.surfaceId,
                )
                null
            }
        }
    }

    private fun parseAndDesugarFragment(
        surface: MacroSurface,
        node: MacroCallNode,
        parser: MacroFragmentParser,
        tokens: List<MacroSurfaceToken>,
        registry: MacroExpansionRegistry,
    ): MacroFragmentResult? {
        val mode = when (surface) {
            is MacroSurfaceDecl, is MacroSurfaceParam, is MacroSurfaceNode, is BuiltinNonMacroSurface ->
                MacroFragmentParser.Mode.DECLARATION
            is MacroSurfaceExpr -> MacroFragmentParser.Mode.EXPRESSION
        }
        val parsed = parser.parse(node, tokens, mode)
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
            MacroConstructionDiagnostic.Kind.MACRO_REEVALUATION_FAILED,
            MacroConstructionDiagnostic.Kind.MACRO_UNRESOLVED -> true
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
        for (surface in pre.allSurfaces) {
            registry.registerPlaceholder(
                placeholderId = surface.surfaceId,
                originSurfaceId = surface.surfaceId,
            )
            when (val carrier = surface.replaceHandle.carrier) {
                is CfirValueParameter -> parameterPlaceholders[carrier] = buildParameterMacroErrorPlaceholder(surface, carrier)
                is CfirDeclaration -> declarationPlaceholders[carrier] = buildDeclarationMacroErrorPlaceholder(surface, carrier)
            }
        }
        for (file in files) {
            replaceDegradedDeclarationPlaceholders(file.declarations, declarationPlaceholders, parameterPlaceholders)
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
                    replaceDegradedDeclarationPlaceholders(current.declarations, declarationPlaceholders, parameterPlaceholders)
                is CfirExtend ->
                    replaceDegradedDeclarationPlaceholders(current.declarations, declarationPlaceholders, parameterPlaceholders)
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

    private fun buildParameterMacroErrorPlaceholder(
        surface: MacroSurface,
        parameter: CfirValueParameter,
    ): CfirValueParameter {
        val diagnostic = macroPlaceholderDiagnostic(surface)
        val source = surface.macroOriginSource(parameter.source)
        return org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter {
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
    ) {
        registry.addDiagnostic(
            MacroConstructionDiagnostic(
                severity = MacroConstructionDiagnostic.Severity.ERROR,
                message = message,
                originSurfaceId = originSurfaceId,
                originSource = originSource,
                kind = kind,
                relatedName = relatedName,
                relatedTargets = relatedTargets,
            )
        )
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
        val unsupportedSlots = mutableListOf<MacroReplaceSlot>()

        for (slot in slots) {
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

        require(unsupportedSlots.isEmpty()) {
            "Stable splice is not implemented for macro surface(s) without typed carrier: " +
                unsupportedSlots.joinToString { it.origin.qualifiedName?.asString().orEmpty() }
        }

        for (file in files) {
            replaceDeclarationSlots(file.declarations, declarationSlotsWithCarrier, parameterSlotsWithCarrier)
        }

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
        return files
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
            is CfirClassLikeDeclaration -> replaceDeclarationSlots(declaration.declarations, declarationSlots, parameterSlots)
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
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not produce a CfirExpression payload.")
    }

    private fun MacroReplaceSlot.toDeclarationPayload(): CfirDeclaration {
        val success = fragment as? MacroFragmentResult.Success
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not parse successfully.")
        return success.payload as? CfirDeclaration
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not produce a CfirDeclaration payload.")
    }

    private fun MacroReplaceSlot.toValueParameterPayload(): CfirValueParameter {
        val success = fragment as? MacroFragmentResult.Success
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not parse successfully.")
        return success.payload as? CfirValueParameter
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not produce a CfirValueParameter payload.")
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
    return MacroCallInfo(
        idName = entry.name.asString(),
        methodName = entry.name.asString(),
        packageName = entry.packageFqName.asString().takeUnless { it == "<root>" }.orEmpty(),
        libPath = entry.libPath.orEmpty(),
        hasAttrs = refreshedTokens.attrTokens.isNotEmpty(),
        argTokens = refreshedTokens.inputTokens.toTokenInfo(start, end, linesMapping),
        attrTokens = refreshedTokens.attrTokens.toTokenInfo(start, end, linesMapping),
        parentNames = parentNames,
        position = start,
        endPosition = end,
    )
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

private fun List<TokenInfo>.toMacroSurfaceTokens(): List<MacroSurfaceToken> = map { token ->
    MacroSurfaceToken(
        text = token.value,
        startOffset = token.begin.line,
        endOffset = token.end.line,
        kindName = token.kind.toString(),
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
