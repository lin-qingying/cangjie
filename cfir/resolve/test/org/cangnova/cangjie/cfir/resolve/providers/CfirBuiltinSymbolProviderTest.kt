package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirBuiltinSymbolProvider] 内建符号暴露行为测试。
 */
class CfirBuiltinSymbolProviderTest {
    /**
     * 构造注册了内建类型与 scope provider 组件的测试 session。
     */
    private fun newSession(): Pair<CfirSession, CfirModuleData> {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        session.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
        session.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        return session to moduleData
    }

    /**
     * 验证 provider 将 primitive 类型暴露为 class-like symbol。
     */
    @Test
    fun `builtin provider exposes primitive types as class-like symbols`() {
        val (session, _) = newSession()
        val provider = CfirBuiltinSymbolProvider(session)
        val primitiveClassId = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier("Int64"))

        val symbol = provider.getClassLikeSymbolByClassId(primitiveClassId) as? CfirPrimitiveTypeSymbol
        assertNotNull(symbol)
        assertEquals(PrimitiveTypeKind.INT64, symbol?.kind)
        assertEquals(ConePrimitiveType.INT64, symbol?.constructType())
        assertTrue(
            provider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(StandardNames.BASIC_PACKAGE_FQ_NAME)
                .orEmpty()
                .contains(Name.identifier("Int64"))
        )
        assertTrue(provider.hasPackage(StandardNames.BASIC_PACKAGE_FQ_NAME))
    }

    /**
     * 验证 primitive 类型可通过 session builtinTypes 组件解析。
     */
    @Test
    fun `primitive types are resolved from builtinTypes component`() {
        val (session, _) = newSession()

        val primitive = session.builtinTypes.getPrimitiveTypeByName("Int64")
        assertEquals(PrimitiveTypeKind.INT64, primitive?.kind)
    }

    /**
     * 验证 primitive symbol 暴露内建 operator 成员。
     */
    @Test
    fun `primitive symbol exposes builtin operator members`() {
        val (session, _) = newSession()
        val provider = CfirBuiltinSymbolProvider(session)
        val primitiveClassId = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier("Int64"))
        val symbol = provider.getClassLikeSymbolByClassId(primitiveClassId) ?: error("missing builtin Int64 symbol")
        val scope = CfirClassDeclaredMemberScope(symbol)
        val functions = mutableListOf<org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol<*>>()

        scope.processFunctionsByName(org.cangnova.cangjie.name.OperatorNameConventions.PLUS, functions::add)

        assertTrue(functions.isNotEmpty())
        assertEquals(primitiveClassId, functions.first().callableId.classId)
    }
}
