package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(CfirImplementationDetail::class)
class CfirDeclarationToStringTest {
    @Test
    fun `class toString delegates to readability renderer`() {
        val classId = org.cangnova.cangjie.name.ClassId(FqName("sample"), Name.identifier("Box"))
        val declaration = buildClass {
            source = TestBinarySourceElement("class Box")
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            scopeProvider = TestScopeProvider
            status = CfirDeclarationStatusImpl()
            symbol = CfirClassSymbol(classId)
            name = Name.identifier("Box")
        }

        declaration.symbol.bind(declaration)

        assertMatchesReadabilityRenderer(declaration)
    }

    @Test
    fun `named function toString delegates to readability renderer`() {
        val callableId = CallableId(FqName("sample"), Name.identifier("compute"))
        val functionSymbol = CfirNamedFunctionSymbol(callableId)
        val declaration = buildNamedFunction {
            source = TestBinarySourceElement("func compute")
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = functionSymbol
            name = Name.identifier("compute")
            isMut = false
            valueParameters += buildValueParameter {
                source = TestBinarySourceElement("param value")
                moduleData = TestModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                dispatchReceiverType = null
                symbol = CfirValueParameterSymbol(CallableId(Name.identifier("value")))
                containingDeclarationSymbol = functionSymbol
                isNamed = false
                status = CfirDeclarationStatusImpl()
                returnTypeRef = buildImplicitTypeRef()
                name = Name.identifier("value")
            }.also { it.symbol.bind(it) }
        }

        declaration.symbol.bind(declaration)

        assertMatchesReadabilityRenderer(declaration)
    }

    @Test
    fun `file toString delegates to readability renderer`() {
        val file = buildFile {
            source = TestBinarySourceElement("file sample.cj")
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "sample.cj"
            packageDirective = buildPackageDirective {
                packageFqName = FqName("sample")
                isMacroPackage = false
            }
        }

        file.symbol.bind(file)

        assertMatchesReadabilityRenderer(file)
    }

    private fun assertMatchesReadabilityRenderer(declaration: CfirDeclaration) {
        val expected = CfirRenderer.withReadability().renderElementAsString(declaration)
        assertEquals(expected, declaration.toString())
    }

    private object TestSession : CfirSession(Kind.Source) {
        override fun toString(): String = "CfirDeclarationToStringTestSession"
    }

    private class TestBinarySourceElement(identity: String) : CjBinarySourceElement(
        debugText = identity,
        binaryFilePath = null,
        stableIdentity = identity,
    )

    private object TestScopeProvider : CfirScopeProvider() {
        override fun getUseSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = CfirTypeScope.Empty

        override fun getDeclarationSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = CfirTypeScope.Empty
    }

    private object TestModuleData : CfirModuleData() {
        override val name: Name = Name.identifier("cfir-tree-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = targetPlatform.isCommon()
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "cfir-tree-test"
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }
}
