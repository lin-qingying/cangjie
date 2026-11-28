package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirLightTreeMacroDiagnosticsTest
import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirPsiMacroDiagnosticsTest
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructedClassTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.firHandlersStep
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.frontend.CfirAnalysisHandler
import org.cangnova.cangjie.test.frontend.CfirOutputArtifact
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** PSI真实宏构造：采用官方1.1.3已编译并运行的泛型构造器输入。 */
class CfirMacroConstructedTypeParameterPsiTest : AbstractCfirPsiMacroDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.firHandlersStep { useHandlers(::MacroConstructorIdentityHandler) }
    }

    @Test
    fun constructedClassParameters() {
        runTest("cfir/analysis-tests/testData/macroConstructedTypeParameters/constructedClassParameters.cj")
    }
}

/** LightTree使用相同宏包、executor、构造与解析契约，不以raw片段替代完整生产管线。 */
class CfirMacroConstructedTypeParameterLightTreeTest : AbstractCfirLightTreeMacroDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.firHandlersStep { useHandlers(::MacroConstructorIdentityHandler) }
    }

    @Test
    fun constructedClassParameters() {
        runTest("cfir/analysis-tests/testData/macroConstructedTypeParameters/constructedClassParameters.cj")
    }
}

/** 检查宏展开和frontend解析后实际采用的名义声明，禁止隐式构造器冒充宏输出。 */
private class MacroConstructorIdentityHandler(testServices: TestServices) : CfirAnalysisHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer> = emptyList()
    private var checkedHostFile = false

    override fun processModule(module: TestModule, info: CfirOutputArtifact) {
        val host = info.mainFirFilesByTestFile.values.single { it.packageDirective.packageFqName.asString() == "capture_use" }
        checkedHostFile = true
        val owners = host.declarations.filterIsInstance<CfirClassLikeDeclaration>()
        assertEquals(setOf("ViaInit", "ViaPrimary", "Emitted", "ViaNominal"), owners.map { it.symbol.classId.shortClassName.asString() }.toSet())
        for (owner in owners) {
            val parameter = owner.typeParameters.single() as CfirTypeParameter
            assertSame(owner.symbol, parameter.containingDeclarationSymbol)
            val constructor = owner.declarations.filterIsInstance<CfirConstructor>().single()
            assertNotEquals(CjFakeSourceElementKind.ImplicitConstructor, constructor.source?.kind)
            val reference = constructor.typeParameters.single()
            assertTrue(reference is CfirConstructedClassTypeParameterRef)
            assertSame(parameter.symbol, reference.symbol)
            assertSame(parameter, reference.symbol.cfir)
            val constructedType = constructor.returnTypeRef.coneType as ConeLookupTagBasedType
            assertSame(owner.symbol.toLookupTag(), constructedType.lookupTag)
            assertSame(parameter.symbol, (constructedType.typeArguments.single().type as ConeTypeParameterType).lookupTag.typeParameterSymbol)
            if (owner.symbol.classId.shortClassName.asString() == "Emitted") {
                assertTrue(constructor.valueParameters.isEmpty())
            } else {
                val argument = constructor.valueParameters.single()
                assertSame(parameter.symbol, (argument.returnTypeRef.coneType as ConeTypeParameterType).lookupTag.typeParameterSymbol)
                assertSame(constructor.symbol, argument.containingDeclarationSymbol)
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertTrue(checkedHostFile, "The actual macro construction frontend output must be checked")
    }
}
