package org.cangnova.cangjie.idea.references

import com.intellij.mock.MockProject
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjSuperTypeCallEntry
import org.cangnova.cangjie.psi.CjValueArgumentName
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定 `cj-references` 模块自己的 reference utility 契约。
 *
 * 这里不把测试建立在某个后端 provider“碰巧注册成功”的副作用上，
 * 而是显式控制 `CangJieReferenceProvidersService` 返回值，直接验证：
 * 1. `mainReference` 工具对不同 PSI 家族的统一入口行为；
 * 2. `CDocReference` 的抽象协议；
 * 3. `unwrappedTargets` 的结果展开语义。
 */
class CjReferenceUtilitiesTest : AbstractAnalysisApiExecutionTest(
    "analysis/cj-references/testData/referenceUtils",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(TestReferenceServiceRegistrar)

    @Test
    fun mainReferenceFamilies(mainFile: CjFile) {
        val basicType = PsiTreeUtil.findChildrenOfType(mainFile, CjBasicType::class.java).first { it.text == "Int64" }
        val namedArgument = PsiTreeUtil.findChildrenOfType(mainFile, CjValueArgumentName::class.java).single {
            it.asName.asString() == "label"
        }
        /**
         * 当前 source parser 还只产出 `CjSuperTypeEntry`，不会直接物化 `CjSuperTypeCallEntry`。
         * 这里复用同一 ASTNode 包一层 `CjSuperTypeCallEntry`，锁住 `referenceUtils` 在该 PSI 家族上的入口契约。
         */
        val superTypeCall = CjSuperTypeCallEntry(
            mainFile.declarations
                .filterIsInstance<CjClass>()
                .single { it.name == "Document" }
                .superTypeListEntries
                .single()
                .node,
        )
        val simpleName = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java).last { it.referencedName == "helper" }
        val cdocName = PsiTreeUtil.findChildrenOfType(mainFile, CDocName::class.java).single { it.getNameText() == "Document" }

        val service = mainFile.project.getService(CangJieReferenceProvidersService::class.java) as TestReferenceProvidersService
        service.referenceFactory = { element ->
            when (element) {
                is CjBasicType -> arrayOf(TestPsiReference(element, "basic"))
                is CjValueArgumentName -> arrayOf(TestPsiReference(element, "named"))
                is CjSuperTypeCallEntry -> arrayOf(TestPsiReference(element, "super"))
                is CjSimpleNameExpression -> arrayOf(TestSimpleNameReference(element))
                is CDocName -> arrayOf(TestCDocReference(element))
                else -> PsiReference.EMPTY_ARRAY
            }
        }

        assertEquals("basic", (basicType.mainReference as TestPsiReference).label)
        assertEquals("named", (namedArgument.mainReference as TestPsiReference).label)
        assertEquals("super", (superTypeCall.mainReference as TestPsiReference).label)
        assertEquals("helper", simpleName.mainReference.canonicalText)
        val cdocReference = cdocName.mainReference
        val elementMainReference = (cdocName as org.cangnova.cangjie.psi.CjElement).mainReference
        assertEquals("Document", cdocReference.canonicalText)
        assertNotNull(elementMainReference)
        assertTrue(elementMainReference is TestCDocReference)
        assertSame(cdocName, elementMainReference!!.element)
        assertEquals(cdocReference.canonicalText, elementMainReference.canonicalText)
    }

    @Test
    fun cdocReferenceContract(mainFile: CjFile) {
        val cdocName = PsiTreeUtil.findChildrenOfType(mainFile, CDocName::class.java).single { it.getNameText() == "Document" }
        val service = mainFile.project.getService(CangJieReferenceProvidersService::class.java) as TestReferenceProvidersService

        val targetOne = PsiTreeUtil.findChildrenOfType(mainFile, org.cangnova.cangjie.psi.CjTypeStatement::class.java).single { it.name == "Document" }
        val targetTwo = PsiTreeUtil.findChildrenOfType(mainFile, org.cangnova.cangjie.psi.CjNamedFunction::class.java).single { it.name == "helper" }

        service.referenceFactory = { element ->
            when (element) {
                is CDocName -> arrayOf(TestCDocReference(element, listOf(targetOne, targetTwo)))
                else -> PsiReference.EMPTY_ARRAY
            }
        }

        val reference = cdocName.mainReference as TestCDocReference
        assertEquals("Document", reference.canonicalText)
        assertTrue(reference.canRename())
        assertEquals(cdocName.getNameTextRange(), reference.rangeInElement)
        assertEquals(listOf("Document"), reference.resolvesByNames.map { it.asString() })
        assertEquals(setOf(targetOne, targetTwo), reference.unwrappedTargets)
        assertEquals(targetOne, reference.resolve(), "默认策略应选择 multiResolve 的首个结果")
    }

    @Test
    fun cdocReferenceForbiddenNames(mainFile: CjFile) {
        val docName = createLocalDocName(mainFile, "/** @see [this] */\nclass Anchor {}\n")
        val reference = TestCDocReference(docName)
        assertTrue(reference.resolvesByNames.isEmpty(), "`this` / `super` 这类特殊名不应伪装成普通可解析名字")
    }

    @Test
    fun unwrappedTargetsForSingleReference(mainFile: CjFile) {
        val element = PsiTreeUtil.findChildrenOfType(mainFile, CjBasicType::class.java).single { it.text == "UInt8" }
        val target = PsiTreeUtil.findChildrenOfType(mainFile, org.cangnova.cangjie.psi.CjTypeStatement::class.java).single { it.name == "Document" }
        val reference = object : PsiReferenceBase<CjBasicType>(element) {
            override fun resolve(): PsiElement = target
        }

        assertEquals(setOf(target), reference.unwrappedTargets)
    }

    private fun createLocalDocName(mainFile: CjFile, text: String): CDocName {
        val docFile = org.cangnova.cangjie.psi.CjPsiFactory(mainFile.project, markGenerated = false).createFile("doc-temp.cj", text)
        return PsiTreeUtil.findChildrenOfType(docFile, CDocName::class.java).single()
    }
}

private object TestReferenceServiceRegistrar : org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.picoContainer.unregisterComponent(CangJieReferenceProvidersService::class.java.name)
        project.registerService(CangJieReferenceProvidersService::class.java, TestReferenceProvidersService())
    }
}

private class TestReferenceProvidersService : CangJieReferenceProvidersService() {
    var referenceFactory: (PsiElement) -> Array<PsiReference> = { PsiReference.EMPTY_ARRAY }

    override fun getReferences(psiElement: PsiElement): Array<PsiReference> = referenceFactory(psiElement)
}

private open class TestPsiReference(
    element: PsiElement,
    val label: String,
    private val target: PsiElement? = null,
) : PsiReferenceBase<PsiElement>(element) {
    override fun resolve(): PsiElement? = target
}

private class TestSimpleNameReference(
    expression: CjSimpleNameExpression,
    private val target: PsiElement? = null,
) : CjSimpleNameReference(expression) {
    override val resolver: ResolveCache.PolyVariantResolver<CjReference>
        get() = super.resolver

    override fun getImportAlias() = null

    override fun resolveTargetElements(): Collection<PsiElement> = listOfNotNull(target)
}

private class TestCDocReference(
    element: CDocName,
    private val targets: List<PsiElement> = emptyList(),
) : CDocReference(element) {
    override fun resolveTargetElements(): Collection<PsiElement> = targets
}
