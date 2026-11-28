package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructedClassTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 构造器与析构器的非法返回类型属于 parser 诊断，不改变 raw CFIR 的构造结果或 Unit。
 * 真实 own 泛型、where、参数和恢复后的函数体均来自当前声明，不继承外层类型的签名。
 */
@RunWith(JUnit3RunnerWithInners::class)
class ConstructorSignatureRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiNormalPreservesConstructorSignatures() = assertSignatures(BodyBuildingMode.NORMAL, false)
    fun testPsiLazyPreservesConstructorSignatures() = assertSignatures(BodyBuildingMode.LAZY_BODIES, false)
    fun testLightTreeNormalPreservesConstructorSignatures() = assertSignatures(BodyBuildingMode.NORMAL, true)
    fun testLightTreeLazyPreservesConstructorSignatures() = assertSignatures(BodyBuildingMode.LAZY_BODIES, true)

    private fun assertSignatures(mode: BodyBuildingMode, useLightTree: Boolean) {
        for (role in Role.entries) {
            val parameters = if (role == Role.FINALIZER) "()" else "(value: Int64)"
            val ownParameters = if (role == Role.FINALIZER) "()" else "(value: T)"
            val cases = listOf(
                Case("$parameters: Array<Int64>", 1, hasReturn = true),
                Case("<T>$ownParameters: Array<Int64> where T <: LocalBound", 1, ownGeneric = true, hasReturn = true),
                Case("<T>$ownParameters where T <: LocalBound", 0, ownGeneric = true),
                Case("$parameters where", 1, nonGenericWhere = true),
                Case("<T where T <: LocalBound", 2, ownGeneric = true, missingParameters = true),
                Case(
                    "<T where T <: LocalBound", 2, ownGeneric = true, missingParameters = true,
                    body = "func nested(value: Int64): Int64 { value }",
                ),
            ) + if (role == Role.FINALIZER) emptyList() else listOf(Case("$parameters:", 1))
            for (case in cases) {
                val text = source(role, case)
                val diagnostics = mutableListOf<ParsingDiagnostic>()
                val file = if (useLightTree) {
                    val sourceFile = CjInMemoryTextSourceFile("constructorSignatures.cj", null, text)
                    val builder = PsiBuilderFactory.getInstance().createBuilder(
                        CangJieParserDefinition(), CangJieLexer(), text,
                    )
                    val session = createTestSession()
                    val parsed = CangJieLightParser.parse(
                        builder,
                        errorListener = { diagnostics += it },
                        cjmpContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings),
                    )
                    LightTree2Cfir(session, session.cangjieScopeProvider, mode)
                        .buildPreMacroCfirFile(parsed, sourceFile, text.toSourceLinesMapping()).cfirFile
                } else {
                    val psi = createCjFile("constructorSignatures", text)
                    diagnostics += psi.collectParsingDiagnostics()
                    PsiRawCfirBuilder(createTestSession(), mode, CfirAnnotationMetadataBuilder()).buildCfirFile(psi).also {
                        assertEquals(diagnostics, psi.collectParsingDiagnostics(), text)
                    }
                }
                assertDiagnostics(diagnostics, text, role, case)
                assertRawSignature(file, text, role, case, mode)
            }
        }
    }

    private fun assertDiagnostics(diagnostics: List<ParsingDiagnostic>, text: String, role: Role, case: Case) {
        assertEquals(case.diagnosticCount, diagnostics.size, text)
        assertTrue(diagnostics.all { it.origin == ParsingDiagnostic.Origin.INPUT }, text)
        val invalidReturnMessage = CangJieParsingBundle.message(role.returnDiagnosticKey)
        if (case.hasReturn) {
            val diagnostic = diagnostics.single()
            kotlin.test.assertEquals(invalidReturnMessage, diagnostic.message, text)
            val start = text.indexOf("Array<Int64>")
            assertEquals(start, diagnostic.startOffset, text)
            assertEquals(start + "Array<Int64>".length, diagnostic.endOffset, text)
        } else {
            assertTrue(diagnostics.none { it.message == invalidReturnMessage }, text)
        }
        if (case.nonGenericWhere) {
            val diagnostic = diagnostics.single()
            kotlin.test.assertEquals(CangJieParsingBundle.message("parsing.error.unexpected.where"), diagnostic.message, text)
            val start = text.indexOf("where", text.indexOf(role.spelling + case.signature))
            assertEquals(start, diagnostic.startOffset, text)
            assertEquals(start + "where".length, diagnostic.endOffset, text)
        }
    }

    private fun assertRawSignature(
        file: CfirFile,
        text: String,
        role: Role,
        case: Case,
        mode: BodyBuildingMode,
    ) {
        val owner = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "Outer" }
        val outerParameter = assertIs<CfirTypeParameter>(owner.typeParameters.single())
        kotlin.test.assertEquals("U", outerParameter.name.asString(), text)
        assertSame(owner.symbol, outerParameter.containingDeclarationSymbol, text)
        assertEquals(listOf("OwnerBound"), outerParameter.bounds.map { sourceText(text, it.source) }, text)

        val function: CfirFunction = when (role) {
            Role.PRIMARY -> owner.declarations.filterIsInstance<CfirConstructor>().single { it.isPrimary }
            Role.SECONDARY -> owner.declarations.filterIsInstance<CfirConstructor>().single { !it.isPrimary }
            Role.FINALIZER -> owner.declarations.filterIsInstance<CfirFinalizer>().single()
        }
        if (role == Role.FINALIZER) {
            assertEquals(ConePrimitiveType.UNIT, assertIs<CfirResolvedTypeRef>(function.returnTypeRef).coneType, text)
        } else {
            assertIs<CfirImplicitTypeRef>(function.returnTypeRef, text)
            assertNull(function.returnTypeRef.source, text)
        }
        // 构造器先按 header 顺序引用 Outer 的参数，再保留错误输入中的自有泛型声明；finalizer 不借用。
        val constructedRefs = function.typeParameters.filterIsInstance<CfirConstructedClassTypeParameterRef>()
        assertEquals(
            if (role == Role.FINALIZER) emptyList() else listOf(outerParameter.symbol), constructedRefs.map { it.symbol }, text,
        )
        assertEquals(constructedRefs, function.typeParameters.take(constructedRefs.size), text)
        val ownParameters = function.typeParameters.filterIsInstance<CfirTypeParameter>()
        assertEquals(if (case.ownGeneric) listOf("T") else emptyList(), ownParameters.map { it.name.asString() }, text)
        if (case.ownGeneric) {
            val parameter = ownParameters.single()
            assertSame(function.symbol, parameter.containingDeclarationSymbol, text)
            kotlin.test.assertEquals("T", sourceText(text, parameter.source), text)
            assertEquals(listOf("LocalBound"), parameter.bounds.map { sourceText(text, it.source) }, text)
        }
        val expectedParameters = if (role == Role.FINALIZER || case.missingParameters) emptyList() else listOf("value")
        assertEquals(expectedParameters, function.valueParameters.map { it.name.asString() }, text)
        for (parameter in function.valueParameters) {
            assertSame(function.symbol, parameter.containingDeclarationSymbol, text)
            kotlin.test.assertEquals(if (case.ownGeneric) "T" else "Int64", sourceText(text, parameter.returnTypeRef.source), text)
        }

        // 惰性体仅验收占位对象；绝不读取 CfirLazyBlock 的 source/statements 触发展开。
        if (mode == BodyBuildingMode.LAZY_BODIES) {
            assertIs<CfirLazyBlock>(function.body, text)
        } else {
            val body = kotlin.test.assertNotNull(function.body, text)
            assertEquals(1, body.statements.size, text)
            val statement = body.statements.single()
            kotlin.test.assertEquals(case.body, sourceText(text, statement.source), text)
            if (case.missingParameters && case.body != DEFAULT_BODY) {
                // 缺失自身参数列表时，正文内局部函数的签名仍只属于该局部函数。
                val nested = assertIs<CfirNamedFunction>(statement, text)
                kotlin.test.assertEquals("nested", nested.name.asString(), text)
                assertTrue(nested.isLocal, text)
                assertTrue(nested.typeParameters.isEmpty(), text)
                val parameter = nested.valueParameters.single()
                kotlin.test.assertEquals("value", parameter.name.asString(), text)
                assertSame(nested.symbol, parameter.containingDeclarationSymbol, text)
                kotlin.test.assertEquals("value: Int64", sourceText(text, parameter.source), text)
                kotlin.test.assertEquals("Int64", sourceText(text, parameter.returnTypeRef.source), text)
                kotlin.test.assertEquals("Int64", sourceText(text, nested.returnTypeRef.source), text)
                val nestedBody = kotlin.test.assertNotNull(nested.body, text)
                kotlin.test.assertEquals("value", sourceText(text, nestedBody.statements.single().source), text)
            }
        }
        kotlin.test.assertEquals("${role.spelling}${case.signature} { ${case.body} }", sourceText(text, function.source), text)
        val after = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        kotlin.test.assertEquals("after", after.name.asString(), text)
        kotlin.test.assertEquals("func after(): Unit {}", sourceText(text, after.source), text)
        val following = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "Following" }
        assertTrue(following.typeParameters.isEmpty(), text)
        assertTrue(following.declarations.filterIsInstance<CfirConstructor>().single().typeParameters.isEmpty(), text)
    }

    private fun sourceText(text: String, source: org.cangnova.cangjie.source.CjSourceElement?): String {
        val actual = kotlin.test.assertNotNull(source, text)
        return text.substring(actual.startOffset, actual.endOffset)
    }

    private fun source(role: Role, case: Case): String = """
        package constructor_signatures

        interface OwnerBound {}
        interface LocalBound {}
        class Outer<U> where U <: OwnerBound {
            ${role.spelling}${case.signature} { ${case.body} }
            func after(): Unit {}
        }
        class Following {}
    """.trimIndent()

    private enum class Role(val spelling: String, val returnDiagnosticKey: String) {
        PRIMARY("Outer", "parsing.error.primary.constructor.return.type"),
        SECONDARY("init", "parsing.error.constructor.return.type"),
        FINALIZER("~init", "parsing.error.finalizer.return.type"),
    }

    private data class Case(
        val signature: String,
        val diagnosticCount: Int,
        val ownGeneric: Boolean = false,
        val hasReturn: Boolean = false,
        val nonGenericWhere: Boolean = false,
        val missingParameters: Boolean = false,
        val body: String = DEFAULT_BODY,
    )

    private companion object {
        const val DEFAULT_BODY = "let body = 1"
    }
}
