package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConeSubtypeCheckerTest {

    /** 用于测试的 stub ConeTypeContext，通过手动注册超类型关系 */
    private class StubTypeContext : ConeTypeContext {
        private val supertypesMap = mutableMapOf<ConeCangjieType, List<ConeCangjieType>>()

        fun registerSupertypes(type: ConeCangjieType, supertypes: List<ConeCangjieType>) {
            supertypesMap[type] = supertypes
        }

        override fun supertypes(type: ConeCangjieType): Collection<ConeCangjieType> {
            return supertypesMap[type] ?: emptyList()
        }

        override fun isSameTypeConstructor(a: ConeCangjieType, b: ConeCangjieType): Boolean {
            return when {
                a is ConeClassLikeType && b is ConeClassLikeType -> a.classId == b.classId
                a is ConeStructType && b is ConeStructType -> a.classId == b.classId
                a is ConeEnumType && b is ConeEnumType -> a.classId == b.classId
                a is ConePrimitiveType && b is ConePrimitiveType -> a.kind == b.kind
                else -> a == b
            }
        }
    }

    private lateinit var ctx: StubTypeContext
    private lateinit var checker: ConeSubtypeChecker

    @BeforeEach
    fun setUp() {
        ctx = StubTypeContext()
        checker = ConeSubtypeChecker(ctx)
    }

    // ---- 辅助工厂方法 ----

    private fun classType(name: String, vararg args: ConeCangjieType): ConeClassLikeType {
        val classId = ClassId.topLevel(FqName.fromString("test.$name"))
        return ConeClassLikeType(ConeClassLookupTagImpl(classId), args.toList())
    }

    private fun interfaceType(name: String, vararg args: ConeCangjieType): ConeClassLikeType {
        val classId = ClassId.topLevel(FqName.fromString("test.$name"))
        return ConeClassLikeType(ConeClassLookupTagImpl(classId), args.toList(), isInterface = true)
    }

    private fun structType(name: String, vararg args: ConeCangjieType): ConeStructType {
        val classId = ClassId.topLevel(FqName.fromString("test.$name"))
        return ConeStructType(ConeClassLookupTagImpl(classId), args.toList())
    }

    private fun enumType(name: String, vararg args: ConeCangjieType): ConeEnumType {
        val classId = ClassId.topLevel(FqName.fromString("test.$name"))
        return ConeEnumType(ConeClassLookupTagImpl(classId), args.toList())
    }

    // ---- 规则 1: 同类型（reflexive） ----

    @Nested
    inner class ReflexiveRule {
        @Test
        fun `same primitive type is subtype of itself`() {
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.INT64, ConePrimitiveType.INT64))
        }

        @Test
        fun `different primitive types are not subtypes`() {
            assertFalse(checker.isSubtypeOf(ConePrimitiveType.INT64, ConePrimitiveType.INT32))
        }
    }

    // ---- 规则 2: Nothing ----

    @Nested
    inner class NothingRule {
        @Test
        fun `Nothing is subtype of any type`() {
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.NOTHING, ConePrimitiveType.INT64))
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.NOTHING, classType("Foo")))
        }

        @Test
        fun `non-Nothing is not subtype of Nothing`() {
            assertFalse(checker.isSubtypeOf(ConePrimitiveType.INT64, ConePrimitiveType.NOTHING))
        }
    }

    // ---- 规则 3: Any ----

    @Nested
    inner class AnyRule {
        @Test
        fun `any type is subtype of Any`() {
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.INT64, ConeAnyType))
            assertTrue(checker.isSubtypeOf(classType("Foo"), ConeAnyType))
        }

        @Test
        fun `Any is not subtype of concrete types`() {
            assertFalse(checker.isSubtypeOf(ConeAnyType, ConePrimitiveType.INT64))
        }
    }

    // ---- 规则 4: Error ----

    @Nested
    inner class ErrorRule {
        @Test
        fun `ErrorType is subtype of anything`() {
            val error = ConeErrorType("test error")
            assertTrue(checker.isSubtypeOf(error, ConePrimitiveType.INT64))
        }

        @Test
        fun `anything is subtype of ErrorType`() {
            val error = ConeErrorType("test error")
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.INT64, error))
        }
    }

    // ---- 规则 5: IdealType ----

    @Nested
    inner class IdealTypeRule {
        @Test
        fun `IdealInt is subtype of all integer types`() {
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT8))
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.INT64))
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.UINT32))
        }

        @Test
        fun `IdealInt is not subtype of non-integer types`() {
            assertFalse(checker.isSubtypeOf(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.FLOAT64))
            assertFalse(checker.isSubtypeOf(ConePrimitiveType.IDEAL_INT, ConePrimitiveType.BOOLEAN))
        }

        @Test
        fun `IdealFloat is subtype of all float types`() {
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.IDEAL_FLOAT, ConePrimitiveType.FLOAT16))
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.IDEAL_FLOAT, ConePrimitiveType.FLOAT64))
        }

        @Test
        fun `IdealFloat is not subtype of non-float types`() {
            assertFalse(checker.isSubtypeOf(ConePrimitiveType.IDEAL_FLOAT, ConePrimitiveType.INT64))
        }
    }

    // ---- 规则 6: 原始类型 ----

    @Nested
    inner class PrimitiveRule {
        @Test
        fun `same primitive kind is subtype`() {
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.BOOLEAN, ConePrimitiveType.BOOLEAN))
            assertTrue(checker.isSubtypeOf(ConePrimitiveType.RUNE, ConePrimitiveType.RUNE))
        }

        @Test
        fun `different primitive kinds are not subtypes`() {
            assertFalse(checker.isSubtypeOf(ConePrimitiveType.INT32, ConePrimitiveType.INT64))
            assertFalse(checker.isSubtypeOf(ConePrimitiveType.FLOAT32, ConePrimitiveType.FLOAT64))
        }
    }

    // ---- 规则 7: 类/接口 ----

    @Nested
    inner class ClassInterfaceRule {
        @Test
        fun `class is subtype of its supertype via BFS`() {
            val animal = interfaceType("Animal")
            val dog = classType("Dog")
            ctx.registerSupertypes(dog, listOf(animal))

            assertTrue(checker.isSubtypeOf(dog, animal))
        }

        @Test
        fun `class is not subtype of unrelated class`() {
            val cat = classType("Cat")
            val dog = classType("Dog")

            assertFalse(checker.isSubtypeOf(dog, cat))
        }

        @Test
        fun `transitive supertype via BFS`() {
            val a = interfaceType("A")
            val b = classType("B")
            val c = classType("C")
            ctx.registerSupertypes(c, listOf(b))
            ctx.registerSupertypes(b, listOf(a))

            assertTrue(checker.isSubtypeOf(c, a))
        }

        @Test
        fun `same class constructor with different type arguments is not subtype`() {
            val listOfInt = classType("List", ConePrimitiveType.INT64)
            val listOfStr = classType("List", classType("String"))

            assertFalse(checker.isSubtypeOf(listOfInt, listOfStr))
        }
    }

    // ---- 规则 8: 结构体 ----

    @Nested
    inner class StructRule {
        @Test
        fun `same struct is subtype of itself`() {
            val s = structType("Point")
            assertTrue(checker.isSubtypeOf(s, s))
        }

        @Test
        fun `different structs are not subtypes`() {
            val a = structType("PointA")
            val b = structType("PointB")
            assertFalse(checker.isSubtypeOf(a, b))
        }
    }

    // ---- 规则 9: 枚举 ----

    @Nested
    inner class EnumRule {
        @Test
        fun `same enum is subtype of itself`() {
            val e = enumType("Color")
            assertTrue(checker.isSubtypeOf(e, e))
        }

        @Test
        fun `different enums are not subtypes`() {
            val a = enumType("Color")
            val b = enumType("Shape")
            assertFalse(checker.isSubtypeOf(a, b))
        }
    }

    // ---- 规则 10: 函数类型 ----

    @Nested
    inner class FuncTypeRule {
        @Test
        fun `covariant return type`() {
            val animal = interfaceType("Animal")
            val dog = classType("Dog")
            ctx.registerSupertypes(dog, listOf(animal))

            // () -> Dog <: () -> Animal
            val subFunc = ConeFuncType(emptyList(), dog)
            val supFunc = ConeFuncType(emptyList(), animal)
            assertTrue(checker.isSubtypeOf(subFunc, supFunc))
        }

        @Test
        fun `contravariant parameter type`() {
            val animal = interfaceType("Animal")
            val dog = classType("Dog")
            ctx.registerSupertypes(dog, listOf(animal))

            // (Animal) -> Unit <: (Dog) -> Unit
            val subFunc = ConeFuncType(listOf(animal), ConePrimitiveType.UNIT)
            val supFunc = ConeFuncType(listOf(dog), ConePrimitiveType.UNIT)
            assertTrue(checker.isSubtypeOf(subFunc, supFunc))
        }

        @Test
        fun `different parameter count is not subtype`() {
            val f1 = ConeFuncType(listOf(ConePrimitiveType.INT64), ConePrimitiveType.UNIT)
            val f2 = ConeFuncType(emptyList(), ConePrimitiveType.UNIT)
            assertFalse(checker.isSubtypeOf(f1, f2))
        }

        @Test
        fun `non-covariant return is not subtype`() {
            val animal = interfaceType("Animal")
            val dog = classType("Dog")
            ctx.registerSupertypes(dog, listOf(animal))

            // () -> Animal 不是 () -> Dog 的子类型
            val subFunc = ConeFuncType(emptyList(), animal)
            val supFunc = ConeFuncType(emptyList(), dog)
            assertFalse(checker.isSubtypeOf(subFunc, supFunc))
        }
    }

    // ---- 规则 11: 元组 ----

    @Nested
    inner class TupleRule {
        @Test
        fun `same element types are subtypes`() {
            val t1 = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN))
            val t2 = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN))
            assertTrue(checker.isSubtypeOf(t1, t2))
        }

        @Test
        fun `different length tuples are not subtypes`() {
            val t1 = ConeTupleType(listOf(ConePrimitiveType.INT64))
            val t2 = ConeTupleType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN))
            assertFalse(checker.isSubtypeOf(t1, t2))
        }
    }

    // ---- 规则 12: 数组 ----

    @Nested
    inner class ArrayRule {
        @Test
        fun `same element type and dims is subtype`() {
            val a1 = ConeArrayType(ConePrimitiveType.INT64, dims = 1)
            val a2 = ConeArrayType(ConePrimitiveType.INT64, dims = 1)
            assertTrue(checker.isSubtypeOf(a1, a2))
        }

        @Test
        fun `different element types are not subtypes`() {
            val a1 = ConeArrayType(ConePrimitiveType.INT64)
            val a2 = ConeArrayType(ConePrimitiveType.INT32)
            assertFalse(checker.isSubtypeOf(a1, a2))
        }

        @Test
        fun `VArray same type and size is subtype`() {
            val a1 = ConeVArrayType(ConePrimitiveType.INT64, size = 10)
            val a2 = ConeVArrayType(ConePrimitiveType.INT64, size = 10)
            assertTrue(checker.isSubtypeOf(a1, a2))
        }

        @Test
        fun `VArray different size is not subtype`() {
            val a1 = ConeVArrayType(ConePrimitiveType.INT64, size = 10)
            val a2 = ConeVArrayType(ConePrimitiveType.INT64, size = 20)
            assertFalse(checker.isSubtypeOf(a1, a2))
        }
    }

    // ---- 规则 13: 类型参数 ----

    @Nested
    inner class TypeParameterRule {
        @Test
        fun `type parameter is subtype of its upper bound`() {
            val bound = interfaceType("Comparable")
            val tp = ConeTypeParameterType(
                ConeTypeParameterLookupTag("T"),
                upperBounds = listOf(bound),
            )
            assertTrue(checker.isSubtypeOf(tp, bound))
        }

        @Test
        fun `type parameter is not subtype of non-bound type`() {
            val bound = interfaceType("Comparable")
            val other = classType("Unrelated")
            val tp = ConeTypeParameterType(
                ConeTypeParameterLookupTag("T"),
                upperBounds = listOf(bound),
            )
            assertFalse(checker.isSubtypeOf(tp, other))
        }
    }

    // ---- 规则 14: 交叉类型 ----

    @Nested
    inner class IntersectionRule {
        @Test
        fun `T is subtype of intersection when T is subtype of all members`() {
            val a = interfaceType("A")
            val b = interfaceType("B")
            val c = classType("C")
            ctx.registerSupertypes(c, listOf(a, b))

            val intersection = ConeIntersectionType(listOf(a, b))
            assertTrue(checker.isSubtypeOf(c, intersection))
        }

        @Test
        fun `T is not subtype of intersection when T is not subtype of some member`() {
            val a = interfaceType("A")
            val b = interfaceType("B")
            val c = classType("C")
            ctx.registerSupertypes(c, listOf(a)) // C <: A 但 C 不是 B 的子类型

            val intersection = ConeIntersectionType(listOf(a, b))
            assertFalse(checker.isSubtypeOf(c, intersection))
        }

        @Test
        fun `intersection is subtype of T when any member is subtype of T`() {
            val a = interfaceType("A")
            val b = interfaceType("B")
            val intersection = ConeIntersectionType(listOf(a, b))

            assertTrue(checker.isSubtypeOf(intersection, a))
        }

        @Test
        fun `intersection is not subtype of T when no member is subtype of T`() {
            val a = interfaceType("A")
            val b = interfaceType("B")
            val c = classType("C")
            val intersection = ConeIntersectionType(listOf(a, b))

            assertFalse(checker.isSubtypeOf(intersection, c))
        }
    }

    // ---- 规则 15: 联合类型 ----

    @Nested
    inner class UnionRule {
        @Test
        fun `union is subtype of T when all members are subtypes of T`() {
            val animal = interfaceType("Animal")
            val dog = classType("Dog")
            val cat = classType("Cat")
            ctx.registerSupertypes(dog, listOf(animal))
            ctx.registerSupertypes(cat, listOf(animal))

            val union = ConeUnionType(setOf(dog, cat))
            assertTrue(checker.isSubtypeOf(union, animal))
        }

        @Test
        fun `union is not subtype of T when some member is not subtype of T`() {
            val animal = interfaceType("Animal")
            val dog = classType("Dog")
            val rock = classType("Rock")
            ctx.registerSupertypes(dog, listOf(animal))

            val union = ConeUnionType(setOf(dog, rock))
            assertFalse(checker.isSubtypeOf(union, animal))
        }

        @Test
        fun `T is subtype of union when T is subtype of any member`() {
            val intType = ConePrimitiveType.INT64
            val union = ConeUnionType(setOf(ConePrimitiveType.INT64, ConePrimitiveType.FLOAT64))
            assertTrue(checker.isSubtypeOf(intType, union))
        }

        @Test
        fun `T is not subtype of union when T is not subtype of any member`() {
            val boolType = ConePrimitiveType.BOOLEAN
            val union = ConeUnionType(setOf(ConePrimitiveType.INT64, ConePrimitiveType.FLOAT64))
            assertFalse(checker.isSubtypeOf(boolType, union))
        }
    }

    // ---- 规则 16: 柔性类型 ----

    @Nested
    inner class FlexibleRule {
        @Test
        fun `flexible subtype uses lower bound`() {
            val animal = interfaceType("Animal")
            val dog = classType("Dog")
            ctx.registerSupertypes(dog, listOf(animal))

            // Flexible(Dog, Animal) <: Animal → Dog <: Animal → true
            val flex = ConeFlexibleType(dog, animal)
            assertTrue(checker.isSubtypeOf(flex, animal))
        }

        @Test
        fun `flexible supertype uses upper bound`() {
            val animal = interfaceType("Animal")
            val dog = classType("Dog")
            ctx.registerSupertypes(dog, listOf(animal))

            // Dog <: Flexible(Dog, Animal) → Dog <: Animal → true
            val flex = ConeFlexibleType(dog, animal)
            assertTrue(checker.isSubtypeOf(dog, flex))
        }
    }
}
