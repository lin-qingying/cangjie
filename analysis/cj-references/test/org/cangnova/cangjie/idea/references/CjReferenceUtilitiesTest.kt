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
    /**
     * 使用 standalone CFIR 分析 API 配置运行 reference utility 测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 注入测试专用 reference providers service，避免依赖真实扩展点注册状态。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(TestReferenceServiceRegistrar)

    /**
     * 验证不同 PSI 家族的 `mainReference` 扩展属性都能返回预期主引用。
     */
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

    /**
     * 验证 CDoc reference 的 rename、range、名称预过滤和多目标解析契约。
     */
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

    /**
     * 验证 `this` / `super` 这类 CDoc 特殊名称不会进入普通名称搜索集合。
     */
    @Test
    fun cdocReferenceForbiddenNames(mainFile: CjFile) {
        val docName = createLocalDocName(mainFile, "/** @see [this] */\nclass Anchor {}\n")
        val reference = TestCDocReference(docName)
        assertTrue(reference.resolvesByNames.isEmpty(), "`this` / `super` 这类特殊名不应伪装成普通可解析名字")
    }

    /**
     * 验证普通单目标 `PsiReference` 的 `unwrappedTargets` 只包含 resolve 结果。
     */
    @Test
    fun unwrappedTargetsForSingleReference(mainFile: CjFile) {
        val element = PsiTreeUtil.findChildrenOfType(mainFile, CjBasicType::class.java).single { it.text == "UInt8" }
        val target = PsiTreeUtil.findChildrenOfType(mainFile, org.cangnova.cangjie.psi.CjTypeStatement::class.java).single { it.name == "Document" }
        val reference = object : PsiReferenceBase<CjBasicType>(element) {
            override fun resolve(): PsiElement = target
        }

        assertEquals(setOf(target), reference.unwrappedTargets)
    }

    /**
     * 在当前测试项目内创建一个临时 CDoc 名称节点。
     */
    private fun createLocalDocName(mainFile: CjFile, text: String): CDocName {
        val docFile = org.cangnova.cangjie.psi.CjPsiFactory(mainFile.project, markGenerated = false).createFile("doc-temp.cj", text)
        return PsiTreeUtil.findChildrenOfType(docFile, CDocName::class.java).single()
    }
}

/**
 * 将测试项目的 reference provider service 替换为可控实现的注册器。
 */
private object TestReferenceServiceRegistrar : org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar() {
    /**
     * 注册测试专用 [CangJieReferenceProvidersService]。
     */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        project.picoContainer.unregisterComponent(CangJieReferenceProvidersService::class.java.name)
        project.registerService(CangJieReferenceProvidersService::class.java, TestReferenceProvidersService())
    }
}

/**
 * 可由测试动态替换 reference 生产逻辑的 provider service。
 */
private class TestReferenceProvidersService : CangJieReferenceProvidersService() {
    /**
     * 当前测试场景使用的 reference 工厂。
     */
    var referenceFactory: (PsiElement) -> Array<PsiReference> = { PsiReference.EMPTY_ARRAY }

    /**
     * 返回 [referenceFactory] 为指定 PSI 元素创建的引用集合。
     */
    override fun getReferences(psiElement: PsiElement): Array<PsiReference> = referenceFactory(psiElement)
}

/**
 * 测试用普通 PSI reference。
 */
private open class TestPsiReference(
    element: PsiElement,
    /**
     * 便于断言引用来源的标签。
     */
    val label: String,
    /**
     * 当前测试引用解析出的目标元素。
     */
    private val target: PsiElement? = null,
) : PsiReferenceBase<PsiElement>(element) {
    /**
     * 返回测试预设的解析目标。
     */
    override fun resolve(): PsiElement? = target
}

/**
 * 测试用 simple-name reference。
 */
private class TestSimpleNameReference(
    expression: CjSimpleNameExpression,
    /**
     * 当前测试引用解析出的目标元素。
     */
    private val target: PsiElement? = null,
) : CjSimpleNameReference(expression) {
    /**
     * 复用基类默认 resolver。
     */
    override val resolver: ResolveCache.PolyVariantResolver<CjReference>
        get() = super.resolver

    /**
     * 测试引用不绑定 import alias。
     */
    override fun getImportAlias() = null

    /**
     * 返回测试预设的解析目标集合。
     */
    override fun resolveTargetElements(): Collection<PsiElement> = listOfNotNull(target)
}

/**
 * 测试用 CDoc reference。
 */
private class TestCDocReference(
    element: CDocName,
    /**
     * 当前测试 CDoc reference 的多目标解析结果。
     */
    private val targets: List<PsiElement> = emptyList(),
) : CDocReference(element) {
    /**
     * 返回测试预设的解析目标集合。
     */
    override fun resolveTargetElements(): Collection<PsiElement> = targets
}
