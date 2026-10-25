package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.components.findCDoc
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.idea.references.CDocReference
import org.cangnova.cangjie.lexer.cdoc.psi.api.CDocCommentDescriptor
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocLink
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImplementationDetail
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNonPublicApi
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(
    org.cangnova.cangjie.analysis.api.CaNonPublicApi::class,
    CjNonPublicApi::class,
    CjImplementationDetail::class,
)
/**
 * CDoc 主线能力回归。
 *
 * 这组测试锁定两层契约：
 * 1. declaration PSI 上的 CDoc 结构视图稳定；
 * 2. declaration symbol 与 reference-resolved symbol
 *    能恢复到同一份结构化 CDoc。
 */
class AnalysisApiCDocTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/cdoc",
) {
    /**
     * 使用 standalone CFIR 配置执行 CDoc 结构化查询测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证声明 PSI 能直接提供默认段落、参数、返回值与 see 标签内容。
     */
    @Test
    fun declarationCdoc(mainFile: CjFile) {
        val declaration = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == "describe" }

        val docComment = declaration.docComment
        assertNotNull(docComment, "具名声明应直接暴露自身的 CDoc")

        assertEquals("Greets the caller.", docComment!!.getDefaultSection().getContent())
        assertEquals(
            "input value",
            docComment.getDefaultSection().findTagByName(CDocKnownTag.PARAM.name.lowercase())?.getContent(),
        )
        assertEquals(
            "rendered text",
            docComment.getDefaultSection().findTagByName(CDocKnownTag.RETURN.name.lowercase())?.getContent(),
        )
        assertEquals(
            "Document",
            docComment.getDefaultSection().findTagByName(CDocKnownTag.SEE.name.lowercase())?.getSubjectName(),
        )
    }

    /**
     * 验证声明 symbol 与引用解析得到的 symbol 能恢复一致的结构化 CDoc。
     */
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
            assertEquals(1, declarationDescriptor?.additionalSections?.size)
            assertEquals(1, referenceDescriptor?.additionalSections?.size)
        }
    }

    /**
     * 验证 `[Link]` 的 CDoc 名称引用能解析到对应声明。
     *
     * 覆盖单段名（`[Document]`）、包限定名（`[sample.cdoc.Document]`），
     * 以及 `@param [value]` 的形参解析。
     */
    @Test
    fun cdocReference(mainFile: CjFile) {
        val docComment = PsiTreeUtil.findChildrenOfType(mainFile, CjNamedFunction::class.java)
            .single { it.name == "describe" }
            .docComment
        assertNotNull(docComment, "describe 应带 CDoc")

        val names = PsiTreeUtil.findChildrenOfType(docComment, CDocName::class.java)
        val documentTargets = names.single { it.ownLinkText() == "[Document]" }.cdocReferenceTargets()
        assertTrue(
            documentTargets.any { it is CjClass && it.name == "Document" },
            "`[Document]` 应解析到 class Document，实际: $documentTargets",
        )

        val packageQualifiedTargets = names.single { it.ownLinkText() == "[sample.cdoc.Document]" }.cdocReferenceTargets()
        assertTrue(
            packageQualifiedTargets.any { it is CjClass && it.name == "Document" },
            "`[sample.cdoc.Document]` 应解析到 class Document，实际: $packageQualifiedTargets",
        )

        val parameterTargets = names.single { it.ownLinkText() == "[value]" }.cdocReferenceTargets()
        assertTrue(
            parameterTargets.any { it is CjParameter && it.name == "value" },
            "`@param [value]` 应解析到形参，实际: $parameterTargets",
        )
    }

    /**
     * 直接承载该 CDoc 名称的链接文本；限定名会拆成多个 [CDocName]，用链接文本区分是哪一条。
     */
    private fun CDocName.ownLinkText(): String? = (parent as? CDocLink)?.text

    /**
     * 收集 CDoc 名称引用解析出的 PSI 目标。
     */
    private fun CDocName.cdocReferenceTargets(): List<PsiElement> =
        references.filterIsInstance<CDocReference>()
            .flatMap { reference -> reference.multiResolve(false).mapNotNull { it.element } }
}
