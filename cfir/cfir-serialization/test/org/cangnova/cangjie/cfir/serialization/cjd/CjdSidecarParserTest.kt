package org.cangnova.cangjie.cfir.serialization.cjd

import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

/** 共用 parser fixture 提供 application 与 PSI 服务，不启动 CFIR/session。 */
class CjdSidecarParserTest : CjParsingTestCase("", "cj.d", CangJieDeclarationFileType, CangJieParserDefinition()) {
    @BeforeEach fun setupFixture() { setUp() }
    @AfterEach fun teardownFixture() { tearDown() }

    private fun parseBoth(text: String): CjdSidecarIndex {
        val parser = CjdSidecarParser.create()
        val file = createPsiFile("sidecar", text) as CjFile
        val psi = parser.parse(file)
        val path = Files.createTempFile("cjd-index-", ".cj.d")
        try {
            Files.writeString(path, text)
            val light = parser.parse(path)
            assertEquals(psi.declarations, light.declarations)
            assertEquals(psi.annotationContext, light.annotationContext)
            kotlin.test.assertEquals(psi.diagnostics, light.diagnostics, text.take(150) + psi.diagnostics.joinToString { text.substring((it.range.startOffset - 80).coerceAtLeast(0), (it.range.endOffset + 80).coerceAtMost(text.length)) })
            return light
        } finally { Files.deleteIfExists(path) }
    }

    @Test fun testSystemAnnotationSyntaxAndDuplicates() {
        val text = """
            package sample
            @Frozen
            @!APILevel[since: "22", permission: "a" & "b", throwexception: true, future: call(1)]
            @!APILevel[since: "22"]
            public func f(@!APILevel[since: "22"] x: Int64): Unit
        """.trimIndent()
        val index = parseBoth(text)
        assertEquals(emptyList<CjdDiagnostic>(), index.diagnostics)
        val entry = index.declarations.single()
        assertEquals(listOf("Frozen", "APILevel", "APILevel"), entry.annotations.map { it.name })
        val api = entry.annotations[1]
        assertTrue(api.forcedCustom)
        assertTrue(api.rawText.startsWith("@!APILevel["))
        assertEquals(listOf("since", "permission", "throwexception", "future"), api.arguments.map { it.name })
        assertEquals(listOf("\"22\"", "\"a\" & \"b\"", "true", "call(1)"), api.arguments.map { it.expressionText })
        assertEquals(1, entry.parameterAnnotations.single().size)
        assertEquals(api.rawText, text.substring(api.range.startOffset, api.range.endOffset))
    }

    @Test fun testSignaturesAndNestedTypes() {
        val index = parseBoth("""
            package sample
            class Box<T> where T <: A & B {
                Box(x: T)
                init(x: Int64)
                public func f<U>(x!: pkg.Outer<T>.Inner<U>, cb: (Int64) -> ?T): Unit where U <: A
                prop value: T
                let stored: T
            }
            func f<T, U>(x: T, y: U): Unit where T <: A, U <: B
            func f<T, U>(x: U, y: T): Unit where T <: A, U <: B
            extend<T> Box<T> <: A & B where T <: A { func g(x: T): Unit }
            enum Choice { None | Some(Int64, String) }
            type Alias = Int64
        """.trimIndent())
        assertEquals(emptyList<CjdDiagnostic>(), index.diagnostics)
        val box = index.declarations.first()
        assertEquals(listOf("init", "init", "f", "value", "stored"), box.members.map { it.key.identifier })
        val function = box.members[2].key as DeclarationMatchKey.Function
        assertIs<TypeKey.Qualified>(function.parameters[0].type)
        assertEquals(TypeKey.Function(listOf(TypeKey.primitive("Int64")), TypeKey.Option(TypeKey.Ref("T"))), function.parameters[1].type)
        assertNotEquals(index.declarations[1].key, index.declarations[2].key)
        val extension = index.declarations[3].key as DeclarationMatchKey.Extend
        assertEquals(2, extension.inheritedTypes.size)
        assertEquals(1, extension.generic!!.constraints.size)
        val constructors = index.declarations[4].members
        assertIs<DeclarationMatchKey.Variable>(constructors[0].key)
        assertEquals(listOf("p1", "p2"), (constructors[1].key as DeclarationMatchKey.Function).parameters.map { it.name })
    }

    @Test fun testReturnModifiersDefaultsDoNotAffectKey() {
        val index = parseBoth("""
            public func f(x!: Rune = r'a'): Unit
            private func f(x: Rune): Int64
        """.trimIndent())
        assertEquals(emptyList<CjdDiagnostic>(), index.diagnostics)
        assertEquals(index.declarations[0].key, index.declarations[1].key)
        assertEquals(2, index.topLevel.values.single().size)
        assertEquals(2, index.findMatches(index.declarations.first().key).size)
    }

