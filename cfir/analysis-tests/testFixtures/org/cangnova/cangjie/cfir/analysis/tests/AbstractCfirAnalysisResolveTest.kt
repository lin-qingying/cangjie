package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.CheckersComponent
import org.cangnova.cangjie.cfir.analysis.checkers.CommonDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CommonExpressionCheckers
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.pipeline.CfirTotalResolveProcessor
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.session.registerResolveComponents
import org.cangnova.cangjie.cfir.session.registerDiagnosticReporter
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.session.CfirSession
import java.io.File

/**
 * CFIR analysis-tests 基类：对齐 Kotlin CFIR analysis-tests 的“测试数据驱动”模式。
 *
 * 流程：
 * 1. `.cj` -> Raw CFIR
 * 2. 逐阶段推进到目标 resolve phase（默认 BODY_RESOLVE）
 * 3. dump 最终 CFIR，并与 `.cfir.txt` 对比
 */
abstract class AbstractCfirAnalysisResolveTest : AbstractCfirAnalysisTestCase() {

    /**
     * 当前 resolve 测试推进到的目标阶段。
     *
     * 默认覆盖完整 body resolve；子类可覆盖该属性以验证中间阶段的 CFIR 形态。
     */
    protected open val targetPhase: CfirResolvePhase = CfirResolvePhase.BODY_RESOLVE

    /**
     * 执行单个 CFIR resolve golden 测试。
     *
     * 方法读取 `.cj` 测试数据，构建并 resolve CFIR，随后与同名 `.cfir.txt`
     * golden 文件比较。
     */
    protected fun runAnalysisTest(testDataFilePath: String) {
        val sourceFile = resolveTestDataPath(testDataFilePath)
        val sourceText = loadFile(sourceFile.path).trim()
        val cjFile = createCjFile(sourceFile.nameWithoutExtension, sourceText)

        val session = createTestSession()
        val cfirFile = cjFile.toCfirFile(session = session)
        resolveToPhase(cfirFile, session, targetPhase)

        val actual = dumpCfirFile(cfirFile)
        val expectedFile = File(sourceFile.path.replace(".cj", ".cfir.txt"))
        assertEqualsToFile(expectedFile, actual)
    }

    /**
     * 将指定 CFIR 文件推进到目标 resolve 阶段。
     *
     * 该入口集中注册 builtin types、checker component、diagnostic reporter、resolve
     * components 与 extend provider，确保 analysis-tests 使用完整的 CFIR resolve 管线。
     */
    protected fun resolveToPhase(
        cfirFile: CfirFile,
        session: CfirSession,
        targetPhase: CfirResolvePhase,
        diagnosticReporter: CfirDiagnosticReporter = CfirDiagnosticCollector(),
    ) {
        session.register(CfirBuiltinTypes::class, CfirBuiltinTypes())

        session.register(
            CheckersComponent::class,
            CheckersComponent().apply {
                register(CommonDeclarationCheckers)
                register(CommonExpressionCheckers)
            },
        )

        // 先注册 diagnosticReporter，registerResolveComponents 会检测并复用
        session.registerDiagnosticReporter(diagnosticReporter)
        session.registerResolveComponents(
            diagnosticFactoriesStorage = CjRegisteredDiagnosticFactoriesStorage(),
        )

        session.register(
            CfirExtendProvider::class,
            CfirSessionExtendProvider(session, session.extendIndexStore),
        )

        // 使用 CfirTotalResolveProcessor 替代手动遍历
        val processor = CfirTotalResolveProcessor(session)
        processor.process(listOf(cfirFile))
    }
}
