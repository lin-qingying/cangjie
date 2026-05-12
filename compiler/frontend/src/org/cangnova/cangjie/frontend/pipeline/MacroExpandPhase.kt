package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirMacroExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroDesugarer
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinMacroRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.IdentityBuiltinNonMacroDesugarer
import org.cangnova.cangjie.cfir.resolve.providers.macro.IdentityMacroStableSplicer
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
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.messages.CompilerMessageSeverity

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
    private val builtinNonMacroDesugarer: BuiltinNonMacroDesugarer = IdentityBuiltinNonMacroDesugarer,
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
        val residualMacroExpressionCount = countLegacyMacroExpressions(expandedFiles)
        if (residualMacroExpressionCount > 0) {
            if (mode == MacroConstructionService.Mode.DEGRADED &&
                canReplaceResidualMacroExpressions(pre, residualMacroExpressionCount, registry)
            ) {
                replaceResidualMacroExpressionsWithErrorPlaceholders(pre, expandedFiles, registry)
                builtDegradedPlaceholders = true
            } else {
                reportResidualMacroExpressions(residualMacroExpressionCount, registry)
            }
        }
        if (mode == MacroConstructionService.Mode.DEGRADED &&
            residualMacroExpressionCount == 0 &&
            pre.allSurfaces.isNotEmpty() &&
            registry.hasErrors &&
            registry.diagnostics.all(::isDegradableDiagnostic)
        ) {
            registerSurfaceOnlyDegradedPlaceholders(pre, registry)
            builtDegradedPlaceholders = true
        }

        // baseline 第 10 节："session/analysis 长生命周期 registry"挂到 session
        // 上，供 ordinary checker / IDE / LSP 通过 `session.macroExpansionRegistry` 读取。
        session.register(MacroExpansionRegistry::class, registry)

        if (registry.hasErrors) {
            if (mode == MacroConstructionService.Mode.DEGRADED &&
                builtDegradedPlaceholders &&
                countLegacyMacroExpressions(expandedFiles) == 0 &&
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
                val expandedTokens = expandResolvedSurface(
                    surface = surface,
                    resolution = resolution,
                    node = node,
                    childResults = childResults,
                    executor = executor,
                    registry = registry,
                ) ?: return@evaluate null

                val fragment = parseAndDesugarFragment(
                    surface = surface,
                    node = node,
                    parser = parser,
                    tokens = expandedTokens,
                    registry = registry,
                ) ?: return@evaluate null

                slots += MacroReplaceSlot(
                    handle = surface.replaceHandle,
                    origin = surface,
                    fragment = fragment,
                )
                expandedTokens
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
        executor: MacroExecutor?,
        registry: MacroExpansionRegistry,
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
            is MacroResolution.BuiltinNonMacro -> tokensForBuiltinNonMacro(surface)
            is MacroResolution.CustomAnnotation -> surface.inputTokens.ifEmpty { surface.attrTokens }
            is MacroResolution.Builtin -> evaluateBuiltinMacro(surface, resolution.entry, registry)
            is MacroResolution.Resolved -> evaluateExternalMacro(
                surface = surface,
                entry = resolution.entry,
                node = node,
                childResults = childResults,
                executor = executor,
                registry = registry,
            )
        }
    }

    private fun evaluateBuiltinMacro(
        surface: MacroSurface,
        entry: MacroDefinitionEntry,
        registry: MacroExpansionRegistry,
    ): List<MacroSurfaceToken>? {
        val text = when (entry.name) {
            BuiltinMacroRegistry.sourcePackage -> stringLiteral(surface.scopeContext.packageFqName.asString())
            BuiltinMacroRegistry.sourceFile -> stringLiteral(surface.sourceRange?.source?.getElementTextInContextForDebug().orEmpty())
            BuiltinMacroRegistry.sourceLine -> {
                val line = surface.sourceRange?.startOffset ?: 0
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
        executor: MacroExecutor?,
        registry: MacroExpansionRegistry,
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
        val callInfo = surface.toMacroCallInfo(entry, node.parentNames, childResults)
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

    private fun tokensForBuiltinNonMacro(surface: MacroSurface): List<MacroSurfaceToken> {
        return if (surface is org.cangnova.cangjie.cfir.resolve.providers.macro.IfAvailableSurface) {
            surface.branchTokens.ifEmpty { surface.inputTokens }
        } else {
            surface.inputTokens.ifEmpty { surface.attrTokens }
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
            )
        }
    }

    /**
     * 若 raw builder 仍产生旧 [CfirMacroExpression]，construction 必须失败；
     * 它不能进入 provider-visible final CFIR。
     */
    private fun reportResidualMacroExpressions(
        count: Int,
        registry: MacroExpansionRegistry,
    ) {
        reportError(
            registry = registry,
            message = "$count residual CfirMacroExpression node(s) remain after macro construction.",
            kind = MacroConstructionDiagnostic.Kind.MACRO_NOT_EXPANDED,
        )
    }

    private fun canReplaceResidualMacroExpressions(
        pre: PreMacroRawBuildResult,
        residualMacroExpressionCount: Int,
        registry: MacroExpansionRegistry,
    ): Boolean {
        if (residualMacroExpressionCount == pre.allSurfaces.size) return true
        reportError(
            registry = registry,
            message = "Cannot build degraded macro placeholders: $residualMacroExpressionCount residual " +
                "CfirMacroExpression node(s) do not match ${pre.allSurfaces.size} collected macro surface(s).",
            kind = MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED,
        )
        return false
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
     * IDE degraded mode 的 typed placeholder 构造。
     *
     * 这里只把 expression 位点的旧 [CfirMacroExpression] 替换为现有
     * [org.cangnova.cangjie.cfir.expressions.CfirErrorExpression]；不引入
     * `CfirMacroErrorPlaceholder`，也不把 construction surface 放进 final CFIR。
     */
    private fun replaceResidualMacroExpressionsWithErrorPlaceholders(
        pre: PreMacroRawBuildResult,
        files: List<CfirFile>,
        registry: MacroExpansionRegistry,
    ) {
        val surfaces = ArrayDeque(pre.allSurfaces)
        val transformer = object : CfirDefaultTransformer<Unit>() {
            override fun transformMacroExpression(macroExpression: CfirMacroExpression, data: Unit): CfirExpression {
                val surface = surfaces.removeFirst()
                registry.registerPlaceholder(
                    placeholderId = surface.surfaceId,
                    originSurfaceId = surface.surfaceId,
                )
                return buildErrorExpression {
                    source = macroExpression.source
                    annotations.addAll(macroExpression.annotations)
                    diagnostic = ConeSimpleDiagnostic(
                        "Macro call `${macroExpression.name?.asString() ?: surface.capturedRawSyntax ?: "<unknown>"}` " +
                            "was not expanded during macro construction.",
                    )
                }
            }
        }
        for (file in files) {
            file.transform<CfirFile, Unit>(transformer, Unit)
        }
    }

    /**
     * 声明 / 参数 / builtin non-macro surface 不会以 [CfirMacroExpression] 形态进入 final CFIR。
     * DEGRADED 模式下用 registry 记录原始 surface 与占位 id 的映射，由 IDE/LSP
     * 按 original macro site 渲染诊断；不创建 `CfirMacroErrorPlaceholder`，
     * 也不把 construction surface 放进 final tree。
     */
    private fun registerSurfaceOnlyDegradedPlaceholders(
        pre: PreMacroRawBuildResult,
        registry: MacroExpansionRegistry,
    ) {
        for (surface in pre.allSurfaces) {
            registry.registerPlaceholder(
                placeholderId = surface.surfaceId,
                originSurfaceId = surface.surfaceId,
            )
        }
    }

    private fun reportError(
        registry: MacroExpansionRegistry,
        message: String,
        kind: MacroConstructionDiagnostic.Kind,
        originSurfaceId: Long? = null,
    ) {
        configuration.messageCollector.report(CompilerMessageSeverity.ERROR, message)
        registry.addDiagnostic(
            MacroConstructionDiagnostic(
                severity = MacroConstructionDiagnostic.Severity.ERROR,
                message = message,
                originSurfaceId = originSurfaceId,
                kind = kind,
            )
        )
    }
}

private fun countLegacyMacroExpressions(files: List<CfirFile>): Int {
    var count = 0
    val visitor = object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            element.acceptChildren(this, null)
        }

        override fun visitMacroExpression(macroExpression: CfirMacroExpression) {
            count++
            super.visitMacroExpression(macroExpression)
        }
    }
    for (file in files) {
        file.accept(visitor, null)
    }
    return count
}

