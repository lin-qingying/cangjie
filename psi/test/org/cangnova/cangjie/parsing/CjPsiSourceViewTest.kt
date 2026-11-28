package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.pom.PomModel
import com.intellij.pom.PomModelAspect
import com.intellij.pom.event.PomModelEvent
import com.intellij.pom.event.PomModelListener
import com.intellij.pom.impl.PomTransactionBase
import com.intellij.pom.tree.TreeAspect
import com.intellij.pom.tree.events.TreeChangeEvent
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.impl.source.tree.SharedImplUtil
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjBlockCodeFragment
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjExpressionCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjTypeCodeFragment
import org.cangnova.cangjie.psi.parsedLanguageModuleName
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.awt.EventQueue
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertFailsWith as fails
import kotlin.test.assertNotNull as present
import kotlin.test.assertNotSame as distinct
import kotlin.test.assertNull as absent
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as truth

/** 真实 SDK diff 的当前坐标与原 producer 身份各自验收，不通过另一棵树回贴旧事实。 */
class CjPsiSourceViewTest : CjParsingTestCase("", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition()) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun movedUnknownLeafKeepsItsProducerWhileTheCommittedSourceViewChanges() {
        val oldText = "func before(): Unit { let value = 1 }\nfunc after(): Unit { let tokens = quote(§) }"
        val file = createPsiFile("sourceView", oldText) as CjFile
        val oldDiagnostics = file.collectParsingDiagnosticPublication()
        val oldView = file.currentPsiSourceView()
        val oldInput = present(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        identical(oldInput.lexicalInput, oldView.lexicalInput)
        val unknown = unknownLeaves(file.node).single()
        val origin = present(unknown.getCopyableUserData(PARSER_LEXICAL_DIAGNOSTIC_KEY))
        val order = oldDiagnostics.reports.single().order
        identical(origin.report.order, order)

        val point = oldText.indexOf('1')
        val newText = oldText.replaceRange(point, point + 1, "1000")
        val diff = BlockSupportImpl.reparse(file, file.node, TextRange(point, point + 1), newText,
            EmptyProgressIndicator(), oldText)
        equal(oldText, file.text)
        identical(oldView, file.currentPsiSourceView())
        oldView.requireCurrent()

        commit { diff.log.doActualPsiChange(file) }
        equal(newText, file.text)
        fails<IllegalStateException> { oldView.requireCurrent() }
        val current = file.currentPsiSourceView()
        distinct(oldView, current)
        distinct(oldView.lexicalInput, current.lexicalInput)
        equal(newText, current.sourceContext.snapshot.text)
        identical(oldInput, file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        equal(oldText, present(oldInput.lexicalInput).source.text)
        absent(file.parsedRoleRegion, "局部提交不得冒充完整新根 parser 结果")
        identical(unknown, unknownLeaves(file.node).single())
        identical(origin, unknown.getCopyableUserData(PARSER_LEXICAL_DIAGNOSTIC_KEY))

        val publication = file.collectParsingDiagnosticPublication()
        val diagnostic = publication.diagnostics.single()
        equal(newText.indexOf('§') to newText.indexOf('§'), diagnostic.startOffset to diagnostic.endOffset)
        identical(current.sourceContext.snapshot, present(diagnostic.mainRange).source)
        identical(order, publication.reports.single().order)
        equal(oldDiagnostics.diagnostics.single().message, diagnostic.message)
        current.requireCurrent()
    }

    @Test
    fun nativeTypeExpressionAndBlockResultsSurviveACommitToEmptyContents() {
        val prototypes: List<CjCodeFragment> = listOf(
            CjTypeCodeFragment(project, "type.cj", "Int64", null),
            CjExpressionCodeFragment(project, "expression.cj", "1", null, null),
            CjBlockCodeFragment(project, "block.cj", "let value = 1", null, null),
        )
        val fragments = prototypes.flatMap { prototype ->
            listOf(prototype, nativeFragmentWithoutErrorReports(prototype))
        }
        for (fragment in fragments) {
            fragment.node.firstChildNode?.firstChildNode
            val root = fragment.node as FileElement
            val originalTable = root.charTable
            val initial = fragment.currentPsiSourceView()
            val initialResult = present(fragment.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY))
            val initialInput = present(fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            val oldText = fragment.text
            val diff = BlockSupportImpl.reparse(fragment, root, TextRange(0, oldText.length), "",
                EmptyProgressIndicator(), oldText)
            val candidate = diff.newRoot as FileElement
            // native全替换的newRoot是外层DummyHolder，SDK实际搬运的是其firstChild原生FileElement。
            val candidateNative = present(candidate.firstChildNode) as FileElement
            truth(!candidateNative.isParsed)
            val candidateEntry = candidateNative.requireParserSeed()
            // SDK 先实际 parseContents；此入口返回前已 finishParsing、冻结结果并按新表登记。
            // 零孩子仍会置 isParsed，不能用 firstChild 非空代替完成状态。
            candidateNative.firstChildNode
            truth(candidateNative.isParsed, "Native producer 必须完成冻结后才采样候选表")
            val candidateTable = candidateNative.charTable
            distinct(originalTable, candidateTable)
            identical(candidateTable, SharedImplUtil.findCharTableByTree(candidateNative))
            identical(root, fragment.node)
            identical(originalTable, root.charTable)
            identical(initialResult, fragment.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY))
            identical(initialInput, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            identical(initialResult.fileFacts, fragment.parsedFileFacts)
            identical(initialResult.roleRegion, fragment.parsedRoleRegion)
            identical(initial, fragment.currentPsiSourceView())
            initial.requireCurrent()
            equal(oldText, fragment.text)
            commit { diff.log.doActualPsiChange(fragment) }
            equal("", fragment.text)
            identical(root, fragment.node)
            identical(candidateTable, root.charTable)
            // 验收 SDK 正式查表路径，不能仅检查项目私有 key 或候选表上的登记。
            identical(root.charTable, SharedImplUtil.findCharTableByTree(root))
            val result = present(fragment.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY))
            distinct(initialResult, result)
            identical(candidateEntry, result.input)
            equal("", result.sourceText)
            result.sourceContext.requireInput(root.chars)
            identical(result.roleRegion, fragment.parsedRoleRegion)
            val input = present(fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            equal(result.actualInput, input)
            present(input.sourceContext).requireInput(fragment.node.chars)
            identical(present(input.lexicalInput), present(fragment.parsedRoleRegion).lexicalInput)
            truth(present(fragment.parsingDiagnosticScopeOrNull()).path.isEmpty())
            val view = fragment.currentPsiSourceView()
            equal("", view.sourceContext.snapshot.text)
            identical(input.sourceContext, view.sourceContext)
            identical(input.lexicalInput, view.lexicalInput)
            if (!fragment.fragmentParserSeed.context.shouldReportError) {
                equal(emptyList(), fragment.collectParsingDiagnostics())
                assertEmptyNativeGrammar(fragment, root)
            } else {
                fragment.collectParsingDiagnostics()
            }
            view.requireCurrent()
        }
    }

    /** 同一完成 change set 的重投幂等；相同表、结果和文本上的新正文事务仍撤销完整根依据。 */
    @Test
    fun nativeChangeSetReplayKeepsFullBasisButNewBodyTransactionRevokesIt() {
        val oldText = "let value = 1"
        val newText = "let value = 2"
        val fragment = CjBlockCodeFragment(project, "pomSourceView.cj", oldText, null, null)
        fragment.getContentElement()
        val root = fragment.node as FileElement
        val originalTable = root.charTable
        val initial = fragment.currentPsiSourceView()
        val initialResult = present(fragment.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY))
        val initialInput = present(fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        val diff = BlockSupportImpl.reparse(fragment, root, TextRange(0, oldText.length), newText,
            EmptyProgressIndicator(), oldText)
        val candidateNative = present(diff.newRoot.firstChildNode) as FileElement
        truth(!candidateNative.isParsed)
        val candidateEntry = candidateNative.requireParserSeed()
        // 原生候选完成 freeze/登记后才读取实际字段表；此时旧文件仍未提交。
        candidateNative.firstChildNode
        truth(candidateNative.isParsed, "Native producer 必须完成冻结后才采样候选表")
        val candidateTable = candidateNative.charTable
        distinct(originalTable, candidateTable)
        identical(candidateTable, SharedImplUtil.findCharTableByTree(candidateNative))
        identical(root, fragment.node)
        identical(originalTable, root.charTable)
        identical(initialResult, fragment.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY))
        identical(initialInput, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        identical(initialResult.fileFacts, fragment.parsedFileFacts)
        identical(initialResult.roleRegion, fragment.parsedRoleRegion)
        identical(initial, fragment.currentPsiSourceView())
        equal(oldText, fragment.text)
        initial.requireCurrent()
        val model = present(project.getService(PomModel::class.java))
        val aspect = present(model.getModelAspect(TreeAspect::class.java))
        val events = mutableListOf<PomModelEvent>()
        val observer = object : PomModelListener {
            override fun isAspectChangeInteresting(changedAspect: PomModelAspect): Boolean = changedAspect === aspect

            override fun modelChanged(event: PomModelEvent) {
                val change = event.getChangeSet(aspect) as? TreeChangeEvent ?: return
                if (change.rootElement === root) events += event
            }
        }
        model.addModelListener(observer)
        try {
            commit { diff.log.doActualPsiChange(fragment) }
            val nativeEvent = events.single()
            val nativeChange = present(nativeEvent.getChangeSet(aspect) as? TreeChangeEvent)
            identical(root, nativeChange.rootElement)
            identical(root, fragment.node)
            identical(candidateTable, root.charTable)
            identical(root.charTable, SharedImplUtil.findCharTableByTree(root))
            equal(newText, fragment.text)

            val facts = present(fragment.parsedFileFacts)
            val region = present(fragment.parsedRoleRegion)
            val roleSnapshot = present(fragment.parsedRoleSnapshot)
            val input = present(fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            val result = present(fragment.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY))
            distinct(initialResult, result)
            identical(candidateEntry, result.input)
            equal(newText, result.sourceText)
            result.sourceContext.requireInput(root.chars)
            equal(result.actualInput, input)
            val diagnosticScope = present(fragment.parsingDiagnosticScopeOrNull())
            val moduleName = fragment.parsedLanguageModuleName
            val fullView = fragment.currentPsiSourceView()
            identical(input.sourceContext, fullView.sourceContext)
            identical(input.lexicalInput, fullView.lexicalInput)
            identical(region, result.roleRegion)

            // 新外层 event 经正式 POM 事务重投；PomTransactionBase 合并时保留原 change set 身份。
            val submittedReplay = PomModelEvent(model, nativeChange)
            distinct(nativeEvent, submittedReplay)
            identical(nativeChange, submittedReplay.getChangeSet(aspect))
            commit {
                model.runTransaction(object : PomTransactionBase(fragment) {
                    override fun runInner(): PomModelEvent = submittedReplay
                })
            }
            equal(2, events.size)
            val deliveredReplay = events.last()
            distinct(nativeEvent, deliveredReplay)
            identical(model, deliveredReplay.source)
            identical(nativeChange, deliveredReplay.getChangeSet(aspect))
            equal(newText, fragment.text)
            identical(facts, fragment.parsedFileFacts)
            identical(region, fragment.parsedRoleRegion)
            identical(roleSnapshot, fragment.parsedRoleSnapshot)
            identical(input, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            identical(result, fragment.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY))
            identical(diagnosticScope, fragment.parsingDiagnosticScopeOrNull())
            equal(moduleName, fragment.parsedLanguageModuleName)
            identical(candidateTable, root.charTable)
            val replayView = fragment.currentPsiSourceView()
            // SDK 可以更新文件 stamp；完整 basis 的冻结 context/input 必须继续使用原对象。
            identical(fullView.sourceContext, replayView.sourceContext)
            identical(fullView.lexicalInput, replayView.lexicalInput)
            replayView.requireCurrent()

            val body = fragment.getContentElement().node as CompositeElement
            val statement = present(body.findChildByType(CjNodeTypes.VARIABLE))
            val sameTextStatement = statement.copyElement()
            distinct(statement, sameTextStatement)
            equal(statement.text, sameTextStatement.text)
            // 正式 AST replaceChild 生成全新正文 change set；不直接调用任何 POM listener。
            commit { body.replaceChild(statement, sameTextStatement) }
            equal(3, events.size)
            val bodyChange = present(events.last().getChangeSet(aspect) as? TreeChangeEvent)
            distinct(nativeChange, bodyChange)
            identical(root, bodyChange.rootElement)
            equal(listOf(body), bodyChange.changedElements.toList())
            identical(root, fragment.node)
            identical(candidateTable, root.charTable)
            identical(root.charTable, SharedImplUtil.findCharTableByTree(root))
            equal(newText, fragment.text)
            identical(result, fragment.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY))
            identical(input, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            absent(fragment.parsedFileFacts)
            absent(fragment.parsedRoleRegion)
            absent(fragment.parsedRoleSnapshot)
            absent(fragment.parsedLanguageModuleName)
            fails<IllegalStateException> { replayView.requireCurrent() }
            val bodyView = fragment.currentPsiSourceView()
            distinct(fullView.sourceContext.snapshot, bodyView.sourceContext.snapshot)
            distinct(fullView.lexicalInput, bodyView.lexicalInput)
            equal(newText, bodyView.sourceContext.snapshot.text)
            bodyView.requireCurrent()
        } finally {
            model.removeModelListener(observer)
        }
    }

    @Test
    fun renamedFilePublishesCurrentMetadataAndKeepsTheFrozenLexicalOrigin() {
        val file = createPsiFile("beforeRename", "func f(): Unit { let tokens = quote(§) }") as CjFile
        file.collectParsingDiagnosticPublication()
        val oldView = file.currentPsiSourceView()
        val origin = present(unknownLeaves(file.node).single().getCopyableUserData(PARSER_LEXICAL_DIAGNOSTIC_KEY))
        val producerPath = origin.source.filePath
        commit { present(file.virtualFile).rename(this, "afterRename.cj") }
        val renamed = file.currentPsiSourceView()
        distinct(oldView, renamed)
        equal(file.name, renamed.sourceContext.snapshot.fileName)
        equal(file.virtualFile?.path, renamed.sourceContext.snapshot.filePath)
        equal(producerPath, origin.source.filePath)
        identical(origin, unknownLeaves(file.node).single().getCopyableUserData(PARSER_LEXICAL_DIAGNOSTIC_KEY))
        val diagnostic = file.collectParsingDiagnostics().single()
        identical(renamed.sourceContext.snapshot, present(diagnostic.mainRange).source)
        renamed.requireCurrent()
        fails<IllegalStateException> { oldView.requireCurrent() }
    }

    @Test
    fun concurrentReadActionsInstallAndUseOneSourceGeneration() {
        val file = createPsiFile("concurrentView", "func f(): Unit {}") as CjFile
        file.node.firstChildNode
        val executor = Executors.newFixedThreadPool(2)
        val barrier = CyclicBarrier(2)
        try {
            val readers = (0..1).map {
                executor.submit<CjPsiSourceView> {
                    ReadAction.compute<CjPsiSourceView, RuntimeException> {
                        barrier.await(10, TimeUnit.SECONDS)
                        file.currentPsiSourceView().also { view -> view.requireCurrent() }
                    }
                }
            }
            val first = readers[0].get(15, TimeUnit.SECONDS)
            val second = readers[1].get(15, TimeUnit.SECONDS)
            identical(first, second)
            first.requireCurrent()
            second.requireCurrent()
        } finally {
            executor.shutdownNow()
        }
    }

    private fun unknownLeaves(node: ASTNode): List<ASTNode> = buildList {
        if (node.elementType == CjTokens.UNKNOWN_TOKEN_START) add(node)
        var child = node.firstChildNode
        while (child != null) {
            addAll(unknownLeaves(child))
            child = child.treeNext
        }
    }

    /** 用现有 native 构造入口传递完整真实 SOURCE seed，仅关闭错误报告，不伪造 AST 容器。 */
    private fun nativeFragmentWithoutErrorReports(prototype: CjCodeFragment): CjCodeFragment {
        truth(!prototype.isContentsLoaded)
        val original = prototype.fragmentParserSeed
        equal(CjSourceKind.SOURCE, original.sourceKind)
        val seed = original.copy(context = original.context.copy(shouldReportError = false))
        return prototype.createCompilationFragment(prototype.viewProvider.contents.toString(), seed, null).also {
            identical(seed, it.fragmentParserSeed)
            identical(seed, it.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            truth(!it.isContentsLoaded)
        }
    }

    /** 空 type/block 保留正式文法容器，其 children 为零；空 expression 的 native 根直接为零孩子。 */
    private fun assertEmptyNativeGrammar(fragment: CjCodeFragment, root: FileElement) {
        when (fragment) {
            is CjTypeCodeFragment -> {
                val type = present(fragment.getContentElement()).node
                equal(CjNodeTypes.TYPE_REFERENCE, type.elementType)
                identical(type, root.firstChildNode)
                identical(type, root.lastChildNode)
                absent(type.firstChildNode)
                absent(type.lastChildNode)
            }
            is CjBlockCodeFragment -> {
                val body = fragment.getContentElement().node
                equal(CjNodeTypes.BLOCK, body.elementType)
                identical(body, root.firstChildNode)
                identical(body, root.lastChildNode)
                absent(body.firstChildNode)
                absent(body.lastChildNode)
            }
            is CjExpressionCodeFragment -> {
                absent(fragment.getContentElement())
                absent(root.firstChildNode)
                absent(root.lastChildNode)
            }
            else -> error("Unexpected native fragment type: ${fragment.javaClass.name}")
        }
    }

    /** SDK 的实际 PSI 变化使用与既有解析测试相同的 EDT、命令和写事务。 */
    private fun commit(action: () -> Unit) {
        try {
            EventQueue.invokeAndWait {
                CommandProcessor.getInstance().executeCommand(project, {
                    ApplicationManager.getApplication().runWriteAction(action)
                }, "Commit source view", null)
            }
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }
}
