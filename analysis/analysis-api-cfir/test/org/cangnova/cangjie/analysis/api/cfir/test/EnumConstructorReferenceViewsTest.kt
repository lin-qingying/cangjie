package org.cangnova.cangjie.analysis.api.cfir.test

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.resolution.CaEnumConstructorCall
import org.cangnova.cangjie.analysis.api.resolution.successfulCallOrNull
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.analysis.test.framework.test.configurators.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 同一个使用点的 reference API、元素 API 与实际 constructor reference 必须保持同一目标。 */
class EnumConstructorReferenceViewsTest : AbstractAnalysisApiExecutionTest("analysis/analysis-api-cfir/testData/enumConstructorReferenceViews") {
    override val configurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
        AnalysisApiTestConfiguratorFactoryData(FrontendKind.Cfir, TestModuleKind.Source,
            AnalysisSessionMode.Normal, AnalysisApiMode.Ide),
    )

    @OptIn(CaExperimentalApi::class)
    @Test
    fun qualifiedMemberTypeArgumentDoesNotReplaceOwner(mainFile: CjFile, services: TestServices) {
        val call = services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjCallExpression>(mainFile)
        analyzeForTest(mainFile) {
            val cfir = call.getOrBuildCfir((this as CaCfirSession).resolutionFacade) as CfirFunctionCall
            val writtenArgument = cfir.typeArguments.single()
            assertNotNull(writtenArgument.source)
            assertEquals(ConePrimitiveType.INT64, writtenArgument.coneType)
            val resolved = requireNotNull(call.resolveToCall()?.successfulCallOrNull<CaEnumConstructorCall<*>>())
            val payload = resolved.payloadArgumentMapping.entries.single()
            assertEquals(payload.key.expressionType, payload.value)
        }
    }

    @Test
    fun libraryPatternViews(mainFile: CjFile, services: TestServices) {
        val names = listOf("somePattern", "nonePattern").map {
            services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, it)
        }
        val unknown = listOf("some1", "som").map {
            services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, it)
        }
        analyzeForTest(mainFile) {
            names.forEach { assertConstructorViews(it) }
            unknown.forEach { name ->
                val pattern = name.parent as CjEnumPattern
                val actual = pattern.getOrBuildCfir((this as CaCfirSession).resolutionFacade) as CfirEnumPattern
                assertTrue(actual.constructorReference is CfirErrorNamedReference)
                assertSame(actual.constructorReference, name.getOrBuildCfir(resolutionFacade))
                assertFalse(name.tryResolveSymbols() is CaSymbolResolutionSuccess)
                assertTrue(name.resolveToSymbols().isEmpty())
                assertNull(CangJieReferenceProvidersService.getInstance(mainFile.project).getNamedDeclarationTarget(name))
            }
        }
    }

    @Test
    fun localPatternViews(mainFile: CjFile, services: TestServices) {
        val names = listOf("payloadPattern", "readyPattern").map {
            services.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjSimpleNameExpression>(mainFile, it)
        }
        val wildcard = services.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjTypePattern>(mainFile, "wildcard")
        val bare = services.expressionMarkerProvider
            .getBottommostElementOfTypeAtCaret<CjVarOrEnumPattern>(mainFile, "bare")
        analyzeForTest(mainFile) {
            names.forEach { assertConstructorViews(it) }
            assertNull(wildcard.bindingSymbol)
            assertNotNull(bare.bindingSymbol)
            assertSame(bare, bare.bindingSymbol!!.psi)
        }
    }

    private fun CaSession.assertConstructorViews(name: CjSimpleNameExpression) {
        val session = this as CaCfirSession
        val pattern = when (val parent = name.parent) {
            is CjEnumPattern -> parent
            is CjVarOrEnumPattern -> parent
            else -> error("Marker must select a constructor pattern reference")
        }
        val actual = pattern.getOrBuildCfir(session.resolutionFacade) as CfirEnumPattern
        val reference = actual.constructorReference
        val nameMapping = name.getOrBuildCfir(session.resolutionFacade)
        println("ENUM_REFERENCE_VIEW pattern=" + actual::class.simpleName +
            " reference=" + reference::class.simpleName + "@" + System.identityHashCode(reference).toString(16) +
            " nameMapping=" + nameMapping?.let { it::class.simpleName + "@" + System.identityHashCode(it).toString(16) })
        assertSame(reference, nameMapping)
        assertTrue(reference is CfirResolvedNamedReference && reference !is CfirDiagnosticHolder) {
            val file = name.containingCjFile.getOrBuildCfirFile(session.resolutionFacade)
            val subjects = mutableListOf<String>()
            file.accept(object : CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) = element.acceptChildren(this)
                override fun visitMatchExpression(match: CfirMatchExpression) {
                    if (match.branches.any { it.pattern === actual }) {
                        val type = match.subject?.coneTypeOrNull
                        val callee = (match.subject as? CfirResolvable)?.calleeReference
                        subjects += "selectorType=$type; selectorError=${(type as? ConeErrorType)?.diagnostic?.reason}; " +
                            "callee=${callee?.javaClass?.simpleName}; calleeError=${(callee as? CfirDiagnosticHolder)?.diagnostic?.reason}"
                    }
                    visitElement(match)
                }
            })
            "实际 constructor reference 尚未成功解析：${reference::class.simpleName}\n${subjects.joinToString("\n")}\n" +
                CfirRenderer.render(file)
        }
        val target = (reference as CfirResolvedNamedReference).resolvedSymbol
        val byReference = name.resolveToSymbols().single()
        val byElement = (name.tryResolveSymbols() as CaSymbolResolutionSuccess).symbols.single()
        assertTrue(byReference is CaEnumConstructorSymbol)
        assertTrue(byElement is CaEnumConstructorSymbol)
        assertSame(target, (byReference as CaCfirSymbol<*>).cfirSymbol)
        assertSame(target, (byElement as CaCfirSymbol<*>).cfirSymbol)
        assertSame(byReference.psi, byElement.psi)
        assertTrue(byReference.psi is CjEnumConstructor)
        assertNull(CangJieReferenceProvidersService.getInstance(name.project).getNamedDeclarationTarget(name))
        if (pattern is CjVarOrEnumPattern) assertNull(pattern.bindingSymbol)
    }
}
