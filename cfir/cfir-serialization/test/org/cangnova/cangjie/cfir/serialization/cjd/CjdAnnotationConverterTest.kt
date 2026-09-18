package org.cangnova.cangjie.cfir.serialization.cjd

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.*

/** 自包含语法 fixture；不依赖本机 SDK、宏或 declaration resolve。 */
class CjdAnnotationConverterTest : CjParsingTestCase("", "cj.d", CangJieDeclarationFileType, CangJieParserDefinition()) {
    @BeforeEach fun setupFixture() { setUp() }
    @AfterEach fun teardownFixture() { tearDown() }

    private val owner = CfirNamedFunctionSymbol(CallableId(FqName("sample"), Name.identifier("f")))
    private val converter = CjdAnnotationConverter.create()
    private val apiLevel = ClassId.topLevel(FqName("ohos.labels.APILevel"))
    private fun resolution(vararg classes: ClassId, implicit: Boolean = true) = object : CjdAnnotationResolutionContext {
        override fun findClassIds(fqName: FqName, organizationName: String?): List<ClassId> = classes.filter { it.asSingleFqName() == fqName }
        override val implicitSystemAnnotations: Set<FqName> = if (implicit) setOf(apiLevel.asSingleFqName()) else emptySet()
    }
    private fun index(text: String): CjdSidecarIndex = CjdSidecarParser.create().parse(createPsiFile("sidecar", text) as CjFile).also {
        assertTrue(it.isUsable, it.diagnostics.toString())
    }
    private fun convert(index: CjdSidecarIndex, resolution: CjdAnnotationResolutionContext = resolution()): CjdAnnotationConversionResult =
        converter.convert(index.declarations.first().annotations, owner, index.annotationContext, resolution, index.sourceId)

    @Test fun testPlatformIdentityAndDecodedNamedValues() {
        val result = convert(index("""
            package sample
            @!APILevel[since: "22", syscap: "SystemCapability.Test", flag: true, number: 0x16]
            func f(): Unit
        """.trimIndent()))
        val call = result.annotations.single()
        assertEquals(emptyList<CjdAnnotationConversionDiagnostic>(), result.diagnostics)
        assertEquals(CangjieAnnotationOrigin.SYSTEM_MACRO, call.annotationOrigin)
        assertNull(call.annotationKind)
        assertEquals(CangjieAnnotationIdentity.SystemMacro(apiLevel.asSingleFqName(), "APILevel"), call.annotationIdentity)
        assertEquals(apiLevel, (assertIs<CfirResolvedTypeRef>(call.typeRef).coneType as ConeClassLikeType).classId)
        assertSame(owner, call.containingDeclarationSymbol)
        assertEquals("22", (call.argumentMapping.mapping[Name.identifier("since")] as CfirLiteralExpression).value)
        assertEquals(true, (call.argumentMapping.mapping[Name.identifier("flag")] as CfirLiteralExpression).value)
        assertEquals("22", (call.argumentMapping.mapping[Name.identifier("number")] as CfirLiteralExpression).value.toString())
        assertTrue(call.argumentList.arguments.all { it is CfirNamedArgumentExpression })
    }

    @Test fun testUnknownExpressionsPreserveRawTextAndOrder() {
        val result = convert(index("""
            @!APILevel[since: "22", permission: "a" & "b", since: "23", future: call(1)]
            @!Unknown[value: false]
            func f(): Unit
        """.trimIndent()))
        val call = result.annotations.first()
        assertEquals(4, call.argumentList.arguments.size)
        assertEquals(listOf(0, 1, 2, 3), call.argumentView!!.entries.map { it.sourceOrder })
        assertEquals(CfirAnnotationArgumentStatus.DUPLICATE, call.argumentView!!.entries[2].status)
        val error = assertIs<CfirErrorExpression>((call.argumentList.arguments[1] as CfirNamedArgumentExpression).expression)
        assertEquals("\"a\" & \"b\"", (error.diagnostic as CjdAnnotationConversionDiagnostic).rawText)
        assertEquals("\"a\" & \"b\"", error.source!!.getElementTextInContextForDebug())
        assertIs<CfirErrorTypeRef>(result.annotations[1].typeRef)
        assertEquals(CangjieAnnotationIdentity.Unknown, result.annotations[1].annotationIdentity)
        assertEquals(CfirAnnotationResolveState.ERROR, result.annotations[1].annotationResolveState)
        assertEquals(4, result.diagnostics.size)
    }

    @Test fun testImportAliasAndShadowing() {
        val alias = index("import ohos.labels.APILevel as Level\n@!Level[since: \"22\"]\nfunc f(): Unit")
        assertEquals(apiLevel, convert(alias, resolution(apiLevel)).annotations.single().annotationClassId)
        val shadow = ClassId.topLevel(FqName("other.APILevel"))
        val imported = index("import other.APILevel\n@!APILevel[since: \"22\"]\nfunc f(): Unit")
        assertEquals(CangjieAnnotationIdentity.Custom(shadow.asSingleFqName()), convert(imported, resolution(shadow)).annotations.single().annotationIdentity)
        assertEquals(CangjieAnnotationIdentity.Unknown, convert(imported).annotations.single().annotationIdentity)
        val local = index("@!APILevel[since: \"22\"]\nfunc f(): Unit\nclass APILevel {}")
        assertEquals(CangjieAnnotationIdentity.Unknown, convert(local).annotations.single().annotationIdentity)
    }

    @Test fun testWildcardAmbiguityIsObservable() {
        val result = convert(index("import a.*\nimport b.*\n@!Label\nfunc f(): Unit"), resolution(
            ClassId.topLevel(FqName("a.Label")), ClassId.topLevel(FqName("b.Label"))))
        assertNull(result.annotations.single().annotationClassId)
        assertEquals(CjdAnnotationDiagnosticKind.AMBIGUOUS_ANNOTATION, result.diagnostics.single().kind)
    }

    @Test fun testLanguageRegistryAndFreshNodes() {
        val syntax = index("package std.core\n@Frozen\n@OverflowWrapping\n@ConstSafe\nfunc f(): Unit")
        val first = convert(syntax)
        val second = convert(syntax)
        assertEquals(listOf(BuiltInAnnotationKind.FROZEN, BuiltInAnnotationKind.NUMERIC_OVERFLOW, BuiltInAnnotationKind.CONSTSAFE), first.annotations.map { it.annotationKind })
        assertEquals(CangjieAnnotationIdentity.LanguageBuiltIn(BuiltInAnnotationKind.NUMERIC_OVERFLOW, "OverflowWrapping"), first.annotations[1].annotationIdentity)
        for (i in first.annotations.indices) {
            assertNotSame(first.annotations[i], second.annotations[i])
            assertNotSame(first.annotations[i].typeRef, second.annotations[i].typeRef)
            assertNotSame(first.annotations[i].argumentList, second.annotations[i].argumentList)
        }
        val outside = convert(index("package app.core\n@!ConstSafe\nfunc f(): Unit"))
        assertNull(outside.annotations.single().annotationKind)
    }

    @Test fun testEscapesAndParserOwnedOverflow() {
        val result = convert(index("@Deprecated[message: \"line\\nnext\"]\n@OverflowWrapping[checked]\nfunc f(): Unit"))
        assertEquals("line\nnext", (result.annotations[0].argumentMapping.mapping[Name.identifier("message")] as CfirLiteralExpression).value)
        assertIs<CfirNamedAccessExpression>(result.annotations[1].argumentList.arguments.single())
        assertEquals(emptyList<CjdAnnotationConversionDiagnostic>(), result.diagnostics)
    }
}
