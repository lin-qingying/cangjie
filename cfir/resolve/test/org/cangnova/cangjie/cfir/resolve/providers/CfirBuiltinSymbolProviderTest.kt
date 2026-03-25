package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirBuiltinSymbolProviderTest {
    @Test
    fun `builtin provider does not expose primitive types as class symbols`() {
        val (session, _) = ExtendTestFixtures.newSessionAndModule()
        val provider = CfirBuiltinSymbolProvider(session)
        val primitiveClassId = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier("Int64"))

        assertNull(provider.getClassLikeSymbolByClassId(primitiveClassId))
        assertEquals(emptySet<Name>(), provider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(StandardNames.BASIC_PACKAGE_FQ_NAME))
        assertTrue(provider.hasPackage(StandardNames.BASIC_PACKAGE_FQ_NAME))
    }

    @Test
    fun `primitive types are resolved from builtinTypes component`() {
        val (session, _) = ExtendTestFixtures.newSessionAndModule()

        val primitive = session.builtinTypes.getPrimitiveTypeByName("Int64")
        assertEquals(PrimitiveTypeKind.INT64, primitive?.kind)
    }
}