/**
 * 基于 raw builder 保留的 legacy expression carrier 执行稳定替换。
 *
 * 该实现只处理 expression macro：fragment parser 必须把 payload 解析成
 * [CfirExpression]，然后这里按 construction surface 顺序替换对应
 * [CfirMacroExpression]。声明 / 参数 / builtin non-macro 等其它 surface
 * 没有 expression carrier 时会硬失败，避免 silent identity splice。
 */
private object CfirExpressionMacroStableSplicer : MacroStableSplicer {
    override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): List<CfirFile> {
        val expressionSlots = ArrayDeque(slots.filter { it.origin is MacroSurfaceExpr })
        val unsupportedSlots = slots.filterNot { it.origin is MacroSurfaceExpr }
        require(unsupportedSlots.isEmpty()) {
            "Stable splice is not implemented for non-expression macro surface(s): " +
                unsupportedSlots.joinToString { it.origin.qualifiedName?.asString().orEmpty() }
        }

        val transformer = object : CfirDefaultTransformer<Unit>() {
            override fun transformMacroExpression(macroExpression: CfirMacroExpression, data: Unit): CfirExpression {
                val slot = expressionSlots.removeFirstOrNull()
                    ?: return super.transformMacroExpression(macroExpression, data)
                val success = slot.fragment as? MacroFragmentResult.Success
                    ?: error("Macro fragment for `${slot.origin.qualifiedName?.asString().orEmpty()}` did not parse successfully.")
                return success.payload as? CfirExpression
                    ?: error("Macro fragment for `${slot.origin.qua