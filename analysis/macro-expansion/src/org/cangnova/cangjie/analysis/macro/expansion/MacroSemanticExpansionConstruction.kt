package org.cangnova.cangjie.analysis.macro.expansion

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.macro.MacroSemanticSnapshotContract
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirSourcePsiInvalidatedException
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirSourcePsiGeneration
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.buildPreMacroRawBuildResultForModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.hasMacroCallCandidateForModule
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroBuiltinRegistries
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFailurePolicy
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolution
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.session.beginAnnotationMetadataRegistryGeneration
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.macro.expansion.MacroExpansionOutcome
import org.cangnova.cangjie.macro.expansion.MacroConstructionReadAccess
import org.cangnova.cangjie.macro.expansion.MacroExpansionRevision
import org.cangnova.cangjie.macro.expansion.ModuleRoots
import org.cangnova.cangjie.macro.expansion.SdkHandle
import org.cangnova.cangjie.psi.CjFile

/**
 * 一次宏语义快照构造的结果。
 *
 * [publishedFiles] 仅在 [outcome] 为 [MacroExpansionOutcome.PUBLISHED] 时有意义：
 * 复用其他会话已发布快照时为空集合，宿主仍需结束本次构造。
 */
internal data class MacroExpansionCompletion(
    val outcome: MacroExpansionOutcome,
    val publishedFiles: Set<VirtualFile> = emptySet(),
    /** 本次发布的快照实际代表的输入 revision；未发布时为 null。 */
    val publishedRevision: MacroExpansionRevision? = null,
    /** 本次稳定输入中唯一解析成功的宏/注解 surface。 */
    val macroCallResolutionSites: List<MacroCallResolutionSite> = emptyList(),
)

/** 单次构造尝试的推进方式；只有 [INPUT_CHANGED] 会在稳定输入后重试。 */
internal enum class MacroExpansionAttempt {
    COMPLETED,
    INPUT_CHANGED,
}

/**
 * 宿主无关的宏语义快照构造流程。
 *
 * 负责：pre-macro 整模块构建、无宏短路、artifact 准备、符号索引、import 绑定、
 * 展开、快照准备、屏障内发布与输入稳定性重试。所有宿主相关取值都经
 * [MacroSemanticExpansionEnvironment] 获取，所有"何时发布"的决策都在本类内。
 */
internal class MacroSemanticExpansionConstruction(private val project: Project) {
    /**
     * 执行一次完整构造；返回时已决定发布或放弃，但尚未更新服务的 revision 记录。
     *
     * 调用方必须在持有模块级串行锁时调用。
     */
    fun construct(
        session: LLCfirResolvableModuleSession,
        module: CaSourceModule,
        environment: MacroSemanticExpansionEnvironment,
    ): MacroExpansionCompletion {
        environment.onConstructionStarted(session)
        return try {
            constructUntilStable(session, module, environment)
        } catch (cancelled: ProcessCanceledException) {
            // PCE 不记录日志、原样向上抛出，由平台取消机制接管；收尾回调由调用方（服务）补齐。
            throw cancelled
        } catch (interrupted: InterruptedException) {
            LOG.debug(
                "Macro semantic snapshot task interrupted for ${module.moduleDescription}; " +
                    "sessionValid=${session.isValid}",
                interrupted,
            )
            Thread.currentThread().interrupt()
            MacroExpansionCompletion(MacroExpansionOutcome.CANCELLED)
        } catch (failure: Throwable) {
            LOG.error("Failed to construct the macro semantic snapshot for ${module.moduleDescription}", failure)
            MacroExpansionCompletion(MacroExpansionOutcome.FAILED)
        }
    }

