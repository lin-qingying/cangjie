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
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    private object Directives : SimpleDirectivesContainer() {
        val SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK by stringDirective("Suppress individual diagnostics check for the test")
    }

    /**
     * @param name 原始测试文件名。多文件测试输出需要稳定打印该名称。
     */
    protected class PreparedFile(val cjFile: CjFile, val name: String)

    protected open fun prepareCjFile(cjFile: CjFile, testServices: TestServices): PreparedFile =
        PreparedFile(cjFile, cjFile.name)

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

    private fun CaSession.collectFileDiagnostics(cjFile: CjFile): List<DiagnosticKey> =
        cjFile
            .collectDiagnostics(CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
            .map { it.getDiagnosticKey() }
            .sorted()

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

    private fun StringBuilder.printDiagnosticKey(key: DiagnosticKey, indent: Int) {
        val indentString = " ".repeat(indent)
        append(indentString + key.factoryName)
        appendLine("$indentString  text ranges: ${key.textRanges}")
        appendLine("$indentString  PSI: ${key.psi::class.simpleName} at ${key.psi.getLineColumnRange()}")
    }

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

    private data class DiagnosticKey(
        val factoryName: String,
        val psi: PsiElement,
        val textRanges: Collection<TextRange>,
    ) : Comparable<DiagnosticKey> {
        override fun toString(): String {
            val document = psi.containingFile.viewProvider.document
            return "$factoryName on ${psi::class.simpleName} at ${offsetToLineAndColumn(document, psi.startOffset)})"
        }

        override fun compareTo(other: DiagnosticKey): Int = this.toString().compareTo(other.toString())
    }

    private fun CaDiagnosticWithPsi<*>.getDiagnosticKey() = DiagnosticKey(factoryName, psi, textRanges)

    private fun PsiElement.getLineColumnRange(): String = getLineAndColumnRangeInPsiFile(containingFile, textRange).toString()
}

private data class LineAndColumn(val line: Int, val column: Int, val lineContent: String?) {
    override fun toString(): String {
        if (line < 0) {
            return "(offset: $column line unknown)"
        }
        return "($line,$column)"
    }

    companion object {
        val NONE = LineAndColumn(-1, -1, null)
    }
}

private data class LineAndColumnRange(val start: LineAndColumn, val end: LineAndColumn) {
    override fun toString(): String {
        if (start.line == end.line) {
            return "(${start.line},${start.column}-${end.column})"
        }
        return "$start - $end"
    }

    companion object {
        val NONE = LineAndColumnRange(LineAndColumn.NONE, LineAndColumn.NONE)
    }
}

private fun getLineAndColumnRangeInPsiFile(file: PsiFile, range: TextRange): LineAndColumnRange {
    val document = file.viewProvider.document
    return LineAndColumnRange(
        offsetToLineAndColumn(document, range.startOffset),
        offsetToLineAndColumn(document, range.endOffset),
    )
}

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
