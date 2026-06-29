package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.model.psi.PsiSymbolService
import com.intellij.model.psi.impl.targetDeclarationAndReferenceSymbols
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * richer target extraction 场景回归。
 *
 * 这里先锁定 extend 成员引用这条稳定契约。
 * constructor/finalizer/accessor 的声明位 target extraction 后续若提升为公开约束，再单独补实现与回归。
 */
class AnalysisApiRichTargetExtractionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/targetsRich",
) {
    /**
     * 使用 standalone CFIR 配置运行 richer target extraction 场景测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证 extend 成员调用位的目标提取能返回成员声明。
     */
    @Test
    fun richerTargets(mainFile: CjFile) {
        assertReferenceTargets(
            file = mainFile,
            anchor = "doc.prettyPrint()",
            offsetInAnchor = "doc.".length,
            expectedReferences = listOf("prettyPrint"),
        )
    }

    /**
     * 在指定锚点偏移处提取 reference symbols，并断言其还原到期望声明名。
     */
    private fun assertReferenceTargets(
        file: CjFile,
        anchor: String,
        offsetInAnchor: Int,
        expectedReferences: List<String>,
    ) {
        val offset = file.offsetOf(anchor, offsetInAnchor)
        val symbolService = PsiSymbolService.getInstance()
        val (_, referenced) = targetDeclarationAndReferenceSymbols(file, offset)

        val referenceNames = referenced
            .mapNotNull(symbolService::extractElementFromSymbol)
            .filterIsInstance<CjNamedDeclaration>()
            .mapNotNull { declaration -> declaration.name }

        assertEquals(expectedReferences, referenceNames, "Unexpected reference targets at `$anchor`")
    }

    /**
     * 将文本锚点与锚点内偏移转换为文件级绝对偏移。
     */
    private fun CjFile.offsetOf(anchor: String, offsetInAnchor: Int): Int {
        val anchorStart = text.indexOf(anchor)
        assertTrue(anchorStart >= 0, "Cannot find `$anchor` in ${name}")
        return anchorStart + offsetInAnchor
    }
}
