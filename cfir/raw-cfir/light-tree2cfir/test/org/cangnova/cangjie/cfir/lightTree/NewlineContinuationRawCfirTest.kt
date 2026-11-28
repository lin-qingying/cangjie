package org.cangnova.cangjie.cfir.lightTree

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLetPatternExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 换行续接在两条 raw CFIR 构建路径上的回归：
 * `&&` 后换行 + 行首 `let`、行首 `+` 都不能把表达式截断。
 */
@RunWith(JUnit3RunnerWithInners::class)
class NewlineContinuationRawCfirTest : AbstractLightTree2CfirConverterTestCase() {

    fun testPsiIfLetChainContinuesAcrossNewlineBeforeLet() {
        val cfir = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
            .buildCfirFile(createCjFile("ifLetNewlineBeforeLet", IF_LET_SOURCE))
        assertIfLetChain(cfir)
    }

    fun testLightTreeIfLetChainContinuesAcrossNewlineBeforeLet() {
        assertIfLetChain(buildCfirFileFromLightTree(IF_LET_SOURCE, createTestSession(), "ifLetNewlineBeforeLet.cj"))
    }

    fun testPsiLeadingPlusStringConcatenationIsKept() {
        val cfir = PsiRawCfirBuilder(createTestSession(), BodyBuildingMode.NORMAL)
            .buildCfirFile(createCjFile("concatLeadingPlus", CONCAT_SOURCE))
        assertConcat(cfir)
    }

    fun testLightTreeLeadingPlusStringConcatenationIsKept() {
        assertConcat(buildCfirFileFromLightTree(CONCAT_SOURCE, createTestSession(), "concatLeadingPlus.cj"))
    }

    private fun assertIfLetChain(cfir: CfirFile) {
        val scan = scan(cfir)
        assertTrue(scan.errors.isEmpty(), "unexpected error expressions: ${scan.errors}")
        assertEquals(4, scan.letPatternExpressions, "else-if condition must keep all 4 let-pattern operands")
    }

    private fun assertConcat(cfir: CfirFile) {
        val scan = scan(cfir)
        assertTrue(scan.errors.isEmpty(), "unexpected error expressions: ${scan.errors}")
        assertEquals(2, scan.operatorPlusCalls, "both leading '+' must build '*operator_plus' calls")
        assertEquals(3, scan.stringLiterals, "all three string parts must survive")
    }

    private fun scan(cfir: CfirFile): ScanResult {
        val result = ScanResult()
        cfir.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                when (element) {
                    is CfirErrorExpression -> result.errors += element.diagnostic.reason
                    is CfirLetPatternExpression -> result.letPatternExpressions++
                    is CfirFunctionCall -> {
                        val callee = (element.calleeReference as? CfirNamedReference)?.name?.asString()
                        if (callee == OPERATOR_PLUS) result.operatorPlusCalls++
                    }
                    is CfirLiteralExpression -> if (element.kind == CfirLiteralKind.STRING) result.stringLiterals++
                }
                element.acceptChildren(this)
            }
        })
        return result
    }

    private class ScanResult {
        val errors = mutableListOf<String>()
        var letPatternExpressions = 0
        var operatorPlusCalls = 0
        var stringLiterals = 0
    }

    private companion object {
        private const val OPERATOR_PLUS = "*operator_plus"

        private val IF_LET_SOURCE = """
            enum PatternSignType {
                POS | NEG
            }

            func update(signType: PatternSignType, prefix: Option<String>, suffix: Option<String>) {
                var posPrefix: Option<String> = None
                var posSuffix: Option<String> = None
                for (i in 0..1) {
                    if (signType == PatternSignType.POS) {
                        posPrefix = prefix
                        posSuffix = suffix
                    } else if (let Some(pfx) <- prefix && let Some(ppfx) <- posPrefix && pfx == ppfx && let Some(sfx) <- suffix &&
                        let Some(psfx) <- posSuffix && sfx == psfx) {
                        continue
                    }
                }
            }
        """.trimIndent()

        private val CONCAT_SOURCE = """
            let description = "one: "
                + "many: "
                + "other: "
        """.trimIndent()
    }
}
