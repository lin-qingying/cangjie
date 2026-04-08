package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOrigin
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOriginKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.documentation
import org.cangnova.cangjie.analysis.light.declarations.CaLightCallableDeclarationImpl
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
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
 * 回到 Analysis API 的标准 `CaDocProvider` 主线。
 */
class AnalysisApiLightDeclarationDocumentationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/lightDeclarationDocs",
) {
    override val configurator: AnalysisApiTestConfigurator =
        CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                frontend = FrontendKind.Cfir,
                moduleKind = TestModuleKind.Source,
                analysisSessionMode = AnalysisSessionMode.Normal,
                analysisApiMode = AnalysisApiMode.Standalone,
            ),
        )

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
