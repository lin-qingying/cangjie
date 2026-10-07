package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.impl.DocumentImpl
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.InsertionValidity
import org.cangnova.cangjie.ide.completion.impl.shared.insert.DocumentTextSink
import org.cangnova.cangjie.ide.completion.impl.shared.insert.InsertionResult
import org.cangnova.cangjie.ide.completion.impl.shared.insert.InsertionValidator
import org.cangnova.cangjie.ide.completion.impl.shared.insert.performCangjieInsertion
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 各候选家族的端到端插入：**真实区段产出快照 → 平台替换前缀 → 插入 → 断言结果**。
 *
 * 前面的用例各自验证过一半——区段产出什么、以及用合成快照插入会写出什么。
 * 这里把两半接起来，因为两半之间的假设正是最容易出错的地方：
 * 快照里的身份与插入数据必须真的能驱动出预期的文本与 caret。
 *
 * 平台前缀替换这一步由用例显式模拟（把占位符换成长名）并**落在写命令内**，
 * 与产品里插入处理器所处的上下文一致；之后的 caret 与括号处理走产品代码。
 */
@OptIn(CaExperimentalApi::class)
class CfirInsertionIntegrationTest : AbstractAnalysisApiExecutionTest(
    "code-insight/completion/impl-cfir/testData/sections",
) {
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 未导入候选由索引区段产出，测试宿主需要补注册名称索引服务。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(IndexSectionTestServiceRegistrar)

    /**
     * callable 候选：追加一对括号，caret 落在括号内，身份指向该声明。
     */
    @Test
    fun insertsCallableCandidateWithParenthesisAndCaret(mainFile: CjFile) {
        val document = DocumentImpl(mainFile.text)
        val snapshot = callableSnapshot(mainFile, "target", document)
        assertEquals(CompletionIdentityKind.SYMBOL, snapshot.identity.kind, "callable 候选的身份类别")
        assertEquals("target", snapshot.identity.name)

        val outcome = onEdt(mainFile.project) {
            val caret = simulatePlatformPrefixReplacement(document, snapshot)
            insert(snapshot, document, caret)
        }

        assertTrue(outcome.result.wroteText, "callable 候选必须补出括号")
        assertTrue(document.text.contains("target()"), "文档必须含补全后的调用，实际：${document.text}")
        assertEquals(outcome.caret + 1, outcome.result.caretOffset, "caret 必须落在新写入的括号内")
    }

    /**
     * 关键字候选：不补括号，caret 停在插入文本之后。
     */
    @Test
    fun insertsKeywordCandidateWithoutParenthesis(mainFile: CjFile) {
        val document = DocumentImpl(mainFile.text)
        val snapshot = keywordSnapshot(mainFile, "return", document)
        assertEquals(CompletionIdentityKind.KEYWORD, snapshot.identity.kind)
        assertFalse(snapshot.insertion.insertParenthesis, "关键字候选不得要求括号")

        val outcome = onEdt(mainFile.project) {
            val caret = simulatePlatformPrefixReplacement(document, snapshot)
            insert(snapshot, document, caret)
        }

        assertTrue(document.text.contains("return"), "关键字必须被写入")
        assertFalse(outcome.result.wroteText, "关键字候选不得追加任何字符")
        assertEquals(outcome.caret, outcome.result.caretOffset, "无括号候选的 caret 停在插入文本之后")
    }

    /**
     * 插入前文档被编辑：必须一个字符都不写。
     *
     * 快照在分析期冻结，用户可能在弹窗打开期间继续编辑。此时基于旧偏移算出的
     * 括号与 caret 结论都不再可信，硬插会把字符送到错误位置。
     */
    @Test
    fun staleSnapshotWritesNothingIntoEditedDocument(mainFile: CjFile) {
        val document = DocumentImpl(mainFile.text)
        val snapshot = callableSnapshot(mainFile, "target", document)

        val (validity, before, result) = onEdt(mainFile.project) {
            val caret = simulatePlatformPrefixReplacement(document, snapshot)
            // 模拟用户在弹窗打开期间继续编辑。
            document.insertString(0, "// edited\n")
            val current = InsertionValidator.validate(
                snapshot, caret, caret, document, document.modificationStamp,
            )
            val written = performCangjieInsertion(
                snapshot = snapshot,
                sink = DocumentTextSink(document),
                validity = current,
                caretOffset = caret,
            ) { }
            Triple(current, document.text, written)
        }

        assertTrue(validity is InsertionValidity.StaleDocument, "文档被编辑后必须报告过期，实际 $validity")
        assertFalse(result.wroteText, "过期候选不得写文档")
        assertEquals(before, document.text, "过期候选必须一个字符都不写")
    }

    /**
     * 未导入候选：插入计划里必须带上这一步要写的 import。
     *
     * 这是「导入」这一维在端到端路径上的位置——候选能插出去，但表达式引用的名字
     * 在当前文件里根本不可解析，除非同时写 import。
     */
    @Test
    fun insertsUnimportedCandidateAndReportsImport(mainFile: CjFile) {
        val document = DocumentImpl(mainFile.text)
        val snapshot = CfirIndexCompletionSection()
            .collectCandidates(requestWith(mainFile, document.modificationStamp))
            .single { it.lookupString == UNIMPORTED_TARGET }

        assertTrue(
            snapshot.importPlan.requiresImportWrite,
            "未导入候选必须带追加 import 的计划，实际 ${snapshot.importPlan}",
        )
        val plan = snapshot.importPlan.plan
        assertTrue(
            plan is CompletionImportPlan.AddImport,
            "计划必须是 AddImport，实际 $plan",
        )
        assertEquals(
            "insertion.dependency",
            (plan as CompletionImportPlan.AddImport).target.importPath.pathStr,
            "要写的 import 是候选所属的包路径",
        )

        val outcome = onEdt(mainFile.project) {
            val caret = simulatePlatformPrefixReplacement(document, snapshot)
            insert(snapshot, document, caret)
        }
        assertTrue(document.text.contains("insertionTarget()"), "候选文本必须被写入，实际：${document.text}")
        assertEquals(outcome.caret + 1, outcome.result.caretOffset, "caret 落在括号内")
    }

    /**
     * 按当前文档版本戳构造请求，再经顶层区段取得指定名字的真实快照。
     */
    private fun callableSnapshot(mainFile: CjFile, name: String, document: DocumentImpl): CompletionSnapshot =
        CfirTopLevelCompletionSection()
            .collectCandidates(requestWith(mainFile, document.modificationStamp))
            .single { it.lookupString == name }

    /**
     * 按当前文档版本戳构造请求，再经关键字区段取得指定关键字。
     */
    private fun keywordSnapshot(mainFile: CjFile, name: String, document: DocumentImpl): CompletionSnapshot =
        CfirKeywordCompletionSection()
            .collectCandidates(requestWith(mainFile, document.modificationStamp))
            .single { it.lookupString == name && it.kind == CompletionItemKind.KEYWORD }

    /**
     * 构造带文档版本戳的补全请求。
     *
     * 版本戳取自**当前**文档：用例随后在同一份文档上做替换与插入，
     * 戳不对就只能验证「过期」这一条路径。
     */
    private fun requestWith(mainFile: CjFile, stamp: Long): CompletionRequest {
        val base = mainFile.requestAtReference(PLACEHOLDER)
        return TestCompletionRequest(
            position = base.completionPosition,
            prefix = "",
            offset = base.offset,
            replaceStartOffset = base.replaceStartOffset,
            replaceEndOffset = base.replaceEndOffset,
            documentStamp = stamp,
        )
    }

    /**
     * 模拟平台的「前缀替换」：把占位符替换成候选的查找串，返回插入点。
     *
     * 平台在调用插入处理器之前已经完成了这一步，因此插入处理器看到的文档里
     * 占位符已经不在了。
     */
    private fun simulatePlatformPrefixReplacement(document: DocumentImpl, snapshot: CompletionSnapshot): Int {
        val start = document.text.indexOf(PLACEHOLDER)
        require(start >= 0) { "夹具必须含待补全的占位符" }
        document.replaceString(start, start + PLACEHOLDER.length, snapshot.lookupString)
        return start + snapshot.lookupString.length
    }

    /**
     * 走产品的校验 + 插入路径，返回 caret 最终落点。
     */
    private fun insert(snapshot: CompletionSnapshot, document: DocumentImpl, caret: Int): InsertionOutcome {
        val validity = InsertionValidator.validate(
            snapshot, caret, caret, document, snapshot.insertion.documentStamp,
        )
        var finalCaret = caret
        val result = performCangjieInsertion(
            snapshot = snapshot,
            sink = DocumentTextSink(document),
            validity = validity,
            caretOffset = caret,
        ) { finalCaret = it }
        return InsertionOutcome(caret, finalCaret, result)
    }

    /**
     * 在 EDT 的写命令内执行：文档写入的平台契约就是「EDT + 命令上下文」，
     * 与产品里插入处理器所处的上下文一致。
     */
    private fun <T> onEdt(project: com.intellij.openapi.project.Project, action: () -> T): T {
        var outcome: T? = null
        java.awt.EventQueue.invokeAndWait {
            WriteCommandAction.runWriteCommandAction(project, Runnable { outcome = action() })
        }
        return requireNotNull(outcome) { "写命令必须产出结果" }
    }

    /**
     * 一次插入的观测结果。
     */
    private data class InsertionOutcome(
        val caret: Int,
        val finalCaret: Int,
        val result: InsertionResult,
    )

    companion object {
        /** 夹具中的占位标识符。 */
        private const val PLACEHOLDER = "completionTarget"

        /** 未导入夹具中的目标声明名。 */
        private const val UNIMPORTED_TARGET = "insertionTarget"
    }
}
