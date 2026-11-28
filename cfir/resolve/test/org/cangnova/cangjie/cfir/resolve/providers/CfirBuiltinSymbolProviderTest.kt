package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInTypeKind
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.symbols.CfirBuiltInTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirBuiltinSymbolProvider] 内建符号暴露行为测试。
 */
class CfirBuiltinSymbolProviderTest {
    /**
     * 官方 CreateCPointerDecl 只声明一个 T；缺少 pointee 时保留错误类型和诊断，不能变成 class-like。
     */
    @Test
    fun `builtin pointer without pointee produces error instead of class like type`() {
        val symbol = CfirBuiltInTypeSymbol(StdlibClassIds.CPointer, CfirBuiltInTypeKind.CPOINTER)

        val type = symbol.constructType()

        assertTrue(type is ConeErrorType)
        assertFalse(type is ConeClassLikeType)
        val error = type as ConeErrorType
        assertEquals("CPointer expects exactly one pointee type argument", error.diagnostic.reason)
        assertNull(error.delegatedType, "错误类型不能携带普通 class-like CPointer 作为替代载体")
    }

    /** 过量实参与缺少实参同属 arity 错误；不能截取第一个 pointee 后伪装成合法指针。 */
    @Test
    fun `builtin pointer with multiple pointees produces error instead of class like type`() {
        val symbol = CfirBuiltInTypeSymbol(StdlibClassIds.CPointer, CfirBuiltInTypeKind.CPOINTER)

        val type = symbol.constructType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.UINT8))

        assertTrue(type is ConeErrorType)
        assertFalse(type is ConeClassLikeType)
        val error = type as ConeErrorType
        assertEquals("CPointer expects exactly one pointee type argument", error.diagnostic.reason)
        assertNull(error.delegatedType)
    }

    /** 合法的唯一 pointee 必须原样进入专有 ConePointerType，包含已是指针类型的 CType 实参。 */
    @Test
    fun `builtin pointer keeps its pointee in the dedicated pointer type`() {
        val symbol = CfirBuiltInTypeSymbol(StdlibClassIds.CPointer, CfirBuiltInTypeKind.CPOINTER)
        val pointees = listOf(ConePrimitiveType.INT64, ConePrimitiveType.UINT8, ConePointerType(ConePrimitiveType.INT8))

        for (pointee in pointees) {
            val type = symbol.constructType(listOf(pointee))

            assertTrue(type is ConePointerType)
            assertFalse(type is ConeClassLikeType)
            assertFalse(type is ConeErrorType)
            assertSame(pointee, (type as ConePointerType).pointeeType)
        }
    }

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
        val (session, moduleData) = newSession()
        val provider = CfirBuiltinSymbolProvider(session, moduleData)
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
     * 验证五项官方 BuiltInDecl 都有稳定 ClassId、正确泛型参数以及统一的 `std.core` 名称索引。
     */
    @Test
    fun `builtin provider exposes official std core declarations`() {
        val (session, moduleData) = newSession()
        val provider = CfirBuiltinSymbolProvider(session, moduleData)

        CfirBuiltInTypeKind.entries.forEach { kind ->
            val symbol = provider.getClassLikeSymbolByClassId(kind.classId) as? CfirBuiltInTypeSymbol
            assertNotNull(symbol)

            val declaration = checkNotNull(symbol).cfir
            assertEquals(kind, declaration.kind)
            assertEquals(kind.typeParameterCount, declaration.typeParameters.size)
            assertTrue(declaration.declarations.isEmpty())
            assertTrue(declaration.superTypeRefs.isEmpty())
        }

        val expectedNames = CfirBuiltInTypeKind.entries
            .mapTo(linkedSetOf()) { it.classId.shortClassName }
        assertEquals(
            expectedNames,
            provider.symbolNamesProvider
                .getTopLevelClassifierNamesInPackage(StandardNames.STD_CORE_PACKAGE_FQ_NAME),
        )
        assertTrue(provider.hasPackage(StandardNames.STD_CORE_PACKAGE_FQ_NAME))
    }

    /**
     * 验证限定名解析能从 `std.core` 声明包推导出 `std` 命名空间前缀。
     */
    @Test
    fun `builtin provider exposes parent package namespace from package index`() {
        val (session, moduleData) = newSession()
        val provider = CfirBuiltinSymbolProvider(session, moduleData)

        assertFalse(provider.hasPackage(FqName("std")))
        assertTrue(provider.hasPackageOrSubpackages(FqName("std")))
        assertFalse(provider.hasPackageOrSubpackages(FqName("std.objectpool")))
    }

    /**
     * 验证 `CPointer<T>` 的类型参数保留官方 `T <: CType` 接口上界。
     */
    @Test
    fun `builtin pointer type parameter has CType upper bound`() {
        val (session, moduleData) = newSession()
        val provider = CfirBuiltinSymbolProvider(session, moduleData)
        val pointer = checkNotNull(
            provider.getClassLikeSymbolByClassId(StdlibClassIds.CPointer) as? CfirBuiltInTypeSymbol,
        ).cfir

        val typeParameter = pointer.typeParameters.single() as CfirTypeParameter
        val bound = typeParameter.bounds.single().coneTypeOrNull
        assertTrue(bound is ConeClassLikeType)
        val classLikeBound = bound as ConeClassLikeType
        assertEquals(StdlibClassIds.CType, classLikeBound.classId)
        assertTrue(classLikeBound.isInterface)
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
        val (session, moduleData) = newSession()
        val provider = CfirBuiltinSymbolProvider(session, moduleData)
        val primitiveClassId = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, Name.identifier("Int64"))
        val symbol = provider.getClassLikeSymbolByClassId(primitiveClassId) ?: error("missing builtin Int64 symbol")
        val scope = CfirClassDeclaredMemberScope(symbol)
        val functions = mutableListOf<org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol<*>>()

        scope.processFunctionsByName(org.cangnova.cangjie.name.OperatorNameConventions.PLUS, functions::add)

        assertTrue(functions.isNotEmpty())
        assertEquals(primitiveClassId, functions.first().callableId.classId)
    }
}
