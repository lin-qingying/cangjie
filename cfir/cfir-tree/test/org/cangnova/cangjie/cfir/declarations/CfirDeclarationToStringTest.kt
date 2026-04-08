package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(CfirImplementationDetail::class)
class CfirDeclarationToStringTest {
    @Test
    fun `class toString delegates to readability renderer`() {
        val classId = org.cangnova.cangjie.name.ClassId(FqName("sample"), Name.identifier("Box"))
        val declaration = buildClass {
            moduleData = TestModuleData
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
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
        val declaration = buildNamedFunction {
            moduleData = TestModuleData
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirNamedFunctionSymbol(callableId)
            name = Name.identifier("compute")
            isMut = false
            valueParameters += buildValueParameter {
                moduleData = TestModuleData
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                dispatchReceiverType = null
                symbol = CfirValueParameterSymbol(CallableId(Name.identifier("value")))
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
            moduleData = TestModuleData
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "sample.cj"
            packageDirective = buildPackageDirective {
                packageFqName = FqName("sample")
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

    private object TestModuleData : CfirModuleData() {
        override val name: Name = Name.identifier("cfir-tree-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = true
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "cfir-tree-test"
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }
}
