package org.cangnova.cangjie.cfir.serialization.cjd

import PackageFormat.DeclKind
import PackageFormat.TypeKind
import org.cangnova.cangjie.metadata.model.Attribute
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CjdBinaryDeclarationMatcherTest {
    private val intType = TypeKey.Primitive("Int64")
    private fun function(name: String, parameter: String = "x", type: TypeKey = intType, generic: CjdGenericSignature? = null) =
        DeclarationMatchKey.Function(name, listOf(CjdParameterKey(parameter, type)), generic)

    @Test
    fun `overloads require ordered parameter names and resolved types`() {
        val fixture = CjdBinaryFixture()
        val int = fixture.primitive(TypeKind.Int64)
        val bool = fixture.primitive(TypeKind.Bool)
        val x = fixture.declaration("x", DeclKind.FuncParam, int, topLevel = false)
        val y = fixture.declaration("y", DeclKind.FuncParam, int, topLevel = false)
        val b = fixture.declaration("x", DeclKind.FuncParam, bool, topLevel = false)
        val fx = fixture.function("f", listOf(x))
        val fy = fixture.function("f", listOf(y))
        val fb = fixture.function("f", listOf(b))
        val entry = cjdBinaryTestEntry(function("f"))
        val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(entry))
        assertSame(entry, matcher.selection(fx.toInt() - 1)?.entry)
        assertNull(matcher.selection(fy.toInt() - 1))
        assertNull(matcher.selection(fb.toInt() - 1))
        assertTrue(matcher.diagnostics.any { it.kind == CjdBinaryMatchDiagnosticKind.MISSING })
    }

    @Test
    fun `class owner match makes constructor and parameter annotations available before parent publication`() {
        val fixture = CjdBinaryFixture()
        val int = fixture.primitive(TypeKind.Int64)
        val p = fixture.declaration("x", DeclKind.FuncParam, int, topLevel = false)
        val ctor = fixture.function("init", listOf(p), topLevel = false)
        val member = fixture.declaration("value", type = int, topLevel = false)
        val owner = fixture.classDecl("Box", listOf(ctor, member))
        val parameterAnno = cjdBinaryTestAnnotation("Parameter")
        val ctorEntry = cjdBinaryTestEntry(function("init"), parameters = listOf(listOf(parameterAnno)))
        val memberEntry = cjdBinaryTestEntry(DeclarationMatchKey.Variable("value", intType))
        val ownerEntry = cjdBinaryTestEntry(DeclarationMatchKey.TypeDecl("Box", CjdDeclarationKind.CLASS), members = listOf(ctorEntry, memberEntry))
        val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(ownerEntry))
        // 无需先查询/物化父声明。参数先于构造函数、构造函数先于 class 发布。
        assertEquals(listOf(parameterAnno), matcher.selection(p.toInt() - 1)?.annotations)
        assertEquals(0, matcher.selection(p.toInt() - 1)?.parameterIndex)
        assertSame(ctorEntry, matcher.selection(ctor.toInt() - 1)?.entry)
        assertSame(memberEntry, matcher.selection(member.toInt() - 1)?.entry)
        assertSame(ownerEntry, matcher.selection(owner.toInt() - 1)?.entry)
        assertTrue(matcher.diagnostics.isEmpty(), matcher.diagnostics.toString())
    }

    @Test
    fun `top level annotation gate and private owner block members`() {
        for (exported in listOf(true, false)) {
            val fixture = CjdBinaryFixture()
            val child = fixture.function("f", emptyList(), topLevel = false)
            val owner = fixture.classDecl("Box", listOf(child), annotationsExported = exported)
            val entry = cjdBinaryTestEntry(DeclarationMatchKey.TypeDecl("Box", CjdDeclarationKind.CLASS),
                annotations = if (exported) emptyList() else listOf(cjdBinaryTestAnnotation("Private")),
                members = listOf(cjdBinaryTestEntry(DeclarationMatchKey.Function("f", emptyList()))))
            val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(entry))
            assertNull(matcher.selection(child.toInt() - 1))
            assertNull(matcher.selection(owner.toInt() - 1))
        }
    }

    @Test
    fun `internal protected and default export while builtin never matches`() {
        val fixture = CjdBinaryFixture()
        val a = fixture.declaration("a", attributes = listOf(Attribute.INTERNAL))
        val b = fixture.declaration("b", attributes = listOf(Attribute.PROTECTED))
        val c = fixture.declaration("c", attributes = emptyList())
        val d = fixture.declaration("d", attributes = listOf(Attribute.PRIVATE))
        val builtin = fixture.declaration("Array", DeclKind.BuiltInDecl)
        val entries = listOf("a", "b", "c", "d", "Array").map { cjdBinaryTestEntry(DeclarationMatchKey.Variable(it, null)) }
        val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(*entries.toTypedArray()))
        listOf(a, b, c).forEach { assertNotNull(matcher.selection(it.toInt() - 1)) }
        listOf(d, builtin).forEach { assertNull(matcher.selection(it.toInt() - 1)) }
        assertTrue(matcher.diagnostics.any { it.kind == CjdBinaryMatchDiagnosticKind.NON_EXPORTED })
    }

    @Test
    fun `all ambiguous source candidates are exposed but none authorizes a merge`() {
        val fixture = CjdBinaryFixture()
        val v = fixture.declaration("v")
        val first = cjdBinaryTestEntry(DeclarationMatchKey.Variable("v", null))
        val second = first.copy(annotations = listOf(cjdBinaryTestAnnotation("Other")))
        val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(first, second))
        assertEquals(listOf(first, second), matcher.candidates(v.toInt() - 1))
        assertNull(matcher.selection(v.toInt() - 1))
        assertEquals(CjdBinaryMatchDiagnosticKind.AMBIGUOUS, matcher.diagnostics.single().kind)
    }

    @Test
    fun `one wildcard source cannot annotate multiple binary declarations`() {
        val fixture = CjdBinaryFixture()
        val a = fixture.declaration("v", type = fixture.primitive(TypeKind.Int64))
        val b = fixture.declaration("v", type = fixture.primitive(TypeKind.Bool))
        val entry = cjdBinaryTestEntry(DeclarationMatchKey.Variable("v", null))
        val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(entry))
        listOf(a, b).forEach {
            assertEquals(listOf(entry), matcher.candidates(it.toInt() - 1))
            assertNull(matcher.selection(it.toInt() - 1))
        }
        assertTrue(matcher.diagnostics.all { it.kind == CjdBinaryMatchDiagnosticKind.AMBIGUOUS })
    }

    @Test
    fun `generics preserve presence names constraint groups and ordered bounds`() {
        val fixture = CjdBinaryFixture()
        val t = fixture.declaration("T", DeclKind.GenericParamDecl, topLevel = false)
        val tType = fixture.named(TypeKind.Generic, t)
        val int = fixture.primitive(TypeKind.Int64)
        val bool = fixture.primitive(TypeKind.Bool)
        val p = fixture.declaration("x", DeclKind.FuncParam, tType, topLevel = false)
        val generic = fixture.generic(listOf(t), listOf(tType to listOf(int, bool), tType to listOf(int, bool)))
        val f = fixture.function("f", listOf(p), generic = generic)
        val sourceGeneric = CjdGenericSignature(listOf("T"), listOf(CjdTypeConstraint(TypeKey.Ref("T"), listOf(intType, TypeKey.Primitive("Bool")))))
        val key = function("f", type = TypeKey.Ref("T"), generic = sourceGeneric)
        val pkg = fixture.build()
        val entry = cjdBinaryTestEntry(key)
        assertNotNull(CjdBinaryDeclarationMatcher.create(pkg, cjdBinaryTestIndex(entry)).selection(f.toInt() - 1))
        val wrong = listOf(
            key.copy(generic = null),
            key.copy(generic = sourceGeneric.copy(parameterNames = listOf("U"))),
            key.copy(generic = sourceGeneric.copy(constraints = emptyList())),
            key.copy(generic = sourceGeneric.copy(constraints = listOf(sourceGeneric.constraints.single().copy(upperBounds = listOf(TypeKey.Primitive("Bool"), intType))))),
        )
        wrong.forEach { assertNull(CjdBinaryDeclarationMatcher.create(pkg, cjdBinaryTestIndex(cjdBinaryTestEntry(it))).selection(f.toInt() - 1)) }
    }

    @Test
    fun `empty generic is distinct from absent generic and contexts do not share selections`() {
        val fixture = CjdBinaryFixture()
        val f = fixture.function("f", emptyList(), generic = fixture.generic(emptyList()))
        val pkg = fixture.build()
        val present = cjdBinaryTestEntry(DeclarationMatchKey.Function("f", emptyList(), CjdGenericSignature(emptyList())))
        val absent = cjdBinaryTestEntry(DeclarationMatchKey.Function("f", emptyList()))
        val one = CjdBinaryDeclarationMatcher.create(pkg, cjdBinaryTestIndex(present))
        val two = CjdBinaryDeclarationMatcher.create(pkg, cjdBinaryTestIndex(absent))
        assertSame(present, one.selection(f.toInt() - 1)?.entry)
        assertNull(two.selection(f.toInt() - 1))
        assertSame(present, one.selection(f.toInt() - 1)?.entry)
    }

    @Test
    fun `shared raw parameter ownership is rejected before any annotation is selected`() {
        val fixture = CjdBinaryFixture()
        val p = fixture.declaration("x", DeclKind.FuncParam, fixture.primitive(TypeKind.Int64), topLevel = false)
        fixture.function("f", listOf(p))
        fixture.function("g", listOf(p))
        val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(
            cjdBinaryTestEntry(function("f")), cjdBinaryTestEntry(function("g"))))
        assertNull(matcher.selection(p.toInt() - 1))
        assertTrue(matcher.diagnostics.any { it.declarationIndex == p.toInt() - 1 && it.kind == CjdBinaryMatchDiagnosticKind.AMBIGUOUS })
    }

    @Test
    fun `macro declarations are unsupported rather than mistaken for functions`() {
        val fixture = CjdBinaryFixture()
        fixture.function("m", emptyList())
        val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(
            cjdBinaryTestEntry(DeclarationMatchKey.TypeDecl("m", CjdDeclarationKind.MACRO))))
        assertNull(matcher.selection(0))
        assertTrue(matcher.diagnostics.any { it.kind == CjdBinaryMatchDiagnosticKind.UNSUPPORTED })
    }

    @Test
    fun `raw tuple pattern bindings select annotations without materializing wrapper`() {
        val fixture = CjdBinaryFixture()
        val int = fixture.primitive(TypeKind.Int64)
        val x = fixture.declaration("x", type = int, topLevel = false)
        val y = fixture.declaration("y", type = int, topLevel = false)
        val wrapper = fixture.pattern(listOf(x, y))
        val xEntry = cjdBinaryTestEntry(DeclarationMatchKey.Variable("x", intType))
        val yEntry = cjdBinaryTestEntry(DeclarationMatchKey.Variable("y", intType))
        val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(xEntry, yEntry))
        assertSame(xEntry, matcher.selection(x.toInt() - 1)?.entry)
        assertSame(yEntry, matcher.selection(y.toInt() - 1)?.entry)
        assertNull(matcher.selection(wrapper.toInt() - 1))
    }

    @Test
    fun `unsupported extend metadata is not guessed to be exported`() {
        val fixture = CjdBinaryFixture()
        val ext = fixture.extend(fixture.primitive(TypeKind.Type))
        val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(
            cjdBinaryTestEntry(DeclarationMatchKey.Extend(TypeKey.Ref("Alias"), emptyList()))))
        assertNull(matcher.selection(ext.toInt() - 1))
        assertTrue(matcher.diagnostics.any { it.kind == CjdBinaryMatchDiagnosticKind.UNSUPPORTED })
    }

    @Test
    fun `direct extension export follows actual extended declaration package and visibility`() {
        for (isPrivate in listOf(false, true)) {
            val fixture = CjdBinaryFixture()
            val target = fixture.declaration("Box", DeclKind.ClassDecl, attributes = listOf(if (isPrivate) Attribute.PRIVATE else Attribute.INTERNAL))
            val type = fixture.named(TypeKind.Class, target)
            val ext = fixture.extend(type)
            val entry = cjdBinaryTestEntry(DeclarationMatchKey.Extend(TypeKey.Ref("Box"), emptyList()))
            val matcher = CjdBinaryDeclarationMatcher.create(fixture.build(), cjdBinaryTestIndex(entry))
            assertEquals(!isPrivate, matcher.selection(ext.toInt() - 1) != null)
        }
    }
}
