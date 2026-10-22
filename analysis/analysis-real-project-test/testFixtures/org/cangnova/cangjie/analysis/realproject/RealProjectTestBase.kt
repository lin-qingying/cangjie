package org.cangnova.cangjie.analysis.realproject

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * 真实项目测试基座。
 *
 * 负责：注册 standalone 平台服务、读取项目列表、逐个准备 [RealProjectSession]、
 * 汇总每个项目的失败；子类只写对分析结果的断言。
 *
 * @param testDataPath 该测试模块的 testData 目录（相对仓库根）。
 */
abstract class RealProjectTestBase(testDataPath: String) : AbstractAnalysisApiExecutionTest(testDataPath) {

    final override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    final override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(
            CaAnalysisApiDecompiledTestServiceRegistrar,
            RealProjectStandalonePlatformRegistrar,
        )

    /**
     * 按 `realProject.config` / 单项目参数逐个执行 [body]。
     *
     * 单个项目失败（clone、cjpm、分析异常）不会中断其它项目，最后统一断言。
     */
    protected fun runRealProjects(
        project: Project,
        languageVersionSettings: LanguageVersionSettings,
        body: (RealProjectSession) -> Unit,
    ) {
        val configs = RealProjects.resolve()
        assumeTrue(
            configs.isNotEmpty(),
            "未配置真实项目：使用 -PrealProjectRepo/-PrealProjectRef 或 realProject.config",
        )

        val failures = mutableListOf<String>()
        val entries = mutableListOf<RealProjectIndexEntry>()
        for (config in configs) {
            var session: RealProjectSession? = null
            try {
                session = RealProjectSession.prepare(config, project, languageVersionSettings)
                session.requireBuildSucceeded()
                body(session)
                entries += RealProjectIndexEntry(
                    config = config,
                    reportDir = session.reportDir,
                    ok = true,
                    message = null,
                    analyses = session.lastAnalyses,
                )
            } catch (cancelled: ProcessCanceledException) {
                throw cancelled
            } catch (failure: Throwable) {
                val analyses = session?.lastAnalyses.orEmpty()
                val reportDir = session?.reportDir
                    ?: config.workspaceRoot.resolve("reports").resolve(config.id)
                if (session != null && analyses.isEmpty()) {
                    session.writeFailureReport(failure)
                } else if (session == null) {
                    RealProjectHtmlReport.writeFailure(
                        file = reportDir.resolve("report.html"),
                        config = config,
                        checkout = null,
                        buildResults = null,
                        failure = failure,
                    )
                }
                entries += RealProjectIndexEntry(
                    config = config,
                    reportDir = reportDir,
                    // 已经产出分析结果就算“跑过”，状态由诊断/异常决定；否则是环境失败。
                    ok = analyses.isNotEmpty(),
                    message = failure.message?.lineSequence()?.firstOrNull()?.take(300),
                    analyses = analyses,
                )
                failures += "[${config.id}] ${failure.message ?: failure::class.simpleName}"
            }
        }

        val reportsRoot = configs.first().workspaceRoot.resolve("reports")
        runCatching {
            RealProjectHtmlReport.writeIndex(reportsRoot.resolve("index.html"), entries)
        }

        assertTrue(
            failures.isEmpty(),
            buildString {
                appendLine("${failures.size}/${configs.size} 个真实项目分析失败:")
                failures.forEach { appendLine("  $it") }
                appendLine("报告: ${reportsRoot.resolve("index.html")}")
            },
        )
    }

    /** 断言分析期间没有文件抛出异常，也没有元素级异常被收集隔离吞掉。 */
    protected fun assertNoAnalysisExceptions(session: RealProjectSession, analyses: List<FileAnalysis>) {
        val failures = analyses.filter { it.error != null }
        val collectionFailures = analyses.filter { it.collectionFailures.isNotEmpty() }
        assertTrue(
            failures.isEmpty() && collectionFailures.isEmpty(),
            buildString {
                appendLine(
                    "CFIR 在 ${session.config.id} 上抛出 ${failures.size} 个文件异常、" +
                        "${collectionFailures.sumOf { it.collectionFailures.size }} 个被隔离异常" +
                        "（报告: ${session.reportFile}）:",
                )
                failures.take(5).forEach { failure ->
                    appendLine("  ${failure.path}: ${failure.error?.lineSequence()?.firstOrNull()}")
                }
                collectionFailures.take(5).forEach { failure ->
                    appendLine(
                        "  ${failure.path}: " +
                            (failure.collectionFailures.firstOrNull()?.lineSequence()?.firstOrNull() ?: "isolated failure"),
                    )
                }
            },
        )
    }
}
