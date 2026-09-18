package org.cangnova.cangjie.cfir.serialization.cjd

import org.junit.jupiter.api.Test
import kotlin.test.*

class DeclarationMatchKeyTest {
    @Test fun testOpaqueDoesNotBreakHashEquality() {
        val unknown = TypeKey.Opaque("THIS_TYPE", "This")
        val int = TypeKey.primitive("Int64")
        val bool = TypeKey.primitive("Bool")
        assertNotEquals<TypeKey>(unknown, int)
        assertNotEquals<TypeKey>(unknown, bool)
        assertEquals(3, setOf<TypeKey>(unknown, int, bool).size)
        assertTrue(unknown.matches(int))
        assertTrue(unknown.matches(bool))
        assertFalse(int.matches(unknown))
    }

    @Test fun testKindsAndGenericPresenceAreDistinct() {
        val klass = DeclarationMatchKey.TypeDecl("C", CjdDeclarationKind.CLASS)
        assertNotEquals(klass, klass.copy(kind = CjdDeclarationKind.STRUCT))
        assertNotEquals(klass, klass.copy(generic = CjdGenericSignature(emptyList())))
        val variable = DeclarationMatchKey.Variable("p", null)
        assertFalse(variable.matches(variable.copy(kind = CjdDeclarationKind.PROPERTY)))
        assertTrue(variable.matches(variable.copy(type = TypeKey.Ref("T"))))
        assertFalse(variable.copy(type = TypeKey.Ref("T")).matches(variable))
    }

    @Test fun testConstraintUpperBoundsAndDirectionalSuffix() {
        val constraints = listOf(
            CjdTypeConstraint(TypeKey.Ref("T"), listOf(TypeKey.Ref("A"))),
            CjdTypeConstraint(TypeKey.Ref("U"), listOf(TypeKey.Ref("B"))))
        val source = DeclarationMatchKey.Function("f", emptyList(), CjdGenericSignature(listOf("T", "U"), constraints))
        val target = source.copy(generic = source.generic!!.copy(constraints = constraints + constraints))
        assertNotEquals(source, target)
        assertTrue(source.matches(target))
        assertFalse(target.matches(source))
        assertFalse(source.matches(source.copy(generic = source.generic!!.copy(constraints = constraints.map { it.copy(upperBounds = listOf(TypeKey.Ref("X"))) })) ))
    }

    @Test fun testKnownTypeShapesAndParameterNames() {
        val int = TypeKey.primitive("Int64")
        val array = TypeKey.VArray(int, TypeKey.Constant("INTEGER", "3"))
        assertFalse(array.matches(array.copy(size = TypeKey.Constant("INTEGER", "4"))))
        val function = DeclarationMatchKey.Function("f", listOf(CjdParameterKey("x", TypeKey.Tuple(listOf(array, TypeKey.Option(int))))))
        assertTrue(function.matches(function.copy()))
        assertFalse(function.matches(function.copy(parameters = function.parameters.map { it.copy(name = "y") })))
        assertNotEquals(TypeKey.primitive("Rune"), TypeKey.primitive("UInt8"))
        assertFalse(TypeKey.primitive("Rune").matches(TypeKey.primitive("UInt8")))
    }
}
