package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.lang.LighterASTTokenNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertNotNull as assertPresent
import kotlin.test.assertNull as assertAbsent

/** 用实际 IntelliJ builder 验证仓颉 NL 的嵌套消费协议和物理 token 查询。 */
class SemanticWhitespaceAwarePsiBuilderNLTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun lookAheadCountsLogicalTokensAcrossEveryNewline() {
        val builder = builder("a\n+\n!b")

        assertValueEquals(CjTokens.IDENTIFIER, builder.lookAhead(0))
        assertValueEquals(CjTokens.PLUS, builder.lookAhead(1))
        assertValueEquals(CjTokens.EXCL, builder.lookAhead(2))
        assertValueEquals(CjTokens.IDENTIFIER, builder.lookAhead(3))
        assertValueEquals(CjTokens.NL, builder.rawLookup(1))
        assertValueEquals(CjTokens.NL, builder.rawLookup(3))

        builder.withNLTokenSkipping(false) {
            assertValueEquals(CjTokens.NL, builder.lookAhead(1))
            assertValueEquals(CjTokens.PLUS, builder.lookAhead(2))
            assertValueEquals(CjTokens.NL, builder.lookAhead(3))
            assertValueEquals(CjTokens.EXCL, builder.lookAhead(4))
            assertValueEquals(CjTokens.IDENTIFIER, builder.lookAhead(5))
        }

