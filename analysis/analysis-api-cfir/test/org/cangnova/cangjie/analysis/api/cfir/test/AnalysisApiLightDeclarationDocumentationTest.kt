package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOrigin
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOriginKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.documentation
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.light.declarations.CaLightCallableDeclarationImpl
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 锁定 source-backed light declaration 的文档恢复链路。
 *
 * 这里验证 light declaration 并不会把文档能力旁路掉，而是能通过 origin/source PSI
 * 回到 Analysis API 的标准 `findCDoc()` 主线。
 */
class AnalysisApiLightDeclarationDocumentationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/lightDeclarationDocs",
) {
    /**
     * 使用 standalone CFIR 配置运行 source-backed light declaration 文档测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 验证源码 light declaration 可通过 origin 恢复文档，而无 source 的反编译 light declaration 返回空文档。
     */
    @Test
    fun sourceLightDeclarationDocs(mainFile: CjFile, mainModule: CjTestModule) {
        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val declarations = provider.getLightDeclarations(mainFile, mainModule.caModule)
        val classDeclaration = declarations.filterIsInstance<CaLightClassLikeDeclaration>()
            .single { it.name == "Greeter" }
        val callableDeclaration = declarations.filterIsInstance<CaLightCallableDeclaration>()
            .single { it.name == "greet" }

        assertEquals(CaLightDeclarationOriginKind.SOURCE_PSI, classDeclaration.origin.kind)
        assertEquals(CaLightDeclarationOriginKind.SOURCE_PSI, callableDeclaration.origin.kind)
        assertNotNull(classDeclaration.origin.sourceElement)
        assertNotNull(callableDeclaration.origin.sourceElement)

        analyzeForTest(mainFile) {
            assertEquals("Returns greeting.\n@return greeting text", documentation(callableDeclaration))

            val lightDeclaration = CaLightCallableDeclarationImpl(
                name = "builtinsCallable",
                module = null,
                annotations = emptyList(),
                origin = CaLightDeclarationOrigin(
                    kind = CaLightDeclarationOriginKind.DECOMPILED_PSI,
                    description = "std.core.builtinsCallable",
                    containingFile = null,
                    sourceElement = null,
                ),
                token = token,
                callableId = CallableId(FqName("std.core"), Name.identifier("builtinsCallable")),
                signature = null,
            )

            assertEquals(null, documentation(lightDeclaration))
        }
    }
}
