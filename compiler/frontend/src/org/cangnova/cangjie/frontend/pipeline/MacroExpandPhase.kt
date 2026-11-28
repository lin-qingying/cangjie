package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
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
import org.cangnova.cangjie.cfir.declarations.CfirParameterDefaultValueMetadata
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata
import org.cangnova.cangjie.cfir.declarations.builder.buildErrorFunction
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinMacroRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationReplaceCarrier
import org.cangnova.cangjie.cfir.resolve.providers.macro.FinalMacroSurfaceDecision
import org.cangnova.cangjie.cfir.resolve.providers.macro.IdentityMacroStableSplicer
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallForestBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroAppliedDeclarationReplacement
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroAppliedExpressionReplacement
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionCycle
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroForestEvaluator
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentInput
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentPayload
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentSourceMap
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentSourceMapping
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroTokenSourceOrigin
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroReplacementSlotType
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionCacheKey
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroBuiltinRegistries
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadToken
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroPayloadChannel
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroReplaceSlot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolution
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroStableSplicer
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroStableSpliceResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.recoveredBuiltinDeclarationBranchTokens
import org.cangnova.cangjie.cfir.resolve.providers.macro.registerConstructionSurfaceUsage
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadata
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.abortAnnotationMetadata
import org.cangnova.cangjie.cfir.resolve.providers.macro.materializePrimaryConstructorParameterFields
import org.cangnova.cangjie.cfir.resolve.providers.macro.finalizeImplicitConstructorDeclarations
import org.cangnova.cangjie.cfir.symbols.CfirErrorFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.parallelMacroExpansion
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroDiagnosticSeverity
import org.cangnova.cangjie.macro.MacroExpansionFailureKind
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.process.LspMacroServerMacroExecutor
import org.cangnova.cangjie.macro.MacroLibraryLoadFailure
import org.cangnova.cangjie.macro.MacroLibraryLoadFailureKind
import org.cangnova.cangjie.macro.MacroLibraryLoadResult
import org.cangnova.cangjie.macro.MacroTokenKind
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.macro.protocol.MacroMsgCodec
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.presentableDescription
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.text
import java.util.IdentityHashMap

/**
 * 为给定 CFIR session 创建宏执行器的工厂。
 */
fun interface MacroExecutorFactory {
    /**
     * 创建与 [session] 绑定的宏执行器。
     */
    fun create(session: CfirSession): MacroExecutor
}

/**
 * 为给定 CFIR session 创建宏片段 parser 的工厂。
 */
fun interface MacroFragmentParserFactory {
    /**
     * 创建与 [session] 绑定的宏片段 parser。
     */
    fun create(session: CfirSession): MacroFragmentParser
}

/**
 * 为宏构造中必须读取 IntelliJ PSI 的短阶段提供调度边界。
 *
 * 类型实体已下沉到 `macro:macro-common`，使 `analysis:macro-expansion` 能声明读访问边界
 * 而不依赖 `compiler:frontend`；这里保留别名以维持前端与既有调用方的导入路径。
 */
typealias MacroConstructionReadAccess = org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess

/**
 * 前端宏展开配置键。
 */
object FrontendMacroConfigurationKeys {
    @JvmField
    /**
     * 宏执行器工厂配置键。
     */
    val MACRO_EXECUTOR_FACTORY = CompilerConfigurationKey.create<MacroExecutorFactory>("MACRO_EXECUTOR_FACTORY")

    @JvmField
    /**
     * 单次宏展开允许的最大迭代次数配置键。
     */
    val MACRO_EXPAND_MAX_ITERATIONS = CompilerConfigurationKey.create<Int>("MACRO_EXPAND_MAX_ITERATIONS")

    @JvmField
    /**
     * 宏片段 parser 工厂配置键。
     */
    val MACRO_FRAGMENT_PARSER_FACTORY =
        CompilerConfigurationKey.create<MacroFragmentParserFactory>("MACRO_FRAGMENT_PARSER_FACTORY")

    @JvmField
    /**
     * 宏构造短 PSI 阶段的读访问调度器配置键。
     */
    val MACRO_CONSTRUCTION_READ_ACCESS =
        MacroConstructionReadAccess.CONFIGURATION_KEY

    @JvmField
    /**
     * 外部已编译宏 artifact 包配置键。
     */
    val MACRO_ARTIFACT_PACKAGES =
        CompilerConfigurationKey.create<List<MacroArtifactPackage>>("MACRO_ARTIFACT_PACKAGES")

    @JvmField
    /**
     * 宏执行器 ABI 版本配置键。
     */
    val MACRO_EXECUTOR_ABI_VERSION =
        CompilerConfigurationKey.create<String>("MACRO_EXECUTOR_ABI_VERSION")

    @JvmField
    /**
     * 可按需编译的宏源码包请求配置键。
     */
    val MACRO_SOURCE_PACKAGE_COMPILATION_REQUESTS =
        CompilerConfigurationKey.create<List<MacroSourcePackageCompilationRequest>>(
            "MACRO_SOURCE_PACKAGE_COMPILATION_REQUESTS",
        )

    @JvmField
    /**
     * 宏源码包编译调度器配置键。
     */
    val MACRO_PACKAGE_COMPILATION_ORCHESTRATOR =
        CompilerConfigurationKey.create<MacroPackageCompilationOrchestrator>("MACRO_PACKAGE_COMPILATION_ORCHESTRATOR")

    @JvmField
    /**
     * 宏编译缓存上下文配置键。
     */
    val MACRO_COMPILATION_CACHE_CONTEXT =
        CompilerConfigurationKey.create<MacroCompilationCacheContext>("MACRO_COMPILATION_CACHE_CONTEXT")

    @JvmField
    /**
     * 后台自动编译宏源码包开关配置键。
     */
    val MACRO_BACKGROUND_AUTO_COMPILATION_ENABLED =
        CompilerConfigurationKey.create<Boolean>("MACRO_BACKGROUND_AUTO_COMPILATION_ENABLED")

    @JvmField
    /**
     * 宏展开 demand 触发自动编译开关配置键。
     */
    val MACRO_EXPANSION_DEMAND_AUTO_COMPILATION_ENABLED =
        CompilerConfigurationKey.create<Boolean>("MACRO_EXPANSION_DEMAND_AUTO_COMPILATION_ENABLED")

    @JvmField
    /**
     * 宏 SDK 根目录配置键。
     */
    val MACRO_SDK_HOME = CompilerConfigurationKey.create<String>("MACRO_SDK_HOME")

    @JvmField
    /**
     * 编译宏源码包时传递给外部 cjc 进程的 SDK 环境变量。
     */
    val MACRO_COMPILER_ENVIRONMENT = CompilerConfigurationKey.create<Map<String, String>>("MACRO_COMPILER_ENVIRONMENT")

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

/**
 * 当前配置中的宏执行器工厂。
 */
var CompilerConfiguration.macroExecutorFactory: MacroExecutorFactory?
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_FACTORY)
    set(value) {
        putIfNotNull(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_FACTORY, value)
    }

/**
 * 当前配置中的宏展开最大迭代次数。
 */
var CompilerConfiguration.macroExpandMaxIterations: Int
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXPAND_MAX_ITERATIONS, 16)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_EXPAND_MAX_ITERATIONS, value)
    }

/**
 * 当前配置中的宏片段 parser 工厂。
 */
var CompilerConfiguration.macroFragmentParserFactory: MacroFragmentParserFactory?
    get() = get(FrontendMacroConfigurationKeys.MACRO_FRAGMENT_PARSER_FACTORY)
    set(value) {
        putIfNotNull(FrontendMacroConfigurationKeys.MACRO_FRAGMENT_PARSER_FACTORY, value)
    }

/**
 * 当前宏构造短 PSI 阶段的访问调度器；CLI 与非 IntelliJ 调用默认直接运行。
 */
var CompilerConfiguration.macroConstructionReadAccess: MacroConstructionReadAccess
    get() = get(FrontendMacroConfigurationKeys.MACRO_CONSTRUCTION_READ_ACCESS)
        ?: org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess.Direct
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_CONSTRUCTION_READ_ACCESS, value)
    }

/**
 * 当前配置中显式提供的已编译宏 artifact 包。
 */
var CompilerConfiguration.macroArtifactPackages: List<MacroArtifactPackage>
    get() = get(FrontendMacroConfigurationKeys.MACRO_ARTIFACT_PACKAGES, emptyList())
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_ARTIFACT_PACKAGES, value)
    }

/**
 * 当前前端宏构造模式。
 */
