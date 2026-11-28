package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyExpression
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** 验证源码声明按其所属 session 的语言版本计算 common 默认实现。 */
class CfirCjmpSourceDefaultVersionTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun primaryConstructorDefaultTracksOfficialVersionBranches() {
        val cases = listOf(
            LanguageVersion.CANGJIE_1_1_0_ALPHA_34 to true,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_66 to true,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_68 to false,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_70 to false,
            LanguageVersion.CANGJIE_1_1_0_BETA_0 to true,
            LanguageVersion.CANGJIE_1_1_0_BETA_1 to true,
            LanguageVersion.CANGJIE_1_1_0_BETA_20 to false,
            LanguageVersion.CANGJIE_1_1_3 to false,
        )
        for ((version, expected) in cases) {
            val fixture = Fixture(version)
            val primary = fixture.primary(hasBody = true)
            assertTrue(primary.isPrimary)
            assertSame(fixture.session, primary.moduleData.session)
            assertEquals(expected, primary.cjmpHasCommonDefault(), version.toString())
            assertFalse(fixture.primary(hasBody = false).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.secondary(hasBody = true).cjmpHasCommonDefault(), "secondary constructor: $version")
            assertFalse(fixture.secondary(hasBody = false).cjmpHasCommonDefault(), "secondary constructor without body: $version")
            assertTrue(fixture.primary(hasBody = true, specific = true).cjmpHasCommonDefault(), "specific: $version")
            assertTrue(fixture.primary(hasBody = false, specific = true).cjmpHasCommonDefault(), "specific without body: $version")
        }
    }

    @Test
    fun sourceFunctionAndVariableKeepTheirOwnRulesInBothBranches() {
        for (version in listOf(LanguageVersion.CANGJIE_1_1_0_ALPHA_66, LanguageVersion.CANGJIE_1_1_0_ALPHA_68, LanguageVersion.CANGJIE_1_1_0_BETA_1, LanguageVersion.CANGJIE_1_1_0_BETA_20)) {
            val fixture = Fixture(version)
            assertTrue(fixture.function(true).cjmpHasCommonDefault(), version.toString())
            assertFalse(fixture.function(false).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.function(true, specific = true).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.function(false, specific = true).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.variable(true).cjmpHasCommonDefault(), version.toString())
            assertFalse(fixture.variable(false).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.variable(true, specific = true).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.variable(false, specific = true).cjmpHasCommonDefault(), version.toString())
            assertFalse(fixture.patternVariable().cjmpHasCommonDefault(), "pattern variable is not a VAR_DECL: $version")
            assertFalse(fixture.property(false, false).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.property(true, false).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.property(false, true).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.property(true, true, specific = true).cjmpHasCommonDefault(), version.toString())
            assertTrue(fixture.property(false, false, specific = true).cjmpHasCommonDefault(), version.toString())
        }
    }

    private inner class Fixture(version: LanguageVersion) {
        val session = CallResolutionTestFixtures.newTestSession().apply {
            register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl(version)))
        }
        private val source = (createPsiFile("defaultVersion", "func anchor(): Unit {}") as CjFile)
            .declarations.single().toCjPsiSourceElement()
        private val id = CallableId(FqName.ROOT, Name.identifier("value"))
        private fun status(specific: Boolean = false) = CfirDeclarationStatusImpl().apply {
            isCommon = !specific
            isSpecific = specific
        }

        fun primary(hasBody: Boolean, specific: Boolean = false): CfirConstructor = buildPrimaryConstructor {
            source = this@Fixture.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = this@Fixture.status(specific)
            returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef(null)
            symbol = CfirConstructorSymbol(id)
            body = if (hasBody) buildBlock {} else null
        }

        fun secondary(hasBody: Boolean): CfirConstructor = buildConstructor {
            source = this@Fixture.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = this@Fixture.status()
            returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef(null)
            symbol = CfirConstructorSymbol(id)
            body = if (hasBody) buildBlock {} else null
        }

        fun function(hasBody: Boolean, specific: Boolean = false): CfirNamedFunction {
            val original = CallResolutionTestFixtures.buildFunctionSymbol(session, "value").cfir
            return buildNamedFunctionCopy(original) {
                source = this@Fixture.source
                origin = CfirDeclarationOrigin.Source
                symbol = CfirNamedFunctionSymbol(id)
                status = this@Fixture.status(specific)
                body = if (hasBody) buildBlock {} else null
            }
        }

        fun variable(hasInitializer: Boolean, specific: Boolean = false): CfirFieldVariable = buildFieldVariable {
            source = this@Fixture.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = this@Fixture.status(specific)
            returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
            symbol = CfirFieldVariableSymbol(id)
            name = id.callableName
            isVar = true
            initializer = if (hasInitializer) buildLazyExpression() else null
        }

        fun patternVariable(): CfirPatternVariable = buildPatternVariable {
            source = this@Fixture.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = this@Fixture.status()
            returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
            symbol = CfirPatternVariableSymbol(id)
            isVar = true
            initializer = buildLazyExpression()
            pattern = buildBindingPattern { name = id.callableName }
        }

        fun property(hasGetter: Boolean, hasSetter: Boolean, specific: Boolean = false): CfirProperty {
            val propertySymbol = CfirPropertySymbol(id)
            fun accessor(getter: Boolean): CfirPropertyAccessor = buildPropertyAccessor {
                source = this@Fixture.source
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                status = this@Fixture.status(specific)
                returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
                symbol = CfirPropertyAccessorSymbol()
                this.propertySymbol = propertySymbol
                isGetter = getter
            }
            return buildProperty {
                source = this@Fixture.source
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                status = this@Fixture.status(specific)
                returnTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
                symbol = propertySymbol
                name = id.callableName
                getter = if (hasGetter) accessor(true) else null
                setter = if (hasSetter) accessor(false) else null
            }
        }
    }
}
