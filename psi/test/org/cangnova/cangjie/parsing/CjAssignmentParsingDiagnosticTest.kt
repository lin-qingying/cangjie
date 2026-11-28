package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.messages.CjParsingDiagnosticScope
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjMacroCodeFragment
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertNotNull as present
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as truth

/**
 * 固定 N=14276740ffec1262b81b6b5236b1d840af270b37，Git 对象库 external/cangjie_compiler：
 * ParseExpr.cpp:130–199,499–672,745–794,954–1037 与 ParserDiag.cpp:774–817 是语言 oracle。
 * K=3c7a5041f238102285764d370665f56f587f9dc4 只证明共享 operation/RHS/marker 框架，不授仓颉文法。
 * 新增 A/B/C 期望均来自固定 N 或真实输出/seed 的工程契约；未运行 cjc/Gradle，未从 actual 填 expected。
 * 每个入口分别断言 operator TOKEN、各非法 child 的完整 hint 和最终首报。
 * 这里只认证 parser 文法；未绑定的 ref/call/index 不构成 Sema 合法程序的证明。
 */
class CjAssignmentParsingDiagnosticTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun referenceMemberIndexAndCallBasedChainsPassAssignmentGrammar() {
        for (lhs in listOf(
            "x", "obj.value", "items[slot]", "obj.items[slot].value",
            "make().value", "make()[slot]", "obj.make().items[slot].value",
        )) {
            checkCase(lhs, rejected = emptyList())
        }
    }

    /** 文档中的全部 compound token 共用左侧入口；RHS 类型适配不在 parser 测试内。 */
    @Test
    fun everyCompoundOperatorAcceptsAReferenceAndRejectsAParenthesizedReference() {
        for (operator in COMPOUND_OPERATORS) {
            checkCase("x", operator, rejected = emptyList())
            checkCase("(x)", operator)
        }
    }

    @Test
    fun conditionalArgumentsIndicesAndRightHandSidesDoNotChangeTheCalleeGrammar() {
        val conditional = "if (flag) { first } else { second }"
        checkCase("select($conditional).value", rejected = emptyList())
        checkCase("obj.select($conditional).value", "+=", rejected = emptyList())
        checkCase("items[$conditional]", rejected = emptyList())
        checkCase("select(x).value", rhs = conditional, rejected = emptyList())
        checkCase("select($conditional).value", rhs = conditional, rejected = emptyList())
        checkCase("select($conditional).value", ">>=", conditional, rejected = emptyList())
    }

    @Test
    fun parenthesesNeverGrantLeftValueOrLeftAuxEligibility() {
        for (lhs in listOf(
            "(x)", "(x).value", "(x)[slot]",
            "(make())", "(make()).value", "(make())[slot]",
            "(if (flag) { first } else { second })",
            "(if (flag) { first } else { second }).value",
            "(if (flag) { first } else { second })[slot]",
            "((x)).value", "make()", "obj.make()",
        )) {
            checkCase(lhs)
        }
    }

    @Test
    fun simpleTupleChecksEachChildAndRetainsOnlyTheFirstPublishedReport() {
        checkCase("(x, obj.value, make()[slot], _)", rejected = emptyList())
        checkCase("(1, (x), f(), obj.g())", rejected = listOf("1", "(x)", "f()", "obj.g()"))
        // 嵌套 tuple 返回 true，但内部非法 child 的独立 producer 不能丢失或被重复报告。
        checkCase("(x, (f(), (y)), z)", rejected = listOf("f()", "(y)"))
    }

    @Test
    fun compoundTupleRestrictionIsIndependentOfChildEligibilityAndBrokenState() {
        for (operator in COMPOUND_OPERATORS) {
            checkCase("(x, y)", operator, rejected = emptyList(), tupleCompound = true)
            checkCase("(f(), (x))", operator, rejected = listOf("f()", "(x)"), tupleCompound = true)
        }
        checkCase("(f(,), x)", "+=", rejected = listOf("f(,)"), tupleCompound = true, broken = true)
    }

    /** 参数的 InvalidExpr 只使 Call HAS_BROKEN；Call 不是 IS_BROKEN，仍须报告 tuple child。 */
    @Test
    fun brokenCallArgumentsDoNotSuppressTheTupleChildLeftHandReport() {
        for (call in listOf("f(,)", "obj.make(,)", "obj.make<Int64>(,)")) {
            checkCase("($call, x)", rejected = listOf(call), broken = true)
        }
        // 真实 Missing child 有 IS_BROKEN，不再制造一条非法 LHS；后面的 bare Call 仍报告。
        checkCase("(, f())", rejected = listOf("f()"), broken = true)
        // 首 assignment 的 HAS_BROKEN 门与 tuple child 的 IS_BROKEN 门是两种职责。
        checkCase("f(,)", rejected = emptyList(), broken = true)
    }

    @Test
    fun shiftRightAssignmentKeepsTheWholeTokenAndTheWholeMultilineHint() {
        checkCase("(x)", ">>=")
        checkCase("(if (flag) {\n        first\n    } else {\n        second\n    }).value", ">>=")
    }

    /**
     * N ParseExpr.cpp:499–601 / ParserDiag.cpp:810–817：
     * 第二/第三 operator 以紧前 operator 作 hint；前一 RHS 的 HAS_BROKEN 才控制重复门。
     * 下面所有 offset 是输入常量的独立几何，不读取 actual 或上一报告来产生期望。
     */
    @Test
    fun multipleAssignmentOperatorsKeepEachTokenPreviousHintAndRhsBrokenGate() {
        val cases = listOf(
            ChainCase("a = b = c = d", listOf(
                nonassoc(6, 7, 2, 3), nonassoc(10, 11, 6, 7),
            )),
            ChainCase("a >>= b += c <<= d", listOf(
                nonassoc(8, 10, 2, 5), nonassoc(13, 16, 8, 10),
            )),
            ChainCase("(x) = normal >>= middle += tail", listOf(
                invalidLhs(4, 5, "=", 0, 3), nonassoc(13, 16, 4, 5), nonassoc(24, 26, 13, 16),
            )),
            // RHS f(,) 只有 HAS_BROKEN：第二 operator 不报；第三 operator 的 c 是正常 RHS，仍报。
            ChainCase("a = f(,) >>= c += d", listOf(nonassoc(15, 17, 9, 12)), broken = true),
            ChainCase("(x) = f(,) >>= c += d", listOf(
                invalidLhs(4, 5, "=", 0, 3), nonassoc(17, 19, 11, 14),
            ), broken = true),
            ChainCase("a = b += f(,) >>= d", listOf(nonassoc(6, 8, 2, 3)), broken = true),
            ChainCase("a = f(,) >>= g(,) += d", emptyList(), broken = true),
        )
        for (case in cases) {
            // 各 current operator 的完整范围不同，N 两层去重保留整个显式列表，绝非全文件 take(1)。
            checkExpression(case.text, case.expected, case.expected, case.broken)
        }
    }

    /**
     * N ParseExpr.cpp:119–127,153–199,579–601,646–672,775–794,954–1037：
     * assignment 前 Call/Member/Index 仍按真实 base 查询；RHS 后的 wrapper 不能补授非法 LHS。
     */
    @Test
    fun optionalAssignmentsPublishIndependentlyWithoutGrantingBareQuestCallsALeftValue() {
        for (lhs in listOf(
            "obj?.value", "obj?[slot]", "f?().value", "f?()[slot]",
            "obj?.make().value", "obj.make?().value", "obj?[slot].value", "obj?.items?[slot]",
        )) {
            checkCase(lhs, rejected = emptyList(), optionalAssignment = true)
            checkCase(lhs, ">>=", rhs = "other?.value", rejected = emptyList(), optionalAssignment = true)
        }
        for ((lhs, optionalAssignment) in listOf(
            "f?()" to true, "obj.make?()" to true, "(f?()).value" to false,
            "(obj?.value)[slot]" to false, "(x)?.value" to true,
        )) {
            checkCase(lhs, optionalAssignment = optionalAssignment)
            checkCase(lhs, "+=", optionalAssignment = optionalAssignment)
        }
    }

    /**
     * expected 来自调用者明确列出的 N 分类，不能从解析出的树或 message 推导。
     * broken 参数只声明存在参数/缺表达式语法错误；未认证其错误全集的文本与数量。
     * 所有额外诊断仍进入两入口全值 parity 与规范范围检查，不删改 actual。
     */
    private fun checkCase(
        lhs: String,
        operator: String = "=",
        rhs: String = "rhs",
        rejected: List<String> = listOf(lhs),
        tupleCompound: Boolean = false,
        broken: Boolean = false,
        optionalAssignment: Boolean = false,
    ) {
        val expected = expectedReports(lhs, operator, rejected, tupleCompound)
        checkExpression("$lhs $operator $rhs", expected, expected.take(1), broken, optionalAssignment)
    }

    /** 每个 caller 明确给 reports 和最终 publication，单 operator tuple 才在 caller 取首报。 */
    private fun checkExpression(
        expression: String, expected: List<ParsingDiagnostic>,
        expectedPublished: List<ParsingDiagnostic>, broken: Boolean = false, optionalAssignment: Boolean = false,
    ) {
        val text = "$PREFIX$expression$SUFFIX"
        val fullExpected = expected.map { translate(it, PREFIX.length) }
        val fullPublished = expectedPublished.map { translate(it, PREFIX.length) }
        val file = CjPsiFactory(project, markGenerated = false).createFile(text)
        val psi = file.collectParsingDiagnosticPublication()
        val input = present(publishedParserSeed(file).lexicalInput)
        val context = present(publishedParserSeed(file).sourceContext)
        val target = CangJieParser.parseCompilationFile(file, CjmpParsingContext.Compilation(false, false, true))
        target.requireConsistentInput()
        identical(input, target.actualInputSeed.lexicalInput)
        val light = CangJieLightParser.parseBuffered(sdkBuilder(text), lexicalInput = input)
        light.file.requireConsistentInput()
        identical(input, light.file.lexicalInput)
        equal(CangJieLightDiagnosticOwnership.BUFFERED, light.file.diagnosticOwnership)
        equal(text, target.file.text)
        equal(0, light.file.tree.root.startOffset)
        equal(text.length, light.file.tree.root.endOffset)
        val psiProductOwner = psiOwner(file, psi)
        val targetOwner = psiOwner(target.file, target.diagnosticPublication)
        val lightProductOwner = lightOwner(light.file, psiProductOwner.regions)
        val routes = listOf("SDK PSI" to psiProductOwner, "compilation PSI" to targetOwner, "buffered LT" to lightProductOwner)
        for ((route, owner) in routes) {
            assertPublication(fullExpected, fullPublished, owner, context, broken, "$route: $expression")
        }
        assertPublicationsEqual(psiProductOwner, targetOwner, context, expression)
        assertPublicationsEqual(psiProductOwner, lightProductOwner, context, expression)

        // 真实 scoped fragment 直接 roots 入口，复用 PSI 发布的 seed/input；不手造 LT source。
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Expression(CjExprKind.ALL), CjMacroFragmentParent.OTHER,
            null, CjMacroDeclarationPrefix.EMPTY,
        )
        val seed = createMacroFragmentParserSeed(
            ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), CjmpParsingContext.SyntaxOnly, entry,
        )
        val fragment = CjMacroCodeFragment(project, "assignment.cj", expression, context = null, seed = seed)
        fragment.getContentElement()
        val actualSeed = assertPublishedParserSeed(seed, fragment)
        val fragmentContext = present(actualSeed.sourceContext)
        val fragmentPsi = fragment.collectParsingDiagnosticPublication()
        val fragmentLight = CangJieLightParser.parseMacroCodeFragmentBuffered(sdkBuilder(expression), actualSeed)
        fragmentLight.consume { lightFragment, published ->
            identical(actualSeed.lexicalInput, lightFragment.lexicalInput)
            lightFragment.requireConsistentInput()
            equal(1, fragment.getContentElement().roots.size, expression)
            equal(1, lightFragment.roots.size, expression)
            val expectedRoot = if (optionalAssignment) CjNodeTypes.OPTIONAL_CHAIN_EXPRESSION else CjNodeTypes.BINARY_EXPRESSION
            val psiExpression = fragment.getContentElement().roots.single().node
            val lightExpression = lightFragment.roots.single()
            equal(expectedRoot, psiExpression.elementType, expression)
            equal(expectedRoot, lightExpression.tokenType, expression)
            equal(0, psiExpression.startOffset, expression)
            equal(expression.length, psiExpression.textLength, expression)
            equal(0, lightExpression.startOffset, expression)
            equal(expression.length, lightExpression.endOffset, expression)
            if (optionalAssignment) {
                // whole wrapper 的直接 binary child 必须包含原 LHS/operator/RHS，不能只包 postfix。
                val binary = present(psiExpression.findChildByType(CjNodeTypes.BINARY_EXPRESSION))
                equal(expression, binary.text, expression)
                equal(0, binary.startOffset, expression)
                equal(expression.length, binary.textLength, expression)
                val ref = Ref<Array<LighterASTNode?>>()
                val count = lightFragment.tree.getChildren(lightExpression, ref)
                val children = ref.get()
                try {
                    val binaries = (0 until count).map { present(children?.get(it)) }
                        .filter { it.tokenType == CjNodeTypes.BINARY_EXPRESSION }
                    equal(listOf(0 to expression.length), binaries.map { it.startOffset to it.endOffset }, expression)
                } finally {
                    if (children != null) lightFragment.tree.disposeChildren(children, count)
                }
            }
            equal(lightFragment.diagnosticPublication.diagnostics, published, expression)
            val psiFragmentOwner = fragmentOwner(fragmentPsi, present(fragment.parsingDiagnosticScopeOrNull()), expression.length)
            val lightFragmentOwner = fragmentOwner(lightFragment.diagnosticPublication, lightFragment.diagnosticScope, expression.length)
            assertPublication(expected, expectedPublished, psiFragmentOwner, fragmentContext, broken, "fragment PSI: $expression")
            assertPublication(expected, expectedPublished, lightFragmentOwner, fragmentContext, broken, "fragment LT: $expression")
            assertPublicationsEqual(psiFragmentOwner, lightFragmentOwner, fragmentContext, expression)
        }
    }

    /** 用已知输入拼接位置计算 UTF-16 oracle；不调用被测 collector 或 nativePosition。 */
    private fun expectedReports(
        lhs: String, operator: String, rejected: List<String>, tupleCompound: Boolean,
    ): List<ParsingDiagnostic> {
        val start = lhs.length + 1
        val children = rejected.map { child ->
            val offset = lhs.indexOf(child)
            require(offset >= 0 && lhs.indexOf(child, offset + 1) < 0) { "Hint 必须唯一定位：$child in $lhs" }
            ParsingDiagnostic(
                start, start + operator.length, "invalid left-hand expression of assignment '$operator'",
                rangeKind = ParsingDiagnostic.RangeKind.TOKEN,
                hints = listOf(ParsingDiagnosticHint(offset, offset + child.length, "cannot assign to this expression")),
            )
        }
        return children + if (tupleCompound) listOf(
            ParsingDiagnostic(start, start + operator.length, "cannot '$operator' a tuple",
                rangeKind = ParsingDiagnostic.RangeKind.TOKEN),
        ) else emptyList()
    }

    private fun translate(diagnostic: ParsingDiagnostic, delta: Int): ParsingDiagnostic = diagnostic.copy(
        startOffset = diagnostic.startOffset + delta,
        endOffset = diagnostic.endOffset + delta,
        hints = diagnostic.hints.map { it.copy(startOffset = it.startOffset + delta, endOffset = it.endOffset + delta) },
    )

    private fun assertPublication(
        expected: List<ParsingDiagnostic>,
        publishedExpected: List<ParsingDiagnostic>,
        owner: PublicationOwner,
        context: CjParserSourceContext,
        broken: Boolean,
        route: String,
    ) {
        val publication = owner.publication
        assertReportOwners(owner, route)
        val reports = publication.reports.map { it.diagnostic }
        val assignmentReports = reports.filter(::isAssignmentDiagnostic)
        assertLegacyParsingDiagnostics(expected, assignmentReports, context, route)
        truth(assignmentReports.all { it.severity == ParsingDiagnostic.Severity.ERROR &&
            it.rangeKind == ParsingDiagnostic.RangeKind.TOKEN }, route)
        // publishedExpected 来自明确 oracle；多 operator 不能按全文件首项缩减。
        assertLegacyParsingDiagnostics(publishedExpected, publication.diagnostics.filter(::isAssignmentDiagnostic), context, route)
        if (broken) {
            truth(reports.any { !isAssignmentDiagnostic(it) && it.isError }, "必须实际触发参数/缺表达式错误：$route")
            truth(publication.diagnostics.any { !isAssignmentDiagnostic(it) && it.isError }, route)
        } else {
            // 完整的 oracle：不能借目标消息过滤掉意外 grammar 错误或 warning。
            assertLegacyParsingDiagnostics(expected, reports, context, route)
            assertLegacyParsingDiagnostics(publishedExpected, publication.diagnostics, context, route)
        }
        truth(reports.all { it.category == ParsingDiagnostic.Category.PARSE && it.isError }, route)
        assertCanonicalParsingDiagnostics(reports, context, route)
        assertCanonicalParsingDiagnostics(publication.diagnostics, context, route)
        equal(publication.reports.size, publication.reports.map { it.order }.distinct().size, route)
        publication.reports.zipWithNext().forEach { (first, next) ->
            truth(first.order.compareWithinRoot(next.order) < 0, route)
        }
    }

    /** 只选择已明确认证的赋值诊断家族；额外 syntax 报告仍原样全值比较。 */
    private fun isAssignmentDiagnostic(diagnostic: ParsingDiagnostic): Boolean =
        diagnostic.message.startsWith("invalid left-hand expression of assignment '") ||
            (diagnostic.message.startsWith("cannot '") && diagnostic.message.endsWith("' a tuple")) ||
            diagnostic.message == "assignment operators cannot be chained"

    private fun assertPublicationsEqual(
        firstOwner: PublicationOwner,
        secondOwner: PublicationOwner,
        context: CjParserSourceContext,
        text: String,
    ) {
        val first = firstOwner.publication
        val second = secondOwner.publication
        assertReportOwners(firstOwner, text)
        assertReportOwners(secondOwner, text)
        truth(firstOwner.scope.rootIdentity != secondOwner.scope.rootIdentity, text)
        assertPublishedParsingDiagnosticParity(first.diagnostics, second.diagnostics, context, text)
        equal(first.reports.map { it.diagnostic }, second.reports.map { it.diagnostic }, text)
        // 根 UUID 必须独立，逻辑 scope 路径和 sequence 必须逐报告一致。
        equal(first.reports.map { it.order.scope.path to it.order.sequence },
            second.reports.map { it.order.scope.path to it.order.sequence }, text)
    }

    /**
     * 两个真实正文区域来自各自 BLOCK 的 parser seed，不从报告 path 或固定数字造区域。
     * 同 operator tuple 的全部 child/cannot producer 与多 operator producer 均绑定包含它的区域。
     */
    @Test
    fun reportsBindTheActualOutputRootAndTheirIndependentContainingRegions() {
        val first = "(f(), (x)) += rhs"
        val second = "(y) >>= z = tail"
        val firstReports = expectedReports("(f(), (x))", "+=", listOf("f()", "(x)"), true)
        val secondReports = listOf(invalidLhs(4, 7, ">>=", 0, 3), nonassoc(10, 11, 4, 7))
        val firstPrefix = "func first() {"
        val secondPrefix = "$firstPrefix$first}\nfunc second() {"
        val text = "$secondPrefix$second}\n"
        val expected = firstReports.map { translate(it, firstPrefix.length) } +
            secondReports.map { translate(it, secondPrefix.length) }
        val expectedPublished = firstReports.take(1).map { translate(it, firstPrefix.length) } +
            secondReports.map { translate(it, secondPrefix.length) }
        val source = CjPsiFactory(project, markGenerated = false).createFile(text)
        val psi = psiOwner(source, source.collectParsingDiagnosticPublication())
        val inputSeed = publishedParserSeed(source)
        val context = present(inputSeed.sourceContext)
        val input = present(inputSeed.lexicalInput)
        val target = CangJieParser.parseCompilationFile(source, CjmpParsingContext.Compilation(false, false, true))
        target.requireConsistentInput()
        val targetOwner = psiOwner(target.file, target.diagnosticPublication)
        val light = CangJieLightParser.parseBuffered(sdkBuilder(text), lexicalInput = input)
        light.file.requireConsistentInput()
        val ltOwner = lightOwner(light.file, psi.regions)
        for ((route, owner) in listOf("regions PSI" to psi, "regions target" to targetOwner, "regions LT" to ltOwner)) {
            assertPublication(expected, expectedPublished, owner, context, false, route)
            val bodies = owner.regions.drop(1)
            equal(2, bodies.size, route)
            truth(bodies[0].scope != bodies[1].scope, route)
            bodies.forEach { equal(owner.scope.path, it.scope.path.dropLast(1), route) }
        }
        assertPublicationsEqual(psi, targetOwner, context, text)
        assertPublicationsEqual(psi, ltOwner, context, text)

        val bodies = psi.regions.drop(1)
        for ((index, body) in bodies.withIndex()) {
            val original = present(body.seed)
            val blockContext = present(original.sourceContext)
            val slice = CjParserSourceContext(blockContext.snapshot, blockContext.inputStart + 1, blockContext.inputEnd - 1)
            val expression = if (index == 0) first else second
            equal(expression, slice.snapshot.text.substring(slice.inputStart, slice.inputEnd))
            val entry = CjMacroFragmentContext.Nodes(
                MacroParsingScope.Expression(CjExprKind.ALL), CjMacroFragmentParent.OTHER,
                null, CjMacroDeclarationPrefix.EMPTY,
            )
            // 新片段冻结自己的真实来源；继承的是父 producer 区域，不伪装为父文件同一输入。
            val seed = createMacroFragmentParserSeed(
                ParsingFileFacts(original.sourceKind, original.fileRole), original.cjmpContext, entry, original.settings,
            ).copy(diagnosticScope = body.scope)
            val fragment = CjMacroCodeFragment(project, "region.cj", expression, context = null, seed = seed)
            fragment.getContentElement()
            val actualSeed = assertPublishedParserSeed(seed, fragment)
            val publication = fragment.collectParsingDiagnosticPublication()
            val actualContext = present(actualSeed.sourceContext)
            val actualScope = present(fragment.parsingDiagnosticScopeOrNull())
            equal(body.scope, actualScope)
            val owner = fragmentOwner(publication, actualScope, expression.length)
            val reports = if (index == 0) firstReports else secondReports
            val published = if (index == 0) firstReports.take(1) else secondReports
            assertPublication(reports, published, owner, actualContext, false, "seeded region PSI")
            CangJieLightParser.parseMacroCodeFragmentBuffered(sdkBuilder(expression), actualSeed).consume { product, diagnostics ->
                // 本次 LT 的输出根实际继承原 producer 区域；错误挂到另一个 UUID/兄弟 path 都不能通过。
                equal(body.scope, product.diagnosticScope)
                equal(body.scope, present(product.inputSeed.diagnosticScope))
                equal(body.scope, present(product.actualInputSeed.diagnosticScope))
                identical(present(actualSeed.lexicalInput), product.lexicalInput)
                equal(product.diagnosticPublication.diagnostics, diagnostics)
                assertPublication(reports, published,
                    fragmentOwner(product.diagnosticPublication, product.diagnosticScope, expression.length),
                    actualContext, false, "seeded region LT")
                assertPublishedParsingDiagnosticParity(publication.diagnostics, diagnostics, actualContext, expression)
                equal(publication.reports.map { it.diagnostic }, product.diagnosticPublication.reports.map { it.diagnostic })
            }
        }
    }

    private fun nonassoc(start: Int, end: Int, previousStart: Int, previousEnd: Int): ParsingDiagnostic =
        ParsingDiagnostic(start, end, "assignment operators cannot be chained",
            rangeKind = ParsingDiagnostic.RangeKind.TOKEN,
            hints = listOf(ParsingDiagnosticHint(previousStart, previousEnd)))

    private fun invalidLhs(start: Int, end: Int, operator: String, hintStart: Int, hintEnd: Int): ParsingDiagnostic =
        ParsingDiagnostic(start, end, "invalid left-hand expression of assignment '$operator'",
            rangeKind = ParsingDiagnostic.RangeKind.TOKEN,
            hints = listOf(ParsingDiagnosticHint(hintStart, hintEnd, "cannot assign to this expression")))

    private data class ChainCase(val text: String, val expected: List<ParsingDiagnostic>, val broken: Boolean = false)

    /** 输出根与其实际正文 seed 区域分开持有，不能拿 report 自己的 scope 当 owner。 */
    private data class ReportRegion(val start: Int, val end: Int, val scope: CjParsingDiagnosticScope, val seed: CjParserSeed? = null)
    private data class PublicationOwner(
        val publication: CjParsingDiagnosticPublication,
        val scope: CjParsingDiagnosticScope,
        val regions: List<ReportRegion>,
    )

    private fun psiOwner(file: CjFile, publication: CjParsingDiagnosticPublication): PublicationOwner {
        val output = present(file.parsingDiagnosticScopeOrNull())
        val regions = mutableListOf(ReportRegion(0, file.textLength, output))
        fun visit(node: ASTNode) {
            if (node.elementType == CjNodeTypes.BLOCK) {
                val seed = present(node.parserSeedOrNull(), "真实 BLOCK 必须持有正文入口 seed")
                val region = present(seed.diagnosticScope)
                val context = present(seed.sourceContext)
                identical(present(publishedParserSeed(file).lexicalInput), seed.lexicalInput)
                equal(node.startOffset, context.inputStart)
                equal(node.startOffset + node.textLength, context.inputEnd)
                regions += ReportRegion(node.startOffset, node.startOffset + node.textLength, region, seed)
            }
            var child = node.firstChildNode
            while (child != null) {
                visit(child)
                child = child.treeNext
            }
        }
        visit(file.node)
        return PublicationOwner(publication, output, regions)
    }

    /**
     * 缓冲文件 LT 未公开 child seed 表；以同份冻结来源的真实 PSI seed 绑定语法 BLOCK 对应区域，
     * 将其区域 path 迁移到 LT 实际输出 rootIdentity，绝不从 LT report 反推 path 或写固定槽号。
     * seeded region 测试另以 LT 自己的实际 inputSeed/outputScope 独立验收完整 scope 等值。
     */
    private fun lightOwner(file: ParsedCangJieLightFile, psiRegions: List<ReportRegion>): PublicationOwner {
        val blocks = mutableListOf<Pair<Int, Int>>()
        visitLight(file.tree.root, file.tree) { node ->
            if (node.tokenType == CjNodeTypes.BLOCK) blocks += node.startOffset to node.endOffset
        }
        val root = file.diagnosticScope
        equal(psiRegions.first().start, file.tree.root.startOffset)
        equal(psiRegions.first().end, file.tree.root.endOffset)
        val regions = psiRegions.mapIndexed { index, region ->
            if (index > 0) {
                equal(1, blocks.count { it == (region.start to region.end) }, "LT 对应 BLOCK 必须唯一且完整")
                val seed = present(region.seed)
                identical(file.lexicalInput, seed.lexicalInput)
                identical(file.sourceContext.snapshot, present(seed.sourceContext).snapshot)
            }
            ReportRegion(region.start, region.end, CjParsingDiagnosticScope(root.rootIdentity, region.scope.path))
        }
        return PublicationOwner(file.diagnosticPublication, root, regions)
    }

    private fun fragmentOwner(publication: CjParsingDiagnosticPublication, output: CjParsingDiagnosticScope, length: Int) =
        PublicationOwner(publication, output, listOf(ReportRegion(0, length, output)))

    /** 每个报告均绑定产品实际输出根和由非报告载体取得的最内包含区域。 */
    private fun assertReportOwners(owner: PublicationOwner, route: String) {
        for ((index, region) in owner.regions.withIndex()) {
            equal(owner.scope.rootIdentity, region.scope.rootIdentity, route)
            equal(owner.scope.path, region.scope.path.take(owner.scope.path.size), route)
            if (index > 0) truth(region.scope.path.size > owner.scope.path.size, route)
        }
        equal(owner.regions.size, owner.regions.map { it.scope }.distinct().size, route)
        for (report in owner.publication.reports) {
            equal(owner.scope.rootIdentity, report.order.scope.rootIdentity, route)
            equal(owner.scope.path, report.order.scope.path.take(owner.scope.path.size), route)
            val diagnostic = report.diagnostic
            val region = owner.regions.filter {
                diagnostic.startOffset >= it.start && diagnostic.endOffset <= it.end
            }.maxBy { it.scope.path.size }
            equal(region.scope, report.order.scope, "报告必须属于实际包含区域：$route")
            for (hint in diagnostic.hints) {
                truth(hint.startOffset >= region.start && hint.endOffset <= region.end, route)
            }
        }
        // tuple child-invalid/cannot 与同 operator 的其它报告必须共享真实 assignment 产生区域。
        owner.publication.reports.filter { isAssignmentDiagnostic(it.diagnostic) }
            .groupBy { it.diagnostic.startOffset to it.diagnostic.endOffset }.values.forEach { reports ->
                equal(1, reports.map { it.order.scope }.distinct().size, route)
            }
    }

    private fun visitLight(
        node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>, read: (LighterASTNode) -> Unit,
    ) {
        read(node)
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get()
        try {
            for (index in 0 until count) visitLight(present(children?.get(index)), tree, read)
        } finally {
            if (children != null) tree.disposeChildren(children, count)
        }
    }

    private fun sdkBuilder(text: String) = PsiBuilderFactory.getInstance().createBuilder(
        CangJieParserDefinition(), CangJieLexer(), text,
    ).also { it.setDebugMode(true) }

    private companion object {
        const val PREFIX = "func probe() {\n    "
        const val SUFFIX = "\n}\n"
        val COMPOUND_OPERATORS = listOf("**=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", "&=", "^=", "|=", "&&=", "||=")
    }
}
