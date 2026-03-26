package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ConeTypeUtilsTest {
    @Test
    fun `getConstructor returns self for structural rigid types`() {
        val functionType = ConeFuncType(
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
}
