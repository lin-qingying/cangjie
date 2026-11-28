package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjMacroInput
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjQuoteExpression
import org.cangnova.cangjie.psi.CjQuoteInterpolate
import org.cangnova.cangjie.psi.CjSourceRangeErrorElement
import org.cangnova.cangjie.psi.CjTokenRangeErrorElement
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.awt.EventQueue
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals as equal
import kotlin.test.assertNotNull as present
import kotlin.test.assertNotSame as distinct
import kotlin.test.assertNull as absent
import kotlin.test.assertSame as same
import kotlin.test.assertTrue as truth

/**
 * 官方 v1.1.3 ParseExpr.cpp 只细化最终直接 MacroExpandExpr 根，Parser.cpp 的语句首宏保留声明入口。
 * ParseAtom.cpp/ParseAnnotations.cpp 的直接 parent 决定 comma，ParseQuote.cpp 区分 token 与插值。
 * 用真实源码槽位和实际 AT 载体分别检查事实；不将 PSI/LT 相等或测试文件名作为语义依据。
 */
class MacroInvocationFactsParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun classFinalizerIsTheInputOfItsDeclarationMacro() {
        val file = CjPsiFactory(project, markGenerated = false).createFile(
            "class C { @APILevel ~init() {} }",
        )
        val macro = PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java).single()

        truth(macro.unwrappedDeclaration is CjFinalizer, macro.text)
    }

    @Test
    fun directVariableAndFieldInitializersUseVarInit() {
        assertSource(
            "let global = @Global()\nclass Owner { let field = @Field() }\n" +
                "func host() { let local = @Local() }",
            listOf(
                expression("@Global()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
                expression("@Field()", CjExprKind.VAR_INIT, owner = "Owner", parent = CjNodeTypes.FIELD),
                expression("@Local()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
            ),
        )
    }

    /** NodeX.h 明确普通 IfAvailable 是 MacroExpandExpr；不能按短名跳过 invocation 事实。 */
    @Test
    fun ordinaryIfAvailableKeepsItsSourceMacroEntryAndOperations() {
        val invocation = "@IfAvailable(level: 19, { => 1 }, { => 2 })"
        assertSource(
            "func host() { let value = $invocation }",
            listOf(expression(invocation, CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE)),
        )
        assertSource(
            "func host() { $invocation + 1 }",
            listOf(declaration(invocation, CjScopeKind.FUNC_BODY, parent = CjNodeTypes.BINARY_EXPRESSION)),
        )
        assertSource(
            "func host() { let value = $invocation + 1 }",
            listOf(declaration(invocation, parent = CjNodeTypes.BINARY_EXPRESSION)),
        )
    }

    /** EnableCustomAnno 的 INDEX_EXPR 优先于专有化；低层 RHS 也不进入高层专用分派。 */
    @Test
    fun annotationModeIndexAndBinaryOperandsKeepOrdinaryIfAvailableAtoms() {
        assertSource(
            "func host() { let value = values[@IfAvailable()] }",
            listOf(expression("@IfAvailable()", CjExprKind.INDEX_EXPR, parent = CjNodeTypes.INDICES)),
            annotationOnly = true,
        )
        assertSource(
            "func host() { let value = 1 + @IfAvailable() }",
            listOf(declaration("@IfAvailable()", parent = CjNodeTypes.BINARY_EXPRESSION)),
            annotationOnly = true,
        )
    }

    @Test
    fun parameterDefaultsHaveAnExpressionScopeEvenInPrimaryConstructors() {
        assertSource(
            "func host(value!: Int64 = @Regular()) {}\n" +
                "class Owner { Owner(value!: Int64 = @ClassDefault()) {} }\n" +
                "struct Value { Value(value!: Int64 = @StructDefault()) {} }",
            listOf(
                expression("@Regular()", CjExprKind.ALL, parent = CjNodeTypes.VALUE_PARAMETER),
                expression("@ClassDefault()", CjExprKind.ALL, owner = "Owner", parent = CjNodeTypes.VALUE_PARAMETER),
                expression("@StructDefault()", CjExprKind.ALL, owner = "Value", parent = CjNodeTypes.VALUE_PARAMETER),
            ),
        )
    }

    @Test
    fun binaryPrefixAndPostfixOperandsKeepUnknownDeclarationScope() {
        for ((operand, parent) in listOf(
            "@M() + 1" to CjNodeTypes.BINARY_EXPRESSION,
            "1 + @M()" to CjNodeTypes.BINARY_EXPRESSION,
            "-@M()" to CjNodeTypes.PREFIX_EXPRESSION,
            "@M().value" to CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
            "@M()[0]" to CjNodeTypes.ARRAY_ACCESS_EXPRESSION,
            "@M()()" to CjNodeTypes.CALL_EXPRESSION,
        )) {
            for ((prefix, suffix) in listOf(
                "func host() { let value = " to "\n}",
                "func host(value!: Int64 = " to ") {}",
                "func host() { consume(" to ")\n}",
            )) {
                assertSource(prefix + operand + suffix, listOf(declaration("@M()", parent = parent)))
            }
        }
    }

    /** 普通括号引入自己的高层入口；外层 binary 不会撤销括号内已经记录的 TUPLE。 */
    @Test
    fun aParenthesizedOperandRetainsItsOwnExpressionEntry() {
        assertSource(
            "func host() { let value = (@M()) + 1 }",
            listOf(expression("@M()", CjExprKind.EXPR_IN_TUPLE, parent = CjNodeTypes.PARENTHESIZED)),
        )
    }

    @Test
    fun statementInitialMacroKeepsFunctionScopeWhileOperationsContinue() {
        for ((statement, parent) in listOf(
            "@M()" to CjNodeTypes.BLOCK,
            "@M() + 1" to CjNodeTypes.BINARY_EXPRESSION,
            "@M().value" to CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
            "@M()[0]" to CjNodeTypes.ARRAY_ACCESS_EXPRESSION,
            "@M()()" to CjNodeTypes.CALL_EXPRESSION,
        )) {
            assertSource(
                "func host() { $statement\n}",
                listOf(declaration("@M()", CjScopeKind.FUNC_BODY, parent = parent)),
            )
        }
        assertSource(
            "func host() { 1 + @Right()\n-@Prefixed()\n}",
            listOf(
                declaration("@Right()", parent = CjNodeTypes.BINARY_EXPRESSION),
                declaration("@Prefixed()", parent = CjNodeTypes.PREFIX_EXPRESSION),
            ),
        )
    }

    @Test
    fun macroLambdaAndSynchronizedBodiesKeepTheirActualStatementEntries() {
        assertSource(
            "macro package invocation_facts\npublic macro make(input: Tokens): Tokens { @M() + 1 }",
            listOf(declaration("@M()", CjScopeKind.MACRO_BODY, parent = CjNodeTypes.BINARY_EXPRESSION)),
        )
        assertSource(
            "func host() { let callback = { => @Lambda() + 1 }\n" +
                "synchronized(@Mutex()) { @Body() + 1 }\n}",
            listOf(
                declaration("@Lambda()", CjScopeKind.FUNC_BODY, parent = CjNodeTypes.BINARY_EXPRESSION),
                expression("@Mutex()", CjExprKind.ALL),
                declaration("@Body()", parent = CjNodeTypes.BINARY_EXPRESSION),
            ),
        )
    }

    @Test
    fun directConditionsUseTheConditionEntryWithoutACommaParent() {
        assertSource(
            "func host() { if (@If()) {}\nwhile (@While()) {}\ndo {} while (@Do())\n}",
            listOf(
                expression("@If()", CjExprKind.IF_COND_EXPR, parent = CjNodeTypes.CONDITION),
                expression("@While()", CjExprKind.WHILE_COND_EXPR, parent = CjNodeTypes.CONDITION),
                expression("@Do()", CjExprKind.ALL, parent = CjNodeTypes.CONDITION),
            ),
        )
    }

    @Test
    fun aSecondConditionParenthesisUsesConditionalTupleAndAThirdUsesOrdinaryTuple() {
        for ((keyword, tupleKind) in listOf(
            "if" to CjExprKind.EXPR_IN_IF_COND_TUPLE,
            "while" to CjExprKind.EXPR_IN_WHILE_COND_TUPLE,
        )) {
            assertSource(
                "func host() { $keyword ((@M())) {} }",
                listOf(expression("@M()", tupleKind, parent = CjNodeTypes.PARENTHESIZED)),
            )
            assertSource(
                "func host() { $keyword (((@M()))) {} }",
                listOf(expression("@M()", CjExprKind.EXPR_IN_TUPLE, parent = CjNodeTypes.PARENTHESIZED)),
            )
        }
    }

    /** 前缀只清除低层条件文法；不把其宏操作数细化为独立 ALL 根。 */
    @Test
    fun prefixOperatorsClearConditionalTupleGrammarBeforeEnteringParentheses() {
        for (keyword in listOf("if", "while")) {
            assertSource(
                "func host() { $keyword (!(@M())) {} }",
                listOf(expression("@M()", CjExprKind.EXPR_IN_TUPLE, parent = CjNodeTypes.PARENTHESIZED)),
            )
            assertSource(
                "func host() { $keyword (!@M()) {} }",
                listOf(declaration("@M()", parent = CjNodeTypes.PREFIX_EXPRESSION)),
            )
        }
    }

    @Test
    fun binaryRightOperandsRetainConditionGrammarWithoutRefiningTheirRootMacros() {
        assertSource(
            "func host() { if (true && @Direct()) {}\n" +
                "if (true && (@Nested())) {}\nif (true && !(@Prefixed())) {}\n}",
            listOf(
                declaration("@Direct()", parent = CjNodeTypes.BINARY_EXPRESSION),
                expression("@Nested()", CjExprKind.EXPR_IN_IF_COND_TUPLE, parent = CjNodeTypes.PARENTHESIZED),
                expression("@Prefixed()", CjExprKind.EXPR_IN_TUPLE, parent = CjNodeTypes.PARENTHESIZED),
            ),
        )
    }

    @Test
    fun onlyTheFirstConditionTupleItemRetainsConditionalGrammar() {
        for ((keyword, firstKind) in listOf(
            "if" to CjExprKind.EXPR_IN_IF_COND_TUPLE,
            "while" to CjExprKind.EXPR_IN_WHILE_COND_TUPLE,
        )) {
            assertSource(
                "func host() { $keyword ((@First(), @Second())) {} }",
                listOf(
                    expression("@First()", firstKind, comma = true, parent = CjNodeTypes.TUPLE_EXPRESSION),
                    expression("@Second()", CjExprKind.EXPR_IN_TUPLE, comma = true, parent = CjNodeTypes.TUPLE_EXPRESSION),
                ),
            )
        }
    }

    @Test
    fun tupleCommaRefinementTouchesOnlyDirectMacroItems() {
        for ((value, expected) in listOf(
            "(@M())" to expression("@M()", CjExprKind.EXPR_IN_TUPLE, parent = CjNodeTypes.PARENTHESIZED),
            "(@M(), 1)" to expression("@M()", CjExprKind.EXPR_IN_TUPLE, comma = true, parent = CjNodeTypes.TUPLE_EXPRESSION),
            "(1, @M())" to expression("@M()", CjExprKind.EXPR_IN_TUPLE, comma = true, parent = CjNodeTypes.TUPLE_EXPRESSION),
            "(@M() + 1, 2)" to declaration("@M()", parent = CjNodeTypes.BINARY_EXPRESSION),
            "((@M()), 1)" to expression("@M()", CjExprKind.EXPR_IN_TUPLE, parent = CjNodeTypes.PARENTHESIZED),
        )) {
            assertSource("func host() { let value = $value }", listOf(expected))
        }
    }

    @Test
    fun arraysAndCallArgumentsAreCommaParentsEvenWithOneItem() {
        for (argument in listOf("@M()", "value: @M()", "inout @M()")) {
            assertSource(
                "func host() { consume($argument) }",
                listOf(expression("@M()", CjExprKind.EXPR_IN_CALLSUFFIX, comma = true, parent = CjNodeTypes.VALUE_ARGUMENT)),
            )
        }
        assertSource(
            "func host() { let values = [@Array()]\nconsume((@Grouped()))\n}",
            listOf(
                expression("@Array()", CjExprKind.EXPR_IN_ARRAY, comma = true),
                expression("@Grouped()", CjExprKind.EXPR_IN_TUPLE, parent = CjNodeTypes.PARENTHESIZED),
            ),
        )
    }

    @Test
    fun arrayAndCallParentsDoNotMarkNestedBinaryOperandsAsCommaConnected() {
        assertSource(
            "func host() { let values = [@Array() + 1]\nconsume(1 + @Argument())\n}",
            listOf(
                declaration("@Array()", parent = CjNodeTypes.BINARY_EXPRESSION),
                declaration("@Argument()", parent = CjNodeTypes.BINARY_EXPRESSION),
            ),
        )
    }

    @Test
    fun indicesUseIndexEntryAndNeverGainAnArrayCommaParent() {
        assertSource(
            "func host() { let first = values[@Index()]\nlet pair = values[@First(), @Second()]\n" +
                "let grouped = values[(@Grouped())]\n}",
            listOf(
                expression("@Index()", CjExprKind.INDEX_EXPR, parent = CjNodeTypes.INDICES),
                expression("@First()", CjExprKind.INDEX_EXPR, parent = CjNodeTypes.INDICES),
                expression("@Second()", CjExprKind.INDEX_EXPR, parent = CjNodeTypes.INDICES),
                expression("@Grouped()", CjExprKind.EXPR_IN_TUPLE, parent = CjNodeTypes.PARENTHESIZED),
            ),
        )
    }

    @Test
    fun rangeStartStopAndStepStayInLowLevelExpressionParsing() {
        for ((value, expected, parent) in listOf(
            Triple("@Start()..@Stop():@Step()", listOf("@Start()", "@Stop()", "@Step()"), CjNodeTypes.RANGE_EXPRESSION),
            Triple("values[@Start()..@Stop():@Step()]", listOf("@Start()", "@Stop()", "@Step()"), CjNodeTypes.RANGE_EXPRESSION),
            Triple("values[..@Stop()]", listOf("@Stop()"), CjNodeTypes.SLICE_EXPRESSION),
            Triple("values[@Start()..]", listOf("@Start()"), CjNodeTypes.SLICE_EXPRESSION),
        )) {
            assertSource(
                "func host() { let value = $value }",
                expected.map { declaration(it, parent = parent) },
            )
        }
    }

    @Test
    fun annotationArgumentsAndWhenBinaryOperandsRetainTheirDistinctEntries() {
        assertSource(
            "@Tag[@Argument(), (@Grouped())] func host() {}",
            listOf(
                expression("@Argument()", CjExprKind.EXPR_IN_ANNOTATION, comma = true, parent = CjNodeTypes.VALUE_ARGUMENT),
                expression("@Grouped()", CjExprKind.EXPR_IN_TUPLE, parent = CjNodeTypes.PARENTHESIZED),
            ),
            annotationOnly = true,
        )
        // 官方 ParseWhenAnnotation 只接受 unary/binary/ref 根；直接宏条件的拒绝诊断另行验收。
        assertSource(
            "@When[feature && @Condition()] func host() {}",
            listOf(declaration("@Condition()", parent = CjNodeTypes.BINARY_EXPRESSION)),
        )
    }

    @Test
    fun tryResourceInitializersUseVarInitDespiteTheirResourceList() {
        assertSource(
            "func host() { try (first = @First(), second = @Second()) {} }",
            listOf(
                expression("@First()", CjExprKind.VAR_INIT, parent = CjNodeTypes.TRY_RESOURCE),
                expression("@Second()", CjExprKind.VAR_INIT, parent = CjNodeTypes.TRY_RESOURCE),
            ),
        )
    }

    @Test
    fun returnThrowAndLetPatternInitializersUseTheirActualExpressionEntries() {
        assertSource(
            "func returning() { return @Return() }\nfunc thrower() { throw @Throw() }\n" +
                "func matching() { if (let value <- @Pattern()) {} }",
            listOf(
                expression("@Return()", CjExprKind.ALL, parent = CjNodeTypes.RETURN),
                expression("@Throw()", CjExprKind.ALL, parent = CjNodeTypes.THROW),
                expression("@Pattern()", CjExprKind.LET_PATTERN, parent = CjNodeTypes.LET_EXPRESSION),
            ),
        )
    }

    @Test
    fun declarationMacroChainsRetainTheirRealDeclarationEntries() {
        val chain = "@Outer @Inner func host(value!: Int64 = @Default()) {}"
        assertSource(
            chain,
            listOf(
                declaration(chain, CjScopeKind.TOPLEVEL),
                declaration("@Inner func host(value!: Int64 = @Default()) {}", CjScopeKind.TOPLEVEL, parent = CjNodeTypes.MACRO_INPUT),
                expression("@Default()", CjExprKind.ALL, parent = CjNodeTypes.VALUE_PARAMETER),
            ),
        )
        for ((kind, scope, owner) in listOf(
            Triple("class", CjScopeKind.CLASS_BODY, "Owner"),
            Triple("struct", CjScopeKind.STRUCT_BODY, "Owner"),
            Triple("extend", CjScopeKind.EXTEND_BODY, null),
        )) {
            assertSource(
                "$kind Owner { @M func member() {} }",
                listOf(declaration("@M func member() {}", scope, owner = owner)),
            )
        }
    }

    @Test
    fun invalidNestedOwnersKeepInnerAndResumedOuterFactsWithTheScopeError() {
        val source = "class `Outer` { class `Inner` { let inside = @Inner() }\nlet resumed = @Outer()\n}"
        val invalidKeyword = source.indexOf("class `Inner`")
        assertSource(
            source,
            listOf(
                expression("@Inner()", CjExprKind.VAR_INIT, owner = "Inner", parent = CjNodeTypes.FIELD),
                expression("@Outer()", CjExprKind.VAR_INIT, owner = "Outer", parent = CjNodeTypes.FIELD),
            ),
            expectedDiagnostics = listOf(ParsingDiagnostic(
                invalidKeyword, invalidKeyword + "class".length,
                CangJieParsingBundle.message(
                    "parsing.error.invalid.declaration.in.class", "class", CangJieParsingBundle.message("parsing.scope.class"),
                ),
            )),
        )
    }

    @Test
    fun adjacentTopLevelOwnersKeepSemanticNamesAndClearTheNameAfterEachDeclaration() {
        assertSource(
            "class `First` { let first = @First() }\nlet afterFirst = @AfterFirst()\n" +
                "class `Second` { let second = @Second() }\nlet afterSecond = @AfterSecond()",
            listOf(
                expression("@First()", CjExprKind.VAR_INIT, owner = "First", parent = CjNodeTypes.FIELD),
                expression("@AfterFirst()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
                expression("@Second()", CjExprKind.VAR_INIT, owner = "Second", parent = CjNodeTypes.FIELD),
                expression("@AfterSecond()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
            ),
        )
    }

    /** 清除直接成员文法不能清除原生 curPrimaryDeclIdent；正文重新 lazy 解析也保留原入口。 */
    @Test
    fun enclosingNominalNameSurvivesFunctionAndLambdaEntriesAndFileCopies() {
        val source = "class `Owner` { func host(value!: Int64 = @Default()) {\n" +
            "@Body()\nlet callback = { => @Lambda() }\n} }"
        val expected = listOf(
            expression("@Default()", CjExprKind.ALL, owner = "Owner", parent = CjNodeTypes.VALUE_PARAMETER),
            declaration("@Body()", CjScopeKind.FUNC_BODY, owner = "Owner", parent = CjNodeTypes.BLOCK),
            declaration("@Lambda()", CjScopeKind.FUNC_BODY, owner = "Owner", parent = CjNodeTypes.BLOCK),
        )
        val file = CjPsiFactory(project, markGenerated = false).createFile(source)
        val copy = file.copy() as CjFile
        distinct(file.node, copy.node)
        assertOccurrences(file, expected)
        assertOccurrences(copy, expected)
        assertLightOccurrences(source, expected, sourceFile = file)
        val originalMacros = PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java)
        val copiedMacros = PsiTreeUtil.findChildrenOfType(copy, CjMacroExpression::class.java)
        for ((original, copied) in originalMacros.zip(copiedMacros)) distinct(original.node, copied.node)
    }

    @Test
    fun quotedMacroTokensAreNotSourceInvocationsButDollarExpressionsAre() {
        val source = "func host() { let tokens = quote(@Token() ${'$'}(@Interpolated()) ${'$'}value) }"
        val file = assertSource(
            source,
            listOf(expression("@Interpolated()", CjExprKind.ALL, parent = CjNodeTypes.QUOTE_INTERPOLATE)),
        )
        assertTokenOnly(file, "@Token()")
    }

    @Test
    fun quoteRecognizesInterpolationInsideMacroShapedParentheses() {
        val tokenText = "@Token(${'$'}(@Interpolated()))"
        val file = assertSource(
            "func host() { let tokens = quote($tokenText) }",
            listOf(expression("@Interpolated()", CjExprKind.ALL, parent = CjNodeTypes.QUOTE_INTERPOLATE)),
        )
        assertTokenOnly(file, tokenText)
    }

    @Test
    fun quoteNewlineReportsAtKeywordAndMissingLeftParenDoesNotConsumeNextStatement() {
        val message = CangJieParsingBundle.message("parsing.error.unexpected.line.break.after.quote")
        val source = "func host() { let tokens = quote\nlet after = @After() }\nfunc following() {}"
        val quoteOffset = source.indexOf("quote")
        val file = assertSource(
            source,
            listOf(expression("@After()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE)),
            expectedDiagnostics = listOf(ParsingDiagnostic(quoteOffset, quoteOffset + "quote".length, message)),
        )

        equal("quote\n", PsiTreeUtil.findChildrenOfType(file, CjQuoteExpression::class.java).single().text)
        equal(listOf("host", "following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
    }

    @Test
    fun quoteInterpolationTemporarilySkipsNewlinesAndRestoresTokenMode() {
        val source = "func host() { let tokens = quote(\n" +
            "${'$'}(\n@Inside()\n)\n@Quoted()\n) }"
        val file = assertSource(
            source,
            listOf(expression("@Inside()", CjExprKind.ALL, parent = CjNodeTypes.QUOTE_INTERPOLATE)),
        )

        assertTokenOnly(file, "@Quoted()")
        equal(source.substringAfter("let tokens = ").substringBefore(" }"),
            PsiTreeUtil.findChildrenOfType(file, CjQuoteExpression::class.java).single().text)
    }

    /** ParseQuoteTokens 接受词法器产生的全部普通词元，终止条件只由 quote 定界符决定。 */
    @Test
    fun quoteConsumesEveryLexerTokenUntilItsMatchingDelimiter() {
        for (tokens in listOf("@!Token()", "left /= right", "left **= right", "left &&= right", "left::right", "~left ^ right", "b'a'")) {
            val quoted = "quote($tokens)"
            val file = assertSource(
                "func host() { let tokens = $quoted }\nfunc following() {}",
                emptyList(),
            )

            equal(quoted, PsiTreeUtil.findChildrenOfType(file, CjQuoteExpression::class.java).single().text)
            equal(listOf("host", "following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
        }
    }

    /** 宏输入的公开 token 列表不能因手写词元集合遗漏赋值运算符。 */
    @Test
    fun macroInputTokensExposeOperatorsMissingFromTheFormerTokenSet() {
        val input = "left /* block comment */ // line comment\n /= right"
        val invocation = "@M($input)"
        val file = assertSource(
            "func host() { let value = $invocation }",
            listOf(expression(invocation, CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE)),
        )

        val macroInput = PsiTreeUtil.findChildrenOfType(file, CjMacroInput::class.java).single()
        truth(macroInput.tokens.any { it.text == "/=" })
        truth(macroInput.tokens.none { CjTokens.COMMENTS.contains(it.node.elementType) })
    }

    /** ParseQuoteTokens 诊断裸美元符号后只消费一次，真实右括号仍结束 quote。 */
    @Test
    fun bareQuoteDollarPreservesTheQuoteDelimiterAndFollowingDeclarations() {
        val quoted = "quote(${'$'})"
        val source = "func host() { let tokens = $quoted\nlet value = @After() }\nfunc following() {}"
        val dollar = source.indexOf('$')
        val file = assertSource(
            source,
            listOf(expression("@After()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE)),
            expectedDiagnostics = listOf(ParsingDiagnostic(
                dollar, dollar + 1,
                CangJieParsingBundle.message("parsing.error.expected.identifier.after.dollar"),
            )),
        )
        equal(quoted, PsiTreeUtil.findChildrenOfType(file, CjQuoteExpression::class.java).single().text)
        equal(listOf("host", "following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
    }

    /** quote 的五种合法转义保留 token 身份，转义美元符号不能建立插值节点。 */
    @Test
    fun quoteEscapesRemainTokensAndPreserveFollowingDeclarations() {
        for (tokens in listOf("\\${'$'}", "\\@", "\\(", "\\)", "\\${'$'}identifier")) {
            val quoted = "quote($tokens)"
            val source = "func host() { let tokens = $quoted\nlet after = @After() }\nfunc following() {}"
            val file = assertSource(
                source,
                listOf(expression("@After()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE)),
            )
            equal(quoted, PsiTreeUtil.findChildrenOfType(file, CjQuoteExpression::class.java).single().text)
            truth(PsiTreeUtil.findChildrenOfType(file, CjQuoteInterpolate::class.java).isEmpty(), source)
            equal(listOf("host", "following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
        }
    }

    /** v1.1.3 ParseQuote.cpp:78–95 报 LEX 位置诊断；后面的 token 和真正右括号仍由 quote 消费。 */
    @Test
    fun illegalQuoteEscapesReportOnlyTheBackslashAndPreserveFollowingDeclarations() {
        for (tokens in listOf("\\[", "\\]", "\\ plain")) {
            val quoted = "quote($tokens)"
            val source = "func host() { let tokens = $quoted\nlet after = @After() }\nfunc following() {}"
            val backslash = source.indexOf('\\')
            val diagnostics = listOf(lexicalBackslashPoint(backslash))
            val file = assertSource(
                source,
                listOf(expression("@After()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE)),
                expectedDiagnostics = diagnostics,
                expectedReportDiagnostics = diagnostics,
            )
            equal(quoted, PsiTreeUtil.findChildrenOfType(file, CjQuoteExpression::class.java).single().text)
            equal(listOf("host", "following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
        }
    }

    /** 宏圆括号 token 输入接受圆括号和 AT 转义，裸美元符号与非调用 AT 保持普通 token。 */
    @Test
    fun macroParenthesizedTokenInputsAcceptEscapesAndUninterpretedTokens() {
        val invocation = "@Input(\\( \\) \\@ ${'$'} @NotACall)"
        val source = "func host() { let value = $invocation\nlet after = @After() }\nfunc following() {}"
        val file = assertSource(
            source,
            listOf(
                expression(invocation, CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
                expression("@After()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
            ),
        )
        assertTokenOnly(file, "@NotACall")
        equal(listOf("host", "following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
    }

    /** attrs 的方括号转义不改变真实 attrs 边界，也不建立嵌套宏事实。 */
    @Test
    fun macroAttributeTokenInputsAcceptBracketAndAtEscapes() {
        val invocation = "@Attr[\\[ \\] \\@]()"
        val source = "func host() { let value = $invocation\nlet after = @After() }\nfunc following() {}"
        val file = assertSource(
            source,
            listOf(
                expression(invocation, CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
                expression("@After()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
            ),
        )
        equal(listOf("host", "following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
    }

    /** ParseMacroCallTokens 不将裸美元符号解释为 quote 插值或语法错误。 */
    @Test
    fun macroTokenInputsAndAttributesAcceptBareDollar() {
        for (invocation in listOf("@Input(${'$'})", "@Attr[${'$'}]()")) {
            val source = "func host() { let value = $invocation\nlet after = @After() }\nfunc following() {}"
            val file = assertSource(
                source,
                listOf(
                    expression(invocation, CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
                    expression("@After()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE),
                ),
            )
            equal(listOf("host", "following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
        }
    }

    /** quote 内的方括号只是 token，不会建立真正宏 attrs 的独立解析环境。 */
    @Test
    fun quoteRecognizesInterpolationInsideMacroShapedAttributes() {
        val tokenText = "@Token[${'$'}(@InterpolatedAttr())]()"
        val file = assertSource(
            "func host() { let tokens = quote($tokenText) }",
            listOf(expression("@InterpolatedAttr()", CjExprKind.ALL, parent = CjNodeTypes.QUOTE_INTERPOLATE)),
        )
        assertTokenOnly(file, tokenText)
    }

    @Test
    fun quoteInterpolationStillUsesNestedExpressionAndCommaEntries() {
        val source = "func host() { let tokens = quote(${'$'}(@Binary() + 1) ${'$'}((@Tuple(), 1))) }"
        assertSource(
            source,
            listOf(
                declaration("@Binary()", parent = CjNodeTypes.BINARY_EXPRESSION),
                expression("@Tuple()", CjExprKind.EXPR_IN_TUPLE, comma = true, parent = CjNodeTypes.TUPLE_EXPRESSION),
            ),
        )
    }

    @Test
    fun parenthesesAndAttributesKeepNestedMacroShapesAsTokenInput() {
        val source = "func host() { let value = @Outer[@Attribute()](@Input()) }"
        val file = assertSource(
            source,
            listOf(expression("@Outer[@Attribute()](@Input())", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE)),
        )
        assertTokenOnly(file, "@Attribute()")
        assertTokenOnly(file, "@Input()")
    }

    /** 原生 input scanner 在非法转义目标处停止，报告范围不能消费或包裹恢复词元。 */
    @Test
    fun illegalMacroInputEscapesKeepTheirActualScannerStopToken() {
        for ((source, width) in listOf("@M(\\${'$'}bad)" to 4, "@M(\\])" to 1, "@M(\\@!Hidden())" to 2)) {
            assertMacroInputScannerStop(source, width)
        }
    }

    /** Peek 跳过横向空白和 COMMENT，但 Lexer 仍报告非相邻反斜杠，目标诊断不得吸收 trivia。 */
    @Test
    fun illegalMacroInputEscapesPreserveTriviaAndBothDiagnosticRanges() {
        for (trivia in listOf("  ", "\t", " /*comment*/ ", "/*first*//*second*/")) {
            for ((target, width) in listOf("${'$'}bad" to 4, "]" to 1, "@!Hidden()" to 2)) {
                val source = "@M(\\$trivia$target)"
                val targetOffset = source.indexOf('\\') + 1 + trivia.length
                assertMacroInputScannerStop(
                    source, width, diagnosticOffset = targetOffset, stopOffset = targetOffset,
                    hasLexicalError = true,
                )
            }
        }
    }

    /** 原生恢复只越过连续 '['、'('、'.'，错误始终指向恢复前的首个词元。 */
    @Test
    fun illegalMacroInputEscapeConsumesOnlyConsecutiveRecoveryStarts() {
        for (recovery in listOf("[", "[([. .", "[ /*comment*/ ( . [")) {
            val source = "@M(\\${recovery}bad)"
            assertMacroInputScannerStop(source, 1, stopOffset = source.indexOf("bad"))
        }
    }

    /** 属性 scanner 的错误宽度来自真实 Token；外层仍可尝试解析后续 input。 */
    @Test
    fun illegalMacroAttributeEscapesReportTheWholeOriginalTargetToken() {
        val message = CangJieParsingBundle.message("parsing.error.macro.attribute.escape")
        for ((target, width) in listOf("${'$'}bad" to 4, ")" to 1, "@!Hidden()" to 2)) {
            val source = "@M[\\$target]()"
            val delegate = CangJiePsiBuilder(CangJieParserDefinition(), CangJieLexer(), source)
            val builder = SemanticWhitespaceAwarePsiBuilderImpl(delegate)
            val parser = CangJieParsing.createForTopLevelNonLazy(builder)
            val root = builder.mark()
            with(AbstractCangJieParsing.ParsingContext.DEFAULT) {
                parser.expressionParsing.parseMacroExpression(CjScopeKind.TOPLEVEL, DeclarationParsingMode.TOPLEVEL)
            }
            val offset = source.indexOf('\\') + 1
            val errors = builder.productions.filter {
                (it.tokenType as? CjTokenRangeErrorElementType)?.message == message
            }.distinct()
            val error = errors.single()
            equal(offset, error.startOffset, source)
            equal(offset, error.endOffset, source)
            equal(width, (error.tokenType as CjTokenRangeErrorElementType).reportedWidth, source)
            while (!builder.eof()) builder.advanceLexer()
            root.done(CjNodeTypes.CJ_FILE)
            delegate.finishParsing(builder.macroParsingJournal, null)
            val ast = delegate.treeBuilt
            equal(source, ast.text)
            val materialized = collectAstErrors(ast).filterIsInstance<CjTokenRangeErrorElement>()
                .single { it.errorDescription == message }
            equal(TextRange(offset, offset), materialized.textRange, source)
            equal(TextRange(offset, offset + width), materialized.reportedRange, source)
        }
    }

    /** END 没有实际 token 宽度；ParserUtils.cpp:91–95 将其位置放在最后非注释 token 的 end。 */
    @Test
    fun illegalMacroInputEscapeAtEofUsesPointDiagnosticWithoutTokenCarrier() {
        for (trailingTrivia in listOf("", "   /* trailing comment */", " // trailing comment")) {
            val source = "@M(\\$trailingTrivia"
            val slashOffset = source.indexOf('\\')
            val delegate = CangJiePsiBuilder(CangJieParserDefinition(), CangJieLexer(), source)
            val builder = SemanticWhitespaceAwarePsiBuilderImpl(delegate)
            val parser = CangJieParsing.createForTopLevelNonLazy(builder)
            val root = builder.mark()
            with(AbstractCangJieParsing.ParsingContext.DEFAULT) { parser.expressionParsing.parseExpression() }
            truth(builder.eof())
            equal(source.length, builder.currentOffset)
            equal(slashOffset + 1, builder.lastConsumedTokenEndOffset)
            val typedErrors = builder.productions.filter { it.tokenType is CjTokenRangeErrorElementType }.distinct()
            truth(typedErrors.isEmpty(), "EOF 无目标 token，LEX 反斜杠也不是 parser token-error carrier：$source")
            val pointError = builder.productions.filter { it.tokenType is CjSourceRangeErrorElementType }.distinct().single()
            val pointType = pointError.tokenType as CjSourceRangeErrorElementType
            equal(ParsingDiagnostic.Category.PARSE, pointType.category)
            equal(ParsingDiagnostic.RangeKind.POINT, pointType.rangeKind)
            equal(0, pointType.reportedWidth)
            root.done(CjNodeTypes.CJ_FILE)
            delegate.finishParsing(builder.macroParsingJournal, null)
            val publication = collectBuilderPublication(builder)
            val ast = delegate.treeBuilt
            equal(source, ast.text)
            val lexical = lexicalBackslashPoint(slashOffset)
            val parsing = ParsingDiagnostic(slashOffset + 1, slashOffset + 1,
                CangJieParsingBundle.message("parsing.error.macro.input.escape"),
                category = ParsingDiagnostic.Category.PARSE, rangeKind = ParsingDiagnostic.RangeKind.POINT)
            val sourceContext = builder.macroParsingJournal.sourceContext
            assertLegacyParsingDiagnostics(listOf(lexical), publication.diagnostics, sourceContext, source)
            assertLegacyParsingDiagnostics(listOf(lexical, parsing), publication.reports.map { it.diagnostic }, sourceContext, source)
            val materialized = collectAstErrors(ast).single()
            truth(materialized is CjSourceRangeErrorElement, source)
            val eofError = materialized as CjSourceRangeErrorElement
            equal(TextRange(source.length, source.length), eofError.textRange, source)
            equal(TextRange(slashOffset + 1, slashOffset + 1), eofError.reportedRange, source)
            equal(ParsingDiagnostic.Category.PARSE, eofError.category)
            equal(ParsingDiagnostic.RangeKind.POINT, eofError.rangeKind)
            val slash = sourceTokenNodes(ast).single { it.startOffset == slashOffset && it.elementType == CjTokens.UNKNOWN_TOKEN_START }
            equal("\\", slash.text, source)
            equal(TextRange(slashOffset, slashOffset + 1), slash.textRange, source)
        }
    }

    private fun collectAstErrors(root: ASTNode): List<PsiErrorElement> {
        val errors = mutableListOf<PsiErrorElement>()
        fun collect(node: ASTNode) {
            if (node is PsiErrorElement) errors += node
            for (child in node.getChildren(null)) collect(child)
        }
        collect(root)
        return errors
    }

    /** 推测解析回滚删除已创建的错误和宏登记，关闭报告只影响错误节点。 */
    @Test
    fun macroInputScannerRollbackAndSuppressedErrorsPreserveConsumption() {
        assertMacroInputScannerStop("@M(\\])", 1, rollbackFirstParse = true)
        for ((source, width) in listOf("@M(\\${'$'}bad)" to 4, "@M(\\])" to 1, "@M(\\@!Hidden())" to 2)) {
            assertMacroInputScannerStop(source, width, reportErrors = false)
        }
    }

    /** LEX POINT 与真实反斜杠 token 的宽度分别核验，copy 和实际 block diff 均保持完整发布事实。 */
    @Test
    fun quoteTokenDiagnosticRangesSurviveCopyAndRealBlockDiff() {
        val oldText = "func host() { let tokens = quote(\\[)\nlet after = @After() }\nfunc following() {}"
        val offset = oldText.indexOf('\\')
        val expected = listOf(expression("@After()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE))
        val diagnostics = listOf(lexicalBackslashPoint(offset))
        val file = assertSource(oldText, expected, expectedDiagnostics = diagnostics, expectedReportDiagnostics = diagnostics)
        val originalPublication = file.collectParsingDiagnosticPublication()
        val originalSlash = present(file.findElementAt(offset)).node
        equal("\\", originalSlash.text)
        equal(TextRange(offset, offset + 1), originalSlash.textRange)
        val originalToken = present(originalSlash.parsedSourceTokenOrNull())
        val copy = file.copy() as CjFile
        distinct(file.node, copy.node)
        assertOccurrences(copy, expected, diagnostics, expectedReportDiagnostics = diagnostics)
        val copiedSlash = present(copy.findElementAt(offset)).node
        distinct(originalSlash, copiedSlash)
        equal(originalSlash.elementType, copiedSlash.elementType)
        equal("\\", copiedSlash.text)
        equal(TextRange(offset, offset + 1), copiedSlash.textRange)
        same(originalToken, copiedSlash.parsedSourceTokenOrNull())
        equal(originalPublication.reports.map { it.order }, copy.collectParsingDiagnosticPublication().reports.map { it.order })
        val newText = oldText.replaceRange(offset + 1, offset + 2, " plain")
        val result = BlockSupportImpl.reparse(
            file, file.node, TextRange(offset + 1, offset + 2), newText, EmptyProgressIndicator(), oldText,
        )
        equal(oldText, file.text)
        applyPsiChangesOnEdt { result.log.doActualPsiChange(file) }
        equal(newText, file.text)
        assertOccurrences(file, expected, diagnostics, expectedReportDiagnostics = diagnostics)
        assertLightOccurrences(newText, expected, expectedDiagnostics = diagnostics, sourceFile = file,
            expectedReportDiagnostics = diagnostics)
        val updatedSlash = present(file.findElementAt(offset)).node
        equal(CjTokens.UNKNOWN_TOKEN_START, updatedSlash.elementType)
        equal("\\", updatedSlash.text)
        equal(TextRange(offset, offset + 1), updatedSlash.textRange)
        distinct(originalToken, present(updatedSlash.parsedSourceTokenOrNull()))
        equal(listOf("host", "following"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
    }

    /** 仅由真实表达式入口决定停点；后续手动消费用于物化低层测试根，不模拟文件恢复。 */
    private fun assertMacroInputScannerStop(
        source: String,
        width: Int,
        reportErrors: Boolean = true,
        rollbackFirstParse: Boolean = false,
        diagnosticOffset: Int = source.indexOf('\\') + 1,
        stopOffset: Int = diagnosticOffset,
        hasLexicalError: Boolean = false,
    ) {
        val delegate = CangJiePsiBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val builder = SemanticWhitespaceAwarePsiBuilderImpl(delegate)
        builder.setDebugMode(true)
        val parser = CangJieParsing.createForTopLevelNonLazy(builder)
        val root = builder.mark()
        val context = AbstractCangJieParsing.ParsingContext.DEFAULT.copy(shouldReportError = reportErrors)
        val offset = stopOffset
        if (rollbackFirstParse) {
            val speculative = builder.mark()
            with(context) { parser.expressionParsing.parseExpression() }
            equal(offset, builder.currentOffset, source)
            truth(builder.productions.any { it.tokenType is CjTokenRangeErrorElementType }, source)
            speculative.rollbackTo()
            equal(0, builder.currentOffset, source)
            absent(builder.macroParsingJournal.factsAt(0))
            truth(builder.productions.none { it.tokenType is CjTokenRangeErrorElementType }, source)
        }
        with(context) { parser.expressionParsing.parseExpression() }
        equal(offset, builder.currentOffset, source)
        val targetText = present(builder.tokenText)
        equal(offset, source.indexOf(targetText, offset), source)
        val tokenType = present(builder.tokenType)
        // SDK production 列表的 start/done 项返回同一个 marker，按实际 carrier 身份计数。
        val errors = builder.productions.filter { it.tokenType is CjTokenRangeErrorElementType }.distinct()
        equal(if (reportErrors) 1 else 0, errors.size, source)
        if (reportErrors) {
            val error = errors.single { it.startOffset == diagnosticOffset }
            equal(width, (error.tokenType as CjTokenRangeErrorElementType).reportedWidth, source)
            equal(diagnosticOffset, error.startOffset, source)
            equal(diagnosticOffset, error.endOffset, source)
        }
        while (!builder.eof()) builder.advanceLexer()
        root.done(CjNodeTypes.CJ_FILE)
        delegate.finishParsing(builder.macroParsingJournal, null)
        val publication = if (hasLexicalError) collectBuilderPublication(builder) else null
        val ast = delegate.treeBuilt
        val target = ast.getChildren(null).single { it.startOffset == offset && it.elementType == tokenType }
        same(ast, target.treeParent)
        equal(targetText, target.text, source)
        equal(source, ast.text, source)
        val materializedErrors = mutableListOf<CjTokenRangeErrorElement>()
        fun collect(node: ASTNode) {
            if (node is CjTokenRangeErrorElement) materializedErrors += node
            for (child in node.getChildren(null)) collect(child)
        }
        collect(ast)
        equal(if (reportErrors) 1 else 0, materializedErrors.size, source)
        if (reportErrors) {
            val error = materializedErrors.single { it.startOffset == diagnosticOffset }
            equal(TextRange(diagnosticOffset, diagnosticOffset), error.textRange, source)
            equal(TextRange(diagnosticOffset, diagnosticOffset + width), error.reportedRange, source)
        }
        if (hasLexicalError) {
            val slashOffset = source.indexOf('\\')
            val lexical = lexicalBackslashPoint(slashOffset)
            val parsing = if (reportErrors) listOf(ParsingDiagnostic(diagnosticOffset, diagnosticOffset + width,
                CangJieParsingBundle.message("parsing.error.macro.input.escape"),
                category = ParsingDiagnostic.Category.PARSE, rangeKind = ParsingDiagnostic.RangeKind.TOKEN)) else emptyList()
            val complete = present(publication)
            val sourceContext = builder.macroParsingJournal.sourceContext
            assertLegacyParsingDiagnostics(listOf(lexical), complete.diagnostics, sourceContext, source)
            assertLegacyParsingDiagnostics(listOf(lexical) + parsing, complete.reports.map { it.diagnostic }, sourceContext, source)
            val slash = sourceTokenNodes(ast).single { it.startOffset == slashOffset && it.elementType == CjTokens.UNKNOWN_TOKEN_START }
            equal("\\", slash.text, source)
            equal(TextRange(slashOffset, slashOffset + 1), slash.textRange, source)
        }
    }

    /** 低层表达式根没有 CjFile；将真实 lexer 报告与 SDK typed productions 交给公共 collector 完成发布。 */
    private fun collectBuilderPublication(builder: SemanticWhitespaceAwarePsiBuilderImpl): CjParsingDiagnosticPublication {
        val journal = builder.macroParsingJournal
        val sourceContext = journal.sourceContext
        val collector = CjParsingDiagnosticCollector(sourceContext)
        journal.lexicalInput.tokens.forEach { token ->
            journal.lexicalInput.diagnosticForToken(token.id, sourceContext)?.let(collector::restoreReport)
        }
        builder.productions.filter { it.tokenType is CjParsingErrorElementType }.distinct().forEach { production ->
            val type = production.tokenType as CjParsingErrorElementType
            collector.restoreReport(CjParsingDiagnosticReport(
                type.diagnostic(production.startOffset, production.endOffset), type.diagnosticOrder,
            ))
        }
        collector.finish()
        return collector.publication
    }

    private fun lexicalBackslashPoint(offset: Int): ParsingDiagnostic = ParsingDiagnostic(
        offset, offset, CangJieParsingBundle.message("parsing.error.unknown.token.start", "\\"),
        category = ParsingDiagnostic.Category.LEX, rangeKind = ParsingDiagnostic.RangeKind.POINT,
    )

    private fun sourceTokenNodes(root: ASTNode): List<ASTNode> {
        val tokens = mutableListOf<ASTNode>()
        fun collect(node: ASTNode) {
            if (node.parsedSourceTokenOrNull() != null) tokens += node
            for (child in node.getChildren(null)) collect(child)
        }
        collect(root)
        return tokens
    }

    /** tuple 第一项的逗号细化必须随整个真实 parse 回滚；同一 token 重新解析不得重复登记。 */
    @Test
    fun actualTupleParsingRollsBackRegistrationAndCommaRefinementTogether() {
        val source = "(@First(), @Second())"
        val builder = SemanticWhitespaceAwarePsiBuilderImpl(
            PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source),
        )
        val parser = CangJieParsing.createForTopLevelNonLazy(builder)
        val root = builder.mark()
        val speculative = builder.mark()
        with(AbstractCangJieParsing.ParsingContext.DEFAULT) { parser.expressionParsing.parseExpression() }
        truth(builder.eof())
        val first = source.indexOf("@First()")
        val second = source.indexOf("@Second()")
        val expected = MacroParsingFacts(
            MacroParsingScope.Expression(CjExprKind.EXPR_IN_TUPLE),
            CjMacroFragmentParent.TUPLE_LITERAL,
            null,
        )
        equal(expected, builder.macroParsingJournal.factsAt(first))
        equal(expected, builder.macroParsingJournal.factsAt(second))
        speculative.rollbackTo()
        equal(0, builder.currentOffset)
        absent(builder.macroParsingJournal.factsAt(first))
        absent(builder.macroParsingJournal.factsAt(second))
        with(AbstractCangJieParsing.ParsingContext.DEFAULT) { parser.expressionParsing.parseExpression() }
        truth(builder.eof())
        equal(expected, builder.macroParsingJournal.factsAt(first))
        equal(expected, builder.macroParsingJournal.factsAt(second))
        root.done(CjNodeTypes.CJ_FILE)
        builder.lightTree
    }

    /** SDK 生成候选 diff 不改旧 facts；提交后相同 @ 文本也必须得到新的低层入口。 */
    @Test
    fun applyingARealBlockDiffUpdatesFactsForAnUnchangedMacroToken() {
        val oldText = "func host() { let value = @M()\n}\nfunc following() {}"
        val file = assertSource(oldText, listOf(expression("@M()", CjExprKind.VAR_INIT, parent = CjNodeTypes.VARIABLE)))
        val host = file.declarations.filterIsInstance<CjNamedFunction>().first()
        val body = present(host.bodyBlockExpression)
        val macro = PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java).single()
        val oldFacts = present(macro.parsingFacts)
        val oldToken = present(macro.node.findChildByType(CjTokens.AT))
        val insertion = oldText.indexOf("@M()") + "@M()".length
        val range = TextRange(insertion, insertion)
        val newText = oldText.replaceRange(insertion, insertion, " + 1")
        val result = BlockSupportImpl.reparse(file, file.node, range, newText, EmptyProgressIndicator(), oldText)
        same(body.node, result.oldRoot)
        equal(oldText, file.text)
        same(oldToken, macro.node.findChildByType(CjTokens.AT))
        equal(oldFacts, macro.parsingFacts)
        applyPsiChangesOnEdt { result.log.doActualPsiChange(file) }
        equal(newText, file.text)
        val expected = listOf(declaration("@M()", parent = CjNodeTypes.BINARY_EXPRESSION))
        assertOccurrences(file, expected)
        assertLightOccurrences(newText, expected, sourceFile = file)
    }

    @Test
    fun lightFactsAreImmutableAndRemainBoundToTheirOwnParseRoot() {
        val firstSource = "class First { let value = @M() }"
        val laterSource = "class Later { let value = @M() }"
        val first = assertLightOccurrences(
            firstSource, listOf(expression("@M()", CjExprKind.VAR_INIT, owner = "First", parent = CjNodeTypes.FIELD)),
        )
        val later = assertLightOccurrences(
            laterSource, listOf(expression("@M()", CjExprKind.VAR_INIT, owner = "Later", parent = CjNodeTypes.FIELD)),
        )
        distinct(first.tree, later.tree)
        equal("First", present(first.macroFacts[firstSource.indexOf("@M()")]).outerDeclIdent)
        equal("Later", present(later.macroFacts[laterSource.indexOf("@M()")]).outerDeclIdent)
        assertFailsWith<UnsupportedOperationException> {
            (first.macroFacts as MutableMap<Int, MacroParsingFacts>).clear()
        }
        equal(1, first.macroFacts.size)
    }

    private fun assertSource(
        source: String,
        expected: List<Expected>,
        annotationOnly: Boolean = false,
        expectedDiagnostics: List<ParsingDiagnostic> = emptyList(),
        expectedReportDiagnostics: List<ParsingDiagnostic>? = null,
    ): CjFile {
        val factory = CjPsiFactory(project, markGenerated = false)
        val file = if (annotationOnly) factory.createAnnotationOnlyFile(source) else factory.createFile(source)
        assertOccurrences(file, expected, expectedDiagnostics, expectedReportDiagnostics)
        assertLightOccurrences(source, expected, annotationOnly, expectedDiagnostics, sourceFile = file,
            expectedReportDiagnostics = expectedReportDiagnostics)
        return file
    }

    /** LT 对独立语义期望作同样断言；冻结 Map 必须对应本次返回树的实际宏节点与 AT 位置。 */
    private fun assertLightOccurrences(
        source: String, expected: List<Expected>, annotationOnly: Boolean = false,
        expectedDiagnostics: List<ParsingDiagnostic> = emptyList(),
        sourceFile: CjFile? = null,
        expectedReportDiagnostics: List<ParsingDiagnostic>? = null,
    ): ParsedCangJieLightFile {
        val diagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        builder.setDebugMode(true)
        val parsed = if (sourceFile != null) {
            equal(source, sourceFile.text)
            val result = if (annotationOnly) parseAnnotationLightWithPsiInput(sourceFile)
                else parseLightWithPsiInput(sourceFile, builder)
            diagnostics += result.diagnostics
            result.file
        } else if (annotationOnly) {
            CangJieLightParser.parseAnnotationOnly(builder, errorListener = { diagnostics += it })
        } else {
            CangJieLightParser.parse(builder, errorListener = { diagnostics += it })
        }
        assertLegacyParsingDiagnostics(expectedDiagnostics, diagnostics,
            parsed.sourceContext, "LT\n$source")
        assertCanonicalParsingDiagnostics(diagnostics, parsed.sourceContext, "LT\n$source")
        expectedReportDiagnostics?.let { reports ->
            assertLegacyParsingDiagnostics(reports, parsed.diagnosticPublication.reports.map { it.diagnostic },
                parsed.sourceContext, "LT reports\n$source")
        }
        if (sourceFile != null) assertPublishedParsingDiagnosticParity(sourceFile.collectParsingDiagnostics(), diagnostics,
            parsed.sourceContext, source)
        val expectedByOffset = expected.associateBy { uniqueOffset(source, it.text) }
        equal(expectedByOffset.mapValues { it.value.facts }, parsed.macroFacts, "LT\n$source")
        val macros = mutableListOf<LightMacro>()
        val tokens = mutableSetOf<Int>()
        collectLightNodes(parsed.tree.root, parsed.tree, null, macros, tokens)
        truth(tokens.containsAll(parsed.macroFacts.keys), "冻结事实必须属于本次树的真实 AT 叶：$source")
        for ((offset, item) in expectedByOffset) {
            val matches = macros.filter { it.start == offset }
            equal(1, matches.size, "LT 唯一真实宏必须位于 $offset：$source")
            val macro = matches.single()
            equal(item.text, source.substring(macro.start, macro.end), "LT\n$source")
            item.parent?.let { equal(it, macro.parent, "LT\n$source") }
        }
        return parsed
    }

    /** 仅保留范围和类型值；每次取出的 LT 节点数组均在退出递归时归还给 SDK。 */
    private fun collectLightNodes(
        node: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        parent: IElementType?,
        macros: MutableList<LightMacro>,
        tokens: MutableSet<Int>,
    ) {
        if (node.tokenType == CjNodeTypes.MACRO_EXPRESSION) macros += LightMacro(node.startOffset, node.endOffset, parent)
        if (node.tokenType == CjTokens.AT) tokens += node.startOffset
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get() ?: return
        try {
            val actualType = if (node.tokenType is CjParsingErrorElementType) com.intellij.psi.TokenType.ERROR_ELEMENT else node.tokenType
            for (index in 0 until count) collectLightNodes(present(children[index]), tree, actualType, macros, tokens)
        } finally {
            tree.disposeChildren(children, count)
        }
    }

    private data class LightMacro(val start: Int, val end: Int, val parent: IElementType?)

    /** 全量核对真实 AT 载体，包含没有 CjMacroExpression 包装的 annotation/attr token。 */
    private fun assertOccurrences(
        file: CjFile, expected: List<Expected>, expectedDiagnostics: List<ParsingDiagnostic> = emptyList(),
        expectedReportDiagnostics: List<ParsingDiagnostic>? = null,
    ) {
        val source = file.text
        val macros = PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java)
        val publication = file.collectParsingDiagnosticPublication()
        val sourceContext = file.currentPsiSourceView().sourceContext
        assertLegacyParsingDiagnostics(expectedDiagnostics, publication.diagnostics, sourceContext, source)
        assertCanonicalParsingDiagnostics(publication.diagnostics, sourceContext, source)
        expectedReportDiagnostics?.let { reports ->
            assertLegacyParsingDiagnostics(reports, publication.reports.map { it.diagnostic }, sourceContext, "PSI reports\n$source")
        }
        val expectedByOffset = expected.associateBy { uniqueOffset(source, it.text) }
        equal(expected.size, expectedByOffset.size, source)
        equal(
            expectedByOffset.mapValues { it.value.facts },
            atNodes(file.node).mapNotNull { token ->
                token.getCopyableUserData(MACRO_PARSING_FACTS_KEY)?.let { token.startOffset to it }
            }.toMap(),
            source,
        )
        for ((offset, item) in expectedByOffset) {
            val atOffset = macros.filter { it.textOffset == offset }
            equal(1, atOffset.size, "唯一真实宏必须位于 $offset：$source")
            val macro = atOffset.single()
            equal(item.text, macro.text, source)
            equal(item.facts, macro.parsingFacts, "${item.text}\n$source")
            item.parent?.let { equal(it, macro.parent.node.elementType, source) }
        }
        for (macro in macros.filter { it.textOffset !in expectedByOffset }) absent(macro.parsingFacts, macro.text)
    }

    /** token-only 内容只要求真实 AT 叶没有事实，不要求 parser 为它建立宏表达式。 */
    private fun assertTokenOnly(file: CjFile, text: String) {
        val offset = uniqueOffset(file.text, text)
        val leaf = present(file.findElementAt(offset))
        equal(CjTokens.AT, leaf.node.elementType, text)
        equal(offset, leaf.textOffset, text)
        absent(leaf.node.getCopyableUserData(MACRO_PARSING_FACTS_KEY), text)
    }

    private fun atNodes(root: ASTNode): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        fun visit(node: ASTNode) {
            if (node.elementType == CjTokens.AT) result += node
            var child = node.firstChildNode
            while (child != null) {
                visit(child)
                child = child.treeNext
            }
        }
        visit(root)
        return result
    }

    private fun uniqueOffset(source: String, text: String): Int {
        val offset = source.indexOf(text)
        truth(offset >= 0, "$text\n$source")
        equal(-1, source.indexOf(text, offset + 1), "用例必须明确唯一语法位置：$text")
        return offset
    }

    private data class Expected(val text: String, val facts: MacroParsingFacts, val parent: IElementType?)

    private fun expression(
        text: String, kind: CjExprKind, comma: Boolean = false, owner: String? = null, parent: IElementType? = null,
    ): Expected {
        val macroParent = when (parent) {
            CjNodeTypes.TUPLE_EXPRESSION -> CjMacroFragmentParent.TUPLE_LITERAL
            CjNodeTypes.VALUE_ARGUMENT -> CjMacroFragmentParent.FUNCTION_ARGUMENT
            else -> when (kind) {
                CjExprKind.EXPR_IN_ARRAY -> CjMacroFragmentParent.ARRAY_LITERAL
                else -> CjMacroFragmentParent.OTHER
            }
        }
        equal(comma, macroParent.isConnectedByComma, "$text direct parent $parent")
        return Expected(
            text,
            MacroParsingFacts(MacroParsingScope.Expression(kind), macroParent, owner),
            parent,
        )
    }

    private fun declaration(
        text: String, kind: CjScopeKind = CjScopeKind.UNKNOWN_SCOPE, owner: String? = null, parent: IElementType? = null,
    ): Expected = Expected(
        text,
        MacroParsingFacts(MacroParsingScope.Declaration(kind), CjMacroFragmentParent.OTHER, owner),
        parent,
    )

    /** 对位平台真实 PSI 修改要求：EDT、command、write action 各自保持完整生命周期。 */
    private fun applyPsiChangesOnEdt(change: () -> Unit) {
        try {
            EventQueue.invokeAndWait {
                CommandProcessor.getInstance().executeCommand(
                    project,
                    { ApplicationManager.getApplication().runWriteAction { change() } },
                    "更新宏语法事实",
                    null,
                )
            }
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }
}
