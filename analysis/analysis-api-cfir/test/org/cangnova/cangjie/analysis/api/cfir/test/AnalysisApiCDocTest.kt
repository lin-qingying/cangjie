package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * CDoc 主线能力回归。
 *
 * 这组测试锁定两层契约：
 * 1. declaration PSI 上的 CDoc 结构视图稳定；
 * 2. `CaDocProvider` 能从 declaration symbol 与 reference-resolved symbol
 *    恢复到同一份文档文本。
 */
class AnalysisApiCDocTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/cdoc",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun declarationCdoc(mainFile: CjFile) {
        val declaration = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == "describe" }

        val docComment = declaration.docComment
        assertNotNull(docComment, "具名声明应直接暴露自身的 CDoc")

        assertEquals("Greets the caller.", docComment!!.getDefaultSection().getContent())
        assertEquals(
            "input value",
            docComment.findSectionByTag(CDocKnownTag.PARAM, "value")?.getContent(),
        )
        assertEquals(
            "rendered text",
            docComment.findSectionByTag(CDocKnownTag.RETURN)?.getContent(),
        )
        assertEquals(
            "Document",
            docComment.findSectionByTag(CDocKnownTag.SEE)?.getSubjectName(),
        )
    }

    @Test
    fun symbolDocumentation(mainFile: CjFile) {
        val declaration = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == "describe" }
        val reference = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "describe" }

        analyzeForTest(reference) {
            val declarationDocumentation = declaration.symbol.documentation()
            val referenceDocumentation = reference.resolveToSymbol()?.documentation()

            val expected = """
                Greets the caller.
                @param value input value
                @return rendered text
                @see Document
            """.trimIndent()

            assertEquals(expected, declarationDocumentation)
            assertEquals(expected, referenceDocumentation)
        }
    }
}
