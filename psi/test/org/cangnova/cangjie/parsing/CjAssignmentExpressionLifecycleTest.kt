package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.SyntaxTreeBuilder
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingWarning
import org.cangnova.cangjie.parsing.CangJieExpressionParsing.ParsedExpression
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertFalse as falseValue
import kotlin.test.assertNotNull as present
import kotlin.test.assertTrue as truth

/**
 * 使用真实 SDK builder 和共同 expression parser 返回产品，不构造 ParsedExpression 或反射。
 * N ParseCallExpr / ParseMemberAccess 的完成边界与 InvalidExpr 的显式 IS_BROKEN 是产品 oracle。
 * marker 消息是事务标签，不是新增的仓颉语言诊断期望；计数按实际完成的错误 production 验证。
 */
class CjAssignmentExpressionLifecycleTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun brokenCallHasBrokenWithoutBreakingItsReferenceCallee() {
        val parsed = parse("f(,)")
        assertRoot(parsed.expression, ParsedExpression.CallRoot, 0, 4, hasBroken = true)
        assertRoot(present(parsed.expression.base), ParsedExpression.RefRoot, 0, 1)
        truth(parsed.syntaxErrors > 0)
    }

    @Test
    fun selectorArgumentsNeverRewriteTheCompletedMemberOrCallee() {
        for ((text, calleeText) in listOf(
            "obj.make(,)" to "obj.make",
            "obj.make<Int64>(,)" to "obj.make<Int64>",
            "obj.part.make(,)" to "obj.part.make",
        )) {
            val parsed = parse(text)
            val call = parsed.expression
            assertRoot(call, ParsedExpression.CallRoot, 0, text.length, hasBroken = true)
            val member = present(call.base)
            assertRoot(member, ParsedExpression.MemberRoot, 0, calleeText.length)
            val receiver = present(member.base)
            falseValue(receiver.hasBroken, text)
            falseValue(receiver.isBroken, text)
            val name = member.children.single()
            val nameStart = calleeText.lastIndexOf('.') + 1
            assertRoot(name, ParsedExpression.RefRoot, nameStart, calleeText.length)
            truth(parsed.syntaxErrors > 0, text)
        }
    }

    @Test
    fun tupleAndParenthesisRetainTheActualChildKindsAndBrokenAttributes() {
        val tuple = parse("(f(,), x)").expression
        assertShape(tuple, ParsedExpression.TupleRoot, 0, 9)
        falseValue(tuple.isBroken)
        equal(2, tuple.children.size)
        assertRoot(tuple.children[0], ParsedExpression.CallRoot, 1, 5, hasBroken = true)
        assertRoot(present(tuple.children[0].base), ParsedExpression.RefRoot, 1, 2)
        assertRoot(tuple.children[1], ParsedExpression.RefRoot, 7, 8)
        val missing = parse("(, f())").expression
        assertShape(missing, ParsedExpression.TupleRoot, 0, 7)
        falseValue(missing.isBroken)
        assertShape(missing.children[0], ParsedExpression.Missing, 1, 1)
        truth(missing.children[0].isBroken)
        assertRoot(missing.children[1], ParsedExpression.CallRoot, 3, 6)
        val parenthesized = parse("(f(,))").expression
        assertShape(parenthesized, ParsedExpression.ParenRoot, 0, 6)
        falseValue(parenthesized.isBroken)
        assertRoot(present(parenthesized.base), ParsedExpression.CallRoot, 1, 5, hasBroken = true)
    }

    @Test
    fun invalidLeftHandGrammarPreservesBothOperandsAndTheirOriginalRanges() {
        val parsed = parse("(x) = rhs")
        assertRoot(parsed.expression, ParsedExpression.AssignmentRoot, 0, 9, hasBroken = true)
        equal(2, parsed.expression.children.size)
        val left = parsed.expression.children[0]
        assertShape(left, ParsedExpression.ParenRoot, 0, 3)
        assertRoot(present(left.base), ParsedExpression.RefRoot, 1, 2)
        assertRoot(parsed.expression.children[1], ParsedExpression.RefRoot, 6, 9)
        equal(1L, parsed.syntaxErrors)
    }

    @Test
    fun rollbackRestoresSyntaxErrorCountCursorAndReparsedExpressionFacts() {
        for (precede in listOf(false, true)) {
            val session = session("obj.make(,)")
            val speculation = session.builder.mark()
            val before = session.builder.macroParsingJournal.syntaxErrorCount
            val first = expression(session)
            truth(first.hasBroken)
            falseValue(first.isBroken)
            truth(session.builder.macroParsingJournal.syntaxErrorCount > before)
            equal(session.text.length, session.builder.lastConsumedTokenEndOffset)
            if (precede) {
                speculation.done(CjNodeTypes.REFERENCE_EXPRESSION)
                speculation.precede().rollbackTo()
            } else {
                speculation.rollbackTo()
            }
            equal(before, session.builder.macroParsingJournal.syntaxErrorCount)
            session.builder.tokenType
            equal(0, session.builder.currentOffset)
            equal(null, session.builder.lastConsumedTokenEndOffset)
            val reparsed = expression(session)
            equal(first, reparsed)
            truth(session.builder.macroParsingJournal.syntaxErrorCount > before)
            finish(session)
        }
    }

    /** 不依赖 collector 去重：每个实际错误载体应且仅应计数一次。 */
    @Test
    fun everySdkErrorCompletionCountsItsOwnProductionExactlyOnce() {
        for (mode in listOf(
            "builderError", "markerError", "done", "collapse",
            "errorBeforePsi", "errorBeforeSyntax",
            "doneBeforePsi", "doneBeforeSyntax",
            "messageBeforePsi", "messageBeforeSyntax",
            "twoBeforePsi", "twoBeforeSyntax",
        )) {
            val session = session("a")
            val builder = session.builder
            val journal = builder.macroParsingJournal
            val owner = builder.mark()
            builder.advanceLexer()
            // 只有 before 重载拥有后续未完成 marker；普通 done/error 必须遵守 SDK 嵌套完成顺序。
            val boundary = if (mode.contains("Before")) builder.mark() else null
            fun beforeMarker() = checkNotNull(boundary)
            fun errorType() = CjSyntaxErrorElementType("transaction error", journal.nextDiagnosticOrder())
            equal(0L, journal.syntaxErrorCount, mode)
            when (mode) {
                "builderError" -> { builder.error("transaction error"); owner.drop() }
                "markerError" -> owner.error("transaction error")
                "done" -> owner.done(errorType())
                "collapse" -> owner.collapse(errorType())
                "errorBeforePsi" -> owner.errorBefore("transaction error", beforeMarker())
                "errorBeforeSyntax" -> (owner as SyntaxTreeBuilder.Marker).errorBefore("transaction error", beforeMarker())
                "doneBeforePsi" -> owner.doneBefore(errorType(), beforeMarker())
                "doneBeforeSyntax" -> (owner as SyntaxTreeBuilder.Marker).doneBefore(errorType(), beforeMarker())
                "messageBeforePsi" -> owner.doneBefore(CjNodeTypes.REFERENCE_EXPRESSION, beforeMarker(), "independent error")
                "messageBeforeSyntax" -> (owner as SyntaxTreeBuilder.Marker).doneBefore(CjNodeTypes.REFERENCE_EXPRESSION, beforeMarker(), "independent error")
                "twoBeforePsi" -> owner.doneBefore(errorType(), beforeMarker(), "independent error")
                "twoBeforeSyntax" -> (owner as SyntaxTreeBuilder.Marker).doneBefore(errorType(), beforeMarker(), "independent error")
                else -> error("Unknown transaction fixture: $mode")
            }
            equal(if (mode.startsWith("twoBefore")) 2L else 1L, journal.syntaxErrorCount, mode)
            boundary?.drop()
            finish(session)
        }
    }

    @Test
    fun dropKeepsCompletedChildCountWhileRollbackRevokesTheWholeSpeculation() {
        for (precede in listOf(false, true)) {
            val session = session("a")
            val builder = session.builder
            val journal = builder.macroParsingJournal
            builder.error("before speculation")
            equal(1L, journal.syntaxErrorCount)
            val speculation = builder.mark()
            val owner = builder.mark()
            val child = builder.mark()
            builder.advanceLexer()
            child.error("surviving child")
            owner.error("dropped parent")
            equal(3L, journal.syntaxErrorCount)
            owner.drop()
            equal(3L, journal.syntaxErrorCount)
            if (precede) {
                speculation.done(CjNodeTypes.REFERENCE_EXPRESSION)
                speculation.precede().rollbackTo()
            } else {
                speculation.rollbackTo()
            }
            equal(1L, journal.syntaxErrorCount)
            builder.tokenType
            equal(0, builder.currentOffset)
            val clean = expression(session)
            assertRoot(clean, ParsedExpression.RefRoot, 0, 1)
            equal(1L, journal.syntaxErrorCount)
            finish(session)
        }
    }

    /**
     * warning、角色事件和预留诊断 scope 使用真实 journal API；消息只作事务标签。
     * 错误发生前已有的这些事件不得授 HAS_BROKEN 或使非法 (x) LHS 被去重门吞掉。
     */
    @Test
    fun warningsReservedScopesAndRoleClockDoNotPolluteExpressionBrokenFacts() {
        for (text in listOf("x", "(x) = rhs")) {
            val session = session(text)
            val journal = session.builder.macroParsingJournal
            val type = present(session.builder.tokenType)
            val tokenEnd = session.builder.currentOffset + present(session.builder.tokenText).length
            journal.captureSourceToken(session.builder.currentOffset, tokenEnd, type)
            journal.reportParsingWarning(0, ParsingDiagnostic(
                0, tokenEnd, "transaction warning", rangeKind = ParsingDiagnostic.RangeKind.TOKEN,
                severity = ParsingDiagnostic.Severity.WARNING,
                warning = ParsingWarning.MacroReport("transaction warning", ""),
            ))
            journal.nextDiagnosticOrder()
            journal.reserveBodyDiagnosticScope(0)
            journal.registerRoleEvent(0, CjmpRoleEventKind.Unknown)
            equal(0L, journal.syntaxErrorCount)
            val parsed = expression(session)
            if (text == "x") {
                assertRoot(parsed, ParsedExpression.RefRoot, 0, 1)
                equal(0L, journal.syntaxErrorCount)
            } else {
                assertRoot(parsed, ParsedExpression.AssignmentRoot, 0, text.length, hasBroken = true)
                // 此 fixture 唯一错误 production 是 N 对 (x) 的非法赋值报告。
                equal(1L, journal.syntaxErrorCount)
            }
            finish(session)
        }
    }

    /**
     * N ParseExpr.cpp:499–601 的循环仍注册本轮正常 RHS；
     * 首 LHS 错误与后续 RHS HAS_BROKEN 分开，不能把整个前一 Assignment 的 broken 当重复门。
     */
    @Test
    fun chainedAssignmentsKeepNormalAndBrokenRhsProductsWithoutSwallowingLaterOperands() {
        // N 在 nonassoc 报告时可给当前 RHS 标 HAS_BROKEN；此处只认证原操作数形状/范围。
        val parsed = parse("(x) = normal >>= middle += tail")
        assertShape(parsed.expression, ParsedExpression.AssignmentRoot, 0, 31)
        equal(2, parsed.expression.children.size)
        assertRoot(parsed.expression.children[1], ParsedExpression.RefRoot, 27, 31)
        val middle = parsed.expression.children[0]
        assertShape(middle, ParsedExpression.AssignmentRoot, 0, 23)
        assertShape(middle.children[1], ParsedExpression.RefRoot, 17, 23)
        val first = middle.children[0]
        assertShape(first, ParsedExpression.AssignmentRoot, 0, 12)
        assertShape(first.children[0], ParsedExpression.ParenRoot, 0, 3)
        assertShape(first.children[1], ParsedExpression.RefRoot, 6, 12)
        equal(3L, parsed.syntaxErrors)

        val broken = parse("a = f(,) >>= c += d")
        val second = broken.expression.children[0]
        val initial = second.children[0]
        assertRoot(initial.children[1], ParsedExpression.CallRoot, 4, 8, hasBroken = true)
        assertRoot(present(initial.children[1].base), ParsedExpression.RefRoot, 4, 5)
        assertShape(second.children[1], ParsedExpression.RefRoot, 13, 14)
        assertRoot(broken.expression.children[1], ParsedExpression.RefRoot, 18, 19)
        // 参数错误 + 第三 operator 的独立 nonassoc；不以未实编译的参数错误全集固定总数。
        truth(broken.syntaxErrors > 1L)
    }

    /**
     * N ParseExpr.cpp:119–127,153–199,579–601,646–672,745–794：
     * LET_PATTERN 正式入口停在低于 RANGE 的 assignment token，观察真实赋值前 postfix 产品；
     * rollback 后走 ALL 正式入口，赋值和 RHS 完成后才出现 whole OptionalChain wrapper。
     */
    @Test
    fun optionalPostfixRootsExistBeforeAssignmentAndWrapTheWholeAssignmentAfterRhs() {
        val cases = listOf(
            OptionalCase("obj?.value", ParsedExpression.MemberRoot,
                listOf(ParsedExpression.OptionalRoot, ParsedExpression.RefRoot)),
            OptionalCase("obj?[slot]", ParsedExpression.IndexRoot,
                listOf(ParsedExpression.OptionalRoot, ParsedExpression.RefRoot)),
            OptionalCase("f?().value", ParsedExpression.MemberRoot,
                listOf(ParsedExpression.CallRoot, ParsedExpression.OptionalRoot, ParsedExpression.RefRoot)),
            OptionalCase("f?()[slot]", ParsedExpression.IndexRoot,
                listOf(ParsedExpression.CallRoot, ParsedExpression.OptionalRoot, ParsedExpression.RefRoot)),
            OptionalCase("obj?.make().value", ParsedExpression.MemberRoot,
                listOf(ParsedExpression.CallRoot, ParsedExpression.MemberRoot, ParsedExpression.OptionalRoot, ParsedExpression.RefRoot)),
            OptionalCase("obj.make?().value", ParsedExpression.MemberRoot,
                listOf(ParsedExpression.CallRoot, ParsedExpression.OptionalRoot, ParsedExpression.MemberRoot, ParsedExpression.RefRoot)),
            OptionalCase("f?()", ParsedExpression.CallRoot,
                listOf(ParsedExpression.OptionalRoot, ParsedExpression.RefRoot), invalid = true),
            OptionalCase("obj.make?()", ParsedExpression.CallRoot,
                listOf(ParsedExpression.OptionalRoot, ParsedExpression.MemberRoot, ParsedExpression.RefRoot), invalid = true),
        )
        for (case in cases) {
            for (operator in listOf("=", ">>=")) assertOptionalAssignment(case, operator)
        }
    }

    /** 先完成的 optional wrapper 不允许穿过真实 Paren 为 Member/Index.base 补授 leftAux。 */
    @Test
    fun standaloneOptionalWrappersAndParenthesizedBasesKeepDistinctGrammarRoles() {
        for ((text, kind) in listOf(
            "obj?.value" to ParsedExpression.MemberRoot,
            "obj?[slot]" to ParsedExpression.IndexRoot,
            "f?()" to ParsedExpression.CallRoot,
        )) {
            val expression = parse(text).expression
            assertRoot(expression, ParsedExpression.OptionalChainRoot, 0, text.length)
            assertRoot(present(expression.base), kind, 0, text.length)
        }
        for ((lhs, kind) in listOf(
            "(f?()).value" to ParsedExpression.MemberRoot,
            "(obj?.value)[slot]" to ParsedExpression.IndexRoot,
        )) {
            val text = "$lhs = rhs"
            val parsed = parse(text)
            assertRoot(parsed.expression, ParsedExpression.AssignmentRoot, 0, text.length, hasBroken = true)
            val left = parsed.expression.children[0]
            assertShape(left, kind, 0, lhs.length)
            val parenthesized = present(left.base)
            equal(ParsedExpression.ParenRoot, parenthesized.root)
            equal(ParsedExpression.OptionalChainRoot, present(parenthesized.base).root)
            assertRoot(parsed.expression.children[1], ParsedExpression.RefRoot, lhs.length + 3, text.length)
            equal(1L, parsed.syntaxErrors)
        }
    }

    private data class OptionalCase(
        val lhs: String, val kind: ParsedExpression.Root,
        val bases: List<ParsedExpression.Root>, val invalid: Boolean = false,
    )

    private fun assertOptionalAssignment(case: OptionalCase, operator: String) {
        val lhs = case.lhs
        val rhs = "other?.value"
        val text = "$lhs $operator $rhs"
        val session = session(text)
        val speculation = session.builder.mark()
        val before = with(AbstractCangJieParsing.ParsingContext.DEFAULT) {
            session.parser.expressionParsing.parseExpression(CjExprKind.LET_PATTERN)
        }
        assertRoot(before, case.kind, 0, lhs.length)
        truth(before.hasQuestSuffix)
        falseValue(before.root == ParsedExpression.OptionalChainRoot)
        assertBases(before, case.bases)
        equal(lhs.length + 1, session.builder.currentOffset)
        equal(0L, session.builder.macroParsingJournal.syntaxErrorCount)
        speculation.rollbackTo()
        equal(0L, session.builder.macroParsingJournal.syntaxErrorCount)
        val after = expression(session)
        assertRoot(after, ParsedExpression.OptionalChainRoot, 0, text.length, hasBroken = case.invalid)
        val assignment = present(after.base)
        assertRoot(assignment, ParsedExpression.AssignmentRoot, 0, text.length, hasBroken = case.invalid)
        equal(2, assignment.children.size)
        val left = assignment.children[0]
        assertShape(left, case.kind, 0, lhs.length)
        truth(left.hasQuestSuffix)
        assertBases(left, case.bases)
        val right = assignment.children[1]
        val rhsStart = lhs.length + operator.length + 2
        assertRoot(right, ParsedExpression.OptionalChainRoot, rhsStart, text.length)
        val rightMember = present(right.base)
        assertRoot(rightMember, ParsedExpression.MemberRoot, rhsStart, text.length)
        assertBases(rightMember, listOf(ParsedExpression.OptionalRoot, ParsedExpression.RefRoot))
        equal(if (case.invalid) 1L else 0L, session.builder.macroParsingJournal.syntaxErrorCount)
        finish(session)
    }

    /** 只断言有直接 N 依据的 base 拓扑；OptionalExpr.end 对 quest 的表示不冒称位级镜像。 */
    private fun assertBases(expression: ParsedExpression, expected: List<ParsedExpression.Root>) {
        var base = expression.base
        for (root in expected) {
            val actual = present(base)
            equal(root, actual.root)
            falseValue(actual.isBroken)
            base = actual.base
        }
        equal(null, base)
    }

    private fun parse(text: String): Parsed {
        val session = session(text)
        val result = expression(session)
        val count = session.builder.macroParsingJournal.syntaxErrorCount
        finish(session)
        return Parsed(result, count)
    }

    /** 调用共同 parser 的正式表达式入口；不新建形状判定器或从 AST 后代推断产品。 */
    private fun expression(session: Session): ParsedExpression =
        with(AbstractCangJieParsing.ParsingContext.DEFAULT) { session.parser.expressionParsing.parseExpression() }

    private fun session(text: String): Session {
        val delegate = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        delegate.setDebugMode(true)
        val builder = SemanticWhitespaceAwarePsiBuilderImpl(delegate)
        val root = builder.mark()
        val parser = CangJieParsing.createForTopLevelNonLazy(builder)
        return Session(text, builder, root, parser)
    }

    private fun finish(session: Session) {
        truth(session.builder.eof(), "必须完整消费输入，禁止测试 helper 跳过残余 token：" + session.text)
        session.root.done(CjNodeTypes.CJ_FILE)
        val tree = session.builder.lightTree
        equal(0, tree.root.startOffset)
        equal(session.text.length, tree.root.endOffset)
    }

    /** wrapper 的聚合 HAS_BROKEN 不是 N 节点的逐位镜像，形状断言不擅自认证该位。 */
    private fun assertShape(expression: ParsedExpression, root: ParsedExpression.Root, start: Int, end: Int) {
        equal(root, expression.root)
        equal(start, expression.startOffset)
        equal(end, expression.endOffset)
    }

    private fun assertRoot(
        expression: ParsedExpression, root: ParsedExpression.Root, start: Int, end: Int,
        hasBroken: Boolean = false, isBroken: Boolean = false,
    ) {
        equal(root, expression.root)
        equal(start, expression.startOffset)
        equal(end, expression.endOffset)
        equal(hasBroken, expression.hasBroken)
        equal(isBroken, expression.isBroken)
    }

    private data class Parsed(val expression: ParsedExpression, val syntaxErrors: Long)
    private data class Session(
        val text: String, val builder: SemanticWhitespaceAwarePsiBuilderImpl,
        val root: PsiBuilder.Marker, val parser: CangJieParsing,
    )
}