    @Test fun testPatternsVArrayAndAliasRemainStructural() {
        val index = parseBoth("""
            package sample
            @Frozen let (x, (y, _)): (Int64, (Bool, Int64))
            func a(x: VArray<Int64, ${'$'}3>): Unit
            func a(x: Alias): Unit
            func a(x: Int64): Unit
        """.trimIndent())
        assertEquals(emptyList<CjdDiagnostic>(), index.diagnostics)
        assertEquals(listOf("x", "y", "a", "a", "a"), index.declarations.map { it.key.identifier })
        assertEquals(TypeKey.primitive("Bool"), (index.declarations[1].key as DeclarationMatchKey.Variable).type)
        assertEquals(1, index.declarations[1].annotations.size)
        assertEquals(TypeKey.VArray(TypeKey.primitive("Int64"), TypeKey.Constant("INTEGER", "3")),
            (index.declarations[2].key as DeclarationMatchKey.Function).parameters.single().type)
        assertFalse(index.declarations[3].key.matches(index.declarations[4].key))
    }

    @Test fun testSyntaxErrorsAreObservable() {
        val index = parseBoth("class C { func }")
        assertTrue(index.diagnostics.any { it.kind == CjdDiagnosticKind.SYNTAX })
        assertFalse(index.isUsable)
        assertEquals(emptyList<CjdDeclarationEntry>(), index.findMatches(index.declarations.first().key))
    }

    @Test fun testBodylessMacroDeclarations() {
        val index = parseBoth("macro package sample\npublic macro M(input: Tokens): Tokens\npublic macro N(input: Tokens): Tokens")
        assertEquals(listOf("M", "N"), index.declarations.map { it.key.identifier })
        assertTrue(index.declarations.all { it.key.kind == CjdDeclarationKind.MACRO })
        assertEquals(emptyList<CjdDiagnostic>(), index.diagnostics)
        assertTrue(index.isUsable)
    }

    @Test fun testSoftKeywordInImportPath() {
        val index = parseBoth("package sample\nimport std.unittest.mock.internal.*\nimport std.collection.*\nfunc f(): Unit")
        assertEquals(emptyList<CjdDiagnostic>(), index.diagnostics)
        assertEquals("f", index.declarations.single().key.identifier)
    }

    @Test fun testDocumentationSubgrammarDoesNotProduceDeclarationErrors() {
        val index = parseBoth("""
            package sample
            /**
             * @param handle description
             */
            @!APILevel[since: "22"]
            public func f(handle: Int64): Unit
        """.trimIndent())
        assertEquals(emptyList<CjdDiagnostic>(), index.diagnostics)
        assertEquals("APILevel", index.declarations.single().annotations.single().name)
    }

    @Test fun testAnnotationContextAndExpressionSnapshot() {
        val index = parseBoth("""
            package acme::sample.library
            import ohos.labels.{APILevel as Level, Hide}
            import extras.*
            @!Level[since: "22", permission: "a" & "b"]
            func f(): Unit
            class APILevel {}
        """.trimIndent())
        assertTrue(index.isUsable, index.diagnostics.toString())
        assertEquals("sample.library", index.annotationContext.packageFqName)
        assertEquals("acme", index.annotationContext.organizationName)
        assertEquals(listOf(CjdAnnotationImport("ohos.labels.APILevel", alias = "Level"),
            CjdAnnotationImport("ohos.labels.Hide"), CjdAnnotationImport("extras", isAllUnder = true)), index.annotationContext.imports)
        assertEquals(setOf("f", "APILevel"), index.annotationContext.declaredNames)
        val arguments = index.declarations.first().annotations.single().arguments
        assertEquals("STRING_TEMPLATE", arguments[0].expression.syntaxKind)
        assertEquals("BINARY_EXPRESSION", arguments[1].expression.syntaxKind)
        assertTrue(arguments[1].expression.children.isNotEmpty())
    }

    @Test fun testParserOwnedArgumentsAreNotDropped() {
        val index = parseBoth("@OverflowWrapping[checked]\n@Attribute[Hot, \"Cold\"]\nfunc f(): Unit")
        assertTrue(index.isUsable, index.diagnostics.toString())
        val annotations = index.declarations.single().annotations
        assertEquals("ANNOTATION_OVERFLOW_STRATEGY", annotations[0].arguments.single().expression.syntaxKind)
        assertEquals(listOf("Hot", "\"Cold\""), annotations[1].arguments.map { it.expressionText })
    }

    @Test fun testConfiguredSdk() {
        val directory = System.getenv("CANGJIE_CJD_SDK_DIR")
        assumeTrue(!directory.isNullOrBlank(), "Set CANGJIE_CJD_SDK_DIR to enable real SDK validation")
        val paths = Files.list(Path.of(directory!!)).use { stream -> stream.filter { it.toString().endsWith(".cj.d") }.sorted().toList() }
        assertEquals(38, paths.size, "Expected the 38-file SDK evidence corpus")
        var annotationCount = 0
        val failures = mutableListOf<String>()
        for (path in paths) {
            val index = parseBoth(Files.readString(path))
            fun count(entries: List<CjdDeclarationEntry>): Int = entries.sumOf { entry ->
                entry.annotations.count { it.name == "APILevel" } + entry.parameterAnnotations.sumOf { list -> list.count { it.name == "APILevel" } } + count(entry.members)
            }
            annotationCount += count(index.declarations)
            if (index.diagnostics.isNotEmpty()) failures += "$path: ${index.diagnostics}"
        }
        println("SDK files=${paths.size}, APILevel=$annotationCount\n${failures.joinToString("\n")}")
        assertEquals(6497, annotationCount)
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