var CompilerConfiguration.macroConstructionMode: MacroConstructionService.Mode
    get() = get(FrontendMacroConfigurationKeys.MACRO_CONSTRUCTION_MODE, MacroConstructionService.Mode.STRICT)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_CONSTRUCTION_MODE, value)
    }

/**
 * 当前宏执行器 ABI 版本。
 */
var CompilerConfiguration.macroExecutorAbiVersion: String
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_ABI_VERSION, MacroExecutor.DEFAULT_ABI_VERSION)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_EXECUTOR_ABI_VERSION, value)
    }

/**
 * 当前配置中可按需编译的宏源码包请求列表。
 */
var CompilerConfiguration.macroSourcePackageCompilationRequests: List<MacroSourcePackageCompilationRequest>
    get() = get(FrontendMacroConfigurationKeys.MACRO_SOURCE_PACKAGE_COMPILATION_REQUESTS, emptyList())
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_SOURCE_PACKAGE_COMPILATION_REQUESTS, value)
    }

/**
 * 当前宏源码包编译调度器。
 */
var CompilerConfiguration.macroPackageCompilationOrchestrator: MacroPackageCompilationOrchestrator?
    get() = get(FrontendMacroConfigurationKeys.MACRO_PACKAGE_COMPILATION_ORCHESTRATOR)
    set(value) {
        putIfNotNull(FrontendMacroConfigurationKeys.MACRO_PACKAGE_COMPILATION_ORCHESTRATOR, value)
    }

/**
 * 当前宏编译缓存上下文。
 */
var CompilerConfiguration.macroCompilationCacheContext: MacroCompilationCacheContext
    get() = get(FrontendMacroConfigurationKeys.MACRO_COMPILATION_CACHE_CONTEXT)
        ?: MacroCompilationCacheContext(
            targetPlatform = targetPlatform ?: CangJiePlatforms.defaultCangJiePlatform,
        )
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_COMPILATION_CACHE_CONTEXT, value)
    }

/**
 * 是否允许后台自动编译宏源码包。
 */
var CompilerConfiguration.macroBackgroundAutoCompilationEnabled: Boolean
    get() = get(FrontendMacroConfigurationKeys.MACRO_BACKGROUND_AUTO_COMPILATION_ENABLED, false)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_BACKGROUND_AUTO_COMPILATION_ENABLED, value)
    }

/**
 * 是否允许宏展开 demand 触发宏源码包按需编译。
 */
var CompilerConfiguration.macroExpansionDemandAutoCompilationEnabled: Boolean
    get() = get(FrontendMacroConfigurationKeys.MACRO_EXPANSION_DEMAND_AUTO_COMPILATION_ENABLED, true)
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_EXPANSION_DEMAND_AUTO_COMPILATION_ENABLED, value)
    }

/**
 * 编译调用显式指定的宏 SDK 根目录。环境选择由 invocation 边界处理，不在 getter 中猜测安装。
 */
var CompilerConfiguration.macroSdkHome: String?
    get() = get(FrontendMacroConfigurationKeys.MACRO_SDK_HOME)
    set(value) {
        if (value == null) remove(FrontendMacroConfigurationKeys.MACRO_SDK_HOME)
        else put(FrontendMacroConfigurationKeys.MACRO_SDK_HOME, value)
    }

/**
 * 当前配置中外部宏编译器使用的环境变量。
 */
var CompilerConfiguration.macroCompilerEnvironment: Map<String, String>
    get() = get(FrontendMacroConfigurationKeys.MACRO_COMPILER_ENVIRONMENT, emptyMap())
    set(value) {
        put(FrontendMacroConfigurationKeys.MACRO_COMPILER_ENVIRONMENT, value.toMap())
    }

/**
 * Compiler frontend 的 macro construction 实现。
 *
 * 该实现严格遵守 baseline 第 2 节：text patch / full-file rebuild 不参与语义路径。
 * 在 stable splice 接入前，含 macro surface 的源码只能产 construction diagnostic
 * 并阻止 ordinary resolve；无宏源码走 identity [MacroConstructionResult.Success]。
 */
