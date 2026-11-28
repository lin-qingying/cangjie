package org.cangnova.cangjie.chir.core.type

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 覆盖 CHIR 名义类型成员布局的泛型实例化规则。
 */
class ChirTypeRelationsTest {
    /**
     * 官方 CHIR 表达式兼容检查保留 Nothing 与核心 Any 引用类型的规则。
     */
    @Test
    fun `official expression compatibility follows bottom and core Any rules`() {
        val nothingType = ChirResolvedTypeRef(ChirPrimitiveType.NOTHING)
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val coreAnyReference = ChirResolvedTypeRef(
            ChirRefType(
                ChirResolvedTypeRef(ChirClassType(ChirTypeDeclarationId.CORE_ANY)),
            ),
        )
        val concreteReference = ChirResolvedTypeRef(
            ChirRefType(
                ChirResolvedTypeRef(ChirClassType(ChirTypeDeclarationId("test", "Concrete"))),
            ),
        )

        assertTrue(ChirTypeRelations.isOfficialChirTypeCompatible(nothingType, intType))
        assertFalse(ChirTypeRelations.isOfficialChirTypeCompatible(intType, nothingType))
        assertTrue(ChirTypeRelations.isOfficialChirTypeCompatible(coreAnyReference, concreteReference))
        assertTrue(ChirTypeRelations.isOfficialChirTypeCompatible(concreteReference, coreAnyReference))
        assertFalse(ChirTypeRelations.isOfficialChirTypeCompatible(intType, concreteReference))
    }

    /**
     * 名义类型身份由包和声明共同确定，不能由显示名或内联布局决定。
     */
    @Test
    fun `nominal equivalence uses structured declaration identity`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val left = ChirResolvedTypeRef(
            ChirStructType(
                declarationId = ChirTypeDeclarationId("left", "Node"),
                fieldTypes = listOf(intType),
            ),
        )
        val sameIdentityDifferentLayout = ChirResolvedTypeRef(
            ChirStructType(
                declarationId = ChirTypeDeclarationId("left", "Node"),
                fieldTypes = listOf(boolType),
            ),
        )
        val sameNameDifferentPackage = ChirResolvedTypeRef(
            ChirStructType(
                declarationId = ChirTypeDeclarationId("right", "Node"),
                fieldTypes = listOf(intType),
            ),
        )

