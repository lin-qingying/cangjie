package org.cangnova.cangjie.psi

import com.intellij.psi.impl.DebugUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 换行符不变量：`CRLF` 与 `LF` 版本的同一份源码必须产生逐字符相同的 PSI 树。
 *
 * 平台换行符不得影响仓颉源码的语义。若此断言失败，说明 `\r` 泄漏进了词法/语法层，
 * 下游任何按偏移或按 token 序列消费源码的逻辑都会在 CRLF 输入下失准。
 */
class LineEndingEquivalenceParsingTest : CjParsingTestCase(
    "",
    "cj",
    CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    /**
     * 同一源码的 LF 版与 CRLF 版必须解析出完全等价的 PSI 树。
     *
     * 覆盖枚举项、extend 的 where 子句、字符串字面量与 match 表达式——这几处的文法
     * 对 token 序列最敏感，是 `\r` 泄漏最容易显形的位置。
     *
     * 比较前把 CRLF 折回 LF：`PsiWhiteSpace` 的文本是对输入的逐字镜像，本就应随输入变化；
     * 而**树结构**与**非空白 token 的文本**必须完全一致——若不一致，说明 `\r` 泄漏进了词法层，
     * 下游任何按 token 序列消费源码的逻辑都会在 CRLF 输入下失准。
     */
    @Test
    fun crlfAndLfProduceIdenticalPsi() {
        val lfSource = buildString {
            appendLine("enum TimeUnit {")
            appendLine("    | Year(Int32)")
            appendLine("    | Empty")
            appendLine("}")
            appendLine()
            appendLine("interface Display {}")
            appendLine()
            appendLine("extend<T> Wrapper<T> <: Display where T <: Display {}")
            appendLine()
            appendLine("func extractYear(time: TimeUnit): Int32 {")
            appendLine("""    let text = "brace {0} and dollar $ not special" """.trimEnd())
            appendLine("    match (time) {")
            appendLine("        case Year(x) => x")
            appendLine("    }")
            appendLine("}")
        }
        val crlfSource = lfSource.replace("\n", "\r\n")

        val lfDump = dumpNormalized(createPsiFile("lineEndingEquivalence", lfSource) as CjFile)
        val crlfDump = dumpNormalized(createPsiFile("lineEndingEquivalence", crlfSource) as CjFile)

        if (lfDump != crlfDump) {
            val divergence = firstDivergence(lfDump, crlfDump)
            val from = maxOf(0, divergence - 140)
            val until = minOf(minOf(lfDump.length, crlfDump.length), divergence + 140)
            fail(
                "CRLF 与 LF 的 PSI 树在第 $divergence 字符处分歧" +
                    "（LF 长 ${lfDump.length}，CRLF 长 ${crlfDump.length}）\n" +
                    "LF  : ...${lfDump.substring(from, until)}...\n" +
                    "CRLF: ...${crlfDump.substring(from, until)}..."
            )
        }
    }

    /** 首个不同的字符下标；一侧是另一侧前缀时返回较短者的长度。 */
    private fun firstDivergence(a: String, b: String): Int {
        val shared = minOf(a.length, b.length)
        for (i in 0 until shared) {
            if (a[i] != b[i]) return i
        }
        return shared
    }

    /**
     * PSI 树转储，并把转义后的 CRLF 折回 LF，以便与换行符无关地比较。
     *
     * [DebugUtil.psiToString] 会把空白文本转义成字面量 `\r\n`（反斜杠 + r），因此这里
     * 折回的是**转义表示**，而不是输入里的真实回车字符。
     */
    private fun dumpNormalized(file: CjFile): String =
        DebugUtil.psiToString(file, true).replace("\\r\\n", "\\n")
}