package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 回归 IDE Analysis API 下的 decompiled type-position reference 解析。
 *
 * 目标是锁定真实路径：
 * `simple-name reference -> getOrBuildCfir(typeRef) -> stub-based library typealias deserialization`。
 */
class AnalysisApiIdeDecompiledReferenceTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/ideDecompiledReferences",
) {
    override val configurator: AnalysisApiTestConfigurator =
        CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
            AnalysisApiTestConfiguratorFactoryData(
                frontend = FrontendKind.Cfir,
                moduleKind = TestModuleKind.Source,
                analysisSessionMode = AnalysisSessionMode.Normal,
                analysisApiMode = AnalysisApiMode.Ide,
            ),
        )

    @Test
    fun decompiledTypeAliasReference(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        val libraryModule = testServices.cjTestModuleStructure.getModule("lib")
        assertTrue(
            libraryModule.caModule in mainModule.caModule.directRegularDependencies,
            "主模块必须直接依赖 `LibraryBinaryDecompiled` 库模块，才能覆盖 IDE 下的真实 library reference 路径。",
        )
        val referenceExpression = testServices.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile)

        val references = CangJieReferenceProvidersService.getReferencesFromProviders(referenceExpression)
        assertFalse(references.isEmpty(), "type-position simple-name 必须通过 IDE reference provider 产出引用。")

        val (resolvedSymbolKinds, resolvedTypeAliasNames) = analyzeForTest(referenceExpression) {
            val resolvedSymbols = referenceExpression.resolveToSymbols().toList()
            resolvedSymbols.map { it::class.simpleName } to
                resolvedSymbols.filterIsInstance<CaTypeAliasSymbol>().map { it.name.asString() }
        }
        assertEquals(
            1,
            resolvedTypeAliasNames.size,
            "Analysis API resolver 必须先解析到唯一的 typealias symbol；实际=$resolvedSymbolKinds",
        )
        assertEquals("RemoteAlias", resolvedTypeAliasNames.single())

        val resolvedTargets = references.mapNotNull { it.resolve() }
        val resolvedTypeAliases = resolvedTargets.filterIsInstance<CjTypeAlias>()
        assertEquals(
            1,
            resolvedTypeAliases.size,
            "provider 引用必须解析到唯一的 decompiled type alias；引用数=${references.size}，解析结果=${resolvedTargets.map { it::class.simpleName }}",
        )

        val resolvedTypeAlias = assertInstanceOf(CjTypeAlias::class.java, resolvedTypeAliases.single())
        assertEquals("RemoteAlias", resolvedTypeAlias.name)
        val resolvedContainingFile = resolvedTypeAlias.containingFile
        assertInstanceOf(
            CjDecompiledFile::class.java,
            resolvedContainingFile,
            "resolved file=${resolvedContainingFile.javaClass.name}; " +
                "compiled=${(resolvedContainingFile as? CjFile)?.isCompiled}; path=${resolvedContainingFile.virtualFile?.path}",
        )

        val resolvedByMainReference = referenceExpression.mainReference.resolve()
        assertNotNull(resolvedByMainReference, "mainReference 应继续走通 decompiled type alias 解析路径。")
        assertTrue(
            resolvedByMainReference === resolvedTypeAlias,
            "mainReference 与 contributor-based reference service 应解析到同一个 decompiled type alias PSI。",
        )
    }
}
