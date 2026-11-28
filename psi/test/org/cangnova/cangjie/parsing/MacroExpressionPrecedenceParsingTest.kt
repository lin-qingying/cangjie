package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertNotNull as assertPresent
import kotlin.test.assertTrue as assertTrueValue

/**
 * 官方 v1.1.3 ParseAtom.cpp 的 ParseAtom/ParseMacroExprOrLambdaExpr 将普通宏作为原子；
 * ParseExpr.cpp 的 ParseBaseExpr/Postfix、ParseUnaryExpr 和 ParseExpr 随后构造完整表达式。
 * ParseMacro.cpp 的 CheckMacroExprRules 不限制括号 token 输入宏参与运算。
 * 这里独立验证 PSI/LT 的类型、原始范围、子树和直接语法槽位，不验证宏求值或尚未实现的 scope facts。
 */
class MacroExpressionPrecedenceParsingTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun initializerKeepsTheCompleteMacroExpression() = assertExpressionSlot(Slot.INITIALIZER)

    @Test
    fun parameterDefaultKeepsTheCompleteMacroExpression() = assertExpressionSlot(Slot.DEFAULT_VALUE)

    @Test
    fun callArgumentKeepsTheCompleteMacroExpression() = assertExpressionSlot(Slot.ARGUMENT)

    @Test
    fun returnConsumesTheMacroExpressionAsItsOperand() = assertExpressionSlot(Slot.RETURN_VALUE)

    @Test
    fun conditionKeepsTheCompleteMacroExpression() = assertExpressionSlot(Slot.CONDITION)

    /** 官方 ParseExprOrDecl 在创建语句宏后继续后缀和二元运算，不在宏 input 处结束语句。 */
    @Test
    fun statementMacroContinuesTheSharedExpressionPrecedence() {
        for ((expression, shape) in expressionCases().filter { it.first.startsWith("@") }) {
            val text = "func target() { $expression\n}\nfunc following() {}"
            val expected = shape.shift(text.indexOf(expression))
            assertEachTree("statementMacro", text) { root, route ->
                val parsed = root.requireNode(expected.type, expected.start, expected.end, route)
                assertValueEquals(expected, parsed, route)
                assertValueEquals(CjNodeTypes.BLOCK, root.parentOf(parsed)?.type, route)
                assertFollowingFunction(root, text, route)
            }
        }
    }

    @Test
    fun statementMacroPreservesMultiplicationAndRightAssociativeExponentiation() {
        for ((expression, expected) in listOf(
            "@M(1)+2*3" to node(
                CjNodeTypes.BINARY_EXPRESSION, 0, 9, macro(0), operation(CjTokens.PLUS, 5),
                node(CjNodeTypes.BINARY_EXPRESSION, 6, 9, integer(6, "2"), operation(CjTokens.MUL, 7), integer(8, "3")),
            ),
            "@M(1)**2**3" to node(
                CjNodeTypes.BINARY_EXPRESSION, 0, 11, macro(0),
                node(CjNodeTypes.OPERATION_REFERENCE, 5, 7, node(CjTokens.MULMUL, 5, 7)),
                node(
                    CjNodeTypes.BINARY_EXPRESSION, 7, 11, integer(7, "2"),
                    node(CjNodeTypes.OPERATION_REFERENCE, 8, 10, node(CjTokens.MULMUL, 8, 10)), integer(10, "3"),
                ),
            ),
        )) {
            val text = "func target() { $expression\n}"
            assertEachTree("statementOperators", text) { root, route ->
                val shape = expected.shift(text.indexOf(expression))
                assertValueEquals(shape, root.requireNode(shape.type, shape.start, shape.end, route), route)
            }
        }
    }

    @Test
    fun statementMacroDoesNotContinueAcrossPhysicalNewline() {
        val text = "func target() { @M(1)\n-2\n}"
        assertEachTree("statementBoundary", text) { root, route ->
            val macroStart = text.indexOf("@M(1)")
            val invocation = root.requireNode(CjNodeTypes.MACRO_EXPRESSION, macroStart, macroStart + 5, route)
            val negativeStart = text.indexOf("-2")
            val negative = root.requireNode(CjNodeTypes.PREFIX_EXPRESSION, negativeStart, negativeStart + 2, route)
            assertValueEquals(CjNodeTypes.BLOCK, root.parentOf(invocation)?.type, route)
            assertValueEquals(root.parentOf(invocation), root.parentOf(negative), route)
        }
    }

    @Test
    fun scopedLocalStatementUsesTheSameMacroExpressionChain() {
        val text = "@M(1)+2"
        val expected = expressionCases().single { it.first == text }.second
        for (light in listOf(false, true)) {
            val builder = SemanticWhitespaceAwarePsiBuilderImpl(
                PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text),
            )
            val parser = CangJieParsing.createForTopLevelNonLazy(builder)
            val root = builder.mark()
            with(AbstractCangJieParsing.ParsingContext.DEFAULT) {
                parser.expressionParsing.parseStatementByScope(DeclarationParsingMode.LOCAL)
            }
            assertTrueValue(builder.eof())
            root.done(CjNodeTypes.CJ_FILE)
            val shape = if (light) builder.lightTree.let { snapshot(it.root, it) } else snapshot(builder.treeBuilt)
            assertValueEquals(listOf(expected), shape.children)
        }
    }

    @Test
    fun annotationOnlyStatementPrefixRemainsAnAnnotationOnItsDeclaration() {
        val text = "func target() { @Tag[1] func nested() {}\n}"
        val start = text.indexOf("@Tag")
        assertEachTree("annotationStatementPrefix", text, annotationOnly = true) { root, route ->
            val annotation = root.requireNode(CjNodeTypes.ANNOTATION, start, start + 7, route)
            val annotations = assertPresent(root.parentOf(annotation), route)
            assertValueEquals(CjStubElementTypes.ANNOTATIONS, annotations.type, route)
            assertValueEquals(CjNodeTypes.FUNC, root.parentOf(annotations)?.type, route)
            assertValueEquals(emptyList<Node>(), root.descendants().filter { it.type == CjNodeTypes.MACRO_EXPRESSION }, route)
        }
    }

    @Test
    fun statementMacroContinuesDotAfterPhysicalNewline() {
        val expression = "@M(1)\n.foo"
        val text = "func target() { $expression\n}"
        val expected = node(
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION, 0, 10, macro(0), node(CjTokens.DOT, 6, 7),
            node(CjNodeTypes.REFERENCE_EXPRESSION, 7, 10, node(CjTokens.IDENTIFIER, 7, 10)),
        ).shift(text.indexOf(expression))
        assertEachTree("statementDotContinuation", text) { root, route ->
            assertValueEquals(expected, root.requireNode(expected.type, expected.start, expected.end, route), route)
        }
    }

    /** 外层声明宏的 backToken 不能使普通默认值里的内层宏丢失自己的 marker。 */
    @Test
    fun declarationMacroInputKeepsItsNestedDefaultExpressionMacro() {
        val text = "@Outer func target(value!: Int64 = @Inner()+1) {}"
        val expressionStart = text.indexOf("@Inner()")
        val expected = node(
            CjNodeTypes.BINARY_EXPRESSION, 0, 10,
            macro(0, "Inner", ""), operation(CjTokens.PLUS, 8), integer(9),
        ).shift(expressionStart)
        assertEachTree("nestedDefault", text) { root, route ->
            val expression = root.requireNode(expected.type, expected.start, expected.end, route)
            assertValueEquals(expected, expression, route)
            assertValueEquals(CjNodeTypes.VALUE_PARAMETER, root.parentOf(expression)?.type, route)
            val outer = root.requireNode(CjNodeTypes.MACRO_EXPRESSION, 0, text.length, route)
            val input = outer.children.single { it.type == CjNodeTypes.MACRO_INPUT }
            assertValueEquals(listOf(CjNodeTypes.FUNC), input.children.map { it.type }, route)
            assertValueEquals(text.indexOf("func target"), input.children.single().start, route)
            assertValueEquals(text.length, input.children.single().end, route)
            assertTrueValue(input.descendants().any { it === expression }, route)
        }
    }

    /** 官方 ParseExpr(ExprKind) 仅在 enableCustomAnno 下专门解析 IfAvailable；不添加后缀许可。 */
    @Test
    fun ifAvailableKeepsItsDedicatedExpressionAndThreeArgumentsInAnnotationMode() {
        val expression = "@IfAvailable(level: 19, { => 1 }, { => 2 })"
        val text = "func target() { let value = $expression\n}"
        val start = text.indexOf(expression)
        assertEachTree("ifAvailable", text, annotationOnly = true) { root, route ->
            val available = root.requireNode(CjNodeTypes.IF_AVAILABLE_EXPRESSION, start, start + expression.length, route)
            assertValueEquals(CjNodeTypes.VARIABLE, root.parentOf(available)?.type, route)
            assertValueEquals(
                listOf(
                    CjTokens.AT, CjTokens.IDENTIFIER, CjTokens.LPAR, CjNodeTypes.VALUE_ARGUMENT,
                    CjTokens.COMMA, CjNodeTypes.LAMBDA_EXPRESSION, CjTokens.COMMA,
                    CjNodeTypes.LAMBDA_EXPRESSION, CjTokens.RPAR,
                ),
                available.children.map { it.type }, route,
            )
            val conditionStart = text.indexOf("level:")
            val condition = available.requireNode(CjNodeTypes.VALUE_ARGUMENT, conditionStart, conditionStart + "level: 19".length, route)
            val valueStart = text.indexOf("19")
            assertValueEquals(integer(valueStart, "19"), condition.requireNode(CjNodeTypes.INTEGER_CONSTANT, valueStart, valueStart + 2, route), route)
            for (value in listOf("1", "2")) {
                val lambdaText = "{ => $value }"
                val lambdaStart = text.indexOf(lambdaText)
                val lambda = available.requireNode(CjNodeTypes.LAMBDA_EXPRESSION, lambdaStart, lambdaStart + lambdaText.length, route)
                assertValueEquals(integer(lambdaStart + 5, value), lambda.requireNode(CjNodeTypes.INTEGER_CONSTANT, lambdaStart + 5, lambdaStart + 6, route), route)
            }
            assertValueEquals(emptyList<Node>(), available.descendants().filter { it.type == CjNodeTypes.MACRO_EXPRESSION }, route)
        }
    }

    /** 官方 ParseAnnotationLambdaExpr 接受 NUMERIC_OVERFLOW 注解；保留现有无后缀入口。 */
    @Test
    fun overflowAnnotationKeepsItsLambdaInsteadOfBecomingAMacro() {
        val expression = "@OverflowWrapping { => 1 }"
        val text = "func target() { let value = $expression\n}"
        val start = text.indexOf(expression)
        assertEachTree("annotationLambda", text) { root, route ->
            val annotated = root.requireNode(CjNodeTypes.ANNOTATED_EXPRESSION, start, start + expression.length, route)
            assertValueEquals(CjNodeTypes.VARIABLE, root.parentOf(annotated)?.type, route)
            assertValueEquals(listOf(CjStubElementTypes.ANNOTATIONS, CjNodeTypes.LAMBDA_EXPRESSION), annotated.children.map { it.type }, route)
            annotated.requireNode(CjNodeTypes.ANNOTATION, start, start + "@OverflowWrapping".length, route)
            val lambdaStart = text.indexOf("{ => 1 }")
            val lambda = annotated.requireNode(CjNodeTypes.LAMBDA_EXPRESSION, lambdaStart, lambdaStart + 8, route)
            assertValueEquals(integer(lambdaStart + 5), lambda.requireNode(CjNodeTypes.INTEGER_CONSTANT, lambdaStart + 5, lambdaStart + 6, route), route)
            assertValueEquals(emptyList<Node>(), annotated.descendants().filter { it.type == CjNodeTypes.MACRO_EXPRESSION }, route)
        }
    }

    /** 官方 ParseAnnotations.cpp:228/322 的 enableCustomAnno 将同一声明前缀改为 custom annotation。 */
    @Test
    fun ordinaryAndAnnotationOnlyFilesKeepDistinctDeclarationPrefixNodes() {
        val text = "@Tag[1] func target() {}"
        assertEachTree("ordinaryPrefix", text) { root, route ->
            val macro = root.requireNode(CjNodeTypes.MACRO_EXPRESSION, 0, text.length, route)
            assertValueEquals(
                listOf(CjTokens.AT, CjNodeTypes.REFERENCE_EXPRESSION, CjNodeTypes.MACRO_ATTR, CjNodeTypes.MACRO_INPUT),
                macro.children.map { it.type }, route,
            )
            macro.requireNode(CjNodeTypes.MACRO_ATTR, 4, 7, route)
            val input = macro.requireNode(CjNodeTypes.MACRO_INPUT, 8, text.length, route)
            assertValueEquals(listOf(CjNodeTypes.FUNC), input.children.map { it.type }, route)
            assertValueEquals(emptyList<Node>(), root.descendants().filter { it.type == CjNodeTypes.ANNOTATION }, route)
        }
        assertEachTree("annotationPrefix", text, annotationOnly = true) { root, route ->
            val annotation = root.requireNode(CjNodeTypes.ANNOTATION, 0, 7, route)
            val arguments = annotation.requireNode(CjNodeTypes.VALUE_ARGUMENT_LIST, 4, 7, route)
            assertValueEquals(
                node(
                    CjNodeTypes.VALUE_ARGUMENT_LIST, 4, 7,
                    node(CjTokens.LBRACKET, 4, 5),
                    node(CjNodeTypes.VALUE_ARGUMENT, 5, 6, integer(5)),
                    node(CjTokens.RBRACKET, 6, 7),
                ), arguments, route,
            )
            val annotations = assertPresent(root.parentOf(annotation), route)
            assertValueEquals(CjStubElementTypes.ANNOTATIONS, annotations.type, route)
            assertValueEquals(CjNodeTypes.FUNC, root.parentOf(annotations)?.type, route)
            assertValueEquals(emptyList<Node>(), root.descendants().filter { it.type == CjNodeTypes.MACRO_EXPRESSION }, route)
        }
    }

    /** 官方 ParseMacroExprOrLambdaExpr 的 annotation-lambda 分流同样适用于二元运算右侧。 */
    @Test
    fun overflowAnnotationOnBinaryRightHandSideKeepsItsDedicatedLambda() {
        val expression = "1+@OverflowWrapping { => 1 }"
        val text = "func target() { let value = $expression\n}"
        val start = text.indexOf(expression)
        val annotationStart = start + 2
        assertEachTree("annotationLambdaRhs", text) { root, route ->
            val binary = root.requireNode(CjNodeTypes.BINARY_EXPRESSION, start, start + expression.length, route)
            assertValueEquals(CjNodeTypes.VARIABLE, root.parentOf(binary)?.type, route)
            assertValueEquals(
                listOf(CjNodeTypes.INTEGER_CONSTANT, CjNodeTypes.OPERATION_REFERENCE, CjNodeTypes.ANNOTATED_EXPRESSION),
                binary.children.map { it.type }, route,
            )
            assertValueEquals(integer(start), binary.children[0], route)
            assertValueEquals(operation(CjTokens.PLUS, start + 1), binary.children[1], route)
            val annotated = binary.requireNode(CjNodeTypes.ANNOTATED_EXPRESSION, annotationStart, start + expression.length, route)
            assertValueEquals(listOf(CjStubElementTypes.ANNOTATIONS, CjNodeTypes.LAMBDA_EXPRESSION), annotated.children.map { it.type }, route)
            annotated.requireNode(CjNodeTypes.ANNOTATION, annotationStart, annotationStart + "@OverflowWrapping".length, route)
            val lambdaStart = text.indexOf("{ => 1 }")
            val lambda = annotated.requireNode(CjNodeTypes.LAMBDA_EXPRESSION, lambdaStart, lambdaStart + 8, route)
            assertValueEquals(integer(lambdaStart + 5), lambda.requireNode(CjNodeTypes.INTEGER_CONSTANT, lambdaStart + 5, lambdaStart + 6, route), route)
            assertValueEquals(emptyList<Node>(), binary.descendants().filter { it.type == CjNodeTypes.MACRO_EXPRESSION }, route)
        }
    }

    /** 官方 ParseAtom.cpp:1444 的 builtin 拒绝先于 ParseMacroCall，不能把后续 func 吞进宏输入。 */
    @Test
    fun builtinAnnotationOnBinaryRightHandSideIsRejectedAsAnExpression() {
        val text = "func target() { let value = 1+@C func stolen() {}\n}\nfunc following() {}"
        assertRejectedBuiltin("builtinRhs", text)
    }

    /** 实参循环必须在拒绝 builtin 后取得进展，最终仍能解析后续顶层函数。 */
    @Test
    fun builtinAnnotationInCallArgumentIsRejectedWithoutStallingOrCreatingAMacro() {
        for (input in listOf("@C, 2", "@C func stolen() {}")) {
            val text = "func target() { consume($input)\n}\nfunc following() {}"
            assertRejectedBuiltin("builtinArgument", text)
        }
    }

    /** 官方 ParseMemberAccess 只调用 ExpectIdentifierWithPos，宏和整数字面量都不是 selector。 */
    @Test
    fun macroAndIntegerCannotBecomeMemberSelectors() {
        for ((expression, invalidToken) in listOf("obj.@M()" to "@", "obj. @M()" to "@", "obj. 1" to "1")) {
            val text = "func target() { let value = $expression\n}\nfunc following() {}"
            val expressionStart = text.indexOf(expression)
            val invalidStart = expressionStart + expression.indexOf(invalidToken)
            assertEachRejectedTree("invalidSelector", text) { root, diagnostics, route ->
                assertDiagnosticAt(diagnostics, "parsing.error.expecting.identifier", invalidStart, route)
                val qualified = root.descendants().filter { it.type == CjNodeTypes.DOT_QUALIFIED_EXPRESSION && it.start == expressionStart }
                for (member in qualified) {
                    val selectorNodes = member.children.dropWhile { it.type != CjTokens.DOT }.drop(1).flatMap { it.descendants() }
                    assertTrueValue(selectorNodes.none {
                        it.type == CjNodeTypes.MACRO_EXPRESSION || it.type == CjNodeTypes.INTEGER_CONSTANT ||
                            it.type == CjNodeTypes.REFERENCE_EXPRESSION || it.type == CjNodeTypes.CALL_EXPRESSION
                    }, "$route\nInvalid token became a member selector: $member")
                }
                assertFollowingFunction(root, text, route)
            }
        }
    }

    /** 官方 Lexer.cpp:1950 将紧邻数字的点交给 ScanNumber；obj.1 没有成员访问的 DOT token。 */
    @Test
    fun adjacentDotNumberKeepsItsFloatTokenAndIsRejectedAsAnUnseparatedExpression() {
        val expression = "obj.1"
        val lexer = CangJieLexer()
        lexer.start(expression)
        val tokens = mutableListOf<Node>()
        while (lexer.tokenType != null) {
            tokens += node(requireNotNull(lexer.tokenType), lexer.tokenStart, lexer.tokenEnd)
            lexer.advance()
        }
        assertValueEquals(
            listOf(node(CjTokens.IDENTIFIER, 0, 3), node(CjTokens.FLOAT_LITERAL, 3, 5)),
            tokens,
        )

        val text = "func target() { let value = $expression\n}\nfunc following() {}"
        val expressionStart = text.indexOf(expression)
        val floatStart = expressionStart + 3
        assertEachRejectedTree("adjacentDotNumber", text) { root, diagnostics, route ->
            val message = CangJieParsingBundle.message("parsing.error.unexpected.tokens.same.line")
            assertTrueValue(diagnostics.any {
                it.message == message && it.startOffset == floatStart && it.endOffset == floatStart + 2
            }, "$route\nExpected $message at [$floatStart, ${floatStart + 2}), got $diagnostics")
            assertTrueValue(root.descendants().none {
                it.type == CjNodeTypes.DOT_QUALIFIED_EXPRESSION && it.start == expressionStart
            }, "$route\nThe float token became a member selector")
            assertFollowingFunction(root, text, route)
        }
    }

    /** 官方 ExpectIdentifierWithPos 接受 GetContextualKeyword；此处的 public/open/features 都在 v1.1.3 名单中。 */
    @Test
    fun ordinaryContextualAndMacroReceiverMemberAccessKeepValidSelectors() {
        for (name in listOf("foo", "public", "open", "features")) {
            val expression = "obj.$name"
            val text = "func target() { let value = $expression\n}"
            val start = text.indexOf(expression)
            val expected = node(
                CjNodeTypes.DOT_QUALIFIED_EXPRESSION, 0, expression.length,
                node(CjNodeTypes.REFERENCE_EXPRESSION, 0, 3, node(CjTokens.IDENTIFIER, 0, 3)),
                node(CjTokens.DOT, 3, 4),
                node(CjNodeTypes.REFERENCE_EXPRESSION, 4, expression.length, node(CjTokens.IDENTIFIER, 4, expression.length)),
            ).shift(start)
            assertEachTree("validSelector$name", text) { root, route ->
                assertValueEquals(expected, root.requireNode(expected.type, expected.start, expected.end, route), route)
            }
        }
        val expression = "@M().foo"
        val text = "func target() { let value = $expression\n}"
        val start = text.indexOf(expression)
        val expected = node(
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION, 0, 8, macro(0, input = ""), node(CjTokens.DOT, 4, 5),
            node(CjNodeTypes.REFERENCE_EXPRESSION, 5, 8, node(CjTokens.IDENTIFIER, 5, 8)),
        ).shift(start)
        assertEachTree("macroReceiverSelector", text) { root, route ->
            assertValueEquals(expected, root.requireNode(expected.type, expected.start, expected.end, route), route)
        }
    }

    private fun assertRejectedBuiltin(name: String, text: String) {
        val invalidStart = text.indexOf("@C")
        assertEachRejectedTree(name, text) { root, diagnostics, route ->
            assertDiagnosticAt(diagnostics, "parsing.error.expecting.expression", invalidStart, route)
            assertValueEquals(emptyList<Node>(), root.descendants().filter { it.type == CjNodeTypes.MACRO_EXPRESSION }, route)
            assertFollowingFunction(root, text, route)
        }
    }

    /** 非法输入只约束首要错误的含义与位置，以及后续合法声明；不固定恢复子树和附带错误数量。 */
    private fun assertDiagnosticAt(diagnostics: List<ParsingDiagnostic>, key: String, offset: Int, route: String) {
        val message = CangJieParsingBundle.message(key)
        assertTrueValue(diagnostics.any { it.message == message && it.startOffset <= offset && offset <= it.endOffset }, "$route\nExpected $message at $offset, got $diagnostics")
    }

    private fun assertFollowingFunction(root: Node, text: String, route: String) {
        val following = root.requireNode(CjNodeTypes.FUNC, text.indexOf("func following"), text.length, route)
        assertTrueValue(root.children.any { it === following }, "$route\nFollowing function was swallowed by error recovery")
    }

    private fun assertEachRejectedTree(name: String, text: String, assertion: (Node, List<ParsingDiagnostic>, String) -> Unit) {
        val file = createPsiFile(name, text) as CjFile
        val psiTree = snapshot(file.node)
        assertion(psiTree, file.collectParsingDiagnostics(), "PSI $name\n$text")

        val diagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        builder.setDebugMode(true)
        val tree = CangJieLightParser.parse(builder, errorListener = { diagnostics += it }).tree
        assertion(snapshot(tree.root, tree), diagnostics, "LT $name\n$text")
    }

    private fun assertExpressionSlot(slot: Slot) {
        for ((expressionText, expectedRelative) in expressionCases()) {
            val (prefix, suffix) = when (slot) {
                Slot.INITIALIZER -> "func target() { let value = " to "\n}"
                Slot.DEFAULT_VALUE -> "func target(value!: Int64 = " to ") {}"
                Slot.ARGUMENT -> "func target() { consume(" to ")\n}"
                Slot.RETURN_VALUE -> "func target() { return " to "\n}"
                Slot.CONDITION -> "func target() { if (" to ") {}\n}"
            }
            val text = prefix + expressionText + suffix
            val expected = expectedRelative.shift(prefix.length)
            assertEachTree(slot.name, text) { root, route ->
                val expression = root.requireNode(expected.type, expected.start, expected.end, route)
                assertValueEquals(expected, expression, route)
                assertValueEquals(slot.parentType, root.parentOf(expression)?.type, route)
                if (slot == Slot.RETURN_VALUE) {
                    val returnStart = text.indexOf("return ")
                    assertValueEquals(
                        node(CjNodeTypes.RETURN, returnStart, expected.end, node(CjTokens.RETURN_KEYWORD, returnStart, returnStart + 6), expected),
                        root.parentOf(expression), route,
                    )
                }
            }
        }
    }

    /** 两路分别对显式期望断言；诊断空集也分别检查，不把 PSI/LT 相互一致当作正确性依据。 */
    private fun assertEachTree(name: String, text: String, annotationOnly: Boolean = false, assertion: (Node, String) -> Unit) {
        val file = if (annotationOnly) CjPsiFactory(project, markGenerated = false).createAnnotationOnlyFile(text)
        else createPsiFile(name, text) as CjFile
        val psiTree = snapshot(file.node)
        assertValueEquals(emptyList<ParsingDiagnostic>(), file.collectParsingDiagnostics(), "PSI $name\n$text")
        assertion(psiTree, "PSI $name\n$text")

        val diagnostics = mutableListOf<ParsingDiagnostic>()
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        builder.setDebugMode(true)
        val tree = if (annotationOnly) CangJieLightParser.parseAnnotationOnly(builder, errorListener = { diagnostics += it }).tree
        else CangJieLightParser.parse(builder, errorListener = { diagnostics += it }).tree
        val lightTree = snapshot(tree.root, tree)
        assertValueEquals(emptyList<ParsingDiagnostic>(), diagnostics, "LT $name\n$text")
        assertion(lightTree, "LT $name\n$text")
    }

    private fun snapshot(ast: ASTNode): Node {
        val children = mutableListOf<Node>()
        var child = ast.firstChildNode
        while (child != null) {
            if (!isTrivia(child.elementType)) children += snapshot(child)
            child = child.treeNext
        }
        return Node(ast.elementType, ast.startOffset, ast.startOffset + ast.textLength, children)
    }

    /** 只保存值快照；必须在 finally 中归还 LT 子节点，不能把可回收的 token identity 留在期望中。 */
    private fun snapshot(ast: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Node {
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(ast, ref)
        val children = ref.get()
        val result = mutableListOf<Node>()
        if (children != null) {
            try {
                for (index in 0 until count) {
                    val child = requireNotNull(children[index])
                    if (!isTrivia(child.tokenType)) result += snapshot(child, tree)
                }
            } finally {
                tree.disposeChildren(children, count)
            }
        }
        return Node(ast.tokenType, ast.startOffset, ast.endOffset, result)
    }

    private fun isTrivia(type: IElementType): Boolean = type == TokenType.WHITE_SPACE || CjTokens.COMMENTS.contains(type)

    private data class Node(val type: IElementType, val start: Int, val end: Int, val children: List<Node>) {
        fun shift(offset: Int): Node = Node(type, start + offset, end + offset, children.map { it.shift(offset) })

        fun descendants(): List<Node> = listOf(this) + children.flatMap { it.descendants() }

        fun requireNode(type: IElementType, start: Int, end: Int, message: String): Node {
            val matches = descendants().filter { it.type == type && it.start == start && it.end == end }
            assertValueEquals(1, matches.size, "$message\nExpected $type [$start, $end), tree=$this")
            return matches.single()
        }

        fun parentOf(child: Node): Node? = descendants().singleOrNull { parent -> parent.children.any { it === child } }
    }

    private enum class Slot(val parentType: IElementType) {
        INITIALIZER(CjNodeTypes.VARIABLE),
        DEFAULT_VALUE(CjNodeTypes.VALUE_PARAMETER),
        ARGUMENT(CjNodeTypes.VALUE_ARGUMENT),
        RETURN_VALUE(CjNodeTypes.RETURN),
        CONDITION(CjNodeTypes.CONDITION),
    }

    private fun expressionCases(): List<Pair<String, Node>> = listOf(
        "@M(1)" to macro(0),
        "@M(1)+2" to node(CjNodeTypes.BINARY_EXPRESSION, 0, 7, macro(0), operation(CjTokens.PLUS, 5), integer(6, "2")),
        "1+@M(2)" to node(CjNodeTypes.BINARY_EXPRESSION, 0, 7, integer(0), operation(CjTokens.PLUS, 1), macro(2, input = "2")),
        "-@M(1)" to node(CjNodeTypes.PREFIX_EXPRESSION, 0, 6, operation(CjTokens.MINUS, 0), macro(1)),
        "@M(1).foo" to node(
            CjNodeTypes.DOT_QUALIFIED_EXPRESSION, 0, 9, macro(0), node(CjTokens.DOT, 5, 6),
            node(CjNodeTypes.REFERENCE_EXPRESSION, 6, 9, node(CjTokens.IDENTIFIER, 6, 9)),
        ),
        "@M(1)[0]" to node(
            CjNodeTypes.ARRAY_ACCESS_EXPRESSION, 0, 8, macro(0),
            node(CjNodeTypes.INDICES, 5, 8, node(CjTokens.LBRACKET, 5, 6), integer(6, "0"), node(CjTokens.RBRACKET, 7, 8)),
        ),
        "@M(1)(2)" to node(
            CjNodeTypes.CALL_EXPRESSION, 0, 8, macro(0),
            node(
                CjNodeTypes.VALUE_ARGUMENT_LIST, 5, 8, node(CjTokens.LPAR, 5, 6),
                node(CjNodeTypes.VALUE_ARGUMENT, 6, 7, integer(6, "2")), node(CjTokens.RPAR, 7, 8),
            ),
        ),
    )

    private fun macro(start: Int, name: String = "M", input: String = "1"): Node {
        val inputStart = start + 1 + name.length
        val tokenStart = inputStart + 1
        val tokenEnd = tokenStart + input.length
        val tokens = if (input.isEmpty()) node(CjNodeTypes.QUOTE_TOKENS, tokenStart, tokenEnd)
        else node(CjNodeTypes.QUOTE_TOKENS, tokenStart, tokenEnd, node(CjTokens.INTEGER_LITERAL, tokenStart, tokenEnd))
        return node(
            CjNodeTypes.MACRO_EXPRESSION, start, tokenEnd + 1,
            node(CjTokens.AT, start, start + 1),
            node(CjNodeTypes.REFERENCE_EXPRESSION, start + 1, inputStart, node(CjTokens.IDENTIFIER, start + 1, inputStart)),
            node(CjNodeTypes.MACRO_INPUT, inputStart, tokenEnd + 1, node(CjTokens.LPAR, inputStart, tokenStart), tokens, node(CjTokens.RPAR, tokenEnd, tokenEnd + 1)),
        )
    }

    private fun integer(start: Int, text: String = "1"): Node =
        node(CjNodeTypes.INTEGER_CONSTANT, start, start + text.length, node(CjTokens.INTEGER_LITERAL, start, start + text.length))

    private fun operation(type: IElementType, start: Int): Node =
        node(CjNodeTypes.OPERATION_REFERENCE, start, start + 1, node(type, start, start + 1))

    private fun node(type: IElementType, start: Int, end: Int, vararg children: Node): Node = Node(type, start, end, children.toList())
}
