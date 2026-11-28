package org.cangnova.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.SyntaxTreeBuilder
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.messages.CjParsingDiagnosticScope
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertFailsWith as fails
import kotlin.test.assertNotNull as present
import kotlin.test.assertNull as absent
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as truth

/**
 * 使用真实 SDK marker 和 LightTree 验证报告身份的事务生命周期。
 * 文本仅提供 SDK 词元和边界，消息是 marker 事务标签，不构造新的仓颉语义期望。
 * visitor 只读取错误类型已经保存的 order；丢弃、回滚和折叠的归属与原生 SDK 对照。
 */
class CjParsingDiagnosticOrderLifecycleTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun errorBeforeKeepsTheChildFirstReportDespiteParentFirstTreeTraversal() {
        for (syntaxInterface in listOf(false, true)) {
            val context = CjParserSourceContext.complete("a")
            val builder = semantic(context)
            val root = builder.mark()
            val parent = builder.mark()
            val child = builder.mark()
            builder.advanceLexer()
            assertLogicalCursor(builder, 1)
            child.error("child first")
            val boundary = builder.mark()
            if (syntaxInterface) {
                (parent as SyntaxTreeBuilder.Marker).errorBefore("parent later", boundary)
            } else {
                parent.errorBefore("parent later", boundary)
            }
            boundary.drop()
            root.done(CjNodeTypes.CJ_FILE)
            val reports = reports(builder.lightTree, context)

            equal(TokenType.ERROR_ELEMENT, child.tokenType)
            equal(TokenType.ERROR_ELEMENT, parent.tokenType)
            equal("child first", child.errorMessage)
            equal("parent later", parent.errorMessage)
            equal(listOf("parent later", "child first"), reports.map { it.diagnostic.message })
            val first = reports.single { it.diagnostic.message == "child first" }
            val later = reports.single { it.diagnostic.message == "parent later" }
            truth(first.order.compareWithinRoot(later.order) < 0)
            equal(listOf(Span(0, 1, "child first")), spans(publish(context, reports)))
            equal(listOf(Span(0, 1, "child first")), spans(publish(context, reports.asReversed())))
        }
    }

    @Test
    fun droppingDoneBeforeOwnerKeepsItsIndependentErrorInBothSdkInterfaces() {
        for (syntaxInterface in listOf(false, true)) {
            val context = CjParserSourceContext.complete("a b")
            // v47 完整 XML：SDK 的 DEFAULT_RIGHT 将独立零宽报告绑定到 trivia 起点。
            val expected = listOf(Span(1, 1, "independent boundary report"))
            val sdk = sdkBuilder(context.snapshot.text)
            dropDoneBeforeOwner(sdk, syntaxInterface)
            val native = sdkErrors(sdk.lightTree)
            equal(expected, native)

            val builder = semantic(context)
            dropDoneBeforeOwner(builder, syntaxInterface)
            val kept = reports(builder.lightTree, context)
            equal(native, kept.map { span(it.diagnostic) })
            equal(expected, spans(publish(context, kept)))
        }
    }

    @Test
    fun droppingAnErrorParentKeepsTheChildReportAsInTheNativeSdk() {
        val context = CjParserSourceContext.complete("a b")
        val expected = listOf(Span(0, 1, "surviving child"))
        val sdk = sdkBuilder(context.snapshot.text)
        dropErrorParent(sdk)
        equal(expected, sdkErrors(sdk.lightTree))

        val builder = semantic(context)
        dropErrorParent(builder)
        val kept = reports(builder.lightTree, context)
        equal(expected, kept.map { span(it.diagnostic) })
        equal(expected, spans(publish(context, kept)))
    }

    @Test
    fun rollbackAndPrecedeRollbackRevokeReportsAndTheirReservedChildScope() {
        for (precede in listOf(false, true)) {
            val context = CjParserSourceContext.complete("{}")
            val builder = semantic(context)
            val journal = builder.macroParsingJournal
            val root = builder.mark()
            val speculation = builder.mark()
            val abandoned = journal.reserveBodyDiagnosticScope(builder.currentOffset)
            journal.withDiagnosticScope(abandoned) { builder.error("rolled back child") }
            builder.advanceLexer()
            if (precede) {
                speculation.done(CjNodeTypes.REFERENCE_EXPRESSION)
                speculation.precede().rollbackTo()
            } else {
                speculation.rollbackTo()
            }

            equal(0, builder.currentOffset)
            fails<IllegalStateException> { journal.withDiagnosticScope(abandoned) { builder.error("stale scope") } }
            val replacement = journal.reserveBodyDiagnosticScope(builder.currentOffset)
            // 重解析应回到原逻辑槽位；不限定序号的具体数值或 journal 的计数器布局。
            equal(abandoned, replacement)
            journal.withDiagnosticScope(replacement) { builder.error("reparsed child first") }
            builder.error("parent after reparse")
            finish(builder, root)
            val kept = reports(builder.lightTree, context)

            equal(setOf("reparsed child first", "parent after reparse"), kept.map { it.diagnostic.message }.toSet())
            equal(listOf(Span(0, 0, "reparsed child first")), spans(publish(context, kept.asReversed())))
        }
    }

    @Test
    fun collapsedSdkLightTreeHidesChildReportsAndOuterRollbackAllowsReparsing() {
        for (rollback in listOf(false, true)) {
            val context = CjParserSourceContext.complete("a b")
            val expected = if (rollback) {
                listOf(Span(0, 1, "reparsed visible child"), Span(1, 1, "after collapsed region"))
            } else {
                listOf(Span(1, 1, "after collapsed region"))
            }
            val sdk = sdkBuilder(context.snapshot.text)
            collapseChild(sdk, rollback)
            val native = sdkErrors(sdk.lightTree)
            equal(expected, native)

            val builder = semantic(context)
            collapseChild(builder, rollback)
            val kept = reports(builder.lightTree, context)
            equal(native, kept.map { span(it.diagnostic) })
            equal(expected, spans(publish(context, kept.asReversed())))
        }
    }

    @Test
    fun completeAndReverseLazyExpansionUseTheSamePathsAndFirstReportWinners() {
        val context = CjParserSourceContext.complete("x{}{}")
        val input = CjLexicalSourceInput(context.snapshot)
        val parent = semantic(context, input)
        val rootScope = parent.macroParsingJournal.diagnosticScope
        val parentRoot = parent.mark()
        parent.advanceLexer()
        val slots = listOf("first", "second").map { label ->
            val start = parent.currentOffset
            val scope = parent.macroParsingJournal.reserveBodyDiagnosticScope(start)
            val childContext = CjParserSourceContext(context.snapshot, start, start + 2)
            val seed = seed(childContext, input, scope)
            // 父报告实际先产生，逻辑位置仍在已预留的正文槽位之后。
            parent.error("$label parent later")
            val body = parent.mark()
            repeat(2) { parent.advanceLexer() }
            body.collapse(CjNodeTypes.REFERENCE_EXPRESSION)
            Slot(label, seed)
        }
        parentRoot.done(CjNodeTypes.CJ_FILE)
        val parentReports = reports(parent.lightTree, context)
        equal(listOf("first parent later", "second parent later"), parentReports.map { it.diagnostic.message })

        // 完整路径从同一真实根入口重放，scope 来自父 producer，不在测试中拼接路径。
        val complete = semantic(context, input, seed(context, input, rootScope))
        val completeRoot = complete.mark()
        complete.advanceLexer()
        for (slot in slots) {
            val wrapper = complete.mark()
            val scope = complete.macroParsingJournal.reserveBodyDiagnosticScope(complete.currentOffset)
            equal(present(slot.seed.diagnosticScope), scope)
            complete.macroParsingJournal.withDiagnosticScope(scope) { childErrors(complete, slot.label) }
            val boundary = complete.mark()
            wrapper.errorBefore("${slot.label} parent later", boundary)
            boundary.drop()
            repeat(2) { complete.advanceLexer() }
        }
        completeRoot.done(CjNodeTypes.CJ_FILE)
        val completeReports = reports(complete.lightTree, context)
        equal("first parent later", completeReports.first().diagnostic.message)
        assertSlotWinners(publish(context, completeReports))
        assertSlotWinners(publish(context, completeReports.asReversed()))

        for (expansionOrder in listOf(slots, slots.asReversed())) {
            val expanded = expansionOrder.flatMap { slot ->
                val childContext = present(slot.seed.sourceContext)
                val child = semantic(childContext, input, slot.seed)
                identical(input, child.macroParsingJournal.lexicalInput)
                // seeded 正文必须复用父槽位，不再次保留一个子槽位。
                val scope = child.macroParsingJournal.reserveBodyDiagnosticScope(child.currentOffset)
                equal(slot.seed.diagnosticScope, scope)
                val childRoot = child.mark()
                child.macroParsingJournal.withDiagnosticScope(scope) { childErrors(child, slot.label) }
                finish(child, childRoot)
                reports(child.lightTree, childContext)
            }
            val restored = parentReports.asReversed() + expanded
            // 只比较实际报告的身份，不能按遍历顺序补号或从完整结果复制 order。
            equal(completeReports.associate { it.diagnostic.message to it.order },
                restored.associate { it.diagnostic.message to it.order })
            assertSlotWinners(publish(context, restored))
            assertSlotWinners(publish(context, restored.asReversed()))
        }
    }

    private fun dropDoneBeforeOwner(builder: PsiBuilder, syntaxInterface: Boolean) {
        assertLogicalCursor(builder, 0)
        val root = builder.mark()
        val owner = builder.mark()
        builder.advanceLexer()
        assertLogicalCursor(builder, 2)
        val boundary = builder.mark()
        if (syntaxInterface) {
            (owner as SyntaxTreeBuilder.Marker).doneBefore(
                CjNodeTypes.REFERENCE_EXPRESSION, boundary, "independent boundary report",
            )
        } else {
            owner.doneBefore(CjNodeTypes.REFERENCE_EXPRESSION, boundary, "independent boundary report")
        }
        equal(CjNodeTypes.REFERENCE_EXPRESSION, owner.tokenType)
        absent(owner.errorMessage)
        owner.drop()
        boundary.drop()
        finish(builder, root)
    }

    private fun dropErrorParent(builder: PsiBuilder) {
        assertLogicalCursor(builder, 0)
        val root = builder.mark()
        val parent = builder.mark()
        val child = builder.mark()
        builder.advanceLexer()
        assertLogicalCursor(builder, 2)
        child.error("surviving child")
        parent.error("dropped parent")
        equal(TokenType.ERROR_ELEMENT, parent.tokenType)
        equal("dropped parent", parent.errorMessage)
        parent.drop()
        finish(builder, root)
    }

    private fun collapseChild(builder: PsiBuilder, rollback: Boolean) {
        assertLogicalCursor(builder, 0)
        val root = builder.mark()
        val speculation = builder.mark()
        val body = builder.mark()
        builder.error("hidden child")
        builder.advanceLexer()
        assertLogicalCursor(builder, 2)
        body.collapse(CjNodeTypes.REFERENCE_EXPRESSION)
        truth(body.isCollapsed)
        if (rollback) {
            speculation.rollbackTo()
            assertLogicalCursor(builder, 0)
            val reparsed = builder.mark()
            builder.advanceLexer()
            assertLogicalCursor(builder, 2)
            reparsed.error("reparsed visible child")
        } else {
            speculation.drop()
        }
        assertLogicalCursor(builder, 2)
        builder.error("after collapsed region")
        finish(builder, root)
    }

    /** 两边先读取各自真实逻辑 lookahead 和 offset，不把未读 lookahead 的 trivia 游标作为对照。 */
    private fun assertLogicalCursor(builder: PsiBuilder, expectedOffset: Int) {
        builder.tokenType
        equal(expectedOffset, builder.currentOffset)
    }

    private fun childErrors(builder: PsiBuilder, label: String) {
        builder.error("$label child first")
        builder.error("$label child duplicate")
    }

    private fun assertSlotWinners(diagnostics: List<ParsingDiagnostic>) {
        equal(listOf(Span(1, 1, "first child first"), Span(3, 3, "second child first")), spans(diagnostics))
        equal(listOf(1 to 2, 1 to 4), diagnostics.map { present(it.mainRange).begin })
        equal(listOf(1 to 2, 1 to 4), diagnostics.map { present(it.mainRange).end })
        truth(diagnostics.all { it.nativeHints.isEmpty() })
    }

    private fun sdkBuilder(text: String): PsiBuilder = PsiBuilderFactory.getInstance().createBuilder(
        CangJieParserDefinition(), CangJieLexer(), text,
    )

    private fun semantic(
        context: CjParserSourceContext,
        input: CjLexicalSourceInput = CjLexicalSourceInput(context.snapshot),
        entry: CjParserSeed? = null,
    ): SemanticWhitespaceAwarePsiBuilderImpl {
        val text = context.snapshot.text.substring(context.inputStart, context.inputEnd)
        val builder = SemanticWhitespaceAwarePsiBuilderImpl(sdkBuilder(text), context, input)
        val journal = builder.macroParsingJournal
        journal.initializeFileState(CjSourceKind.SOURCE, "", CjmpParsingContext.SyntaxOnly)
        if (entry != null) {
            identical(context, entry.sourceContext)
            identical(input, entry.lexicalInput)
            journal.initializeFileSeed(entry)
        }
        return builder
    }

    private fun seed(context: CjParserSourceContext, input: CjLexicalSourceInput, scope: CjParsingDiagnosticScope): CjParserSeed =
        CjParserSeed(
            CjSourceKind.SOURCE, "", CjmpParsingContext.SyntaxOnly, CjmpFileRole.NONE, 0,
            AbstractCangJieParsing.ParsingContext.DEFAULT,
            sourceContext = context, lexicalInput = input, diagnosticScope = scope,
        )

    private fun finish(builder: PsiBuilder, root: PsiBuilder.Marker) {
        while (!builder.eof()) builder.advanceLexer()
        root.done(CjNodeTypes.CJ_FILE)
    }

    /** 在 SDK 释放临时子节点之前读取报告；只映射来源坐标，不生成或重排身份。 */
    private fun reports(tree: FlyweightCapableTreeStructure<LighterASTNode>, context: CjParserSourceContext): List<CjParsingDiagnosticReport> {
        val result = mutableListOf<CjParsingDiagnosticReport>()
        visit(tree.root, tree) { node ->
            val type = node.tokenType
            if (type is CjParsingErrorElementType) {
                val diagnostic = type.diagnostic(node.startOffset, node.endOffset)
                absent(diagnostic.source, "事务 fixture 不应生成外部来源诊断")
                result += CjParsingDiagnosticReport(diagnostic.copy(
                    source = context.snapshot,
                    startOffset = context.inputStart + diagnostic.startOffset,
                    endOffset = context.inputStart + diagnostic.endOffset,
                ), type.diagnosticOrder)
            } else {
                truth(type != TokenType.ERROR_ELEMENT, "有序 builder 不得遗留缺失 producer 身份的原生错误")
            }
        }
        return result
    }

    private fun sdkErrors(tree: FlyweightCapableTreeStructure<LighterASTNode>): List<Span> {
        val result = mutableListOf<Span>()
        visit(tree.root, tree) { node ->
            if (node.tokenType == TokenType.ERROR_ELEMENT) {
                result += Span(node.startOffset, node.endOffset, present(PsiBuilderImpl.getErrorMessage(node)))
            }
        }
        return result
    }

    private fun visit(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>, read: (LighterASTNode) -> Unit) {
        read(node)
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get() ?: return
        try {
            for (index in 0 until count) visit(present(children[index]), tree, read)
        } finally {
            tree.disposeChildren(children, count)
        }
    }

    private fun publish(context: CjParserSourceContext, reports: List<CjParsingDiagnosticReport>): List<ParsingDiagnostic> {
        val collector = CjParsingDiagnosticCollector(context)
        reports.forEach(collector::restoreReport)
        return collector.finish()
    }

    private fun span(diagnostic: ParsingDiagnostic): Span = Span(diagnostic.startOffset, diagnostic.endOffset, diagnostic.message)
    private fun spans(diagnostics: List<ParsingDiagnostic>): List<Span> = diagnostics.map(::span)
    private data class Span(val start: Int, val end: Int, val message: String)
    private data class Slot(val label: String, val seed: CjParserSeed)
}
