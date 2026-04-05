package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AnalysisApiReferenceServiceTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/references",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    @Test
    fun simpleReference(mainFile: CjFile) {
        val referenceExpression = PsiTreeUtil.findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == "greet" }

        val referenceService = CangJieReferenceProvidersService.getInstance(mainFile.project)
        assertEquals(
            "org.cangnova.cangjie.analysis.references.CaReferenceProvidersService",
            referenceService::class.java.name,
            "测试环境没有装配 Analysis API 引用服务",
        )

        val references = CangJieReferenceProvidersService.getReferencesFromProviders(referenceExpression)
        assertFalse(references.isEmpty(), "simple-name 节点没有拿到任何引用实现")

        val resolvedDeclaration = references
            .singleOrNull()
            ?.resolve() as? CjNamedDeclaration

        assertNotNull(
            resolvedDeclaration,
            "引用未解析成功；当前文件声明=${mainFile.declarations.map { it::class.simpleName to it.name }}，引用数=${references.size}",
        )
        assertEquals("greet", resolvedDeclaration?.name)
    }
}
