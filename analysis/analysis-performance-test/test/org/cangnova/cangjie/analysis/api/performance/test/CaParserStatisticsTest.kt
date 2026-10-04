package org.cangnova.cangjie.analysis.api.performance.test

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import java.nio.charset.StandardCharsets
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.parsing.CangjiePsiParseKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * PSI 解析耗时统计的端到端用例。
 *
 * 验证整条链路真的通了：low-level 描述符注册了 [org.cangnova.cangjie.parsing.CangjiePsiParseTimingService]
 * 实现 → 解析入口按 kind 取到服务 → 采样进入 SDK。本测试宿主同时加载 platform-interface 与
 * cfir 描述符，与 IDE 侧走同一条注册路径。
 *
 * 用例自己新建 PSI 文件而不是直接对注入的 `mainFile` 断言：`mainFile` 在测试体进入前就已被
 * 解析，采样落在快照之前，增量恒为 0。此处只借它的文本，源码仍由 testData 唯一提供。
 */
class CaParserStatisticsTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 强制一次整文件 PSI 解析后，`parse.file` 必须留下耗时采样与执行次数。
     */
    @Test
    fun psiFileParseDurationIsRecorded(mainFile: CjFile) {
        val pointsBefore = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersBefore = CaPerformanceTestTelemetry.collectLongCounters()

        parsePsiFile("parseProbe", mainFile.text)

        val pointsAfter = CaPerformanceTestTelemetry.collectHistogramPointCounts()
        val countersAfter = CaPerformanceTestTelemetry.collectLongCounters()

        val kind = CangjiePsiParseKind.FILE
        val durationName = LLStatisticsMetricNames.psiParseDuration(kind)
        val runsName = LLStatisticsMetricNames.psiParseRuns(kind)

        // 使用前的快照可能还没有这个 key（测试体进入前未解析过文件），缺键当 0 而不是抛异常。
        val points = (pointsAfter[durationName] ?: 0L) - (pointsBefore[durationName] ?: 0L)
        assertTrue(points >= 1, "整文件 PSI 解析必须记录耗时，实际 delta=$points")

        val runs = (countersAfter[runsName] ?: 0L) - (countersBefore[runsName] ?: 0L)
        assertTrue(runs >= 1, "整文件 PSI 解析必须记录次数，实际 delta=$runs")
        assertEquals(points, runs, "每次整文件解析都应当只记录一个耗时采样")
    }

    /**
     * 新建 PSI 文件并强制 AST 构建。
     *
     * `trySetupPsiForFile` 只建立文件元数据，AST 是懒构建的：不访问树就不会进入
     * `doParseContents`，测量窗口内不会产生任何解析采样。因此这里用 `PsiTreeUtil` 遍历一次树——
     * 既强制了解析，又能顺带校验没有解析错误：含错误的文件不算合法解析，
     * 它的耗时也不值得记入指标。
     */
    private fun parsePsiFile(name: String, text: String): PsiFile {
        val virtualFile = LightVirtualFile("$name.cj", CangJieFileType.INSTANCE, text)
        virtualFile.charset = StandardCharsets.UTF_8
        val psiFile = (PsiFileFactory.getInstance(project) as PsiFileFactoryImpl)
            .trySetupPsiForFile(virtualFile, CangJieLanguage, true, false)!!
        val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
        assertTrue(errors.isEmpty(), "$name 解析失败：${errors.joinToString { it.errorDescription }}\n$text")
        return psiFile
    }
}