    private fun constructUntilStable(
        session: LLCfirResolvableModuleSession,
        module: CaSourceModule,
        environment: MacroSemanticExpansionEnvironment,
    ): MacroExpansionCompletion {
        var attemptIndex = 0
        var publishedFiles: Set<VirtualFile> = emptySet()
        var publishedRevision: MacroExpansionRevision? = null
        var macroCallResolutionSites: List<MacroCallResolutionSite> = emptyList()
        var outcome = MacroExpansionOutcome.CANCELLED

        runMacroSemanticExpansionUntilStable(
            shouldContinue = {
                session.isValid && !project.isDisposed && !Thread.currentThread().isInterrupted
            },
            waitForInputStabilization = {
                LOG.info(
                    "Macro semantic snapshot input changed during construction for " +
                        "${module.moduleDescription}; recapturing after input stabilization",
                )
                try {
                    Thread.sleep(INPUT_STABILIZATION_DELAY_MILLIS)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            },
        ) {
            attemptIndex++
            val attempt = runAttempt(session, module, environment)
            outcome = attempt.outcome
            publishedFiles = attempt.publishedFiles
            publishedRevision = attempt.publishedRevision
            macroCallResolutionSites = attempt.macroCallResolutionSites
            attempt.progress
        }

        if (attemptIndex == 0) {
            outcome = MacroExpansionOutcome.CANCELLED
        }
        return MacroExpansionCompletion(outcome, publishedFiles, publishedRevision, macroCallResolutionSites)
    }

    /** 单次构造尝试；异常由 [construct] 统一归类。 */
    private fun runAttempt(
        session: LLCfirResolvableModuleSession,
        module: CaSourceModule,
        environment: MacroSemanticExpansionEnvironment,
    ): AttemptResult = try {
        runAttemptBody(session, module, environment)
    } catch (_: LLCfirSourcePsiInvalidatedException) {
        // 只有明确撤销的 generation 进入既有重捕获协议；未预期的 stale PSI 异常仍按正式失败传播。
        INPUT_CHANGED
    }

    private fun runAttemptBody(
        session: LLCfirResolvableModuleSession,
        module: CaSourceModule,
        environment: MacroSemanticExpansionEnvironment,
    ): AttemptResult {
        val readAccess = environment.readAccess()
        val revisionBeforeBuild = readAccess.compute<MacroExpansionRevision?> {
            if (!session.isValid || project.isDisposed) return@compute null
            session.beginAnnotationMetadataRegistryGeneration()
            environment.revision(module)
        } ?: return AttemptResult(MacroExpansionAttempt.COMPLETED, MacroExpansionOutcome.CANCELLED)

        // 逐文件读访问构建，不把整模块构建包进单个读动作：
        // 否则 EDT 上的写动作（SDK 变更后的 dropPsiCaches 等）会等待全部文件构建完成，冻结 UI。
        //
        // 成本门禁：先做廉价的 PSI 预扫描。模块里没有任何注解 / 表达式宏调用时不可能产生
        // surface，直接短路，避免为无宏工程执行一遍整模块 raw CFIR 构建再丢弃。
        if (!session.hasMacroCallCandidateForModule()) {
            LOG.info(
                "Skip macro semantic snapshot for ${module.moduleDescription}: " +
                    "no macro call candidates in module sources",
            )
            session.invalidateMacroSemanticSnapshot()
            return AttemptResult(MacroExpansionAttempt.COMPLETED, MacroExpansionOutcome.NO_MACRO_SURFACE)
        }

        val pre = session.buildPreMacroRawBuildResultForModule() ?: run {
            LOG.warn(
                "Macro semantic snapshot input is unavailable for ${module.moduleDescription}: " +
                    "the source module has no complete pre-macro raw CFIR input",
            )
            return AttemptResult(MacroExpansionAttempt.COMPLETED, MacroExpansionOutcome.FAILED)
        }

        /*
         * 没有任何宏调用 surface 时，展开是恒等变换。发布这种快照会为同一处声明制造第二棵
         * CFIR 视图：类型引用由实时树提供候选，模块级候选聚合却从快照取声明，同一 ClassId
         * 被判成重声明并退化为 error type。无宏即不构建、不发布，保持单一实时视图。
         *
         * 判据统一定义在 MacroSemanticSnapshotContract；这里只是展开前的廉价短路，宿主不得重复判断。
         */
        if (!MacroSemanticSnapshotContract.canHaveMacroSemanticEffect(pre)) {
            LOG.info("Skip macro semantic snapshot for ${module.moduleDescription}: no macro surfaces")
            session.invalidateMacroSemanticSnapshot()
            return AttemptResult(MacroExpansionAttempt.COMPLETED, MacroExpansionOutcome.NO_MACRO_SURFACE)
        }

        val capturedInput = readAccess.compute<CapturedInput?> {
            if (!session.isValid || project.isDisposed) {
                null
            } else {
                val sourceGeneration = LLCfirSourcePsiGeneration.capture(pre.files.map { file ->
                    checkNotNull(file.sourceFileIdentity as? CjFile) { "LL macro input must retain its actual source PSI file" }
                })
                CapturedInput(
                    roots = environment.roots(module),
                    sdk = environment.sdk(module),
                    revision = environment.revision(module),
                    sourceGeneration = sourceGeneration,
                    sourceFiles = sourceGeneration.files.mapNotNullTo(linkedSetOf()) { it.virtualFile },
                    sourcePaths = sourceGeneration.files.map { it.virtualFile?.path ?: it.name },
                )
            }
        } ?: return AttemptResult(MacroExpansionAttempt.COMPLETED, MacroExpansionOutcome.CANCELLED)

        // 构建期间插入的写动作会改变 revision，此时本次快照输入已不完整，需要重新捕获。
        if (revisionBeforeBuild != capturedInput.revision || Thread.currentThread().isInterrupted) {
            LOG.info(
                "Macro semantic input changed while building pre-macro CFIR for ${module.moduleDescription}; " +
                    "before=$revisionBeforeBuild; after=${capturedInput.revision}; " +
                    "interrupted=${Thread.currentThread().isInterrupted}",
            )
            return INPUT_CHANGED
        }

        val configuration = environment.compilerConfiguration(module, capturedInput.roots, capturedInput.sdk)
        // 短读访问调度属于宏编排；通用 source generation 所有者不依赖 macro 模块。
        val sourceReadAccess = object : MacroConstructionReadAccess {
            override fun <T> compute(action: () -> T): T = readAccess.compute {
                capturedInput.sourceGeneration.requireCurrent()
                val value = action()
                capturedInput.sourceGeneration.requireCurrent()
                value
            }
        }
        sourceReadAccess.compute { environment.installFragmentParser(module, configuration) }
        configuration.put(MacroConstructionReadAccess.CONFIGURATION_KEY, sourceReadAccess)
        session.ensureAnnotationMetadataRegistry()

        val classification = MacroDemandClassification.create(
            pre = pre,
            defaultMacroImports = emptyList(),
            builtinRegistries = MacroBuiltinRegistries.DEFAULT,
            externalMacroPackages = environment.externalMacroPackages(module),
        )
        session.register(MacroDemandClassification::class, classification)
        val engine = environment.engine()

        // 宏源码编译和 artifact 校验必须留在后台，不持有 PSI 读锁。
        val artifactPreparation = engine.prepareArtifacts(configuration, listOf(classification))
        if (!capturedInput.isCurrent(session, module, environment) || Thread.currentThread().isInterrupted) {
            return INPUT_CHANGED
        }
        classification.freezeFinal(
            macroArtifactDefinitions = artifactPreparation.definitions,
            failurePolicy = MacroFailurePolicy.DEGRADED,
        )
        val symbolIndex = sourceReadAccess.compute { engine.buildSymbolIndex(pre, artifactPreparation) }
        val macroResolutionContext = sourceReadAccess.compute { engine.bindImports(pre, symbolIndex) }
        val macroCallResolutionSites = sourceReadAccess.compute {
            collectMacroCallResolutionSites(pre, classification, macroResolutionContext)
        }
        val result = engine.expand(
            pre = pre,
            context = macroResolutionContext,
            classification = classification,
            configuration = configuration,
            diagnostics = artifactPreparation.diagnostics,
            mode = MacroConstructionService.Mode.DEGRADED,
        )

        val recordableResult = result as? MacroConstructionResult.Recordable ?: run {
            logUnpublishableResult(session, result)
            return AttemptResult(
                progress = MacroExpansionAttempt.COMPLETED,
                outcome = MacroExpansionOutcome.FAILED,
                macroCallResolutionSites = macroCallResolutionSites,
            )
        }
        if (!readAccess.compute<Boolean> {
                capturedInput.isCurrent(session, module, environment)
            }
        ) {
            return INPUT_CHANGED
        }

        val preparedSnapshot = sourceReadAccess.compute {
            session.prepareMacroSemanticSnapshot(pre, recordableResult)
        }
        if (!capturedInput.isCurrent(session, module, environment)) return INPUT_CHANGED
        if (preparedSnapshot == null) {
            val alreadyPublished = session.macroSemanticSnapshot != null
            LOG.warn(
                "Macro semantic snapshot could not be prepared for ${module.moduleDescription}; " +
                    "alreadyPublished=$alreadyPublished",
            )
            return AttemptResult(
                progress = MacroExpansionAttempt.COMPLETED,
                outcome = MacroExpansionOutcome.FAILED,
                macroCallResolutionSites = macroCallResolutionSites,
            )
        }

        // 预构建整模块索引已经结束；在宿主写屏障内等待现有分析读者退出，再原子切换语义视图与关联缓存。
        val published = environment.barrier().publish<Boolean?> {
            if (!capturedInput.isCurrent(session, module, environment)) {
                null
            } else {
                session.publishMacroSemanticSnapshot(pre, recordableResult, preparedSnapshot)
            }
        } ?: return INPUT_CHANGED

        if (!published) {
            LOG.warn(
                "Macro semantic snapshot did not match its source session for ${module.moduleDescription}",
            )
            return AttemptResult(
                progress = MacroExpansionAttempt.COMPLETED,
                outcome = MacroExpansionOutcome.FAILED,
                macroCallResolutionSites = macroCallResolutionSites,
            )
        }

        logPublished(session, module, pre, result, recordableResult, capturedInput.sourcePaths)
        return AttemptResult(
            progress = MacroExpansionAttempt.COMPLETED,
            outcome = MacroExpansionOutcome.PUBLISHED,
            publishedFiles = capturedInput.sourceFiles,
            publishedRevision = capturedInput.revision,
            macroCallResolutionSites = macroCallResolutionSites,
        )
    }

    /** 从最终宏路由结果记录每个调用面的成功类别，供 IDE 与 LSP 的语义 token 查询复用。 */
    private fun collectMacroCallResolutionSites(
        pre: PreMacroRawBuildResult,
        classification: MacroDemandClassification,
        resolutionContext: MacroResolutionContext,
    ): List<MacroCallResolutionSite> {
        val kindsBySurfaceId = classification.finalDecisions.mapNotNull { decision ->
            val resolution = decision.resolution
            val kind = when (resolution) {
                is MacroResolution.Resolved -> {
                    if (!resolutionContext.resolvesUniquelyTo(decision.surface, resolution.entry)) {
                        return@mapNotNull null
                    }
                    MacroInvocationSemanticKind.MACRO
                }

                is MacroResolution.Builtin -> MacroInvocationSemanticKind.MACRO

                is MacroResolution.BuiltinAnnotation -> MacroInvocationSemanticKind.BUILTIN_ANNOTATION
                is MacroResolution.CustomAnnotation -> MacroInvocationSemanticKind.ANNOTATION_CANDIDATE

                else -> null
            } ?: return@mapNotNull null
            decision.surface.surfaceId to kind
        }.toMap()
        if (kindsBySurfaceId.isEmpty()) return emptyList()

        return pre.files.flatMap { preFile ->
            val file = preFile.sourceFileIdentity as? CjFile ?: return@flatMap emptyList()
            val fileUrl = file.virtualFile?.url ?: return@flatMap emptyList()
            preFile.surfaces.mapNotNull { surface ->
                val kind = kindsBySurfaceId[surface.surfaceId] ?: return@mapNotNull null
                val range = surface.sourceRange ?: return@mapNotNull null
                val name = surface.qualifiedName?.shortName()?.asString() ?: return@mapNotNull null
                MacroCallResolutionSite(
                    fileUrl = fileUrl,
                    startOffset = range.startOffset,
                    endOffset = range.endOffset,
                    name = name,
                    kind = kind,
                )
            }
        }
    }

    /** 记录不可发布的结果类别与结构化诊断，便于定位"为什么没有快照"。 */
    private fun logUnpublishableResult(
        session: LLCfirResolvableModuleSession,
        result: MacroConstructionResult,
    ) {
        val surfacesById = result.registry.originSurfaceById
        val details = result.registry.diagnostics.joinToString(separator = "; ") { diagnostic ->
            val surface = diagnostic.originSurfaceId?.let(surfacesById::get)
            val call = surface?.let {
                val syntax = it.capturedRawSyntax
                    ?.replace('\n', ' ')
                    ?.take(120)
                " [macro=${it.qualifiedName}, offset=${it.sourceRange?.startOffset}" +
                    (syntax?.let { text -> ", call=$text" } ?: "") + "]"
            }.orEmpty()
            "${diagnostic.severity}/${diagnostic.kind}${call}: ${diagnostic.message}"
        }
        LOG.warn(
            "Macro semantic snapshot was not published for ${session.caModule.moduleDescription}: " +
                "${result::class.simpleName}${details.takeIf(String::isNotBlank)?.let { "; $it" }.orEmpty()}",
        )
    }

    /** 发布成功后的结构化日志；与旧宿主路径保持同样的可观测字段。 */
    private fun logPublished(
        session: LLCfirResolvableModuleSession,
        module: CaSourceModule,
        pre: PreMacroRawBuildResult,
        result: MacroConstructionResult,
        recordableResult: MacroConstructionResult.Recordable,
        sourcePaths: List<String>,
    ) {
        val macroSurfaces = pre.allSurfaces.joinToString { surface ->
            "${surface.qualifiedName?.asString() ?: "<unresolved>"}@${surface.sourceRange?.startOffset}"
        }
        LOG.info(
            "Published macro semantic snapshot for ${module.moduleDescription}; " +
                "sourceFiles=$sourcePaths; macroSurfaces=[$macroSurfaces]; " +
                "constructionResult=${result::class.simpleName}; " +
                "expandedFiles=${recordableResult.recordableFiles.files.size}; " +
                "generatedSources=${recordableResult.registry.generatedSources.size}; " +
                "scheduling highlighting refresh for ${pre.files.size} source files",
        )
    }

    /** 构建前捕获的宿主输入；用于在构造期间判定输入是否仍然有效。 */
    private data class CapturedInput(
        val roots: ModuleRoots,
        val sdk: SdkHandle?,
        val revision: MacroExpansionRevision,
        val sourceGeneration: LLCfirSourcePsiGeneration,
        // 仅在捕获时读取 PSI；发布后调度和日志使用真实文件身份与当时的不可变展示信息。
        val sourceFiles: Set<VirtualFile>,
        val sourcePaths: List<String>,
    ) {
        fun isCurrent(
            session: LLCfirResolvableModuleSession,
            module: CaSourceModule,
            environment: MacroSemanticExpansionEnvironment,
        ): Boolean {
            if (!session.isValid || session.project.isDisposed) return false
            if (!sourceGeneration.isCurrent()) return false
            val currentRevision = environment.revision(module)
            if (revision == currentRevision) return true
            LOG.info(
                "Macro semantic input revision changed for ${module.moduleDescription}; " +
                    "captured=$revision; current=$currentRevision",
            )
            return false
        }
    }

    /** 单次尝试的返回值。 */
    private data class AttemptResult(
        val progress: MacroExpansionAttempt,
        val outcome: MacroExpansionOutcome,
        val publishedFiles: Set<VirtualFile> = emptySet(),
        val publishedRevision: MacroExpansionRevision? = null,
        val macroCallResolutionSites: List<MacroCallResolutionSite> = emptyList(),
    )

    private companion object {
        private const val INPUT_STABILIZATION_DELAY_MILLIS = 250L
        private val INPUT_CHANGED = AttemptResult(
            MacroExpansionAttempt.INPUT_CHANGED,
            MacroExpansionOutcome.INPUT_CHANGED,
        )
        val LOG: Logger = Logger.getInstance(MacroSemanticExpansionConstruction::class.java)
    }
}

/**
 * 输入在后台构造期间变化时，等待短暂稳定后重新捕获同一 session 的最新 PSI。
 *
 * 只有 [MacroExpansionAttempt.INPUT_CHANGED] 会重试；其余结果立即结束。
 */
internal fun runMacroSemanticExpansionUntilStable(
    shouldContinue: () -> Boolean,
    waitForInputStabilization: () -> Unit,
    attempt: () -> MacroExpansionAttempt,
) {
    while (shouldContinue()) {
        when (attempt()) {
            MacroExpansionAttempt.COMPLETED -> return
            MacroExpansionAttempt.INPUT_CHANGED -> waitForInputStabilization()
        }
    }
}
