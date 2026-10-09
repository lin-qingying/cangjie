package org.cangnova.cangjie.ide.completion.impl.cfir.dsl

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionService
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.TestCompletionRequest
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.lookupNames
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 补全测试 DSL（任务 14.1）。
 *
 * Kotlin 侧的补全套件用指令描述「该位置应当/不应当出现什么」：`EXIST` / `ABSENT` / `NUMBER` /
 * `NOTHING_ELSE` / `WITH_ORDER` / `INVOCATION_COUNT`。这里移植**适用**的那部分，不复制 Kotlin 的
 * 夹具答案，也不复制它不适用的一半：
 *
 * - **源码是仓颉语法**：位置用既有夹具约定 `completionTarget` 这个引用名标注，
 *   而不是 Kotlin 的 `<caret>`（后者不是合法仓颉，会让夹具无法分析）；
 * - **多一个 `PREFIX` 指令**：仓颉侧的前缀过滤发生在请求上，写成指令才能让
 *   `NOTHING_ELSE` 之类的断言在生产**确定**的候选集上（否则断言会随无关候选漂移）；
 * - 断言跑在**真实服务**（`CfirCompletionService`）上，不是某个区段：DSL 要描述的是
 *   用户看到的候选列表，区段级断言另有专门套件。
 *
 * 指令写在夹具头部（`// NAME: 值`），与测试数据同文件，改夹具时改不动指令。
 */
@OptIn(CaExperimentalApi::class)
class CompletionDslTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/completionDsl",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /** `EXIST` 与 `ABSENT`：该出现的出现、不该出现的不出现。 */
    @Test
    fun existAndAbsent(mainFile: CjFile) = assertCompletionDsl(mainFile)

    /** `NOTHING_ELSE`：候选集必须**恰好**是指令列出的那些。 */
    @Test
    fun nothingElse(mainFile: CjFile) = assertCompletionDsl(mainFile)

    /** `WITH_ORDER`：列出的候选必须按该相对次序出现。 */
    @Test
    fun withOrder(mainFile: CjFile) = assertCompletionDsl(mainFile)

    /** `NUMBER`：候选数量必须精确等于指令给出的值（用于「前缀唯一命中」这类断言）。 */
    @Test
    fun number(mainFile: CjFile) = assertCompletionDsl(mainFile)

    /**
     * `INVOCATION_COUNT`：指令必须真的走进请求。
     *
     * 只断言「解析出来是多少」不够——解析对了但没传给请求，重复触发的放宽规则就永远不会生效。
     */
    @Test
    fun invocationCount(mainFile: CjFile) {
        val directives = CompletionDslDirectives.parse(mainFile.text)
        assertEquals(3, directives.invocationCount, "[${mainFile.name}] INVOCATION_COUNT 必须被解析")
        assertCompletionDsl(mainFile)
    }

    /**
     * 重复触发按设计**放宽**匹配：候选集必须成为首轮的超集，且确实变大。
     *
     * 这条规则写在索引区段里（`repeated = invocationCount > 1`，理由是「用户再次请求补全通常
     * 意味着上一轮前缀太窄」）。它只能这样断言：**不指定具体多出来的是哪个候选**——
     * 多出来的是库侧名字，逐一点名会把用例绑死在某个 `.cjo` 版本上。
     */
    @Test
    fun repeatedInvocationWidensCandidates(mainFile: CjFile) {
        val directives = CompletionDslDirectives.parse(mainFile.text)
        val position = PsiTreeUtil
            .findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == COMPLETION_TARGET }
        val service = CfirCompletionService()
        val first = service
            .complete(TestCompletionRequest(position, directives.prefix, invocationCount = 1))
            .lookupNames()
            .toSet()
        val repeated = service
            .complete(TestCompletionRequest(position, directives.prefix, invocationCount = 3))
            .lookupNames()
            .toSet()

        require(directives.exist.all { it in first }) {
            "[${mainFile.name}] EXIST 候选在首次触发时就应出现，实际 $first"
        }
        assertTrue(
            first.all { it in repeated },
            "[${mainFile.name}] 重复触发只能放宽不能收窄：首轮独有 ${first - repeated}",
        )
        assertTrue(
            repeated.size > first.size,
            "[${mainFile.name}] 重复触发必须确实放宽（否则这条规则是哑的），首轮 ${first.size} 个、重复后 ${repeated.size} 个",
        )
    }

    /**
     * 按夹具头部的指令跑一次真实补全并逐条断言。
     */
    private fun assertCompletionDsl(mainFile: CjFile) {
        val directives = CompletionDslDirectives.parse(mainFile.text)
        val position = PsiTreeUtil
            .findChildrenOfType(mainFile, CjSimpleNameExpression::class.java)
            .last { it.referencedName == COMPLETION_TARGET }
        val request = TestCompletionRequest(
            position = position,
            prefix = directives.prefix,
            invocationCount = directives.invocationCount,
        )

        val names = CfirCompletionService().complete(request).lookupNames()

        directives.exist.forEach { name ->
            assertTrue(name in names, "[${mainFile.name}] EXIST 候选缺失：$name，实际 $names")
        }
        directives.absent.forEach { name ->
            assertTrue(name !in names, "[${mainFile.name}] ABSENT 候选取不应出现：$name，实际 $names")
        }
        directives.number?.let { expected ->
            assertEquals(expected, names.size, "[${mainFile.name}] NUMBER 不符，实际 $names")
        }
        if (directives.nothingElse) {
            val allowed = (directives.exist + directives.withOrder).toSet()
            val unexpected = names.filterNot { it in allowed }
            assertTrue(
                unexpected.isEmpty(),
                "[${mainFile.name}] NOTHING_ELSE：出现未列出的候选 $unexpected，实际 $names",
            )
        }
        if (directives.withOrder.isNotEmpty()) {
            val indices = directives.withOrder.map { name ->
                val index = names.indexOf(name)
                assertTrue(index >= 0, "[${mainFile.name}] WITH_ORDER 列出的候选缺失：$name，实际 $names")
                index
            }
            assertEquals(
                indices.sorted(),
                indices,
                "[${mainFile.name}] WITH_ORDER 次序不符：期望 ${directives.withOrder}，实际 $names",
            )
        }
    }

    private companion object {
        /** 夹具中标注补全位置的引用名（与区段套件同一约定）。 */
        const val COMPLETION_TARGET = "completionTarget"
    }
}

