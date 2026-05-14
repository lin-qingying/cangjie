package org.cangnova.cangjie.cfir.pipeline

import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.collectors.components.DiagnosticComponentsFactory
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.cfir.diagnostics.impl.PendingDiagnosticsReporterImpl
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.diagnosticReporter
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.readSourceFileWithMapping

/**
 * 从 PSI 构建尚未注册的 Pre-Macro Raw CFIR
 * （对齐 K2 `buildFirFromKtFiles` 在 macro construction 之前的部分）。
 *
 * 流程：
 * 1. 创建 [PsiRawCfirBuilder]
 * 2. 遍历 PSI 文件，构建 Raw CFIR（仅结构）
 * 3. **不**写入 [CfirProviderImpl]；返回 [PreMacroRawBuildResult]，
 *    由上层经 [MacroConstructionService] 与 [recordExpandedRawFilesOnce] 完成注册。
 */
fun CfirSession.buildPreMacroRawCfirFromCjFiles(cjFiles: Collection<CjFile>): PreMacroRawBuildResult {
    val firProvider = cfirProvider as CfirProviderImpl
    val builder = PsiRawCfirBuilder(this, firProvider.cangjieScopeProvider)
    val rawFilesWithSurfaces = cjFiles.map { cjFile ->
        val cfirFile = builder.buildCfirFile(cjFile)
        cfirFile to builder.consumeCollectedMacroSurfaces()
    }
    return buildPreMacroRawFiles(
        session = this,
        rawCfirFiles = rawFilesWithSurfaces.map { it.first },
        fileSurfaces = rawFilesWithSurfaces.map { it.second },
    )
}

/**
 * 经 LightTree 构建尚未注册的 Pre-Macro Raw CFIR。
 * 行为镜像 [buildPreMacroRawCfirFromCjFiles] 的 PSI 形式。
 */
fun CfirSession.buildPreMacroRawCfirViaLightTree(
    lightTreeFiles: Collection<CjSourceFile>,
    @Suppress("UNUSED_PARAMETER") diagnosticReporterForLightTree: DiagnosticReporter? = null,
    reportFilesAndLines: ((String, Int) -> Unit)? = null,
): PreMacroRawBuildResult {
    val firProvider = cfirProvider as CfirProviderImpl
    val builder = LightTree2Cfir(
        session = this,
        scopeProvider = firProvider.cangjieScopeProvider,
        diagnosticsReporter = diagnosticReporterForLightTree,
    )
    val rawFilesWithSurfaces = lightTreeFiles.map { sourceFile ->
        val (code, linesMapping) = sourceFile.getContentsAsStream().reader(Charsets.UTF_8).use {
            it.readSourceFileWithMapping()
        }
        val (cfirFile, surfaces) = builder.buildCfirFileWithSurfaces(code, sourceFile, linesMapping)
        reportFilesAndLines?.invoke(sourceFile.path ?: sourceFile.name, linesMapping.linesCount)
        cfirFile to surfaces
    }
    return buildPreMacroRawFiles(
        session = this,
        rawCfirFiles = rawFilesWithSurfaces.map { it.first },
        fileSurfaces = rawFilesWithSurfaces.map { it.second },
    )
}

/**
 * 历史入口：从 PSI 构建 Raw CFIR 并立即注册到 source provider。
 *
 * 内部走 [MacroConstructionService.Identity] 的 stub + [recordExpandedRawFilesOnce]，
 * 保留外部签名以兼容测试 fixture。
 *
 * 生产 pipeline 推荐拆分为：
 *   `buildPreMacroRawCfirFromCjFiles → MacroConstructionService.expand → recordExpandedRawFilesOnce`
 */
fun CfirSession.buildCfirFromCjFiles(cjFiles: Collection<CjFile>): List<CfirFile> {
    return buildRecordableCfirFromCjFiles(cjFiles).files
}

/**
 * 历史入口：经 LightTree 构建 Raw CFIR 并立即注册。
 * 镜像 [buildCfirFromCjFiles]。
 */
