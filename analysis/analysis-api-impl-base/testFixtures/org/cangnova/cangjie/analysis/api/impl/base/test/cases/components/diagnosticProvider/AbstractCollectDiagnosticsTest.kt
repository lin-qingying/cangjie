package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTreeVisitor
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Checks the output of [org.cangnova.cangjie.analysis.api.components.CaDiagnosticProvider.collectDiagnostics]
 * and its consistency with [org.cangnova.cangjie.analysis.api.components.CaDiagnosticProvider.diagnostics]
 * on all source files in the test data.
 *
 * @see AbstractElementDiagnosticsTest
 */
abstract class AbstractCollectDiagnosticsTest : AbstractAnalysisApiComponentTest() {
    /**
     * 当前诊断收集测试额外注册的控制指令。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    /**
     * 文件诊断收集测试的专用指令集合。
     */
    private object Directives : SimpleDirectivesContainer() {
        /**
         * 跳过文件级诊断与元素级诊断一致性检查。
         */
        val SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK by stringDirective("Suppress individual diagnostics check for the test")
    }

    /**
     * @param name 原始测试文件名。多文件测试输出需要稳定打印该名称。
     */
    protected class PreparedFile(
        /**
         * 实际参与诊断分析的 PSI 文件。
         */
        val cjFile: CjFile,
        /**
         * 输出中展示的稳定文件名。
         */
        val name: String,
    )

    /**
     * 准备当前测试要分析的文件。
     *
     * 默认直接使用原始 `CjFile`；dangling/code fragment 测试会覆盖该方法构造非物理或片段文件。
     */
    protected open fun prepareCjFile(cjFile: CjFile, testServices: TestServices): PreparedFile =
        PreparedFile(cjFile, cjFile.name)

    /**
     * 执行所有主模块源文件的诊断收集测试。
     *
     * 方法先准备文件列表，再统一交给 `doTestByPreparedFiles` 渲染和断言。
     */
    override fun doTest(testServices: TestServices) {
        val preparedFiles = testServices.cjTestModuleStructure.mainModules
            .flatMap { it.cjFiles }
            .map { prepareCjFile(it, testServices) }

        doTestByPreparedFiles(preparedFiles, testServices)
    }

