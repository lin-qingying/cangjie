package org.cangnova.cangjie.cfir.pipeline

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.collectors.components.DiagnosticComponentsFactory
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.cfir.diagnostics.impl.PendingDiagnosticsReporterImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.diagnosticReporter
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.readSourceFileWithMapping

/**
 * 从 PSI 构建 Raw CFIR（对齐 K2 的 buildFirFromKtFiles）。
 *
 * 流程：
 * 1. 创建 [PsiRawCfirBuilder]
 * 2. 遍历 PSI 文件，构建 Raw CFIR
 * 3. 通过 [CfirProviderImpl.recordFile] 注册到符号索引
 *
 * @param cjFiles PSI 文件列表
 * @return Raw CFIR 文件列表
 */
fun CfirSession.buildCfirFromCjFiles(cjFiles: Collection<CjFile>): List<CfirFile> {
    val firProvider = cfirProvider as CfirProviderImpl
    val builder = PsiRawCfirBuilder(this, firProvider.cangjieScopeProvider)
    return cjFiles.map { cjFile ->
        builder.buildCfirFile(cjFile).also { cfirFile ->
            firProvider.recordFile(cfirFile)
        }
    }
}

/**
 * Builds raw CFIR via LightTree, mirroring Kotlin's `buildFirViaLightTree`.
 */
fun CfirSession.buildCfirViaLightTree(
    lightTreeFiles: Collection<CjSourceFile>,
    @Suppress("UNUSED_PARAMETER") diagnosticReporterForLightTree: DiagnosticReporter? = null,
    reportFilesAndLines: ((String, Int) -> Unit)? = null,
): List<CfirFile> {
    val firProvider = cfirProvider as CfirProviderImpl
    val builder = LightTree2Cfir(
        session = this,
        scopeProvider = firProvider.cangjieScopeProvider,
        diagnosticsReporter = diagnosticReporterForLightTree,
    )

    return lightTreeFiles.map { sourceFile ->
        val (code, linesMapping) = sourceFile.getContentsAsStream().reader(Charsets.UTF_8).use {
            it.readSourceFileWithMapping()
        }
        val cfirFile = builder.buildCfirFile(code, sourceFile, linesMapping)
        firProvider.recordFile(cfirFile)
        reportFilesAndLines?.invoke(sourceFile.path ?: sourceFile.name, linesMapping.linesCount)
        cfirFile
    }
}

fun List<CjSourceFile>.asCjFilesList(): List<CjFile> {
    return map { (it as CjPsiSourceFile).psiFile as CjFile }
}

/**
 * 运行完整 resolve 流水线（对齐 K2 的 runResolution）。
 *
 * 创建 [CfirTotalResolveProcessor] 并执行所有阶段。
 *
 * @param cfirFiles 待解析的 CFIR 文件
 * @return (ScopeSession, 已解析的 CFIR 文件)
 */
fun CfirSession.runResolution(cfirFiles: List<CfirFile>): Pair<ScopeSession, List<CfirFile>> {
    val resolveProcessor = CfirTotalResolveProcessor(this)
    resolveProcessor.process(cfirFiles)
    return resolveProcessor.scopeSession to cfirFiles
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
    val diagnosticsReporter = PendingDiagnosticsReporterImpl(diagnosticsCollector)
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
 *
 * @param cfirFiles 待处理的 CFIR 文件
 * @return 单模块前端输出
 */
fun resolveAndCheckCfir(
    session: CfirSession,
    cfirFiles: List<CfirFile>,
    diagnosticsCollector: BaseDiagnosticsCollector,
): SingleModuleFrontendOutput {
    val (scopeSession, fir) = session.runResolution(cfirFiles)
    session.runCheckers(scopeSession, fir, diagnosticsCollector)
    return SingleModuleFrontendOutput(session, scopeSession, fir)
}