class FrontendMacroConstructionService(
    /**
     * 当前宏构造使用的编译配置。
     */
    private val configuration: CompilerConfiguration,
    /**
     * 将 executor/parser 返回的片段稳定拼回 CFIR 的 splicer。
     */
    private val stableSplicer: MacroStableSplicer = CfirExpressionMacroStableSplicer,
    /** 与 artifact 准备共享的一次调用快照，不能在每个宏位点重新选择 SDK。 */
    private val sdkSelection: MacroSdkSelection = MacroSdkSelector.select(configuration.macroSdkHome),
) : MacroConstructionService {
    private fun createExecutor(session: CfirSession): MacroExecutor? {
        val factory = configuration.macroExecutorFactory
        if (factory != null) return factory.create(session)
        val sdk = sdkSelection as? MacroSdkSelection.Selected ?: return null
        return LspMacroServerMacroExecutor(
            sdk.executorExecutable.toString(),
            sdkHome = sdk.home.toString(),
            processEnvironment = sdk.processEnvironment(),
            enableParallel = configuration.parallelMacroExpansion,
        )
    }

    /**
     * 对 pre-macro raw CFIR 执行宏构造并返回最终构造结果。
     *
     * 本方法由 [MacroConstructionService.expand] 模板方法调用，构造耗时已在其外侧统计。
     */
    override fun doExpand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        classification: MacroDemandClassification,
        mode: MacroConstructionService.Mode,
        preConstructionDiagnostics: List<MacroConstructionDiagnostic>,
    ): MacroConstructionResult {
        try {
            val session = pre.session
            val registry = MacroExpansionRegistry()
            val rawFiles = pre.files.map { it.cfirFile }
            var builtDegradedPlaceholders = false

            // Baseline 第 9 节："session/analysis 长生命周期 registry"。
            // 把所有 surface 按文件注册到 registry，供 diagnostic / LSP 和文件级 checker
            // 反查原 macro 位点与 original macro forest。
            for (preFile in pre.files) {
                registry.registerFileSurfaces(preFile.cfirFile, preFile.surfaces)
            }
            registry.registerConstructionSurfaceUsage(pre, classification)
            registry.addAll(preConstructionDiagnostics)

            // baseline 第 12 节 Batch 5："alias conflict / macro package / ..."。
            reportAliasConflicts(context, registry)

            val expandedFiles = expandMacroSurfaces(pre, context, classification, registry) ?: rawFiles
            if (mode == MacroConstructionService.Mode.DEGRADED &&
                pre.allSurfaces.isNotEmpty() &&
                registry.hasErrors &&
                registry.parsingFailures.isEmpty() &&
                registry.diagnostics.all(::isDegradableDiagnostic)
            ) {
                replaceSurfaceOnlyDegradedPlaceholders(pre, expandedFiles, registry)
                builtDegradedPlaceholders = true
            }

            require(expandedFiles.size == pre.files.size) { "Construction must retain per-file field ownership." }
            pre.files.forEachIndexed { index, preFile ->
                finalizeImplicitConstructorDeclarations(expandedFiles[index], preFile.annotationMetadata)
                materializePrimaryConstructorParameterFields(expandedFiles[index], preFile.annotationMetadata)
            }

            // baseline 第 11 节 cache key：splice 完成后逐文件计算 13 维 key，
            // 写入 registry 供上游 IDE / build cache 失效判定。
            registerCacheKeys(pre, context, expandedFiles, registry)

            // baseline 第 10 节："session/analysis 长生命周期 registry"挂到 session
            // 上，供 ordinary checker / IDE / LSP 通过 `session.macroExpansionRegistry` 读取。
            session.register(MacroExpansionRegistry::class, registry)

            if (registry.hasErrors) {
                if (mode == MacroConstructionService.Mode.DEGRADED &&
                    builtDegradedPlaceholders &&
                    registry.parsingFailures.isEmpty() &&
                    registry.diagnostics.all(::isDegradableDiagnostic)
                ) {
                    return MacroConstructionService.degradedOf(pre, expandedFiles, registry)
                }
                return MacroConstructionService.failedOf(pre, registry)
            }

            return MacroConstructionService.successOf(pre, expandedFiles, registry)
        } catch (failure: Throwable) {
            pre.abortAnnotationMetadata()
            throw failure
        }
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
        val expansionDecisionsByFile = pre.files.mapNotNull { preFile ->
            val decisions = preFile.surfaces.mapNotNull decisionForSurface@{ surface ->
                val decision =
                    classification.finalDecisions.firstOrNull { it.surface === surface }
                        ?: return@decisionForSurface null
                if (!decision.localConstruction) return@decisionForSurface null
                if ((preFile.isMacroPackage && surface !is MacroSurfaceExpr) || surface.isMacroDefinitionSignatureSurface()) {
                    return@decisionForSurface null
                }
                decision
            }
            decisions.takeIf { it.isNotEmpty() }?.let { preFile to it }
        }
        val expansionDecisions = expansionDecisionsByFile.flatMap { (_, decisions) -> decisions }
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
        val executor = if (expansionDecisions.any(FinalMacroSurfaceDecision::executorRequired)) {
            createExecutor(pre.session)
        } else {
            null
        }
        val slots = mutableListOf<MacroReplaceSlot>()
        val evaluator = MacroForestEvaluator(configuration.macroExpandMaxIterations)

        // sourceRange 是文件内 offset；不同虚拟文件的范围不可直接比较。
        // 每个 PreMacroCfirFile 独立建 forest，保证 parent/child 关系只存在于同一源码文件。
        executor.use {
            for ((preFile, fileDecisions) in expansionDecisionsByFile) {
                val forest = MacroCallForestBuilder.build(fileDecisions.map { it.surface })
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
                        if (decision.resolution is MacroResolution.BuiltinNonMacro) {
                            surface.recoveredBuiltinDeclarationBranchTokens()?.let { branch ->
                                // 声明与其语法诊断已归属原始文件；只把保留的分支交给父宏。
                                val visibleTokens = if (branch.isEmpty()) refreshedTokens.inputTokens else
                                    branch.replaceChildMacroRanges(childResults, MacroPayloadChannel.INPUT)
                                registry.registerGeneratedDisplayText(surface.surfaceId, visibleTokens.joinToString("") { it.text })
                                return@evaluate visibleTokens
                            }
                        }
                        val expandedTokens = expandResolvedSurface(
                            pre = pre,
                            surface = surface,
                            decision = decision,
                            node = node,
                            childResults = childResults,
                            refreshedTokens = refreshedTokens,
                            executor = executor,
                            registry = registry,
                            preFile = preFile,
                        ) ?: return@evaluate null

                        val inherited = inheritedAnnotationsBySurface(preFile, decision, classification, node)
                        val fragment = configuration.macroConstructionReadAccess.compute {
                            parseAndDesugarFragment(
                                surface = surface,
                                decision = decision,
                                annotationSnapshot = decision.annotationCarrier?.let(preFile.annotationMetadata::snapshot),
                                annotationMetadata = preFile.annotationMetadata,
                                node = node,
                                parser = parser,
                                tokens = expandedTokens,
                                inheritedAnnotationSnapshots = inherited.values.toList(),
                                registry = registry,
                            )
                        } ?: return@evaluate null
                        when (fragment) {
                            is MacroFragmentResult.Success -> fragment.payload.roots.forEach { root ->
                                val generated = when (root) {
                                    is MacroFragmentPayload.Root.Declaration -> root.declaration
                                    is MacroFragmentPayload.Root.Expression -> root.expression
                                    is MacroFragmentPayload.Root.Parameter -> root.parameter
                                }
                                registry.registerGeneratedCfirElement(
                                    generated,
                                    surface.surfaceId,
                                    fragment.sourceMapping,
                                    fragmentSourceMap = fragment.sourceMap,
                                )
                            }
                            is MacroFragmentResult.CustomAnnotation -> registry.registerGeneratedCfirElement(
                                fragment.payload,
                                surface.surfaceId,
                                fragment.sourceMapping,
                                fragmentSourceMap = fragment.sourceMap,
                                registerRootSource = false,
                                fallbackToSurfaceOnUnmappedSource = false,
                            )
                            else -> Unit
                        }
                        val parentVisibleTokens = fragment.parentVisibleTokens(refreshedTokens)
                        if (decision.resolution is MacroResolution.BuiltinNonMacro) {
                            registry.registerGeneratedDisplayText(surface.surfaceId, parentVisibleTokens.joinToString("") { it.text })
                        }
                        slots += MacroReplaceSlot(
                            handle = surface.replaceHandle,
                            origin = surface,
                            fragment = fragment,
                            annotationMetadata = preFile.annotationMetadata,
                            embeddedSurfaceIds = if (fragment is MacroFragmentResult.Success)
                                node.descendantSurfaceIds() + inherited.keys else emptySet(),
                        )
                        parentVisibleTokens
                    },
                    onCycle = { cycle -> reportMacroCycle(cycle, registry) },
                )
            }
        }


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

        val spliceResult = configuration.macroConstructionReadAccess.compute {
            stableSplicer.applySlices(pre.files.map { it.cfirFile }, slots)
        }
        spliceResult.declarationReplacements.forEach(registry::registerAppliedDeclarationReplacement)
        spliceResult.expressionReplacements.forEach(registry::registerAppliedExpressionReplacement)
        registry.stageParsingReports(slots)
        return spliceResult.files
    }

    /**
     * 根据最终宏解析决策展开单个宏表面。
     */
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

            is MacroResolution.BuiltinNonMacro -> tokensForBuiltinNonMacro(surface, childResults, registry)
            is MacroResolution.BuiltinAnnotation -> error(
                "Builtin annotation ${resolution.name.asString()} must not enter macro expansion.",
            )
            is MacroResolution.CustomAnnotation -> {
                val snapshot = decision.annotationCarrier
                    ?.let { checkNotNull(preFile).annotationMetadata.snapshot(it) }
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

    /**
     * 计算内建宏的 token 输出。
     */
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

    /**
     * 调用外部宏执行器并把执行结果转换为宏表面 token。
     */
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
                message = "Macro executor is unavailable for `@${entry.name.asString()}`." +
                    if (configuration.macroExecutorFactory == null) " SDK selection: ${sdkSelection.identity}" else "",
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
        val callInput = configuration.macroConstructionReadAccess.compute {
            surface.toMacroCallInfo(entry, node.parentNames, refreshedTokens, preFile)
        }
        val result = executor.execute(listOf(callInput.call)).singleOrNull()
            ?: MacroExpansionResult.Failure(
                message = "Macro executor returned no result for `${entry.name.asString()}`.",
                kind = MacroExpansionFailureKind.PROTOCOL_ERROR,
            )

        return when (result) {
            is MacroExpansionResult.Success -> {
                recordMacroDiagReports(surface, result.diagnostics, registry)
                result.tokens.toMacroSurfaceTokens(callInput.sources)
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

    /**
     * 将 executor diag report 写入宏构造 registry。
     */
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

    /**
     * 将动态库加载失败记录为宏构造诊断。
     */
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

    /**
     * 将宏解析 kind mismatch 映射为构造诊断类型和消息。
     */
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

    /**
     * 解析宏输出 token 并按需对内建非宏表面执行脱糖。
     */
    private fun parseAndDesugarFragment(
        surface: MacroSurface,
        decision: FinalMacroSurfaceDecision,
        annotationSnapshot: org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot?,
        annotationMetadata: CfirAnnotationMetadataBuilder,
        node: MacroCallNode,
        parser: MacroFragmentParser,
        tokens: List<MacroSurfaceToken>,
        inheritedAnnotationSnapshots: List<org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot>,
        registry: MacroExpansionRegistry,
    ): MacroFragmentResult? {
        registry.beginParsingInvocation(node)
        val parsed = parser.parse(
            MacroFragmentInput(
                node = node,
                tokens = tokens,
                decision = decision,
                annotationSnapshot = annotationSnapshot,
                fileFacts = registry.parsingFactsForSurface(surface.surfaceId),
                annotationMetadata = annotationMetadata,
                inheritedAnnotationSnapshots = inheritedAnnotationSnapshots,
                inheritedAnnotationTokens = inheritedAnnotationSnapshots.flatMap {
                    it.tokens + MacroSurfaceToken("\n", 0, 1, kindName = "WHITE_SPACE", isTrivia = true)
                },
                sourceMapping = MacroFragmentSourceMapping(registry::registeredSourceOrigin),
            )
        )
        parsed.parsedFileFacts?.let { registry.acceptParsingFacts(surface.surfaceId, it) }
        val shapeFailure = (parsed as? MacroFragmentResult.Success)?.replacementShapeFailure(decision.slotType)
        if (shapeFailure != null) {
            reportError(
                registry = registry,
                message = shapeFailure.message,
                kind = shapeFailure.diagnosticKind,
                originSurfaceId = surface.surfaceId,
            )
            return null
        }
        return when (parsed) {
            is MacroFragmentResult.SyntaxFailure -> {
                registry.addParsingFailure(parsed)
                null
            }
            is MacroFragmentResult.Failure -> {
                reportError(
                    registry = registry,
                    message = parsed.reason,
                    kind = MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED,
                    originSurfaceId = surface.surfaceId,
                )
                null
            }

            is MacroFragmentResult.Success,
            is MacroFragmentResult.CustomAnnotation -> parsed
        }
    }

    /**
     * 原 annotations 是独立解析输入，按原槽位顺序交给首个输出声明的正常语法解析。
     * 只有冻结决议确认的普通注解进入该通道，宏本身仍由宏森林求值。
     */
    private fun inheritedAnnotationsBySurface(
        preFile: PreMacroCfirFile,
        decision: FinalMacroSurfaceDecision,
        classification: MacroDemandClassification,
        node: MacroCallNode,
    ): Map<Long, org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot> {
        if (decision.parserMode != MacroFragmentParser.Mode.DECLARATION) return emptyMap()
        val current = decision.annotationCarrier ?: return emptyMap()
        val metadata = preFile.annotationMetadata
        val enclosingMacroIndex = generateSequence(node.parent) { it.parent }
            .mapNotNull { parent ->
                classification.finalDecisions.firstOrNull { it.surface === parent.surface }
            }
            .firstOrNull { parent ->
                val carrier = parent.annotationCarrier
                carrier != null && carrier.owner === current.owner &&
                    parent.parserMode == MacroFragmentParser.Mode.DECLARATION &&
                    parent.resolution !is MacroResolution.CustomAnnotation &&
                    parent.resolution !is MacroResolution.BuiltinAnnotation
            }?.annotationCarrier?.annotationIndex ?: -1
        return classification.finalDecisions.asSequence()
            .filter { candidate ->
                val carrier = candidate.annotationCarrier
                carrier != null && carrier.owner === current.owner &&
                    carrier.annotationIndex > enclosingMacroIndex && carrier.annotationIndex < current.annotationIndex &&
                    (candidate.resolution is MacroResolution.BuiltinAnnotation || candidate.resolution is MacroResolution.CustomAnnotation)
            }
            .sortedBy { checkNotNull(it.annotationCarrier).annotationIndex }
            .associate { candidate ->
                candidate.surface.surfaceId to checkNotNull(metadata.snapshot(checkNotNull(candidate.annotationCarrier)))
            }
    }

    /**
     * 内建非宏（当前为 IfAvailable）不执行宏函数；重解析仍须接收完整原调用，
     * 并把真实 input/attr 子宏结果刷新回其源码位置。
     */
    private fun tokensForBuiltinNonMacro(
        surface: MacroSurface,
        childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
        registry: MacroExpansionRegistry,
    ): List<MacroSurfaceToken>? {
        val invocation = surface.capturedRawSyntax ?: run {
            reportError(
                registry = registry,
                message = "Builtin non-macro `${surface.qualifiedName?.asString().orEmpty()}` has no captured invocation syntax.",
                kind = MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED,
                originSurfaceId = surface.surfaceId,
            )
            return null
        }
        val invocationStart = surface.sourceRange?.startOffset ?: 0
        val tokens = MacroPayloadTokenizer.tokenize(invocation, invocationStart).map { token ->
            MacroSurfaceToken(
                text = token.text,
                startOffset = token.startOffset,
                endOffset = token.endOffset,
                kindName = token.kindName,
                sourceRange = surface.sourceRange,
                isSourceToken = true,
            )
        }
        return tokens
            .replaceChildMacroRanges(childResults, MacroPayloadChannel.INPUT)
            .replaceChildMacroRanges(childResults, MacroPayloadChannel.ATTR)
    }

    /**
     * 将宏调用环报告为每个环节点上的构造诊断。
     */
    private fun reportMacroCycle(
        cycle: MacroExpansionCycle,
        registry: MacroExpansionRegistry,
    ) {
        for (node in cycle.nodes.distinctBy { it.surface.surfaceId }) {
            val name = node.surface.qualifiedName?.shortName()
            reportError(
                registry = registry,
                message = "Macro expansion cycle detected for `${name?.asString().orEmpty()}`.",
                kind = MacroConstructionDiagnostic.Kind.MACRO_CYCLE,
                originSurfaceId = node.surface.surfaceId,
            )
        }
    }

    /**
     * 报告同包宏定义不能被当前包宏调用直接解析的错误。
     */
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

    /**
     * 判断诊断是否允许在 DEGRADED 模式下降级为占位符。
     */
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

    /**
     * 判断宏表面是否已经存在阻断构造的 artifact/orchestration 诊断。
     */
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
     * 未展开的声明宏保留原始 CFIR carrier；参数和表达式宏继续替换为类型化错误节点。
     * registry 记录仍需恢复的错误节点与原始 macro site 的映射。
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
            if (surface is MacroSurfaceDecl) continue
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
        require(files.size == pre.files.size) { "Placeholder replacement must retain per-file metadata ownership." }
        for ((index, file) in files.withIndex()) {
            replaceDegradedDeclarationPlaceholders(file.declarations, declarationPlaceholders, parameterPlaceholders,
                pre.files[index].annotationMetadata)
            replaceDegradedExpressionPlaceholders(file, expressionPlaceholders)
        }
    }

    @Suppress("UNCHECKED_CAST")
    /**
     * 替换声明列表中的降级占位符声明，并递归处理嵌套声明。
     */
    private fun replaceDegradedDeclarationPlaceholders(
        declarations: List<CfirDeclaration>,
        declarationPlaceholders: IdentityHashMap<CfirDeclaration, CfirDeclaration>,
        parameterPlaceholders: IdentityHashMap<CfirValueParameter, CfirValueParameter>,
        annotationMetadata: CfirAnnotationMetadataBuilder,
    ) {
        val mutableDeclarations = declarations as? MutableList<CfirDeclaration> ?: return
        val iterator = mutableDeclarations.listIterator()
        while (iterator.hasNext()) {
            val declaration = iterator.next()
            val replacement = declarationPlaceholders.remove(declaration)
            val current = replacement ?: declaration
            if (replacement != null) {
                iterator.set(replacement)
                annotationMetadata.transferOwner(declaration, replacement)
            }
            replaceDegradedParameterPlaceholders(current, parameterPlaceholders, annotationMetadata)
            when (current) {
                is CfirClassLikeDeclaration ->
                    replaceDegradedDeclarationPlaceholders(
                        current.declarations,
                        declarationPlaceholders,
                        parameterPlaceholders,
                        annotationMetadata,
                    )

                is CfirExtend ->
                    replaceDegradedDeclarationPlaceholders(
                        current.declarations,
                        declarationPlaceholders,
                        parameterPlaceholders,
                        annotationMetadata,
                    )

                else -> Unit
            }
        }
    }

    /**
     * 替换函数声明中的降级占位符参数。
     */
    private fun replaceDegradedParameterPlaceholders(
        declaration: CfirDeclaration,
        parameterPlaceholders: IdentityHashMap<CfirValueParameter, CfirValueParameter>,
        annotationMetadata: CfirAnnotationMetadataBuilder,
    ) {
        if (declaration !is CfirFunction || declaration.valueParameters.isEmpty()) return
        val originalParameters = declaration.valueParameters.toList()
        val newParameters = originalParameters.map { parameter ->
            parameterPlaceholders.remove(parameter) ?: parameter
        }
        declaration.replaceValueParameters(newParameters)
        originalParameters.zip(newParameters).forEach { (original, replacement) ->
            if (original !== replacement) annotationMetadata.transferOwner(original, replacement)
        }
    }

    /**
     * 为声明宏构造失败生成错误声明占位符。
     */
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
                attributes = declaration.attributes.copy()
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
            attributes = declaration.attributes.copy()
            reason = diagnostic.reason
        }
    }

    /**
     * 替换文件中的降级表达式占位符。
     */
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

    /**
     * 为表达式宏构造失败生成错误表达式占位符。
     */
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

    /**
     * 为参数宏构造失败生成错误参数占位符。
     */
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
            attributes = parameter.attributes.copy()
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
            defaultValue = parameter.defaultValue ?: buildErrorExpression {
                this.source = source
                this.diagnostic = diagnostic
            }
        }.also { placeholder ->
            // A recovery expression represents the failed macro, not a source default assignment.
            if (parameter.defaultValue == null) {
                placeholder.defaultValueMetadata = CfirParameterDefaultValueMetadata(hasInitial = false, function = null)
            }
        }
    }

    /**
     * 返回宏表面原始源位置，缺失时使用调用方提供的 fallback。
     */
    private fun MacroSurface.macroOriginSource(
        fallback: org.cangnova.cangjie.source.CjSourceElement?,
    ): org.cangnova.cangjie.source.CjSourceElement? {
        return sourceRange?.source ?: fallback
    }

    /**
     * 为降级占位符创建简单诊断。
     */
    private fun macroPlaceholderDiagnostic(surface: MacroSurface): ConeSimpleDiagnostic {
        return ConeSimpleDiagnostic(
            "Macro call `${surface.capturedRawSyntax ?: surface.qualifiedName?.asString().orEmpty()}` " +
                    "was not expanded during macro construction.",
        )
    }

    /**
     * 向 registry 添加错误级宏构造诊断。
     */
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
     * -  6. sdkSignature              ← 语言设置与本次选中的 SDK 身份
     * -  7. macroDependencySignature  ← 非源包 [MacroDefinitionEntry]（lib/shared/artifact/builtin）+ artifact/dylib/BCHIR hash
     * -  8. compilerOptionsHash       ← `useLightTree` + `moduleName` + iteration limit + compiler/debug/parallel/target/env
     * -  9. executorAbi               ← 已冻结的 executor ABI 配置
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
        val sdkSignature = org.cangnova.cangjie.utils.StableHash.sha256Of(
            configuration.languageVersionSettings.toString(),
            sdkSelection.identity,
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
        // Cache identity is configuration metadata; creating an executor here can start a process
        // or invoke a user factory even when the file contains no executable macro calls.
        val executorAbi = configuration.macroExecutorAbiVersion
        val maxIterations = configuration.macroExpandMaxIterations
        require(expandedFiles.size == pre.files.size) { "Construction must retain per-file result ownership." }

        for ((index, preFile) in pre.files.withIndex()) {
            val cfirFile = preFile.cfirFile
            val expanded = expandedFiles[index]
            val identity = cfirFile.sourceFile?.path ?: cfirFile.name
            val key = MacroExpansionCacheKey(
                sourceContentHash = hashSourceContent(cfirFile),
                fileIdentity = identity,
                macroSurfaceRangesHash = hashSurfaces(preFile.surfaces, preFile.annotationMetadata),
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
                    "result=${configuration.macroConstructionReadAccess.compute { hashResultSnapshot(expanded) }}",
                ),
            )
            registry.registerCacheKey(identity, key)
        }
    }

    /**
     * 计算源文件内容 hash。
     */
    private fun hashSourceContent(file: CfirFile): String {
        val text = runCatching {
            file.sourceFile?.getContentsAsStream()?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull() ?: file.name
        return org.cangnova.cangjie.utils.StableHash.sha256(text)
    }

    /**
     * 计算宏表面列表及注解槽快照 hash。
     */
    private fun hashSurfaces(
        surfaces: List<MacroSurface>,
        annotationMetadataRegistry: CfirAnnotationMetadata,
    ): String {
        if (surfaces.isEmpty()) return "surfaces:empty"
        val parts = surfaces.map { surface ->
            val range = surface.sourceRange
            val snapshot = surface.replaceHandle.annotationCarrier
                ?.let { annotationMetadataRegistry.snapshot(it) }
            "${surface.qualifiedName?.asString().orEmpty()}|" +
                    "${range?.startOffset ?: -1}|${range?.endOffset ?: -1}|${surface.kind}|" +
                    "qualified=${surface.isQualifiedName}|module=${surface.scopeContext.sourceModuleName}|" +
                    "slot=${snapshot?.stableCacheText().orEmpty()}"
        }
        return org.cangnova.cangjie.utils.StableHash.sha256Of(parts)
    }

    /**
     * 计算文件 import 与宏 import 绑定信息 hash。
     */
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

    /**
     * 计算当前宏依赖集合签名。
     */
    private fun computeMacroDependencySignature(context: MacroResolutionContext): String {
        val sourceTargets = context.symbolIndex.sources.map { it.fqName.asString() }
        val foreignTargets = context.symbolIndex.foreigns.map { entry ->
            "${entry.fqName.asString()}|exec=${entry.executableFqName.asString()}|${entry.source}|lib=${entry.libPath.orEmpty()}|abi=${entry.executorAbi.orEmpty()}|" +
                    "artifact=${entry.artifactSignature.orEmpty()}|cjo=${entry.cjoHash.orEmpty()}|" +
                    "dylib=${entry.dynamicLibHash.orEmpty()}|deps=${entry.dependenciesArtifactHash.orEmpty()}|" +
                    "resolver=${entry.resolverAlgorithmVersion ?: -1}"
        }
        return org.cangnova.cangjie.utils.StableHash.sha256Of(sourceTargets + foreignTargets)
    }

    /**
     * 计算展开后 CFIR 文件结构快照 hash。
     */
    private fun hashResultSnapshot(file: CfirFile): String {
        if (file.declarations.isEmpty()) return "result:empty"
        val parts = buildList {
            file.declarations.forEach { declaration ->
                collectDeclarationSnapshot(declaration, this)
            }
        }
        return org.cangnova.cangjie.utils.StableHash.sha256Of(parts)
    }

    /**
     * 收集声明及其嵌套结构的稳定快照片段。
     */
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

    /**
     * 返回声明用于 cache key 的稳定名称。
     */
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

    /**
     * 返回注解列表用于 cache key 的稳定文本。
     */
    private fun List<CfirAnnotation>.stableAnnotationSnapshot(): String =
        joinToString(separator = ";") { annotation ->
            when (annotation) {
                is CfirAnnotationCall -> {
                    val args = annotation.argumentList.arguments.joinToString(separator = ",") { argument ->
                        argument.source?.text?.toString().orEmpty()
                    }
                    "${annotation.typeRef.source?.text?.toString().orEmpty()}[$args]|" +
                        "module=${annotation.sourceModuleName}|forced=${annotation.forcedCustom}|" +
                        "compileTimeVisible=${annotation.isCompileTimeVisible}"
                }

                else -> annotation.source?.text?.toString().orEmpty()
            }
        }

    /**
     * 返回注解替换槽快照用于 cache key 的稳定文本。
     */
    private fun org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot.stableCacheText(): String =
        listOf(
            "index=$annotationIndex",
            "raw=$rawSyntax",
            "forced=$forcedCustom",
            "compileTimeVisible=$isCompileTimeVisible",
            "module=${originalAnnotation.sourceModuleName}",
            "fqn=${qualifiedName?.asString().orEmpty()}",
            "args=${argumentText.orEmpty()}",
            "tokens=${tokens.joinToString(separator = "") { it.text }}",
            "callSite=$callSite",
        ).joinToString("|")

    /**
     * 将宏 executor 诊断严重级别映射为构造诊断严重级别。
     */
    private fun Int.toConstructionSeverity(): MacroConstructionDiagnostic.Severity {
        return when (this) {
            MacroDiagnosticSeverity.INFO -> MacroConstructionDiagnostic.Severity.INFO
            MacroDiagnosticSeverity.WARNING -> MacroConstructionDiagnostic.Severity.WARNING
            MacroDiagnosticSeverity.ERROR -> MacroConstructionDiagnostic.Severity.ERROR
            else -> MacroConstructionDiagnostic.Severity.ERROR
        }
    }

    /**
     * 将宏展开失败类型映射为构造诊断类型。
     */
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

    /**
     * 将动态库加载失败类型映射为构造诊断类型。
     */
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
 * 基于 raw builder 保留的 typed expression carrier 执行稳定替换。
 *
 * 声明片段在实际 owning statement/declaration 列表中展开，参数与表达式按各自
 * typed carrier 替换。生成树的默认遍历负责进入函数、分支和嵌套初始化器正文。
 */
private object CfirExpressionMacroStableSplicer : MacroStableSplicer {
    /**
     * 将一批宏替换槽稳定应用到 CFIR 文件树。
     */
    override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): MacroStableSpliceResult {
        val embeddedSurfaceIds = slots.flatMapTo(hashSetOf()) { it.embeddedSurfaceIds }
        val independentlySplicedSlots = slots.filterNot { it.origin.surfaceId in embeddedSurfaceIds }
        val expressionSlotsWithCarrier = IdentityHashMap<CfirExpression, MacroReplaceSlot>()
        val declarationSlotsWithCarrier = IdentityHashMap<CfirDeclaration, MacroReplaceSlot>()
        val parameterSlotsWithCarrier = IdentityHashMap<CfirValueParameter, MacroReplaceSlot>()
        val annotationSlotsWithCarrier = linkedMapOf<CfirAnnotationReplaceCarrier, MacroReplaceSlot>()
        val appliedDeclarationReplacements = mutableListOf<MacroAppliedDeclarationReplacement>()
        val appliedExpressionReplacements = mutableListOf<MacroAppliedExpressionReplacement>()
        val unsupportedSlots = mutableListOf<MacroReplaceSlot>()

        for (slot in independentlySplicedSlots) {
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
                        require(expressionSlotsWithCarrier.put(carrier, slot) == null) {
                            "Stable splice received duplicate expression carrier for ${slot.origin.qualifiedName?.asString().orEmpty()}."
                        }
                    } else {
                        unsupportedSlots += slot
                    }
                }

                is MacroSurfaceParam -> {
                    val carrier = slot.handle.carrier
                    if (carrier is CfirValueParameter) {
                        require(parameterSlotsWithCarrier.put(carrier, slot) == null) {
                            "Stable splice received duplicate parameter carrier for ${slot.origin.qualifiedName?.asString().orEmpty()}."
                        }
                    } else {
                        unsupportedSlots += slot
                    }
                }

                is MacroSurfaceDecl, is MacroSurfaceNode -> {
                    val carrier = origin.replaceHandle.carrier
                    if (carrier is CfirDeclaration) {
                        require(declarationSlotsWithCarrier.put(carrier, slot) == null) {
                            "Stable splice received duplicate declaration carrier for ${slot.origin.qualifiedName?.asString().orEmpty()}."
                        }
                    } else {
                        unsupportedSlots += slot
                    }
                }

                else -> unsupportedSlots += slot
            }
        }

        require(unsupportedSlots.isEmpty()) {
            "Stable splice is not implemented for macro surface(s) without typed carrier: " +
                    unsupportedSlots.joinToString { it.origin.qualifiedName?.asString().orEmpty() }
        }
        val expectedExpressionReplacementCount = expressionSlotsWithCarrier.size

        val transformer = object : CfirDefaultTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E {
                val declarations = when (element) {
                    is CfirFile -> element.declarations
                    is CfirClassLikeDeclaration -> element.declarations
                    is CfirExtend -> element.declarations
                    else -> null
                }
                declarations?.let {
                    replaceDeclarationSlots(
                        it, declarationSlotsWithCarrier, annotationSlotsWithCarrier, appliedDeclarationReplacements,
                    ) { replacement -> replacement }
                }
                // 宏定义等 lazy body 尚无普通声明载体，沿其自身的生成树遍历协议处理。
                if (element is CfirBlock && element !is CfirLazyBlock) {
                    replaceDeclarationSlots(
                        element.statements, declarationSlotsWithCarrier, annotationSlotsWithCarrier, appliedDeclarationReplacements,
                    ) { replacement ->
                        require(replacement is CfirStatement) {
                            "A block declaration macro must produce statement declarations."
                        }
                        replacement
                    }
                }
                if (element is CfirFunction) {
                    replaceParameterSlots(element, parameterSlotsWithCarrier, annotationSlotsWithCarrier, appliedDeclarationReplacements)
                }
                return super.transformElement(element, data)
            }

            override fun transformErrorExpression(errorExpression: CfirErrorExpression, data: Unit): CfirExpression {
                val slot = expressionSlotsWithCarrier.remove(errorExpression)
                    ?: return super.transformErrorExpression(errorExpression, data) as CfirExpression
                val replacement = slot.toExpressionPayload()
                appliedExpressionReplacements += MacroAppliedExpressionReplacement(
                    carrier = errorExpression,
                    replacement = replacement,
                    surfaceId = slot.origin.surfaceId,
                )
                return replacement
            }
        }

        for (file in files) {
            file.transform<CfirFile, Unit>(transformer, Unit)
        }
        replaceAnnotationSlots(annotationSlotsWithCarrier)

        require(expressionSlotsWithCarrier.isEmpty()) {
            "Stable splice could not find expression carrier for macro surface(s): " +
                    expressionSlotsWithCarrier.values.joinToString { it.origin.qualifiedName?.asString().orEmpty() }
        }
        check(appliedExpressionReplacements.size == expectedExpressionReplacementCount) {
            "Stable splice applied ${appliedExpressionReplacements.size} expression replacements, " +
                "but received $expectedExpressionReplacementCount independent expression slots."
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
        return MacroStableSpliceResult(files, appliedDeclarationReplacements, appliedExpressionReplacements)
    }

    /**
     * 替换声明或参数上的注解槽。
     */
    private fun replaceAnnotationSlots(
        annotationSlots: MutableMap<CfirAnnotationReplaceCarrier, MacroReplaceSlot>,
    ) {
        val entries = annotationSlots.entries.toList()
        for ((carrier, slot) in entries) {
            val current = checkNotNull(slot.annotationMetadata.currentSlot(carrier)) { "Annotation slot was consumed before stable splice." }
            val annotations = current.owner.annotations
            require(annotations.getOrNull(current.annotationIndex) === current.originalAnnotation) {
                "Stable splice annotation carrier no longer matches its current owner/index."
            }
            val replacement = (slot.fragment as? MacroFragmentResult.CustomAnnotation)?.payload
                ?: error(
                    "Annotation macro surface `${
                        slot.origin.qualifiedName?.asString().orEmpty()
                    }` did not produce a CfirAnnotationCall payload."
                )
            val mutableAnnotations = annotations.toMutableList()
            mutableAnnotations[current.annotationIndex] = replacement
            slot.annotationMetadata.replaceAnnotationList(current.owner, mutableAnnotations, mapOf(replacement to carrier))
            annotationSlots.remove(carrier)
        }
    }

    @Suppress("UNCHECKED_CAST")
    /**
     * 在 owning 列表中展开声明片段，表达式语句保持原位置，子树交由默认转换器继续遍历。
     */
    private fun <E : CfirAnnotationContainer> replaceDeclarationSlots(
        declarations: List<E>,
        declarationSlots: IdentityHashMap<CfirDeclaration, MacroReplaceSlot>,
        annotationSlots: MutableMap<CfirAnnotationReplaceCarrier, MacroReplaceSlot>,
        appliedDeclarationReplacements: MutableList<MacroAppliedDeclarationReplacement>,
        toListElement: (CfirDeclaration) -> E,
    ) {
        if (declarations.none { it is CfirDeclaration && declarationSlots.containsKey(it) }) return
        val mutableDeclarations = declarations as? MutableList<E>
            ?: error("Stable splice requires mutable declaration carrier list.")
        val iterator = mutableDeclarations.listIterator()
        while (iterator.hasNext()) {
            val declaration = iterator.next() as? CfirDeclaration ?: continue
            val slot = declarationSlots.remove(declaration) ?: continue

            val replacements = slot.toDeclarationPayloads()
            val replacementElements = replacements.map(toListElement)
            appliedDeclarationReplacements += MacroAppliedDeclarationReplacement(
                carrier = declaration,
                replacements = replacements,
                surfaceId = slot.origin.surfaceId,
            )
            // 外层已经重解析的后代与继承前缀在进入 splicer 前已排除；其余槽独立写入首个输出。
            val firstReplacement = replacements.firstOrNull()
            if (firstReplacement == null) {
                annotationSlots.entries.removeIf { (carrier, _) -> carrier.owner === declaration }
                slot.annotationMetadata.replaceAnnotationList(declaration, emptyList())
            } else {
                firstReplacement.consumeAnnotationSlotsFromOwner(declaration, annotationSlots, slot.annotationMetadata)
            }
            iterator.remove()
            for (replacement in replacementElements) {
                iterator.add(replacement)
            }
        }
    }

    /**
     * 替换函数参数列表中的宏参数槽。
     */
    private fun replaceParameterSlots(
        declaration: CfirDeclaration,
        parameterSlots: IdentityHashMap<CfirValueParameter, MacroReplaceSlot>,
        annotationSlots: MutableMap<CfirAnnotationReplaceCarrier, MacroReplaceSlot>,
        appliedDeclarationReplacements: MutableList<MacroAppliedDeclarationReplacement>,
    ) {
        if (declaration !is CfirFunction || declaration.valueParameters.isEmpty()) return
        val newParameters = declaration.valueParameters.flatMap { parameter ->
            val slot = parameterSlots.remove(parameter) ?: return@flatMap listOf(parameter)
            val replacements = slot.toValueParameterPayload()
            require(replacements.isNotEmpty()) {
                "Parameter macro `${slot.origin.qualifiedName?.asString().orEmpty()}` produced no value parameter."
            }
            replacements.first().consumeAnnotationSlotsFromOwner(parameter, annotationSlots, slot.annotationMetadata)
            appliedDeclarationReplacements += MacroAppliedDeclarationReplacement(
                carrier = parameter,
                replacements = replacements,
                surfaceId = slot.origin.surfaceId,
            )
            replacements
        }
        declaration.replaceValueParameters(newParameters)
    }

    /**
     * owner 本身被声明/参数宏替换时，同 owner 的 annotation slot 必须合并到
     * replacement payload 上，而不能再去修改已经脱离最终文件树的旧 owner。
     */
    private fun CfirDeclaration.consumeAnnotationSlotsFromOwner(
        originalOwner: CfirDeclaration,
        annotationSlots: MutableMap<CfirAnnotationReplaceCarrier, MacroReplaceSlot>,
        annotationMetadata: CfirAnnotationMetadataBuilder,
    ) {
        val ownerSlots = annotationSlots.entries
            .filter { (carrier, _) -> carrier.owner === originalOwner }
            .sortedBy { (carrier, _) -> carrier.annotationIndex }
        if (ownerSlots.isEmpty()) return
        require(ownerSlots.all { it.value.annotationMetadata === annotationMetadata }) { "Merged annotation slots must share the owner construction transaction." }

        val replacementAnnotations = ownerSlots.map { (_, slot) -> slot.toAnnotationPayload() }
        val retainedAnnotations = annotations.toMutableList()
        for (replacement in replacementAnnotations) {
            val replacementKey = replacement.annotationMergeKey() ?: continue
            val retainedIndex = retainedAnnotations.indexOfFirst { retained ->
                retained.annotationMergeKey() == replacementKey
            }
            if (retainedIndex >= 0) {
                retainedAnnotations.removeAt(retainedIndex)
            }
        }
        val bindings = IdentityHashMap<CfirAnnotationCall, CfirAnnotationReplaceCarrier>()
        ownerSlots.forEachIndexed { index, (carrier, _) -> bindings[replacementAnnotations[index]] = carrier }
        annotationMetadata.replaceAnnotationList(this, replacementAnnotations + retainedAnnotations, bindings)
        for ((carrier, _) in ownerSlots) {
            annotationSlots.remove(carrier)
        }
    }

    /** 读取 annotation-slot 宏片段产物。 */
    private fun MacroReplaceSlot.toAnnotationPayload(): CfirAnnotationCall {
        return (fragment as? MacroFragmentResult.CustomAnnotation)?.payload
            ?: error(
                "Annotation macro surface `${
                    origin.qualifiedName?.asString().orEmpty()
                }` did not produce a CfirAnnotationCall payload."
            )
    }

    /**
     * 用 annotation 的结构化调用形态识别 declaration macro input 中已经保留的同一 slot。
     *
     * PSI 与 LightTree 的 source identity 不等价：同一个 `@!Hide[...]` 在 child
     * annotation slot 与 parent declaration fragment 中可能分别来自原文件和临时
     * fragment tree。stable splice 因此不能只比较 `source.text`，必须优先使用
     * annotation 名称与实参形态；源码文本仅作为非标准 annotation 节点的稳定补充键。
     */
    private fun CfirAnnotation.annotationMergeKey(): String? {
        val call = this as? CfirAnnotationCall
        val nameKey = call?.annotationNameMergeKey()
        if (nameKey != null) {
            val argumentKey = call.arguments.joinToString(separator = ",") { argument ->
                argument.annotationArgumentMergeKey()
            }
            // `@!` is semantic provenance in the official parser, not spelling
            // noise.  A declaration macro may legally carry both `@Hide` and
            // `@!Hide`; collapsing them here makes replacement consume the
            // wrong slot and is observable as a macro-expansion failure.
            val visibilityKey = if (call.isCompileTimeVisible == true || call.forcedCustom == true) {
                "compile-time-visible:"
            } else {
                "runtime-visible:"
            }
            return "$visibilityKey$nameKey($argumentKey)"
        }
        return source?.text?.toString()?.normalizedAnnotationMergeText()
            ?.takeIf(String::isNotEmpty)
    }

    /** 提取 annotation 调用名，忽略 `@` / `@!` 的语法 provenance。 */
    private fun CfirAnnotationCall.annotationNameMergeKey(): String? {
        val userTypeName = (typeRef as? CfirUserTypeRef)
            ?.qualifier
            ?.joinToString(separator = ".") { it.name.asString() }
            ?.takeIf(String::isNotBlank)
        if (userTypeName != null) return userTypeName

        return (calleeReference as? CfirNamedReference)
            ?.name
            ?.asString()
            ?.takeIf(String::isNotBlank)
    }

    /** 提取 annotation 实参形态，覆盖 named argument 与普通表达式两类 raw builder 输出。 */
    private fun CfirElement.annotationArgumentMergeKey(): String {
        val textKey = source?.text?.toString()?.normalizedAnnotationMergeText()
        if (!textKey.isNullOrEmpty()) return textKey

        return when (this) {
            is CfirNamedArgumentExpression -> buildString {
                append(argumentName.asString())
                append("=")
                append(expression.source?.text?.toString()?.normalizedAnnotationMergeText().orEmpty())
            }
            else -> this::class.qualifiedName.orEmpty()
        }
    }

    /** 只规范化空白；`@!` provenance 由 [annotationMergeKey] 单独保留。 */
    private fun String.normalizedAnnotationMergeText(): String =
        filterNot(Char::isWhitespace)

    /**
     * 从宏替换槽中读取表达式 payload。
     */
    private fun MacroReplaceSlot.toExpressionPayload(): CfirExpression {
        val success = fragment as? MacroFragmentResult.Success
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not parse successfully.")
        val root = success.payload.roots.singleOrNull() as? MacroFragmentPayload.Root.Expression
            ?: error("Expression macro `${origin.qualifiedName?.asString().orEmpty()}` must produce exactly one expression root.")
        return root.expression
    }

    /**
     * 从宏替换槽中读取声明 payload。
     */
    private fun MacroReplaceSlot.toDeclarationPayloads(): List<CfirDeclaration> {
        val success = fragment as? MacroFragmentResult.Success
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not parse successfully.")
        return success.payload.roots.map { root ->
            (root as? MacroFragmentPayload.Root.Declaration)?.declaration
                ?: error("Declaration macro `${origin.qualifiedName?.asString().orEmpty()}` produced a non-declaration root at ${root.index}.")
        }
    }

    /**
     * 从宏替换槽中读取参数 payload。
     */
    private fun MacroReplaceSlot.toValueParameterPayload(): List<CfirValueParameter> {
        val success = fragment as? MacroFragmentResult.Success
            ?: error("Macro fragment for `${origin.qualifiedName?.asString().orEmpty()}` did not parse successfully.")
        return success.payload.roots.map { root ->
            (root as? MacroFragmentPayload.Root.Parameter)?.parameter
                ?: error("Parameter macro `${origin.qualifiedName?.asString().orEmpty()}` produced a non-parameter root at ${root.index}.")
        }
    }
}

/**
 * 在进入稳定替换前验证直接语法根与最终 replacement slot 一致，避免把 parser 合法但槽位不匹配的结果当作内部异常。
 */
private data class MacroFragmentShapeFailure(
    val diagnosticKind: MacroConstructionDiagnostic.Kind,
    val message: String,
)

private fun MacroFragmentResult.Success.replacementShapeFailure(
    slotType: MacroReplacementSlotType,
): MacroFragmentShapeFailure? {
    val roots = payload.roots
    fun failure(message: String): MacroFragmentShapeFailure = MacroFragmentShapeFailure(
        diagnosticKind = if (slotType == MacroReplacementSlotType.DECLARATION) {
            MacroConstructionDiagnostic.Kind.MACRO_EXPECT_DECLARATION
        } else {
            MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED
        },
        message = message,
    )

    return when (slotType) {
        MacroReplacementSlotType.EXPRESSION -> when {
            roots.size != 1 -> failure("Expression replacement requires exactly one expression root; parser produced ${roots.size} roots.")
            roots.single() !is MacroFragmentPayload.Root.Expression ->
                failure("Expression replacement requires an expression root; parser produced ${roots.single().kindName()} root.")
            else -> null
        }

        MacroReplacementSlotType.DECLARATION ->
            roots.firstOrNull { it !is MacroFragmentPayload.Root.Declaration }?.let { root ->
                failure("Declaration replacement requires declaration roots; parser produced a ${root.kindName()} root at index ${root.index}.")
            }

        MacroReplacementSlotType.PARAMETER -> when {
            roots.isEmpty() -> failure("Parameter replacement requires at least one parameter root; parser produced no roots.")
            else -> roots.firstOrNull { it !is MacroFragmentPayload.Root.Parameter }?.let { root ->
                failure("Parameter replacement requires parameter roots; parser produced a ${root.kindName()} root at index ${root.index}.")
            }
        }

        MacroReplacementSlotType.ANNOTATION ->
            failure("Annotation replacement must produce a custom-annotation result, not a regular macro fragment.")
    }
}

private fun MacroFragmentPayload.Root.kindName(): String = when (this) {
    is MacroFragmentPayload.Root.Declaration -> "declaration"
    is MacroFragmentPayload.Root.Expression -> "expression"
    is MacroFragmentPayload.Root.Parameter -> "parameter"
}

/**
 * 返回宏片段对父宏可见的 token 序列。
 */
private fun MacroFragmentResult.parentVisibleTokens(
    refreshedTokens: RefreshedMacroSurfaceTokens,
): List<MacroSurfaceToken> {
    return when (this) {
        is MacroFragmentResult.Success -> tokens
        is MacroFragmentResult.CustomAnnotation -> tokens + refreshedTokens.inputTokens
        is MacroFragmentResult.Failure,
        is MacroFragmentResult.SyntaxFailure -> emptyList()
    }
}

/** 单次 wire 调用及其输入来源；executor 执行时不读取 PSI。 */
private data class MacroCallInput(val call: MacroCallInfo, val sources: MacroProtocolSourceMapping)

/** 将宏表面转换为 wire 调用，同时捕获独立于展开文本坐标的原输入来源。 */
private fun MacroSurface.toMacroCallInfo(
    entry: MacroDefinitionEntry,
    parentNames: List<String>,
    refreshedTokens: RefreshedMacroSurfaceTokens,
    preFile: PreMacroCfirFile?,
): MacroCallInput {
    val rootSource = sourceRange?.source
    val sources = MacroProtocolSourceMapping(rootSource, surfaceId)
    val start = sources.rootPosition(rootSource?.startOffset)
    val end = sources.rootPosition(rootSource?.endOffset?.minus(1)?.coerceAtLeast(rootSource.startOffset))
    val packageName = entry.executablePackageFqName.asString().takeUnless { it == "<root>" }.orEmpty()
    return MacroCallInput(
        call = MacroCallInfo(
            idName = entry.name.asString(),
            methodName = macroWrapperFunctionName(packageName, hasAttr, entry.executableName.asString()),
            packageName = packageName,
            libPath = entry.libPath.orEmpty(),
            hasAttrs = hasAttr,
            argTokens = refreshedTokens.inputTokens.toTokenInfo(start, end, sources),
            attrTokens = refreshedTokens.attrTokens.toTokenInfo(start, end, sources),
            parentNames = parentNames,
            position = start,
            endPosition = end,
        ),
        sources = sources,
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

/**
 * 子宏展开结果刷新后的宏表面 token。
 */
private data class RefreshedMacroSurfaceTokens(
    /**
     * 刷新后的输入 token。
     */
    val inputTokens: List<MacroSurfaceToken>,
    /**
     * 刷新后的属性 token。
     */
    val attrTokens: List<MacroSurfaceToken>,
)

/**
 * 判断当前宏节点是否存在无法映射的子宏 payload 通道。
 */
private fun MacroCallNode.hasUnresolvedChildPayloadChannel(
    childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
): Boolean {
    return childEdges.any { edge ->
        edge.child in childResults && edge.channel == MacroPayloadChannel.UNRESOLVED
    }
}

/**
 * 收集已被当前节点展开结果重新解析的所有后代 surface。
 *
 * Forest evaluator 只会在所有直接子节点都成功后调用父节点的 expand；因此父 fragment
 * 中的 child-first token 结果完整覆盖整棵后代子树，原始后代 carrier 不再属于 final CFIR。
 */
private fun MacroCallNode.descendantSurfaceIds(): Set<Long> = buildSet {
    fun collect(parent: MacroCallNode) {
        for (child in parent.children) {
            add(child.surface.surfaceId)
            collect(child)
        }
    }
    collect(this@descendantSurfaceIds)
}

/**
 * 使用子宏展开结果刷新当前宏节点的输入和属性 token。
 */
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

/**
 * 用子宏展开结果替换父宏 token 序列中的子宏源码范围。
 */
private fun List<MacroSurfaceToken>.replaceChildMacroRanges(
    childResults: Map<MacroCallNode, List<MacroSurfaceToken>>,
    channel: MacroPayloadChannel,
): List<MacroSurfaceToken> {
    if (isEmpty() || childResults.isEmpty()) return this

    var current = this
    val replacements = childResults.entries.asSequence()
        .mapNotNull { (child, replacement) ->
            val edge = child.parent?.childEdges.orEmpty()
                .firstOrNull { it.child === child && it.channel == channel }
                ?: return@mapNotNull null
            Triple(child, replacement, edge.replaceRange)
        }
        .sortedWith(
            compareBy(
                { it.third?.startOffset ?: Int.MAX_VALUE },
                { it.third?.endOffset ?: Int.MAX_VALUE },
                { it.first.surface.surfaceId },
            ),
        ).toList()

    for ((child, replacement, edgeRange) in replacements) {
        val range = edgeRange ?: child.surface.sourceRange ?: continue
        val startIndex = current.indexOfFirst { it.isInside(range) }
        if (startIndex < 0) continue
        val endIndex = current.indexOfLast { it.isInside(range) }
        current = buildList(current.size - (endIndex - startIndex + 1) + replacement.size) {
            addAll(current.subList(0, startIndex))
            addAll(replacement.map { token -> token.copy(sourceRange = token.sourceRange ?: range) })
            addAll(current.subList(endIndex + 1, current.size))
        }
    }
    return current
}

/**
 * 判断 token 是否完整落在给定宏表面源码范围内。
 */
private fun MacroSurfaceToken.isInside(range: org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceSourceRange): Boolean {
    return isSourceToken && startOffset >= range.startOffset && endOffset <= range.endOffset
}

/** wire token 优先使用原输入位置；子宏生成 token 使用原子宏调用位置。 */
private fun List<MacroSurfaceToken>.toTokenInfo(
    defaultBegin: SourcePosition,
    defaultEnd: SourcePosition,
    sources: MacroProtocolSourceMapping,
): List<TokenInfo> = flatMap { token ->
    val lexerTokens = if (token.kindName != null) {
        listOf(MacroPayloadToken(token.text, token.startOffset, token.endOffset, token.kindName))
    } else {
        MacroPayloadTokenizer.tokenize(token.text, token.startOffset)
    }
    val tokenMap = MacroFragmentSourceMap.materialize(listOf(token))
    val childPositions = token.sourceRange?.takeUnless { token.isSourceToken }?.let(sources::childCallPositions)
    lexerTokens.mapNotNull { lexerToken ->
        val encoded = MacroTokenKind.fromLexerToken(lexerToken.kindName, lexerToken.text) ?: return@mapNotNull null
        val start = lexerToken.startOffset - token.startOffset
        val end = lexerToken.endOffset - token.startOffset
        val originSpans = tokenMap.spansForRange(start, end)
        val original = tokenMap.originalRange(start, end, requireUnique = false, includeTrivia = true)
        val input = originSpans.firstOrNull()?.origin as? MacroTokenSourceOrigin.Input
        val info = TokenInfo(
            kind = encoded.kind,
            value = encoded.value,
            delimiterNum = encoded.delimiterNum,
            begin = if (original != null && input != null) sources.inputPosition(input, original.startOffset)
                else childPositions?.first ?: defaultBegin,
            end = if (original != null && input != null) sources.inputPosition(input, original.endOffset)
                else childPositions?.second ?: defaultEnd,
        )
        sources.record(info, lexerToken.text, originSpans)
        info
    }
}

/** 展开文本重建保留每个 wire token 的来源 span，随后 lexer 复扫只变换文本区间。 */
private fun List<TokenInfo>.toMacroSurfaceTokens(sources: MacroProtocolSourceMapping): List<MacroSurfaceToken> {
    if (isEmpty()) return emptyList()
    val rendered = MacroMsgCodec.rebuildExpandedTextWithSpans(this)
    val spans = rendered.spans.flatMap { span ->
        val text = rendered.text.substring(span.startOffset, span.endOffset)
        sources.spansForOutput(this[span.tokenIndex], text).map { origin ->
            origin.copy(
                startOffset = span.startOffset + origin.startOffset,
                endOffset = span.startOffset + origin.endOffset,
            )
        }
    }
    return listOf(MacroSurfaceToken(
        text = rendered.text, startOffset = 0, endOffset = rendered.text.length, sourceSpans = spans,
    ))
}

/**
 * 将普通字符串转为 Kotlin 字符串字面量。
 */
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