        assertValueEquals(CjTokens.PLUS, builder.lookAhead(1))
        assertValueEquals(true, builder.isNLTokenSkippingEnabled)
    }

    /** 首个 SDK marker 覆盖物理全文；读取逻辑游标才忽略其前导 NL。 */
    @Test
    fun firstRootMarkerPreservesPhysicalStartAndRollbackRestoresConsumedEnd() {
        val builder = builder("\na")
        assertValueEquals(CjTokens.NL, builder.rawLookup(0))

        val marker = builder.mark()
        assertValueEquals(0, marker.startOffset)
        assertValueEquals(CjTokens.NL, builder.rawLookup(0))
        assertValueEquals(1, builder.currentOffset)
        marker.rollbackTo()
        assertValueEquals(CjTokens.NL, builder.rawLookup(0))
        assertValueEquals(1, builder.currentOffset)

        val consumed = builder.mark()
        builder.advanceLexer()
        assertValueEquals(2, builder.lastConsumedTokenEndOffset)
        consumed.rollbackTo()
        assertAbsent(builder.lastConsumedTokenEndOffset)
        assertValueEquals(1, builder.currentOffset)
    }

    /** 前瞻可归一化当前位置；只为计数越过的未来 NL 必须保留原词元。 */
    @Test
    fun lookAheadNormalizesCurrentNewlinesWithoutRemappingFutureNewlines() {
        val builder = builder("\na\nb")
        val root = builder.mark()
        assertValueEquals(0, root.startOffset)

        assertValueEquals(CjTokens.IDENTIFIER, builder.lookAhead(0))
        assertValueEquals(CjTokens.IDENTIFIER, builder.lookAhead(1))
        assertAbsent(builder.lookAhead(2))
        assertValueEquals(1, builder.rawTokenIndex())
        assertValueEquals(TokenType.WHITE_SPACE, builder.rawLookup(-1))
        assertValueEquals(CjTokens.NL, builder.rawLookup(1))
        assertAbsent(builder.lastConsumedTokenEndOffset)

        builder.withNLTokenSkipping(false) {
            assertValueEquals(CjTokens.IDENTIFIER, builder.tokenType)
            assertValueEquals("a", builder.tokenText)
            assertValueEquals(1, builder.currentOffset)

            root.rollbackTo()
            assertValueEquals(0, builder.rawTokenIndex())
            assertValueEquals(CjTokens.NL, builder.rawLookup(0))
            assertValueEquals(CjTokens.NL, builder.tokenType)
            assertValueEquals("\n", builder.tokenText)
            assertValueEquals(0, builder.currentOffset)
            assertAbsent(builder.lastConsumedTokenEndOffset)
        }
    }

    /** 实际忽略 NL 使用 SDK 可回滚 remap；回滚后 opaque 模式必须重新看到同一个 NL。 */
    @Test
    fun ignoredNewlineRemappingRollsBackBeforeOpaqueConsumption() {
        val builder = builder("a\nb")
        val root = builder.mark()
        builder.advanceLexer()
        assertValueEquals(1, builder.lastConsumedTokenEndOffset)

        val checkpoint = builder.mark()
        assertValueEquals(1, checkpoint.startOffset)
        assertValueEquals(CjTokens.NL, builder.rawLookup(0))
        assertValueEquals(CjTokens.IDENTIFIER, builder.tokenType)
        assertValueEquals(2, builder.currentOffset)
        assertValueEquals(TokenType.WHITE_SPACE, builder.rawLookup(-1))
        assertValueEquals(1, builder.lastConsumedTokenEndOffset)

        builder.withNLTokenSkipping(false) {
            checkpoint.rollbackTo()
            assertValueEquals(1, builder.rawTokenIndex())
            assertValueEquals(CjTokens.NL, builder.rawLookup(0))
            assertValueEquals(CjTokens.NL, builder.tokenType)
            assertValueEquals("\n", builder.tokenText)
            assertValueEquals(1, builder.currentOffset)
            assertValueEquals(1, builder.lastConsumedTokenEndOffset)

            builder.advanceLexer()
            assertValueEquals(2, builder.lastConsumedTokenEndOffset)
            assertValueEquals(CjTokens.NL, builder.rawLookup(-1))
            assertValueEquals(CjTokens.IDENTIFIER, builder.tokenType)
        }
        root.drop()
    }

    /** 完成节点的 precede 仍拥有原事务起点，回滚必须撤销其间真正消费的 NL remap。 */
    @Test
    fun precedingCompletedOperandRestoresSkippedNewlineAndConsumedEndOnRollback() {
        val builder = builder("a\nb")
        val root = builder.mark()
        val operand = builder.mark()
        builder.advanceLexer()
        assertValueEquals(CjTokens.IDENTIFIER, builder.tokenType)
        assertValueEquals(2, builder.currentOffset)
        assertValueEquals(TokenType.WHITE_SPACE, builder.rawLookup(-1))
        assertValueEquals(1, builder.lastConsumedTokenEndOffset)
        operand.done(CjNodeTypes.REFERENCE_EXPRESSION)
        val wrapper = operand.precede()

        builder.withNLTokenSkipping(false) {
            wrapper.rollbackTo()
            assertValueEquals(0, builder.rawTokenIndex())
            assertAbsent(builder.lastConsumedTokenEndOffset)
            assertValueEquals(CjTokens.IDENTIFIER, builder.tokenType)
            assertValueEquals("a", builder.tokenText)

            builder.advanceLexer()
            assertValueEquals(CjTokens.NL, builder.rawLookup(0))
            assertValueEquals(CjTokens.NL, builder.tokenType)
            assertValueEquals("\n", builder.tokenText)
            assertValueEquals(1, builder.currentOffset)
            assertValueEquals(1, builder.lastConsumedTokenEndOffset)

            builder.advanceLexer()
            assertValueEquals(2, builder.lastConsumedTokenEndOffset)
            assertValueEquals(CjTokens.NL, builder.rawLookup(-1))
            assertValueEquals(CjTokens.IDENTIFIER, builder.tokenType)
        }
        root.drop()
    }

    /** 完整文件不能在首 marker 前丢弃 trivia，也不能将前导 NL 误判成片段起点。 */
    @Test
    fun completeFilesPreserveEveryLeafAndTheirPackageCarrier() {
        val sources = listOf(
            "empty" to "",
            "newlineOnly" to "\n",
            "multipleNewlinesOnly" to "\n\n",
            "crlfOnly" to "\r\n\r\n",
            "commentAndNewlines" to "// header\n\n",
            "commentAndCrlf" to "// header\r\n\r\n",
            "leadingNewlines" to "\n\nfunc f() {}\n",
            "leadingCrlf" to "\r\n\r\nfunc f() {}\r\n",
            "commentBeforeDeclaration" to "// header\n\nfunc f() {}\n",
            "leadingNewlinesBeforePackage" to "\n\npackage sample\n\nfunc f() {}\n",
        )
        val factory = CjPsiFactory(project, markGenerated = false)
        for ((name, text) in sources) {
            val file = factory.createFile("$name.cj", text)
            assertValueEquals(text, file.viewProvider.contents.toString(), "PSI original input: $name")
            val psiRoot = file.node
            assertValueEquals(text, psiLeafText(psiRoot), "PSI leaves: $name")
            assertValueEquals(0, psiRoot.startOffset, "PSI root start: $name")
            assertValueEquals(text.length, psiRoot.textRange.endOffset, "PSI root end: $name")
            assertValueEquals(text, psiRoot.text, "PSI root text: $name")
            assertValueEquals(
                1,
                psiRoot.getChildren(TokenSet.create(CjNodeTypes.PACKAGE_DIRECTIVE)).size,
                "PSI direct package carrier: $name",
            )
            assertValueEquals(emptyList<ParsingDiagnostic>(), file.collectParsingDiagnostics(), "PSI diagnostics: $name")
            val psiFacts = assertPresent(file.parsedFileFacts, "PSI published file facts: $name")

            val diagnostics = mutableListOf<ParsingDiagnostic>()
            val parsed = CangJieLightParser.parse(
                PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text),
                errorListener = { diagnostic -> diagnostics += diagnostic },
            )
            val lightRoot = parsed.tree.root
            assertValueEquals(0, lightRoot.startOffset, "LT root start: $name")
            assertValueEquals(text.length, lightRoot.endOffset, "LT root end: $name")
            assertValueEquals(text, lightLeafText(lightRoot, parsed.tree), "LT leaves: $name")
            assertValueEquals(1, directLightPackageCount(lightRoot, parsed.tree), "LT direct package carrier: $name")
            assertValueEquals(emptyList<ParsingDiagnostic>(), diagnostics, "LT diagnostics: $name")
            assertValueEquals(psiFacts, parsed.fileFacts, "Published file facts: $name")
        }
    }

    @Test
    fun eofAfterTrailingTriviaUsesLastConsumedNativeTokenEnd() {
        val builder = builder("a  /* tail */\n")
        while (!builder.eof()) builder.advanceLexer()

        assertValueEquals(true, builder.eof())
        assertValueEquals(1, builder.lastConsumedTokenEndOffset)
    }

    @Test
    fun truncatedLookAheadUsesTheSameNewlineModeAndCutoff() {
        val builder = builder("a\n+\nb")
        val truncated = TruncatedSemanticWhitespaceAwarePsiBuilder(builder, 3)

        assertValueEquals(CjTokens.IDENTIFIER, truncated.lookAhead(0))
        assertValueEquals(CjTokens.PLUS, truncated.lookAhead(1))
        assertAbsent(truncated.lookAhead(2))

        truncated.withNLTokenSkipping(false) {
            assertValueEquals(CjTokens.NL, truncated.lookAhead(1))
            assertValueEquals(CjTokens.PLUS, truncated.lookAhead(2))
            assertAbsent(truncated.lookAhead(3))
        }
    }

    private fun psiLeafText(node: ASTNode): String {
        var child = node.firstChildNode ?: return node.text
        val result = StringBuilder()
        while (true) {
            result.append(psiLeafText(child))
            child = child.treeNext ?: break
        }
        return result.toString()
    }

    /** 读取真实 token 文本，不能用源码范围切片代替叶文本；借出的数组必须归还 SDK。 */
    private fun lightLeafText(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): String {
        if (node is LighterASTTokenNode) return node.text.toString()
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get()
        try {
            return buildString {
                for (index in 0 until count) {
                    append(lightLeafText(assertPresent(children?.get(index)), tree))
                }
            }
        } finally {
            if (children != null) tree.disposeChildren(children, count)
        }
    }

    private fun directLightPackageCount(
        node: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
    ): Int {
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get()
        try {
            return (0 until count).count { index ->
                assertPresent(children?.get(index)).tokenType == CjNodeTypes.PACKAGE_DIRECTIVE
            }
        } finally {
            if (children != null) tree.disposeChildren(children, count)
        }
    }

    private fun builder(text: String): SemanticWhitespaceAwarePsiBuilderImpl =
        SemanticWhitespaceAwarePsiBuilderImpl(
            PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text),
        )
}
