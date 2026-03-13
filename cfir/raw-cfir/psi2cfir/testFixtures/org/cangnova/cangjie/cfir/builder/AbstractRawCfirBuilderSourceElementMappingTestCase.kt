package org.cangnova.cangjie.cfir.builder

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PathUtil
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import java.io.File

abstract class AbstractRawCfirBuilderSourceElementMappingTestCase : AbstractRawCfirBuilderTestCase() {
    override fun doRawCfirTest(filePath: String) {
        val resolvedFilePath = resolveTestDataPath(filePath).path
        val fileTextWithTags = loadFile(resolvedFilePath)
        val fileText = fileTextWithTags.replace(START_EXPRESSION_TAG, "").replace(END_EXPRESSION_TAG, "")
        val cjFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(resolvedFilePath)), fileText) as CjFile

        val selected = run {
            val start = fileTextWithTags.indexOf(START_EXPRESSION_TAG)
            if (start < 0) error("$START_EXPRESSION_TAG was not found in $resolvedFilePath")
            val end = fileTextWithTags.indexOf(END_EXPRESSION_TAG)
            if (end < 0) error("$END_EXPRESSION_TAG was not found in $resolvedFilePath")

            val range = TextRange(start, end - START_EXPRESSION_TAG.length)
            findElementByExactRange(cjFile, range)
                ?: error("Expected exactly one element in range, found 0: ")
        }

        val cfirFile = cjFile.toCfirFile()
        val found = FindElementVisitor.find(cfirFile, selected)
        val target = found.minByOrNull {
            val source = it.source
            if (source == null) Int.MAX_VALUE else source.endOffset - source.startOffset
        } ?: PsiRawCfirBuilder(createTestSession()).buildElement(selected)

        val rendered = CfirRenderer.withGoldenCompat().renderElementAsString(target)
        val expectedPath = resolvedFilePath.replace(".cj", ".txt")
        assertEqualsToFile(File(expectedPath), rendered)
    }

    companion object {
        private const val START_EXPRESSION_TAG = "<expr>"
        private const val END_EXPRESSION_TAG = "</expr>"
    }

    private object FindElementVisitor : CfirVisitor<Unit, ElementFindingResult>() {
        override fun visitElement(element: CfirElement, data: ElementFindingResult) {
            val source = element.source
            if (source != null &&
                source.startOffset <= data.psi.textRange.startOffset &&
                source.endOffset >= data.psi.textRange.endOffset
            ) {
                data.result += element
            }
            element.transformChildren(TraversingTransformer(this, data), data)
        }

        fun find(cfirFile: CfirFile, element: CjElement): Set<CfirElement> {
            return ElementFindingResult(element, mutableSetOf()).also { cfirFile.accept(this, it) }.result
        }
    }

    private data class ElementFindingResult(
        val psi: CjElement,
        val result: MutableSet<CfirElement>,
    )

    private fun findElementByExactRange(file: CjFile, range: TextRange): CjElement? {
        val startLeaf = file.findElementAt(range.startOffset) ?: return null
        var current: PsiElement? = startLeaf
        while (current != null) {
            if (current is CjElement && current.textRange == range) {
                return current
            }
            current = current.parent
        }

        val all = PsiTreeUtil.collectElementsOfType(file, CjElement::class.java)
        return all.singleOrNull { it.textRange == range }
    }

    private class TraversingTransformer(
        private val visitor: CfirVisitor<Unit, ElementFindingResult>,
        private val data: ElementFindingResult,
    ) : CfirTransformer<ElementFindingResult>() {
        override fun <E : CfirElement> transformElement(element: E, data: ElementFindingResult): E {
            element.accept(visitor, this@TraversingTransformer.data)
            return element
        }
    }
}