        assertTrue(ChirTypeRelations.areEquivalent(left, sameIdentityDifferentLayout))
        assertFalse(ChirTypeRelations.areEquivalent(left, sameNameDifferentPackage))
        assertFalse(ChirTypeRelations.constrain(left, sameNameDifferentPackage, mutableMapOf()))
    }

    /** matcher 映射只改写显式泛型身份，并递归用于复合类型比较。 */
    @Test
    fun `equivalence follows explicit matcher type parameter identities recursively`() {
        val commonTypeParameter = ChirResolvedTypeRef(
            ChirGenericType("T", semanticIdentity = "common.Box.T"),
        )
        val specificTypeParameter = ChirResolvedTypeRef(
            ChirGenericType("U", semanticIdentity = "specific.Box.U"),
        )
        val commonType = ChirResolvedTypeRef(
            ChirFunctionType(
                parameterTypes = listOf(
                    ChirResolvedTypeRef(
                        ChirClassType(
                            declarationId = ChirTypeDeclarationId("sample", "Box"),
                            typeArguments = listOf(commonTypeParameter),
                        ),
                    ),
                ),
                returnType = ChirResolvedTypeRef(ChirTupleType(listOf(commonTypeParameter))),
            ),
        )
        val specificType = ChirResolvedTypeRef(
            ChirFunctionType(
                parameterTypes = listOf(
                    ChirResolvedTypeRef(
                        ChirClassType(
                            declarationId = ChirTypeDeclarationId("sample", "Box"),
                            typeArguments = listOf(specificTypeParameter),
                        ),
                    ),
                ),
                returnType = ChirResolvedTypeRef(ChirTupleType(listOf(specificTypeParameter))),
            ),
        )

        assertTrue(
            ChirTypeRelations.areEquivalent(
                commonType,
                specificType,
                mapOf("common.Box.T" to "specific.Box.U"),
            ),
        )
        assertFalse(ChirTypeRelations.areEquivalent(commonType, specificType))
        assertFalse(
            ChirTypeRelations.areEquivalent(
                ChirResolvedTypeRef(ChirGenericType("T", semanticIdentity = "common.Unmapped")),
                ChirResolvedTypeRef(ChirGenericType("T", semanticIdentity = "specific.Unmapped")),
                mapOf("common.Box.T" to "specific.Box.U"),
            ),
        )
    }

    /**
     * 存储兼容性遵循 Nothing 底类型和泛型上界，而类型等价仍保持名义身份。
     */
    @Test
    fun `assignability follows bottom type and generic upper bounds`() {
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val nothingType = ChirResolvedTypeRef(ChirPrimitiveType.NOTHING)
        val interfaceType = ChirResolvedTypeRef(
            ChirClassType(ChirTypeDeclarationId("test", "Comparable")),
        )
        val boundedType = ChirResolvedTypeRef(
            ChirGenericType(
                identifier = "T",
                upperBounds = listOf(interfaceType),
                semanticIdentity = "Owner:T",
            ),
        )

        assertTrue(ChirTypeRelations.isAssignable(nothingType, boolType))
        assertTrue(ChirTypeRelations.isAssignable(boundedType, interfaceType))
        assertFalse(ChirTypeRelations.areEquivalent(nothingType, boolType))
    }

    /**
     * 类值可赋给其继承层级中已记录的父类型。
     */
    @Test
    fun `assignability follows direct nominal supertypes`() {
        val parentType = ChirResolvedTypeRef(
            ChirClassType(ChirTypeDeclarationId("test", "Parent")),
        )
        val childType = ChirResolvedTypeRef(
            ChirClassType(
                declarationId = ChirTypeDeclarationId("test", "Child"),
                superTypes = listOf(parentType),
            ),
        )

        assertTrue(ChirTypeRelations.isAssignable(childType, parentType))
        assertFalse(ChirTypeRelations.isAssignable(parentType, childType))
    }

    /**
     * 以名义类型的形参与实参替换字段和构造器 payload 中的嵌套类型。
     */
    @Test
    fun `instantiates member layouts by generic semantic identity`() {
        val parameter = ChirResolvedTypeRef(
            ChirGenericType(identifier = "T", semanticIdentity = "Box:T"),
        )
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val declaredMembers = listOf(
            parameter,
            ChirResolvedTypeRef(ChirTupleType(listOf(parameter, ChirResolvedTypeRef(ChirRefType(parameter))))),
        )

        val instantiatedMembers = ChirTypeRelations.instantiateMemberTypes(
            formalTypeParameters = listOf(parameter),
            actualTypeArguments = listOf(intType),
            memberTypes = declaredMembers,
        )

        assertEquals(
            listOf(
                intType,
                ChirResolvedTypeRef(
                    ChirTupleType(listOf(intType, ChirResolvedTypeRef(ChirRefType(intType)))),
                ),
            ),
            instantiatedMembers,
        )
    }

    /**
     * 沿 tuple 与 struct 布局解析字段路径，并识别 enum 标签投影。
     */
    @Test
    fun `projects field types through aggregate layout paths`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val uintType = ChirResolvedTypeRef(ChirPrimitiveType.UINT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val optionLikeType = ChirEnumType(
            declarationId = ChirTypeDeclarationId("test", "OptionLike"),
            cases = listOf(
                ChirEnumCaseType("None"),
                ChirEnumCaseType("Some", listOf(intType)),
            ),
        )
        val nestedType = ChirResolvedTypeRef(
            ChirStructType(
                declarationId = ChirTypeDeclarationId("test", "Nested"),
                fieldTypes = listOf(boolType, intType),
            ),
        )
        val tupleType = ChirResolvedTypeRef(ChirTupleType(listOf(intType, nestedType)))

        assertEquals(
            ChirTypeRelations.FieldProjection(boolType, isEnumSelector = false),
            ChirTypeRelations.projectFieldType(tupleType, listOf(1uL, 0uL)),
        )
        assertEquals(
            ChirTypeRelations.FieldProjection(uintType, isEnumSelector = true),
            ChirTypeRelations.projectFieldType(
                ChirResolvedTypeRef(ChirEnumType(ChirTypeDeclarationId("test", "Result"))),
                listOf(0uL),
            ),
        )
        assertEquals(
            ChirTypeRelations.FieldProjection(boolType, isEnumSelector = true),
            ChirTypeRelations.projectFieldType(ChirResolvedTypeRef(optionLikeType), listOf(0uL)),
        )
        assertEquals(
            ChirPrimitiveType.UINT32,
            optionLikeType.copy(nonExhaustive = true).selectorType,
        )
        assertEquals(null, ChirTypeRelations.projectFieldType(tupleType, listOf(2uL)))
    }
}
