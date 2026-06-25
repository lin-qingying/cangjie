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

/**
 * raw CFIR source element 映射 golden 测试基类。
 */
abstract class AbstractRawCfirBuilderSourceElementMappingTestCase : AbstractRawCfirBuilderTestCase() {
    /**
     * 执行带 `<expr>` 标记的 source element 映射测试。
     */
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

    /**
     * 在 CFIR 树中查找覆盖指定 PSI 区间的最小元素。
     */
    private object FindElementVisitor : CfirVisitor<Unit, ElementFindingResult>() {
        /**
         * 收集 source 覆盖目标 PSI 的 CFIR 元素。
         */
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

        /**
         * 在指定 CFIR 文件中查找覆盖 [element] 的所有元素。
         */
        fun find(cfirFile: CfirFile, element: CjElement): Set<CfirElement> {
            return ElementFindingResult(element, mutableSetOf()).also { cfirFile.accept(this, it) }.result
        }
    }

    /**
     * source element 查找过程的可变结果。
     */
    private data class ElementFindingResult(
        /**
         * 目标 PSI 元素。
         */
        val psi: CjElement,
        /**
         * 已找到的 CFIR 元素集合。
         */
        val result: MutableSet<CfirElement>,
    )

    /**
     * 根据精确文本区间查找仓颉 PSI 元素。
     */
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

    /**
     * 通过 transformer 遍历 CFIR 子树并委托 visitor 处理每个元素。
     */
    private class TraversingTransformer(
        /**
         * 实际执行查找逻辑的 visitor。
         */
        private val visitor: CfirVisitor<Unit, ElementFindingResult>,
        /**
         * 查找过程共享的数据对象。
         */
        private val data: ElementFindingResult,
    ) : CfirTransformer<ElementFindingResult>() {
        /**
         * 访问元素后继续保持原节点不变。
         */
        override fun <E : CfirElement> transformElement(element: E, data: ElementFindingResult): E {
            element.accept(visitor, this@TraversingTransformer.data)
            return element
        }
    }
}
