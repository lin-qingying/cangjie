package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.components.findCDoc
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNonPublicApi
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * CDoc 主线能力回归。
 *
 * 这组测试锁定两层契约：
 * 1. declaration PSI 上的 CDoc 结构视图稳定；
 * 2. declaration symbol 与 reference-resolved symbol
 *    能恢复到同一份结构化 CDoc。
 */
@OptIn(
    org.cangnova.cangjie.analysis.api.CaNonPublicApi::class,
    CjNonPublicApi::class,
    CjImplementationDetail::class,
)
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
    fun symbolCDoc(mainFile: CjFile) {
        val declaration = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == "describe" }
        val reference = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "describe" }

        analyzeForTest(reference) {
            val declarationDescriptor: CDocCommentDescriptor? = declaration.symbol.findCDoc()
            val referenceDescriptor: CDocCommentDescriptor? = (reference.resolveToSymbol() as? CaDeclarationSymbol)?.findCDoc()

            assertEquals("Greets the caller.", declarationDescriptor?.primaryTag?.getContent())
            assertEquals("Greets the caller.", referenceDescriptor?.primaryTag?.getContent())
            assertEquals(4, declarationDescriptor?.additionalSections?.size)
            assertEquals(4, referenceDescriptor?.additionalSections?.size)
        }
    }
}