/**
 * 夹具头部指令的解析结果。
 */
internal data class CompletionDslDirectives(
    val prefix: String,
    val exist: List<String>,
    val absent: List<String>,
    val number: Int?,
    val nothingElse: Boolean,
    val withOrder: List<String>,
    val invocationCount: Int,
) {
    companion object {
        /**
         * 指令格式：`// NAME: 值`（`NOTHING_ELSE` 无值）。未知指令原样忽略——
         * 夹具里出现别的注释（如 `// FILE:`）是常态。
         */
        private val DIRECTIVE = Regex("""^//\s*([A-Z_]+)\s*:?\s*(.*)$""")
        private val VALUES = Regex("""\s*,\s*""")

        fun parse(fixtureText: String): CompletionDslDirectives {
            var prefix = ""
            val exist = mutableListOf<String>()
            val absent = mutableListOf<String>()
            var number: Int? = null
            var nothingElse = false
            val withOrder = mutableListOf<String>()
            var invocationCount = 1

            fixtureText.lineSequence().forEach { line ->
                val match = DIRECTIVE.matchEntire(line.trimEnd()) ?: return@forEach
                val (name, rawValue) = match.destructured
                val values = rawValue.trim().takeIf { it.isNotEmpty() }?.split(VALUES)?.filter { it.isNotEmpty() } ?: emptyList()
                when (name) {
                    "PREFIX" -> prefix = rawValue.trim()
                    "EXIST" -> exist += values
                    "ABSENT" -> absent += values
                    "NUMBER" -> number = values.singleOrNull()?.toIntOrNull()
                    "NOTHING_ELSE" -> nothingElse = true
                    "WITH_ORDER" -> withOrder += values
                    "INVOCATION_COUNT" -> invocationCount = values.singleOrNull()?.toIntOrNull() ?: 1
                }
            }
            return CompletionDslDirectives(
                prefix = prefix,
                exist = exist,
                absent = absent,
                number = number,
                nothingElse = nothingElse,
                withOrder = withOrder,
                invocationCount = invocationCount,
            )
        }
    }
}
