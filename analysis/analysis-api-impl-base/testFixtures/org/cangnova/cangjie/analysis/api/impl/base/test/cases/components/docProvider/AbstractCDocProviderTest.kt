package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.docProvider

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.components.findCDoc
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNonPublicApi
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * CDoc provider 抽象测试。
 *
 * 该测试对齐 Kotlin analysis 中 KDoc provider 的架构位置，但只验证仓颉真实存在的能力：
 * 1. declaration 自带 CDoc 的 PSI 结构视图；
 * 2. declaration symbol -> CDoc descriptor；
 * 3. reference-resolved symbol -> CDoc descriptor。
 *
 * 不在这里镜像 Kotlin 的 constructor/property/script 派生规则。
 */
@OptIn(
    org.cangnova.cangjie.analysis.api.CaNonPublicApi::class,
    CjNonPublicApi::class,
    CjImplementationDetail::class,
)
abstract class AbstractCDocProviderTest : AbstractAnalysisApiComponentTest() {
    override fun doTestByMainFile(
        mainFile: CjFile,
        mainModule: org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule,
        testServices: TestServices,
    ) {
        val declaration = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == "describe" }
        val reference = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "describe" }

        val docComment = declaration.docComment
        assertNotNull(docComment, "声明自身应暴露 CDoc")
        assertEquals("Greets the caller.", docComment!!.getDefaultSection().getContent())
        assertEquals("input value", docComment.findSectionByTag(CDocKnownTag.PARAM, "value")?.getContent())
        assertEquals("rendered text", docComment.findSectionByTag(CDocKnownTag.RETURN)?.getContent())
        assertEquals("Document", docComment.findSectionByTag(CDocKnownTag.SEE)?.getSubjectName())

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
