package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.model.psi.PsiSymbolService
import com.intellij.model.psi.impl.targetDeclarationAndReferenceSymbols
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Analysis API target extraction 的基础光标位置回归测试。
 *
 * 覆盖声明名、类型位置、普通引用、索引调用和 pattern binding 声明位，
 * 确保 IntelliJ symbol target extraction 与仓颉 PSI 语义边界一致。
 */
class AnalysisApiTargetExtractionTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/targets",
) {
    /**
     * 使用 standalone CFIR 配置运行 target extraction 基础场景。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证多个光标锚点分别产出期望的 declaration targets 与 reference targets。
     */
    @Test
    fun caretTargets(mainFile: CjFile) {
        assertTargets(
            file = mainFile,
            anchor = "func b(): Int64",
            offsetInAnchor = "func ".length,
            expectedDeclarations = listOf("b"),
            expectedReferences = emptyList(),
        )
        assertTargets(
            file = mainFile,
            anchor = "func b(): Int64",
            offsetInAnchor = "func b(): ".length,
            expectedDeclarations = emptyList(),
            expectedReferences = emptyList(),
        )
        assertTargets(
            file = mainFile,
            anchor = "value: Box = a()",
            offsetInAnchor = "value: ".length,
            expectedDeclarations = emptyList(),
            expectedReferences = listOf("Box"),
        )
        assertHasReferenceTargets(
            file = mainFile,
            anchor = "value: Box = a()",
            offsetInAnchor = "value: Box = ".length,
        )
        assertTargets(
            file = mainFile,
            anchor = "return 0",
            offsetInAnchor = 0,
            expectedDeclarations = emptyList(),
            expectedReferences = emptyList(),
        )
        assertTargets(
            file = mainFile,
            anchor = "func b(): Int64 {",
            offsetInAnchor = "func b(): Int64 ".length,
            expectedDeclarations = emptyList(),
            expectedReferences = emptyList(),
        )
        assertHasReferenceTargets(
            file = mainFile,
            anchor = "indexer[0]",
            offsetInAnchor = "indexer".length,
        )
        assertBindingPatternDeclarationTarget(
            file = mainFile,
            anchor = "let bound = 1",
            offsetInAnchor = "let ".length,
        )
    }

    /**
     * 在指定锚点处提取声明与引用 target，并断言还原出的声明名集合。
     */
    private fun assertTargets(
        file: CjFile,
        anchor: String,
        offsetInAnchor: Int,
        expectedDeclarations: List<String>,
        expectedReferences: List<String>,
    ) {
        val offset = file.offsetOf(anchor, offsetInAnchor)
        val symbolService = PsiSymbolService.getInstance()
        val (declared, referenced) = targetDeclarationAndReferenceSymbols(file, offset)

        val declarationNames = declared
            .mapNotNull(symbolService::extractElementFromSymbol)
            .filterIsInstance<CjNamedDeclaration>()
            .mapNotNull { declaration -> declaration.name }

        val referenceNames = referenced
            .mapNotNull(symbolService::extractElementFromSymbol)
            .filterIsInstance<CjNamedDeclaration>()
            .mapNotNull { declaration -> declaration.name }

        assertEquals(expectedDeclarations, declarationNames, "Unexpected declaration targets at `$anchor`")
        assertEquals(expectedReferences, referenceNames, "Unexpected reference targets at `$anchor`")
    }

    /**
     * 将测试文本锚点和锚点内偏移转换为文件绝对偏移。
     */
    private fun CjFile.offsetOf(anchor: String, offsetInAnchor: Int): Int {
        val anchorStart = text.indexOf(anchor)
        assertTrue(anchorStart >= 0, "Cannot find `$anchor` in ${name}")
        return anchorStart + offsetInAnchor
    }

    /**
     * 断言指定锚点能至少提取到一个引用 target。
     */
    private fun assertHasReferenceTargets(
        file: CjFile,
        anchor: String,
        offsetInAnchor: Int,
    ) {
        val offset = file.offsetOf(anchor, offsetInAnchor)
        val (_, referenced) = targetDeclarationAndReferenceSymbols(file, offset)
        assertTrue(referenced.isNotEmpty(), "Expected non-empty reference targets at `$anchor`")
    }

    /**
     * 断言 pattern binding 声明名位置只产出声明 target，不产出引用 target。
     */
    private fun assertBindingPatternDeclarationTarget(
        file: CjFile,
        anchor: String,
        offsetInAnchor: Int,
    ) {
        val offset = file.offsetOf(anchor, offsetInAnchor)
        val symbolService = PsiSymbolService.getInstance()
        val (declared, referenced) = targetDeclarationAndReferenceSymbols(file, offset)

        val bindingPattern = declared
            .mapNotNull(symbolService::extractElementFromSymbol)
            .filterIsInstance<CjBindingPattern>()
            .firstOrNull()

        assertNotNull(bindingPattern, "Expected declaration target for binding pattern at `$anchor`")
        assertEquals("bound", bindingPattern?.name)
        assertTrue(referenced.isEmpty(), "Declaration name should not also produce reference targets at `$anchor`")
    }

}
