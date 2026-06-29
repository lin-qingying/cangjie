package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定 contributor-based reference service 的基础契约。
 *
 * 这里聚焦两件事：
 * 1. simple-name 引用必须能通过统一 provider 正常解析到声明；
 * 2. 声明名位置不能被错误地产生为引用。
 *
 * 更依赖具体 PSI 形态的 parent-level target extraction 场景，统一放到
 * `AnalysisApiTargetExtractionTest` 中校验。
 */
class AnalysisApiReferenceServiceTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/references",
) {
    /**
     * 使用 standalone CFIR 配置运行 contributor-based reference service 测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证 simple-name 引用通过统一 provider 解析到同名函数声明。
     */
    @Test
    fun simpleReference(mainFile: CjFile) {
        val referenceExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "greet" }

        val referenceService = CangJieReferenceProvidersService.getInstance(mainFile.project)
        assertEquals(
            "org.cangnova.cangjie.analysis.references.CangJieReferenceProvidersServiceImpl",
            referenceService::class.java.name,
            "测试环境没有装配 Analysis API 引用服务",
        )

        val references = CangJieReferenceProvidersService.getReferencesFromProviders(referenceExpression)
        assertFalse(references.isEmpty(), "simple-name 节点没有拿到任何引用实现")

        val resolvedDeclaration = references.singleOrNull()?.resolve() as? CjNamedDeclaration
        assertNotNull(
            resolvedDeclaration,
            "引用未解析成功；当前文件声明=${mainFile.declarations.map { it::class.simpleName to it.name }}，引用数=${references.size}",
        )
        assertEquals("greet", resolvedDeclaration?.name)
    }

    /**
     * 验证 `mainReference` 入口与 contributor-based 引用服务解析结果一致。
     */
    @Test
    fun simpleReferenceMainReference(mainFile: CjFile) {
        val referenceExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "greet" }

        val resolvedDeclaration = referenceExpression.mainReference.resolve() as? CjNamedDeclaration
        assertNotNull(resolvedDeclaration, "mainReference 应与 contributor-based 引用服务对齐并可解析到声明")
        assertEquals("greet", resolvedDeclaration?.name)
    }

    /**
     * 验证类型位置 simple-name 节点也能通过引用服务解析到类型声明。
     */
    @Test
    fun typePositionReference(mainFile: CjFile) {
        val referenceExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .filter { it.referencedName == "Box" }
            .sortedBy { it.textOffset }
            .lastOrNull()
            ?: error("Cannot locate usage simple-name `Box` in `${mainFile.name}`")

        val references = CangJieReferenceProvidersService.getReferencesFromProviders(referenceExpression)
        assertFalse(references.isEmpty(), "type-position simple-name 节点没有拿到任何引用实现")

        val resolvedDeclaration = references.singleOrNull()?.resolve() as? CjNamedDeclaration
        assertNotNull(resolvedDeclaration, "type-position 引用未解析成功")
        assertEquals("Box", resolvedDeclaration?.name)
    }

    /**
     * 验证声明名位置不会被 simple-name reference provider 误识别为引用。
     */
    @Test
    fun declarationNamesDoNotProduceReferences(mainFile: CjFile) {
        val declarationOffset = mainFile.text.indexOf("result")
        assertTrue(declarationOffset >= 0, "Cannot find declaration name `result` in ${mainFile.name}")

        val declarationName = generateSequence(mainFile.findElementAt(declarationOffset)) { current -> current.parent }
            .flatMap { element -> sequenceOf(element) + element.children.asSequence() }
            .filterIsInstance<CjSimpleNameExpression>()
            .firstOrNull { expression -> expression.referencedName == "result" }
            ?: error("Cannot locate simple-name PSI for declaration `result`")

        val references = CangJieReferenceProvidersService.getReferencesFromProviders(declarationName)
        assertTrue(references.isEmpty(), "声明名位置不应被 simple-name provider 错误当成 reference")
    }
}
