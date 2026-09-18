package org.cangnova.cangjie.cfir.serialization.cjd

import PackageFormat.DeclKind
import PackageFormat.TypeKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CjdBinaryTypeAdapterTest {
    @Test
    fun `raw type adapter retains Option tuple function VArray and generic identity`() {
        val fixture = CjdBinaryFixture()
        val option = fixture.declaration("Option", DeclKind.EnumDecl)
        val generic = fixture.declaration("T", DeclKind.GenericParamDecl, topLevel = false)
        val int = fixture.primitive(TypeKind.Int64)
        val optionTy = fixture.named(TypeKind.Enum, option, listOf(int))
        val genericTy = fixture.named(TypeKind.Generic, generic)
        val tuple = fixture.compound(TypeKind.Tuple, listOf(int, genericTy))
        val array = fixture.compound(TypeKind.VArray, listOf(optionTy), size = 4)
        val function = fixture.compound(TypeKind.Func, listOf(tuple, array), result = optionTy)
        val adapter = CjdBinaryTypeAdapter.create(fixture.build("std.core"))
        val intKey = TypeKey.Primitive("Int64")
        val optional = TypeKey.Option(intKey)
        assertTrue(optional.matchesResolved(adapter.typeFromField(optionTy)))
        assertTrue(TypeKey.Ref("Option", listOf(intKey)).matchesResolved(adapter.typeFromField(optionTy)))
        assertTrue(TypeKey.Ref("T").matchesResolved(adapter.typeFromField(genericTy)))
        val tupleKey = TypeKey.Tuple(listOf(intKey, TypeKey.Ref("T")))
        val arrayKey = TypeKey.VArray(optional, TypeKey.Constant("INTEGER", "4"))
        assertTrue(TypeKey.Function(listOf(tupleKey, arrayKey), optional).matchesResolved(adapter.typeFromField(function)))
        assertFalse(arrayKey.copy(size = TypeKey.Constant("INTEGER", "5")).matchesResolved(adapter.typeFromField(array)))
        assertFalse(arrayKey.copy(size = TypeKey.Constant("INTEGER", "0x4")).matchesResolved(adapter.typeFromField(array)))
    }

    @Test
    fun `resolved qualified matching does not fabricate source base or compare lost arguments`() {
        val binary = CjdResolvedType.Named("Box", listOf(CjdResolvedType.Primitive("Int64")))
        val source = TypeKey.Qualified(TypeKey.Ref("unrecoverable"), "Box", listOf(TypeKey.Primitive("Bool")))
        assertTrue(source.matchesResolved(binary))
        assertFalse(source.copy(name = "Other").matchesResolved(binary))
        assertFalse(TypeKey.Ref("Box", listOf(TypeKey.Primitive("Bool"))).matchesResolved(binary))
        assertNotEquals(source, source.copy(base = TypeKey.Ref("other")))
    }

    @Test
    fun `unknown target and aliases never act as wildcard or trigger alias expansion`() {
        val fixture = CjdBinaryFixture()
        val underlying = fixture.declaration("Underlying", DeclKind.StructDecl)
        val alias = fixture.declaration("Alias", DeclKind.TypeAliasDecl)
        val underlyingType = fixture.named(TypeKind.Struct, underlying)
        val aliasType = fixture.named(TypeKind.Type, alias)
        val adapter = CjdBinaryTypeAdapter.create(fixture.build())
        assertFalse(TypeKey.Ref("Alias").matchesResolved(adapter.typeFromField(underlyingType)))
        assertInstanceOf(CjdResolvedType.Unknown::class.java, adapter.typeFromField(aliasType))
        assertFalse(TypeKey.Opaque("UNKNOWN", "Alias").matchesResolved(adapter.typeFromField(aliasType)))
        assertFalse(TypeKey.Opaque("UNKNOWN", "?").matchesResolved(adapter.typeFromField(UInt.MAX_VALUE)))
        assertFalse(TypeKey.Primitive("Int64").matchesResolved(adapter.typeFromField(999u)))
        assertTrue(TypeKey.Opaque("UNKNOWN", "?").matchesResolved(adapter.typeFromField(underlyingType)))
    }

    @Test
    fun `same spelling Option outside core is not optional syntax`() {
        val fixture = CjdBinaryFixture()
        val option = fixture.declaration("Option", DeclKind.EnumDecl)
        val int = fixture.primitive(TypeKind.Int64)
        val optionTy = fixture.named(TypeKind.Enum, option, listOf(int))
        val binary = CjdBinaryTypeAdapter.create(fixture.build("user.pkg")).typeFromField(optionTy)
        assertFalse(TypeKey.Option(TypeKey.Primitive("Int64")).matchesResolved(binary))
        assertTrue(TypeKey.Ref("Option", listOf(TypeKey.Primitive("Int64"))).matchesResolved(binary))
    }

    @Test
    fun `Rune retains its identity and independent adapter caches cannot leak`() {
        val runeFixture = CjdBinaryFixture()
        val rune = runeFixture.primitive(TypeKind.Rune)
        val intFixture = CjdBinaryFixture()
        val int = intFixture.primitive(TypeKind.Int64)
        assertEquals(rune, int)
        val one = CjdBinaryTypeAdapter.create(runeFixture.build())
        val two = CjdBinaryTypeAdapter.create(intFixture.build())
        assertTrue(TypeKey.primitive("Rune").matchesResolved(one.typeFromField(rune)))
        assertFalse(TypeKey.primitive("UInt8").matchesResolved(one.typeFromField(rune)))
        assertFalse(TypeKey.primitive("Rune").matchesResolved(CjdResolvedType.Primitive("UInt8")))
        assertFalse(TypeKey.primitive("UInt8").matchesResolved(two.typeFromField(int)))
        assertEquals(CjdResolvedType.Primitive("Unit"), one.typeFromField(0u))
    }

    @Test
    fun `recursive raw type does not recurse forever or resolve declarations`() {
        val fixture = CjdBinaryFixture()
        val array = fixture.compound(TypeKind.Array, listOf(1u))
        val adapter = CjdBinaryTypeAdapter.create(fixture.build()) { error("Array must not resolve a declaration") }
        val target = adapter.typeFromField(array)
        assertFalse(TypeKey.Ref("Array", listOf(TypeKey.Ref("Array"))).matchesResolved(target))
        assertEquals(target, adapter.typeFromField(array))
    }
}
