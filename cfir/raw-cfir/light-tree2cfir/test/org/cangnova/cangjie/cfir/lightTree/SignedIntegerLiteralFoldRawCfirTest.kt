package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * 源码里的 `-` + 整数字面量在 raw 构建期折叠成一个带符号字面量。
 *
 * 官方仓颉编译器 `ParserImpl::ParseNegativeLiteral`（`external/cangjie_compiler/src/Parse/ParseAtom.cpp:109-111,154-173`）
 * 在解析期把 `-` 与数字合成一个 `LitConstExpr`，`stringValue` 自带符号；Kotlin psi2fir 的
 * `convertUnaryPlusMinusCallOnIntegerLiteralIfNecessary`（`external/kotlin/compiler/fir/raw-fir/raw-fir.common/.../AbstractRawFirBuilder.kt:581-603`）
 * 在建 `FirLiteralExpression` 前把一元正负号折进字面量的值。CFIR 两条 raw 构建路径必须同形：
 * 否则检查器只能回扫源码文本才能还原符号，而 IDE 里 VFS 文本与 PSI 偏移不同源。
 *
 * 括号形态 `-(123)` 不折叠：官方 `ParseNegativeLiteral` 只在 `SUB` 后紧跟字面量词元时进入，
 * 遇到 `(` 退回一元表达式，内层按正字面量值域判断（cjc 1.0.5 探针
 * `var c: Int64 = -(9223372036854775808)` 报 LITERAL_NUMERIC_OVERFLOW，锚点只覆盖内层数字）。
 * 取负运算自身的溢出由 `CfirConstEvalArithmeticChecker` 报运算级诊断，与字面量级分属两个 owner。
 */
@RunWith(JUnit3RunnerWithInners::class)
class SignedIntegerLiteralFoldRawCfirTest : AbstractLightTree2CfirConverterTestCase() {

    fun testPsiFoldsNegativeIntegerLiteral() {
        val session = createTestSession()
        val file = PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL)
            .buildCfirFile(createCjFile("signed.cj", SOURCE))
        assertFoldedLiteral(file)
    }

    fun testLightTreeFoldsNegativeIntegerLiteral() {
        val session = createTestSession()
        val file = buildCfirFileFromLightTree(SOURCE, session, "signed.cj")
        assertFoldedLiteral(file)
    }

    fun testPsiKeepsParenthesizedBaseAsUnaryCall() {
        val session = createTestSession()
        val file = PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL)
            .buildCfirFile(createCjFile("parenthesized.cj", PARENTHESIZED_SOURCE))
        assertUnaryMinusCall(file)
    }

    fun testLightTreeKeepsParenthesizedBaseAsUnaryCall() {
        val session = createTestSession()
        val file = buildCfirFileFromLightTree(PARENTHESIZED_SOURCE, session, "parenthesized.cj")
        assertUnaryMinusCall(file)
    }

    private fun assertFoldedLiteral(file: CfirFile) {
        val literal = assertIs<CfirLiteralExpression>(singleStatement(file, "f"))
        assertEquals(CfirLiteralKind.INT, literal.kind)
        assertEquals("-9223372036854775808", literal.value)
    }

    private fun assertUnaryMinusCall(file: CfirFile) {
        val call = assertIs<CfirFunctionCall>(singleStatement(file, "f"))
        val reference = assertIs<CfirNamedReference>(call.calleeReference)
        assertEquals("*operator_unaryMinus", reference.name.asString())
    }

    private fun singleStatement(file: CfirFile, functionName: String): CfirStatement {
        val function = file.declarations
            .filterIsInstance<CfirNamedFunction>()
            .first { it.name.asString() == functionName }
        return requireNotNull(function.body) { "function $functionName must have a body" }.statements.single()
    }

    private companion object {
        private val SOURCE = """
            func f() {
                -9223372036854775808
            }
        """.trimIndent()

        private val PARENTHESIZED_SOURCE = """
            func f() {
                -(9223372036854775808)
            }
        """.trimIndent()
    }
}
