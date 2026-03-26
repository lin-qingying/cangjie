package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CfirTypeRelationsTest {

    private val relations = CfirTypeRelations(CfirTypeRelationsTestContext())

    @Test
    fun `same primitive type is identical`() {
        val result = relations.compare(ConePrimitiveType.INT32, ConePrimitiveType.INT32)

        assertEquals(CfirTypeRelationResult.IDENTICAL, result)
        assertEquals(true, relations.isIdentical(ConePrimitiveType.INT32, ConePrimitiveType.INT32))
    }

    @Test
    fun `child type is subtype of parent`() {
        val result = relations.compare(TYPE_CHILD, TYPE_PARENT)

        assertEquals(CfirTypeRelationResult.SUBTYPE, result)
    }

    @Test
    fun `ideal int is compatible with int32 through conversion`() {
        val result = relations.compare(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT32)

        assertEquals(CfirTypeRelationResult.COMPATIBLE_WITH_CONVERSION, result)
        assertEquals(true, relations.isCompatible(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT32))
    }

    @Test
    fun `unrelated primitive types are incompatible`() {
        val result = relations.compare(ConePrimitiveType.BOOLEAN, ConePrimitiveType.INT32)

        assertEquals(CfirTypeRelationResult.INCOMPATIBLE, result)
    }

    @Test
    fun `quest type is compatible through fallback`() {
        val result = relations.compare(ConeQuestType(), ConePrimitiveType.INT32)

        assertEquals(CfirTypeRelationResult.COMPATIBLE_WITH_QUEST_FALLBACK, result)
        assertEquals(true, relations.canUseQuestFallback(ConeQuestType(), ConePrimitiveType.INT32))
    }

    @Test
    fun `placeholder type parameter is constrainable`() {
        val result = relations.compare(
            ConeTypeParameterType(ConeTypeParameterLookupTag("T"), isPlaceholder = true),
            ConePrimitiveType.INT32,
        )

        assertEquals(CfirTypeRelationResult.CONSTRAINABLE_BY_VARIABLES, result)
        assertEquals(
            true,
            relations.isConstrainableByVariables(
                ConeTypeParameterType(ConeTypeParameterLookupTag("T"), isPlaceholder = true),
                ConePrimitiveType.INT32,
            ),
        )
    }
}

private class CfirTypeRelationsTestContext : ConeTypeContext {
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> {
        if (type is ConeClassLikeType && type.classId == TYPE_CHILD.classId) {
            return listOf(TYPE_PARENT)
        }
        return emptyList()
    }

    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
        return a == b
    }
}

private val TYPE_PARENT = ConeClassLikeType(ConeClassLookupTagImpl(ClassId(FqName("test"), Name.identifier("Parent"))))
private val TYPE_CHILD = ConeClassLikeType(ConeClassLookupTagImpl(ClassId(FqName("test"), Name.identifier("Child"))))