    /**
     * [preparedFiles] may contain fake files for dangling module tests.
     */
    protected fun doTestByPreparedFiles(preparedFiles: List<PreparedFile>, testServices: TestServices) {
        val actual = buildString {
            preparedFiles.forEachIndexed { index, preparedFile ->
                val cjFile = preparedFile.cjFile
                analyzeForTest(cjFile) {
                    val diagnosticsFromFile = collectFileDiagnostics(cjFile)
                    printFileDiagnostics(preparedFile, diagnosticsFromFile, preparedFiles.size > 1)
                    if (index != preparedFiles.lastIndex) {
                        appendLine()
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)

        if (Directives.SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK !in
            testServices.cjTestModuleStructure.testModuleStructure.allDirectives
        ) {
            for (preparedFile in preparedFiles) {
                val cjFile = preparedFile.cjFile
                analyzeForTest(cjFile) {
                    val diagnosticsFromFile = collectFileDiagnostics(cjFile)
                    checkDiagnosticsFromElements(cjFile, diagnosticsFromFile)
                }
            }
        }
    }

    /**
     * 从文件级诊断入口收集并排序诊断键。
     *
     * 排序后的键用于稳定 golden 输出，也用于和元素级诊断入口结果比较。
     */
    private fun CaSession.collectFileDiagnostics(cjFile: CjFile): List<DiagnosticKey> =
        cjFile
            .collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            .map { it.getDiagnosticKey() }
            .sorted()

    /**
     * 将单个准备文件的诊断列表写入输出。
     *
     * 多文件场景会在标题中带上原始文件名，单文件场景使用固定标题。
     */
    private fun StringBuilder.printFileDiagnostics(
        preparedFile: PreparedFile,
        diagnostics: List<DiagnosticKey>,
        hasMultipleTestFiles: Boolean,
    ) {
        val heading = if (hasMultipleTestFiles) {
            "Diagnostics from ${preparedFile.name}:"
        } else {
            "Diagnostics from file:"
        }

        appendLine(heading)
        if (diagnostics.isNotEmpty()) {
            for (key in diagnostics) {
                val element = key.psi
                appendLine("  for PSI element of type ${element::class.simpleName} at ${element.getLineColumnRange()}")
                printDiagnosticKey(key, 4)
            }
        } else {
            appendLine("  <NO DIAGNOSTICS>")
        }
    }

    /**
     * 按固定缩进渲染单个诊断键。
     *
     * 输出包含诊断 factory、文本范围和 PSI 元素位置。
     */
    private fun StringBuilder.printDiagnosticKey(key: DiagnosticKey, indent: Int) {
        val indentString = " ".repeat(indent)
        append(indentString + key.factoryName)
        appendLine("$indentString  text ranges: ${key.textRanges}")
        appendLine("$indentString  PSI: ${key.psi::class.simpleName} at ${key.psi.getLineColumnRange()}")
    }

    /**
     * 检查文件级诊断入口与逐元素诊断入口的一致性。
     *
     * 该检查遍历整个 PSI 树并收集每个元素的诊断，随后与文件级收集结果比较。
     */
    private fun CaSession.checkDiagnosticsFromElements(cjFile: CjFile, diagnosticsFromFile: List<DiagnosticKey>) {
        val diagnosticsFromElements = buildList {
            cjFile.accept(object : CjTreeVisitor<Unit?>() {
                override fun visitCjElement(element: CjElement, data: Unit?) {
                    element
                        .diagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                        .mapTo(this@buildList) { it.getDiagnosticKey() }

                    super.visitCjElement(element, data)
                }
            }, null)
        }.sorted()

        assertEquals(
            diagnosticsFromFile,
            diagnosticsFromElements,
            "diagnostics collected from files should be the same as those collected from individual PSI elements.",
        )
    }

    /**
     * 用于诊断比较和排序的稳定键。
     *
     * 键由诊断 factory、承载 PSI 元素和文本范围组成。
     */
    private data class DiagnosticKey(
        /**
         * 诊断 factory 的公开名称。
         */
        val factoryName: String,
        /**
         * 诊断绑定的 PSI 元素。
         */
        val psi: PsiElement,
        /**
         * 诊断报告的文本范围集合。
         */
        val textRanges: Collection<TextRange>,
    ) : Comparable<DiagnosticKey> {
        /**
         * 渲染用于排序和调试的诊断摘要。
         */
        override fun toString(): String {
            val document = psi.containingFile.viewProvider.document
            return "$factoryName on ${psi::class.simpleName} at ${offsetToLineAndColumn(document, psi.startOffset)})"
        }

        /**
         * 按诊断摘要文本排序。
         */
        override fun compareTo(other: DiagnosticKey): Int = this.toString().compareTo(other.toString())
    }

    /**
     * 将公开诊断对象转换为稳定比较键。
     */
    private fun CaDiagnosticWithPsi<*>.getDiagnosticKey() = DiagnosticKey(factoryName, psi, textRanges)

    /**
     * 获取 PSI 元素文本范围对应的行列区间文本。
     */
    private fun PsiElement.getLineColumnRange(): String = getLineAndColumnRangeInPsiFile(containingFile, textRange).toString()
}

/**
 * 诊断位置渲染中的单个行列坐标。
 *
 * 行列均为面向输出的一基坐标；未知行时用 offset 表示。
 */
private data class LineAndColumn(val line: Int, val column: Int, val lineContent: String?) {
    /**
     * 渲染行列坐标。
     */
    override fun toString(): String {
        if (line < 0) {
            return "(offset: $column line unknown)"
        }
        return "($line,$column)"
    }

    /**
     * 行列坐标常量集合。
     */
    companion object {
        /**
         * 表示无法从 document 计算出的未知坐标。
         */
        val NONE = LineAndColumn(-1, -1, null)
    }
}

/**
 * 诊断文本范围对应的起止行列坐标。
 */
private data class LineAndColumnRange(val start: LineAndColumn, val end: LineAndColumn) {
    /**
     * 渲染起止行列范围。
     */
    override fun toString(): String {
        if (start.line == end.line) {
            return "(${start.line},${start.column}-${end.column})"
        }
        return "$start - $end"
    }

    /**
     * 行列范围常量集合。
     */
    companion object {
        /**
         * 表示无法从 document 计算出的未知范围。
         */
        val NONE = LineAndColumnRange(LineAndColumn.NONE, LineAndColumn.NONE)
    }
}

/**
 * 将 PSI 文件中的文本范围转换为行列范围。
 *
 * 该函数基于文件 view provider 的 document 计算起止 offset 的行列坐标。
 */
private fun getLineAndColumnRangeInPsiFile(file: PsiFile, range: TextRange): LineAndColumnRange {
    val document = file.viewProvider.document
    return LineAndColumnRange(
        offsetToLineAndColumn(document, range.startOffset),
        offsetToLineAndColumn(document, range.endOffset),
    )
}

/**
 * 将 document offset 转换为一基行列坐标。
 *
 * document 不可用时返回未知行并保留 offset，方便定位非物理文件或特殊 PSI。
 */
private fun offsetToLineAndColumn(document: Document?, offset: Int): LineAndColumn {
    if (document == null || document.textLength == 0) {
        return LineAndColumn(-1, offset, null)
    }

    val lineNumber = document.getLineNumber(offset)
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    val column = offset - lineStartOffset

    val lineEndOffset = document.getLineEndOffset(lineNumber)
    val lineContent = document.charsSequence.subSequence(lineStartOffset, lineEndOffset).toString()

    return LineAndColumn(lineNumber + 1, column + 1, lineContent)
}
