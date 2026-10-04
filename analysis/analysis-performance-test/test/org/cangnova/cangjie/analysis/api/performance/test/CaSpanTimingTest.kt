package org.cangnova.cangjie.analysis.api.performance.test

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile
import io.opentelemetry.sdk.trace.data.SpanData
import java.nio.charset.StandardCharsets
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsMetricNames
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsSpanAttributes
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.parsing.CangjiePsiParseKind
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 链路 span 的端到端用例。
 *
 * 验证的是"观察者 → 统计域 → span 导出"这条链真的通了：接缝的单测只证明观察者接口存在，
 * 指标断言只证明指标到了，链路是另一个信号，必须自己有一条用例因它没接上而变红。
 *
 * 断言前用 [CaPerformanceTestTelemetry.resetSpans] 清掉宿主装配阶段的 span，
 * 保证看到的是本次操作产生的链路。
 */
class CaSpanTimingTest : AbstractAnalysisApiPerformanceTest(
    "analysis/analysis-performance-test/testData/performance"
) {
    /**
     * 收集诊断必须留下分析会话根 span，且各阶段 span 的父链最终到达它（而不是成为根 span）。
     */
    @Test
    fun resolvePhaseSpansNestUnderAnalysisSessionSpan(mainFile: CjFile) {
        CaPerformanceTestTelemetry.resetSpans()

        analyzeForTest(mainFile) {
            mainFile.collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
        }

        val spans = CaPerformanceTestTelemetry.finishedSpans()
        val analysisSpans = spans.filter { it.name == LLStatisticsMetricNames.analysisSessionSpan }
        assertTrue(
            analysisSpans.isNotEmpty(),
            "分析必须留下会话根 span，实际 span=${spans.map { it.name }}",
        )

        val phaseSpans = spans.filter { it.name.startsWith("cangjie.analysis.resolve.phases.") }
        assertTrue(
            phaseSpans.isNotEmpty(),
            "按需解析的每个阶段都必须留下 span，实际 span=${spans.map { it.name }}",
        )

        phaseSpans.forEach { span ->
            val phase = span.attributes.get(LLStatisticsSpanAttributes.resolvePhase)
            assertTrue(
                phase != null && span.name.endsWith(".${phase.lowercase()}"),
                "阶段 span 必须携带阶段属性且与名字一致，实际 name=${span.name} phase=$phase",
            )
        }

        // 阶段 span 的父链上必须出现分析会话 span；中间允许有容器 span（如 diagnostics.collection），
        // 因为文件级诊断收集在分析会话内又打开了自己的 span 并成为当前活动 span，
        // 真实的链路树是"分析会话 → 诊断收集 → 阶段"。
        val spansById = spans.associateBy { it.spanId }
        fun isUnderAnalysisSpan(span: SpanData): Boolean {
            var current: SpanData? = spansById[span.parentSpanId]
            var hops = 0
            while (current != null && hops < 8) {
                if (current.name == LLStatisticsMetricNames.analysisSessionSpan) return true
                current = spansById[current.parentSpanId]
                hops++
            }
            return false
        }

        val notUnderAnalysis = phaseSpans.filterNot { isUnderAnalysisSpan(it) }
        assertTrue(
            notUnderAnalysis.isEmpty(),
            "阶段 span 的父链必须最终到达分析会话 span（而不是成为根 span）：" +
                notUnderAnalysis.joinToString { "${it.name} parent=${it.parentSpanId}" },
        )
    }

    /**
     * 整文件 PSI 解析必须留下 `parse.file` span，并携带入口与成败属性。
     */
    @Test
    fun psiParseSpanIsExportedWithAttributes(mainFile: CjFile) {
        CaPerformanceTestTelemetry.resetSpans()

        parsePsiFile("spanProbe", mainFile.text)

        val spans = CaPerformanceTestTelemetry.finishedSpans()
        val parseSpan = spans.firstOrNull { it.name == LLStatisticsMetricNames.psiParseSpan(CangjiePsiParseKind.FILE) }
        assertTrue(
            parseSpan != null,
            "整文件解析必须留下 parse.file span，实际 span=${spans.map { it.name }}",
        )
        val span = requireNotNull(parseSpan)
        assertEquals(
            "file",
            span.attributes.get(LLStatisticsSpanAttributes.parseKind),
            "解析 span 必须携带入口种类属性",
        )
        assertEquals(
            true,
            span.attributes.get(LLStatisticsSpanAttributes.parseSucceeded),
            "正常完成的解析 span 必须标记 succeeded=true",
        )
    }

    /**
     * 新建 PSI 文件并强制 AST 构建；与 `CaParserStatisticsTest` 的探针一致：
     * 不访问树就不会进入解析入口，测量窗口内不会有任何 span。
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