fun CfirSession.buildCfirViaLightTree(
    lightTreeFiles: Collection<CjSourceFile>,
    @Suppress("UNUSED_PARAMETER") diagnosticReporterForLightTree: DiagnosticReporter? = null,
    reportFilesAndLines: ((String, Int) -> Unit)? = null,
): List<CfirFile> {
    return buildRecordableCfirViaLightTree(lightTreeFiles, diagnosticReporterForLightTree, reportFilesAndLines).files
}

/**
 * 构建无宏源码的 identity recordable 包装，并通过唯一入口写入 source provider。
 *
 * 这是 Kotlin `buildFirFromKtFiles -> provider.recordFile` 在仓颉宏 construction
 * 边界下的对应形态：调用方拿到的是 [RecordableRawCfirFiles]，后续只能通过
 * [runResolution] 进入 ordinary resolve。
 */
fun CfirSession.buildRecordableCfirFromCjFiles(cjFiles: Collection<CjFile>): RecordableRawCfirFiles {
    val pre = buildPreMacroRawCfirFromCjFiles(cjFiles)
    return finalizeIdentity(pre)
}

/**
 * LightTree 版本的 identity recordable 构建入口，语义同 [buildRecordableCfirFromCjFiles]。
 */
fun CfirSession.buildRecordableCfirViaLightTree(
    lightTreeFiles: Collection<CjSourceFile>,
    diagnosticReporterForLightTree: DiagnosticReporter? = null,
    reportFilesAndLines: ((String, Int) -> Unit)? = null,
): RecordableRawCfirFiles {
    val pre = buildPreMacroRawCfirViaLightTree(lightTreeFiles, diagnosticReporterForLightTree, reportFilesAndLines)
    return finalizeIdentity(pre)
}

private fun CfirSession.finalizeIdentity(pre: PreMacroRawBuildResult): RecordableRawCfirFiles {
    val provider = cfirProvider as CfirProviderImpl
    val result = MacroConstructionService.Identity.expandWithDefaultContext(
        pre = pre,
        mode = MacroConstructionService.Mode.STRICT,
    )
    val success = result as? MacroConstructionResult.Success
        ?: error("Identity macro construction must return Success, got ${result::class.simpleName}")
    recordExpandedRawFilesOnce(provider, success.recordableFiles, success.registry)
    return success.recordableFiles
}

fun List<CjSourceFile>.asCjFilesList(): List<CjFile> {
    return map { (it as CjPsiSourceFile).psiFile as CjFile }
}

/**
 * 运行完整 resolve 流水线（对齐 K2 的 runResolution）。
 *
 * 推荐入口接受 [RecordableRawCfirFiles]：它由 [MacroConstructionService] 产出，
 * 是 source CFIR 文件进入 ordinary resolve 的唯一规范输入（baseline 第 2 节硬性边界 #1）。
 */
fun CfirSession.runResolution(files: RecordableRawCfirFiles): Pair<ScopeSession, List<CfirFile>> {
    return runResolutionFiles(files.files)
}

/**
 * 内部实现：公开 API 不再接收裸 `List<CfirFile>`，避免绕过 macro construction 边界。
 */
private fun CfirSession.runResolutionFiles(cfirFiles: List<CfirFile>): Pair<ScopeSession, List<CfirFile>> {
    val resolveProcessor = CfirTotalResolveProcessor(this)
    val resolvedFiles = resolveProcessor.process(cfirFiles)
    return resolveProcessor.scopeSession to resolvedFiles
}

/**
 * 运行语义检查器（对齐 K2 的 runCheckers）。
 *
 * 通过 [CheckersComponent] 执行所有注册的 checker，
 * 诊断结果通过 [diagnosticReporter] 收集。
 *
 * @param scopeSession 作用域缓存会话
 * @param cfirFiles 待检查的 CFIR 文件
 */
