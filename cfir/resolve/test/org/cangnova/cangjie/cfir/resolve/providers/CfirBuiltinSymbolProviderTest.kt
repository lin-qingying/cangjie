package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirBuiltinSymbolProviderTest {
    @Test
    fun `builtin provider exposes primitive types as class-like symbols`() {
        val (session, _) = ExtendTestFixtures.newSessionAndModule()
        val provider = CfirBuiltinSymbolProvider(session)
        val primitiveClassId = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier("Int64"))

        val symbol = provider.getClassLikeSymbolByClassId(primitiveClassId) as? CfirPrimitiveTypeSymbol
        assertNotNull(symbol)
        assertEquals(PrimitiveTypeKind.INT64, symbol?.kind)
        assertTrue(
            provider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(StandardNames.BASIC_PACKAGE_FQ_NAME)
                .orEmpty()
                .contains(Name.identifier("Int64"))
        )
        assertTrue(provider.hasPackage(StandardNames.BASIC_PACKAGE_FQ_NAME))
    }

    @Test
    fun `primitive types are resolved from builtinTypes component`() {
        val (session, _) = ExtendTestFixtures.newSessionAndModule()

        val primitive = session.builtinTypes.getPrimitiveTypeByName("Int64")
        assertEquals(PrimitiveTypeKind.INT64, primitive?.kind)
    }

    @Test
    fun `primitive symbol exposes builtin operator members`() {
        val (session, _) = ExtendTestFixtures.newSessionAndModule()
        val provider = CfirBuiltinSymbolProvider(session)
        val primitiveClassId = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier("Int64"))
        val symbol = provider.getClassLikeSymbolByClassId(primitiveClassId) ?: error("missing builtin Int64 symbol")
        val scope = CfirClassDeclaredMemberScope(symbol)
        val functions = mutableListOf<org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol<*>>()

        scope.processFunctionsByName(Name.identifier("plus"), functions::add)

        assertTrue(functions.isNotEmpty())
        assertEquals(primitiveClassId, provider.getContainingClassId(functions.first()))
    }
}
