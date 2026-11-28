package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

/** 官方 parser 的 This 位置门及复位必须在 PSI 和 LightTree 共用。 */
class ThisTypePositionParsingTest : CjParsingTestCase("", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition()) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun directClassReturnIsTheOnlyAllowedPosition() {
        val source = """
            class C {
                func ok(): This { return this }
                func param(x: This): Unit {}
                func nested(): Array<This> { [] }
            }
            extend C { func ext(): This { return this } }
            interface I { func method(): This }
            func outside(): This {}
        """.trimIndent()
        val message = CangJieParsingBundle.message("parsing.error.this.type.not.allowed")
        val expected = Regex("\\bThis\\b").findAll(source).drop(1).map { it.range.first to (it.range.last + 1) }.toList()
        val psi = CjPsiFactory(project, markGenerated = false).createFile(source)
        val psiDiagnostics = psi.collectParsingDiagnostics().filter { it.message == message }
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val lightDiagnostics = CangJieLightParser.parseBuffered(builder).diagnostics.filter { it.message == message }
        assertEquals(expected, psiDiagnostics.map { it.startOffset to it.endOffset })
        assertEquals(expected, lightDiagnostics.map { it.startOffset to it.endOffset })
    }

    @Test
    fun lazyFunctionBodyDoesNotInheritClassReturnPermission() {
        val source = "class C { func ok(): This { func local(): This {}\n return this } }"
        val expectedStart = source.indexOf("This", source.indexOf("func local"))
        val message = CangJieParsingBundle.message("parsing.error.this.type.not.allowed")
        val psi = CjPsiFactory(project, markGenerated = false).createFile(source)
        val psiDiagnostics = psi.collectParsingDiagnostics().filter { it.message == message }
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val lightDiagnostics = CangJieLightParser.parseBuffered(builder).diagnostics.filter { it.message == message }
        assertEquals(listOf(expectedStart to expectedStart + 4), psiDiagnostics.map { it.startOffset to it.endOffset })
        assertEquals(listOf(expectedStart to expectedStart + 4), lightDiagnostics.map { it.startOffset to it.endOffset })
    }

    @Test
    fun classStaticReturnAndOptionalReturnUseDifferentParserRules() {
        val source = "class C { static func staticMethod(): This {}\n func optional(): ?This { None } }"
        val expectedStart = source.indexOf("This", source.indexOf("optional"))
        val message = CangJieParsingBundle.message("parsing.error.this.type.not.allowed")
        val psi = CjPsiFactory(project, markGenerated = false).createFile(source)
        val psiDiagnostics = psi.collectParsingDiagnostics().filter { it.message == message }
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val lightDiagnostics = CangJieLightParser.parseBuffered(builder).diagnostics.filter { it.message == message }
        assertEquals(listOf(expectedStart to expectedStart + 4), psiDiagnostics.map { it.startOffset to it.endOffset })
        assertEquals(listOf(expectedStart to expectedStart + 4), lightDiagnostics.map { it.startOffset to it.endOffset })
    }
}
