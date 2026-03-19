package org.cangnova.cangjie.cfir.pipeline

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.analysis.CheckersComponent
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.diagnosticReporter
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile

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
    val builder = PsiRawCfirBuilder(this)
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
    val parserDefinition = CangJieParserDefinition()

    return lightTreeFiles.map { sourceFile ->
        val sourceText = sourceFile.getContentsAsStream().bufferedReader().use { it.readText() }
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            parserDefinition,
            CangJieLexer(),
            sourceText,
        )
        val lightTree = CangJieLightParser.parse(builder)
        val cfirFile = LightTree2Cfir(
            session = this,
            source = sourceText,
            fileName = sourceFile.name,
        ).buildCfirFile(lightTree)
        firProvider.recordFile(cfirFile)
        reportFilesAndLines?.invoke(sourceFile.path ?: sourceFile.name, sourceText.lineSequence().count())
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
    cfirFiles: Collection<CfirFile>,
) {
    // 当前实现：checker 已在 CHECKERS 阶段执行
    // 此函数预留用于未来独立的 checker 执行逻辑
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
): SingleModuleFrontendOutput {
    val (scopeSession, fir) = session.runResolution(cfirFiles)
    session.runCheckers(scopeSession, fir)
    return SingleModuleFrontendOutput(session, scopeSession, fir)
}
