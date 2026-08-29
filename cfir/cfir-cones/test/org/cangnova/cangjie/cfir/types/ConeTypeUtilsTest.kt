package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 验证 CFIR cone 类型工具函数对刚性类型构造器的返回语义。
 */
class ConeTypeUtilsTest {
    /**
     * 验证类型层两个官方 builtin 谓词的成员集合彼此独立且保留 class/interface 差异。
     */
    @Test
    fun `builtin and extendable predicates follow official type sets`() {
        val classType = ConeClassLikeType(
            classLikeTag(ClassId(FqName("sample"), Name.identifier("Class")))
        )
        val interfaceType = ConeClassLikeType(
            classLikeTag(ClassId(FqName("sample"), Name.identifier("Interface"))),
            isInterface = true,
        )
        val rawArrayType = ConeClassLikeType(classLikeTag(StdlibClassIds.RawArray))
        val cFuncType = ConeFunctionType(emptyList(), ConePrimitiveType.UNIT, isCFunc = true)
        val pointerAlias = ConeTypeAliasType(
            classId = ClassId(FqName("sample"), Name.identifier("PointerAlias")),
            expandedType = ConePointerType(ConePrimitiveType.INT8),
        )

        assertTrue(ConePrimitiveType.INT32.isBuiltin)
        assertTrue(ConeVArrayType(ConePrimitiveType.INT32, size = 4).isBuiltin)
        assertTrue(rawArrayType.isBuiltin)
        assertFalse(ConeStructType(classLikeTag(StdlibClassIds.Array)).isBuiltin)
        assertFalse(cFuncType.isBuiltin)

        assertTrue(classType.isExtendable)
        assertFalse(interfaceType.isExtendable)
        assertTrue(rawArrayType.isExtendable)
        assertTrue(ConeStructType(classLikeTag(StdlibClassIds.Array)).isExtendable)
        assertTrue(ConePrimitiveType.INT32.isExtendable)
        assertTrue(ConePointerType(ConePrimitiveType.INT8).isExtendable)
        assertTrue(ConeCStringType().isExtendable)
        assertTrue(pointerAlias.isExtendable)
        assertFalse(ConeVArrayType(ConePrimitiveType.INT32, size = 4).isExtendable)
        assertFalse(cFuncType.isExtendable)
    }

    /**
     * 验证结构化刚性类型的构造器就是类型自身。
     */
    @Test
    fun `getConstructor returns self for structural rigid types`() {
        val functionType = ConeFunctionType(
            parameterTypes = listOf(ConePrimitiveType.INT32),
            returnType = ConePrimitiveType.BOOLEAN,
        )
        val tupleType = ConeTupleType(listOf(ConePrimitiveType.INT64, ConeCStringType()))
        val unionType = ConeUnionType(setOf(ConePrimitiveType.INT32, ConePrimitiveType.FLOAT64))
        val typeAliasType = ConeTypeAliasType(
            classId = ClassId(FqName("sample"), Name.identifier("Alias")),
        )

        assertSame(functionType, functionType.getConstructor())
        assertSame(tupleType, tupleType.getConstructor())
        assertSame(unionType, unionType.getConstructor())
        assertSame(typeAliasType, typeAliasType.getConstructor())
    }

    /**
     * 验证内建刚性类型和标记类型的构造器也是类型自身。
     */
    @Test
    fun `getConstructor returns self for builtin rigid markers`() {
        val pointerType = ConePointerType(ConePrimitiveType.INT8)
        val primitiveType = ConePrimitiveType.FLOAT32
        val idealIntType = ConeIdealIntConstantType(42)
        val placeholderType = ConePlaceholderType("T")

        assertSame(ConeAnyType, ConeAnyType.getConstructor())
        assertSame(pointerType, pointerType.getConstructor())
        assertSame(primitiveType, primitiveType.getConstructor())
        assertSame(idealIntType, idealIntType.getConstructor())
        assertSame(placeholderType, placeholderType.getConstructor())
    }

    /** 构造不依赖 cfir-tree 的测试用 class-like lookup tag。 */
    private fun classLikeTag(id: ClassId): ConeClassLikeLookupTag = object : ConeClassLikeLookupTag() {
        override val classId: ClassId = id
    }
}