fun CfirSession.runCheckers(
    scopeSession: ScopeSession,
    firFiles: Collection<CfirFile>,
    diagnosticsCollector: BaseDiagnosticsCollector,
): Map<CfirFile, List<CjDiagnostic>> {
    val collector = DiagnosticComponentsFactory.create(this, scopeSession)
    val registry = macroExpansionRegistry
    val diagnosticsReporter = PendingDiagnosticsReporterImpl(
        delegate = diagnosticsCollector,
        sourceMapper = { source -> registry?.originSourceForGeneratedSource(source) },
    )
    for (file in firFiles) {
        withFileAnalysisExceptionWrapping(file) {
            collector.collectDiagnostics(file, diagnosticsReporter)
        }
    }
    collector.collectDiagnosticsInSettings(diagnosticsReporter)
    return firFiles.associateWith {
        val path = it.sourceFile?.path ?: return@associateWith emptyList()
        diagnosticsCollector.diagnosticsByFilePath[path] ?: emptyList()
    }
}

/**
 * 完整的 resolve + check 流程（对齐 K2 的 resolveAndCheckFir）。
 *
 * 流程：
 * 1. 运行 resolve 流水线
 * 2. 运行 checker（当前已集成在 resolve 中）
 * 3. 返回前端输出
 */
fun resolveAndCheckCfir(
    session: CfirSession,
    cfirFiles: RecordableRawCfirFiles,
    diagnosticsCollector: BaseDiagnosticsCollector,
): SingleModuleFrontendOutput {
    val (scopeSession, fir) = session.runResolution(cfirFiles)
    session.runCheckers(scopeSession, fir, diagnosticsCollector)
    return SingleModuleFrontendOutput(session, scopeSession, fir)
}

/**
 * 完整的 construction + resolve + check 流程。
 *
 * 这是 baseline 第 1 节定义的"主流程"代码级表达：
 * ```
 * pre → MacroConstructionService.expand → recordExpandedRawFilesOnce → resolve → check
 * ```
 *
 * 当 construction 返回 [MacroConstructionResult.Failed] /
 * [MacroConstructionResult.ExecutorUnavailable] / [MacroConstructionResult.Blocked] 时，
 * 文件不会被 record，ordinary resolve 不会运行；诊断由调用方从 registry 自行处理。
 *
 * 当前 batch 仅暴露 Success / Degraded 两种"可注册"分支；
 * 其他分支返回 `null`，由上层判断 baseline 行为。
 */
fun resolveAndCheckCfirAfterConstruction(
    session: CfirSession,
    pre: PreMacroRawBuildResult,
    constructionService: MacroConstructionService,
    constructionMode: MacroConstructionService.Mode,
    diagnosticsCollector: BaseDiagnosticsCollector,
    macroArtifactDefinitions: List<MacroDefinitionEntry> = emptyList(),
    preConstructionDiagnostics: List<org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic> = emptyList(),
): Pair<MacroConstructionResult, SingleModuleFrontendOutput?> {
    // baseline 第 1 节"主流程"：先 symbol index，再 bindMacroImports，再 expand。
    val symbolIndex = buildMacroSymbolIndex(
        pre = pre,
        macroArtifactDefinitions = macroArtifactDefinitions,
    )
    val context = bindMacroImports(pre, symbolIndex)
    if (preConstructionDiagnostics.any {
            it.severity == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Severity.ERROR
        } && constructionMode == MacroConstructionService.Mode.STRICT
    ) {
        val registry = org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry().apply {
            addAll(preConstructionDiagnostics)
        }
        return MacroConstructionResult.Failed(registry) to null
    }
    val result = constructionService.expand(
        pre = pre,
        context = context,
        mode = constructionMode,
        preConstructionDiagnostics = preConstructionDiagnostics,
    )
    val recordable: RecordableRawCfirFiles = when (result) {
        is MacroConstructionResult.Success -> result.recordableFiles
        is MacroConstructionResult.Degraded -> result.recordableFiles
        is MacroConstructionResult.Failed,
        is MacroConstructionResult.ExecutorUnavailable,
        is MacroConstructionResult.Blocked -> return result to null
    }
    val provider = session.cfirProvider as CfirProviderImpl
    recordExpandedRawFilesOnce(provider, recordable, result.registry)
    val output = resolveAndCheckCfir(session, recordable, diagnosticsCollector)
    return result to output
}
