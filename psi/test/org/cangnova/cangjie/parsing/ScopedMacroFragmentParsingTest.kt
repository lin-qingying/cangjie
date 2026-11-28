package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.lang.LighterASTTokenNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjIfAvailableExpression
import org.cangnova.cangjie.psi.CjMacroCodeFragment
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.awt.EventQueue
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertFalse as falseValue
import kotlin.test.assertNotNull as present
import kotlin.test.assertNotSame as distinct
import kotlin.test.assertNull as absent
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as truth

/** 以真实 PSI/LT 直接 roots 验证官方 ParseNodes；替换槽位的形状检查不属于 parser。 */
class ScopedMacroFragmentParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun scopeKindsReturnMixedDirectDeclarationAndExpressionRoots() {
        val text = "let value = 1\nvalue + 2"
        for (scope in listOf(CjScopeKind.FUNC_BODY, CjScopeKind.MAIN_BODY, CjScopeKind.MACRO_BODY, CjScopeKind.UNKNOWN_SCOPE)) {
            assertParse(text, nodes(scope), listOf(
                root(CjNodeTypes.VARIABLE, "let value = 1"),
                root(CjNodeTypes.BINARY_EXPRESSION, "value + 2"),
            ))
        }
    }

    /** TOPLEVEL/CLASS_BODY 的表达式结果是否合法，留给真实 Decl vector 判定。 */
    @Test
    fun nominalAndTopLevelScopesDoNotForceEveryRootToBeADeclaration() {
        val text = "func generated() {}\n1"
        for (scope in listOf(CjScopeKind.TOPLEVEL, CjScopeKind.CLASS_BODY)) {
            assertParse(text, nodes(scope, outerDeclIdent = "Owner"), listOf(
                root(CjNodeTypes.FUNC, "func generated() {}"),
                root(CjNodeTypes.INTEGER_CONSTANT, "1"),
            ))
        }
    }

    @Test
    fun nestedDeclarationsNeverBecomeAdditionalFragmentRoots() {
        val first = "func first() { func nested() {} }"
        val second = "func second() {}"
        assertParse("$first\n$second", nodes(), listOf(root(CjNodeTypes.FUNC, first), root(CjNodeTypes.FUNC, second)))
    }

    @Test
    fun thisReturnUsesTheMacroEntryDeclarationScope() {
        val text = "func generated(): This { return this }"
        assertParse(text, nodes(CjScopeKind.CLASS_BODY, outerDeclIdent = "Owner"), listOf(root(CjNodeTypes.FUNC, text)))
        for (scope in listOf(CjScopeKind.TOPLEVEL, CjScopeKind.INTERFACE_BODY, CjScopeKind.EXTEND_BODY, CjScopeKind.FUNC_BODY)) {
            assertParse(text, nodes(scope, outerDeclIdent = "Owner"), listOf(root(CjNodeTypes.FUNC, text)),
                expectedDiagnostics = listOf(at(text, "This", CangJieParsingBundle.message("parsing.error.this.type.not.allowed"))))
        }
    }

    @Test
    fun unknownScopeRetainsItsNativeDeclarationPermission() {
        assertParse("class Nested {}", nodes(CjScopeKind.UNKNOWN_SCOPE), listOf(root(CjNodeTypes.CLASS, "class Nested {}")))
    }

    @Test
    fun primaryConstructorRootUsesTheExplicitOriginalNominalName() {
        val text = "Owner(value: Int64) {}"
        for (scope in listOf(CjScopeKind.CLASS_BODY, CjScopeKind.STRUCT_BODY)) {
            assertParse(text, nodes(scope, outerDeclIdent = "Owner"), listOf(root(CjNodeTypes.PRIMARY_CONSTRUCTOR, text)))
        }
    }

    /** fresh ParseNodes 只报告错名；直接成员接收者将其淘汰为 InvalidDecl 的步骤不在这里执行。 */
    @Test
    fun freshMacroParserKeepsTheMisspelledPrimaryConstructorRoot() {
        val text = "OwneX() {}"
        assertParse(
            text,
            nodes(CjScopeKind.CLASS_BODY, outerDeclIdent = "Owner"),
            listOf(root(CjNodeTypes.PRIMARY_CONSTRUCTOR, text)),
            expectedDiagnostics = listOf(at(text, "OwneX", CangJieParsingBundle.message(
                "parsing.error.primary.constructor.name", "OwneX", "Owner",
            ))),
            hasInvalidRoot = false,
        )
    }

    @Test
    fun everyExpressionKindKeepsAnExpressionRoot() {
        for (kind in CjExprKind.entries) {
            assertParse("value", expressions(kind), listOf(root(CjNodeTypes.REFERENCE_EXPRESSION, "value")))
        }
    }

    @Test
    fun expressionEntryReturnsAllNewlineSeparatedRoots() {
        assertParse("1\n2", expressions(), listOf(
            root(CjNodeTypes.INTEGER_CONSTANT, "1"), root(CjNodeTypes.INTEGER_CONSTANT, "2"),
        ))
    }

    @Test
    fun actualCommaParentsPermitMultipleExpressionRoots() {
        for (parent in COMMA_PARENTS) {
            assertParse("1, 2", expressions(parent = parent), listOf(
                root(CjNodeTypes.INTEGER_CONSTANT, "1"), root(CjNodeTypes.INTEGER_CONSTANT, "2"),
            ))
        }
    }

    @Test
    fun missingCommaRetainsTheParsedPrefixAndItsDiagnostic() {
        val text = "1 2"
        for (parent in COMMA_PARENTS) {
            assertParse(text, expressions(parent = parent), listOf(root(CjNodeTypes.INTEGER_CONSTANT, "1")),
                expectedDiagnostics = listOf(at(text, "2", CangJieParsingBundle.message("parsing.error.expecting.symbol", ","))))
        }
    }

    @Test
    fun expressionKindDoesNotInventACommaParent() {
        val text = "1, 2"
        for (kind in listOf(CjExprKind.ALL, CjExprKind.EXPR_IN_ARRAY, CjExprKind.EXPR_IN_TUPLE, CjExprKind.EXPR_IN_CALLSUFFIX)) {
            assertParse(text, expressions(kind), listOf(root(CjNodeTypes.INTEGER_CONSTANT, "1")),
                expectedDiagnostics = listOf(at(text, ",", CangJieParsingBundle.message("parsing.error.macro.node.separator"))))
        }
    }

    @Test
    fun missingStatementSeparatorRetainsTheParsedPrefix() {
        val text = "1 2"
        for (entry in listOf(nodes(), expressions())) {
            assertParse(text, entry, listOf(root(CjNodeTypes.INTEGER_CONSTANT, "1")),
                expectedDiagnostics = listOf(at(text, "2", CangJieParsingBundle.message("parsing.error.macro.node.separator"))))
        }
    }

    @Test
    fun scopeEntrySkipsLeadingSemicolonsAndKeepsEveryRoot() {
        assertParse(";\n1;\n2;", nodes(), listOf(
            root(CjNodeTypes.INTEGER_CONSTANT, "1"), root(CjNodeTypes.INTEGER_CONSTANT, "2"),
        ))
    }

    @Test
    fun trailingCommaAndSeparatorsDoNotCreateAnEmptyRoot() {
        for (parent in COMMA_PARENTS) {
            assertParse("1,\n;", expressions(parent = parent), listOf(root(CjNodeTypes.INTEGER_CONSTANT, "1")))
        }
    }

    /** 空 roots 是有效的 parser 结果；这里不得提前引入表达式或参数槽位的空输出诊断。 */
    @Test
    fun emptyInputKeepsAnEmptyTypedSliceAcrossAllEntries() {
        val entries = buildList<CjMacroFragmentContext> {
            CjScopeKind.entries.forEach { add(nodes(it, outerDeclIdent = "Owner")) }
            CjExprKind.entries.forEach { add(expressions(it)) }
            PARAMETER_GRAMMARS.forEach { add(parameters(it)) }
        }
        for (entry in entries) {
            for (text in listOf("", "\n", "// only trivia\n\n")) assertParse(text, entry, emptyList())
        }
    }

    @Test
    fun onlySeparatorsAreEmptyForScopeAndParameterEntries() {
        for (entry in listOf(nodes(), nodes(CjScopeKind.UNKNOWN_SCOPE), parameters())) {
            assertParse(";\n;;", entry, emptyList())
        }
    }

    @Test
    fun scopedTopLevelDeclarationsUseTheSuppliedCompilationCjmpContext() {
        val text = "common func generated(): Unit {}"
        val expected = CangJieParsingBundle.message("parsing.error.cjmp.common.in.non.common.file")
        val seed = createMacroFragmentParserSeed(
            SOURCE_FACTS,
            CjmpParsingContext.Compilation(compileCommon = false, compileSpecific = false, enabled = true),
            nodes(CjScopeKind.TOPLEVEL),
        )
        val fragment = newFragment(text, seed)
        equal(listOf(root(CjNodeTypes.FUNC, text)), fragment.getContentElement().roots.map { root(it.node.elementType, it.text) })
        truth(fragment.collectParsingDiagnostics().any { it.message == expected })

        val lightDiagnostics = mutableListOf<ParsingDiagnostic>()
        val light = parseLight(text, assertPublishedParserSeed(seed, fragment), lightDiagnostics)
        equal(listOf(root(CjNodeTypes.FUNC, text)), light.roots.map { root(it.tokenType, lightText(it, light.tree)) })
        truth(lightDiagnostics.any { it.message == expected })
    }

    @Test
    fun typedParameterSlicesKeepEveryDirectParameterInTheirNativeScopes() {
        val first = "first: Int64"
        val second = "second!: Int64 = 1"
        for (grammar in TYPED_PARAMETER_GRAMMARS) {
            assertParse("$first, $second", parameters(grammar), listOf(
                root(CjNodeTypes.VALUE_PARAMETER, first), root(CjNodeTypes.VALUE_PARAMETER, second),
            ))
        }
    }

    @Test
    fun primaryConstructorParameterSlicesKeepMemberParameterSyntax() {
        for (grammar in listOf(CjParameterGrammar.CLASS_PRIMARY, CjParameterGrammar.STRUCT_PRIMARY)) {
            for (keyword in listOf("let", "var")) {
                val member = "$keyword field: Int64"
                assertParse("value: Int64, $member", parameters(grammar), listOf(
                    root(CjNodeTypes.VALUE_PARAMETER, "value: Int64"), root(CjNodeTypes.VALUE_PARAMETER, member),
                ))
            }
        }
    }

    /** ParseNodes 不调用完整 setter 参数列表入口，不能在切片中额外执行它的参数数量检查。 */
    @Test
    fun accessorSlicesUseIdentifierOnlyParametersWithoutAListWrapper() {
        for (grammar in listOf(CjParameterGrammar.ACCESSOR_NAME_ONLY)) {
            assertParse("value", parameters(grammar), listOf(root(CjNodeTypes.VALUE_PARAMETER, "value")))
            assertParse("first, second", parameters(grammar), listOf(
                root(CjNodeTypes.VALUE_PARAMETER, "first"), root(CjNodeTypes.VALUE_PARAMETER, "second"),
            ))
        }
    }

    @Test
    fun accessorTypeSuffixRemainsUnconsumedAfterItsIdentifierRoot() {
        val text = "value: Int64"
        for (grammar in listOf(CjParameterGrammar.ACCESSOR_NAME_ONLY)) {
            assertParse(text, parameters(grammar), listOf(root(CjNodeTypes.VALUE_PARAMETER, "value")),
                expectedDiagnostics = listOf(at(text, ":", CangJieParsingBundle.message("parsing.error.macro.node.separator"))))
        }
    }

    @Test
    fun parameterSliceSeparatorsAreNotTheFullParameterListGrammar() {
        val first = "first: Int64"
        val second = "second: Int64"
        assertParse("$first\n$second", parameters(), listOf(
            root(CjNodeTypes.VALUE_PARAMETER, first), root(CjNodeTypes.VALUE_PARAMETER, second),
        ))
        assertParse(", $first", parameters(), listOf(root(CjNodeTypes.VALUE_PARAMETER, first)))
        val missingSeparator = "$first $second"
        assertParse(missingSeparator, parameters(), listOf(root(CjNodeTypes.VALUE_PARAMETER, first)),
            expectedDiagnostics = listOf(at(missingSeparator, "second", CangJieParsingBundle.message("parsing.error.macro.node.separator"))))
    }

    @Test
    fun ordinaryParameterScopeRejectsMemberKeywordWithoutDiscardingItsRoot() {
        val text = "let field: Int64"
        assertParse(text, parameters(), listOf(root(CjNodeTypes.VALUE_PARAMETER, text)),
            expectedDiagnostics = listOf(at(text, "let", CangJieParsingBundle.message(
                "parsing.error.parameter.expected.name.or.rparen", "let",
            ))))
    }

    /** native 在 SetDeclBeginPos 之前检查顺序；普通参数的诊断 begin 此时位于冒号。 */
    @Test
    fun unnamedAfterNamedParameterReportsItsNativeRangeAndKeepsBothRoots() {
        val first = "named!: Int64 = 1"
        val second = "regular: Int64"
        val text = "$first, $second"
        assertParse(text, parameters(), listOf(
            root(CjNodeTypes.VALUE_PARAMETER, first), root(CjNodeTypes.VALUE_PARAMETER, second),
        ), expectedDiagnostics = listOf(at(text, ": Int64", "unnamed parameters must come before named parameters", first.length)))
    }

    @Test
    fun regularAfterMemberParameterReportsItsNativeRangeAndKeepsBothRoots() {
        val first = "let field: Int64"
        val second = "regular: Int64"
        val text = "$first, $second"
        assertParse(text, parameters(CjParameterGrammar.CLASS_PRIMARY), listOf(
            root(CjNodeTypes.VALUE_PARAMETER, first), root(CjNodeTypes.VALUE_PARAMETER, second),
        ), expectedDiagnostics = listOf(at(text, ": Int64", "regular parameters must come before member variable parameters", first.length)))
    }

    @Test
    fun ifAvailableUsesEveryApplicableHighLevelExpressionEntry() {
        for (kind in CjExprKind.entries.filter { it != CjExprKind.INDEX_EXPR }) {
            val parsed = assertParse(IF_AVAILABLE, expressions(kind), listOf(root(CjNodeTypes.IF_AVAILABLE_EXPRESSION, IF_AVAILABLE)))
            assertIfAvailableBranches(parsed)
        }
    }

    /** 相同 IfAvailable root 在 Decl vector 中会失败，但该错误不得由 scoped parser 提前产生。 */
    @Test
    fun ifAvailableRemainsAnExpressionInScopeKindEntries() {
        for (scope in listOf(CjScopeKind.UNKNOWN_SCOPE, CjScopeKind.FUNC_BODY, CjScopeKind.TOPLEVEL, CjScopeKind.CLASS_BODY)) {
            val parsed = assertParse(IF_AVAILABLE, nodes(scope, outerDeclIdent = "Owner"),
                listOf(root(CjNodeTypes.IF_AVAILABLE_EXPRESSION, IF_AVAILABLE)))
            assertIfAvailableBranches(parsed)
        }
    }

    /** INDEX 的残留宏由 MacroExpansion 检查；不能在这里改用 ALL，也不能递归重试。 */
    @Test
    fun indexEntryReturnsTheResidualIfAvailableMacroRoot() {
        assertParse(IF_AVAILABLE, expressions(CjExprKind.INDEX_EXPR), listOf(root(CjNodeTypes.MACRO_EXPRESSION, IF_AVAILABLE)))
    }

    @Test
    fun qualifiedIfAvailableIsNotTheDedicatedHighLevelExpression() {
        val text = "@pkg.IfAvailable(level: 19, { => }, { => })"
        assertParse(text, expressions(), listOf(root(CjNodeTypes.MACRO_EXPRESSION, text)))
    }

    @Test
    fun missingIfAvailableArgumentNameIsAParserDiagnosticAtTheFirstComma() {
        val text = "@IfAvailable(19, { => }, { => })"
        assertParse(text, expressions(), listOf(root(CjNodeTypes.IF_AVAILABLE_EXPRESSION, text)),
            expectedDiagnostics = listOf(at(text, ",", IF_AVAILABLE_ARG_NAME)))
    }

    @Test
    fun nonLiteralIfAvailableBranchesKeepBothExpressionsAndParserDiagnostics() {
        val text = "@IfAvailable(level: 19, first, second)"
        val parsed = assertParse(text, expressions(), listOf(root(CjNodeTypes.IF_AVAILABLE_EXPRESSION, text)),
            expectedDiagnostics = listOf(at(text, "first", IF_AVAILABLE_LAMBDA), at(text, "second", IF_AVAILABLE_LAMBDA)))
        val expression = parsed.psi.getContentElement().roots.single() as CjIfAvailableExpression
        equal(listOf("first", "second"), expression.branchExpressions.map { it.text })
        absent(expression.thenBranch)
        absent(expression.elseBranch)
    }

    @Test
    fun parenthesizedLambdaDoesNotSatisfyTheDirectLambdaGrammar() {
        val branch = "({ => })"
        val text = "@IfAvailable(level: 19, $branch, { => })"
        assertParse(text, expressions(), listOf(root(CjNodeTypes.IF_AVAILABLE_EXPRESSION, text)),
            expectedDiagnostics = listOf(at(text, branch, IF_AVAILABLE_LAMBDA)))
    }

    @Test
    fun functionArgumentEntryRecordsInoutForTheActualRootIndices() {
        for ((text, indices) in listOf("inout first, second" to setOf(0), "first, inout second" to setOf(1))) {
            assertParse(text, expressions(CjExprKind.EXPR_IN_CALLSUFFIX, CjMacroFragmentParent.FUNCTION_ARGUMENT), listOf(
                root(CjNodeTypes.REFERENCE_EXPRESSION, "first"), root(CjNodeTypes.REFERENCE_EXPRESSION, "second"),
            ), expectedInoutRoots = indices)
        }
    }

    @Test
    fun carriedPrefixBelongsOnlyToTheFirstDeclarationRoot() {
        val first = "func first() {}"
        val second = "func second() {}"
        val prefix = capturePrefix("public func origin(): Unit {}")
        assertParse("$first\n$second", nodes(CjScopeKind.TOPLEVEL, prefix = prefix), listOf(
            root(CjNodeTypes.FUNC, first), root(CjNodeTypes.FUNC, second),
        ), expectedPrefixRoot = 0)
    }

    @Test
    fun firstExpressionDoesNotPassItsPrefixToALaterDeclaration() {
        val text = "1\nfunc later() {}"
        val prefix = capturePrefix("public func origin(): Unit {}")
        assertParse(text, nodes(CjScopeKind.TOPLEVEL, prefix = prefix), listOf(
            root(CjNodeTypes.INTEGER_CONSTANT, "1"), root(CjNodeTypes.FUNC, "func later() {}"),
        ), expectedPrefixRoot = null)
    }

    @Test
    fun declarationPrefixCopiesItsMutableInputBeforeCreatingTheSeed() {
        val original = capturePrefix("@C public func origin(): Unit {}")
        val modifiers = original.sourceModifiers.toMutableList()
        val annotations = original.annotations.toMutableList()
        val prefix = CjMacroDeclarationPrefix(modifiers, annotations)
        val entry = nodes(CjScopeKind.TOPLEVEL, prefix = prefix)
        val captured = seed(entry)
        modifiers.clear()
        modifiers += capturePrefix("private func origin(): Unit {}").sourceModifiers
        annotations.clear()
        equal(setOf(CjModifierKind.PUBLIC), prefix.modifiers)
        equal(setOf(BuiltInAnnotationKind.C), prefix.annotationKinds)
        equal(original.sourceModifiers, prefix.sourceModifiers)
        equal(original.annotations, prefix.annotations)
        identical(entry, captured.macroFragment)
        assertParse("func first() {}", entry, listOf(root(CjNodeTypes.FUNC, "func first() {}")), expectedPrefixRoot = 0)
    }

    private fun assertIfAvailableBranches(parsed: Parsed) {
        val expression = parsed.psi.getContentElement().roots.single() as CjIfAvailableExpression
        equal("level", expression.conditionName)
        equal("19", present(expression.condition).text)
        equal("{ => println(1) }", present(expression.thenBranch).text)
        equal("{ => println(2) }", present(expression.elseBranch).text)
        equal(2, expression.branchExpressions.size)
        equal(expectedSpans(IF_AVAILABLE, listOf(
            root(CjNodeTypes.LAMBDA_EXPRESSION, "{ => println(1) }"),
            root(CjNodeTypes.LAMBDA_EXPRESSION, "{ => println(2) }"),
        )), lightDescendants(parsed.light.roots.single(), parsed.light.tree, CjNodeTypes.LAMBDA_EXPRESSION))
    }

    @Test
    fun coldFragmentsAndCopiesKeepTheExplicitSeedAndDirectRoots() {
        val scenarios: List<Triple<CjMacroFragmentContext, String, List<Root>>> = listOf(
            Triple(nodes(CjScopeKind.UNKNOWN_SCOPE), "let value = 1\nvalue", listOf(
                root(CjNodeTypes.VARIABLE, "let value = 1"), root(CjNodeTypes.REFERENCE_EXPRESSION, "value"),
            )),
            Triple(parameters(CjParameterGrammar.CLASS_PRIMARY), "value: Int64, let field: Int64", listOf(
                root(CjNodeTypes.VALUE_PARAMETER, "value: Int64"), root(CjNodeTypes.VALUE_PARAMETER, "let field: Int64"),
            )),
        )
        for ((entry, text, roots) in scenarios) {
            val captured = seed(entry)
            val fragment = newFragment(text, captured)
            falseValue(fragment.isContentsLoaded)
            identical(captured, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            falseValue(fragment.isContentsLoaded, "读取不可变入口不能预热片段正文")

            // SDK clone 可以展开原内容；复制前后都必须使用已有 seed，不能重新推导 scope。
            val copy = fragment.copy() as CjMacroCodeFragment
            val originalContents = fragment.getContentElement()
            val copiedContents = copy.getContentElement()
            assertPublishedParserSeed(captured, fragment)
            assertPublishedParserSeed(captured, copy)
            distinct(originalContents, copiedContents)
            equal(expectedSpans(text, roots), originalContents.roots.map { span(it.node) })
            equal(expectedSpans(text, roots), copiedContents.roots.map { span(it.node) })
            originalContents.roots.zip(copiedContents.roots).forEach { (original, copied) -> distinct(original, copied) }
            equal(SOURCE_FACTS, originalContents.fileFacts)
            equal(SOURCE_FACTS, copiedContents.fileFacts)
            equal(originalContents.parsingInfo, copiedContents.parsingInfo)
            equal(emptyList<ParsingDiagnostic>(), fragment.collectParsingDiagnostics())
            equal(emptyList<ParsingDiagnostic>(), copy.collectParsingDiagnostics())
            identical(entry, captured.macroFragment)
            equal("", captured.languageModuleName)
        }
    }

    @Test
    fun customAnnotationFragmentsKeepTheAnnotationAsTheirOnlyDirectRoot() {
        val text = "@!Hidden[feature: true]"
        val entry = annotations("Owner")
        val parsed = assertParse(text, entry, listOf(root(CjNodeTypes.ANNOTATION, text)))
        val annotation = parsed.psi.getContentElement().roots.single()
        equal(CjNodeTypes.ANNOTATION, annotation.node.elementType)
        equal(
            MacroParsingFacts(
                MacroParsingScope.Declaration(CjScopeKind.UNKNOWN_SCOPE),
                CjMacroFragmentParent.OTHER,
                "Owner",
            ),
            annotation.node.findChildByType(CjTokens.ATEXCL)?.getCopyableUserData(MACRO_PARSING_FACTS_KEY),
        )
        equal(1, parsed.light.macroFacts.size)
        equal(parsed.psi.getContentElement().roots.single().text, text)
    }

    @Test
    fun realDiffPublishesNewInoutFactsOnlyWhenItsSyntaxIsCommitted() {
        val entry = expressions(CjExprKind.EXPR_IN_CALLSUFFIX, CjMacroFragmentParent.FUNCTION_ARGUMENT)
        val captured = seed(entry)
        val oldText = "inout first"
        val newText = "second"
        val fragment = newFragment(oldText, captured)
        val oldContents = fragment.getContentElement()
        val oldInfo = oldContents.parsingInfo
        val oldRoot = fragment.node
        val published = assertPublishedParserSeed(captured, fragment)
        equal(setOf(0), oldInfo.inoutRootIndices)
        val result = BlockSupportImpl.reparse(
            fragment, oldRoot, TextRange(0, oldText.length), newText, EmptyProgressIndicator(), oldText,
        )
        identical(oldRoot, result.oldRoot)
        distinct(oldRoot, result.newRoot)
        identical(captured, present(result.newRoot.firstChildNode).getCopyableUserData(PARSER_SEED_KEY))
        identical(oldRoot, fragment.node)
        identical(oldContents, fragment.getContentElement())
        identical(oldInfo, fragment.getContentElement().parsingInfo)
        equal(oldText, fragment.text)
        identical(published, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))

        applyOnEdt { result.log.doActualPsiChange(fragment) }
        val committed = fragment.getContentElement()
        equal(newText, fragment.text)
        equal(expectedSpans(newText, listOf(root(CjNodeTypes.REFERENCE_EXPRESSION, "second"))), committed.roots.map { span(it.node) })
        equal(emptySet<Int>(), committed.parsingInfo.inoutRootIndices)
        falseValue(committed.parsingInfo.hasInvalidRoot)
        equal(SOURCE_FACTS, committed.fileFacts)
        equal(emptyList<ParsingDiagnostic>(), fragment.collectParsingDiagnostics())
        val committedSeed = assertPublishedParserSeed(captured, fragment)
        val diagnostics = mutableListOf<ParsingDiagnostic>()
        val light = parseLight(newText, committedSeed, diagnostics)
        equal(expectedSpans(newText, listOf(root(CjNodeTypes.REFERENCE_EXPRESSION, "second"))), light.roots.map { span(it, light.tree) })
        equal(emptyList<ParsingDiagnostic>(), diagnostics)
        equal(emptySet<Int>(), light.parsingInfo.inoutRootIndices)
    }

    @Test
    fun realDiffKeepsThePrimaryParameterSliceEntryWithoutCreatingAnOwner() {
        val entry = parameters(CjParameterGrammar.CLASS_PRIMARY)
        val captured = seed(entry)
        val oldText = "value: Int64"
        val newText = "value: Int64, let field: Int64"
        val fragment = newFragment(oldText, captured)
        val oldContents = fragment.getContentElement()
        val oldRoot = fragment.node
        val published = assertPublishedParserSeed(captured, fragment)
        equal(listOf(CjNodeTypes.VALUE_PARAMETER), oldContents.roots.map { it.node.elementType })
        val result = BlockSupportImpl.reparse(
            fragment, oldRoot, TextRange(0, oldText.length), newText, EmptyProgressIndicator(), oldText,
        )
        identical(oldRoot, fragment.node)
        identical(oldContents, fragment.getContentElement())
        equal(oldText, fragment.text)
        identical(published, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        identical(captured, present(result.newRoot.firstChildNode).getCopyableUserData(PARSER_SEED_KEY))

        applyOnEdt { result.log.doActualPsiChange(fragment) }
        val expected = expectedSpans(newText, listOf(
            root(CjNodeTypes.VALUE_PARAMETER, "value: Int64"), root(CjNodeTypes.VALUE_PARAMETER, "let field: Int64"),
        ))
        val committed = fragment.getContentElement()
        equal(newText, fragment.text)
        equal(expected, committed.roots.map { span(it.node) })
        truth(committed.roots.all { it.parent === committed })
        val committedSeed = assertPublishedParserSeed(captured, fragment)
        identical(entry, captured.macroFragment)
        equal("Owner", entry.outerDeclIdent)
        equal(emptyList<ParsingDiagnostic>(), fragment.collectParsingDiagnostics())
        equal(SOURCE_FACTS, committed.fileFacts)
        falseValue(committed.parsingInfo.hasInvalidRoot)
        val diagnostics = mutableListOf<ParsingDiagnostic>()
        val light = parseLight(newText, committedSeed, diagnostics)
        equal(expected, light.roots.map { span(it, light.tree) })
        equal(emptyList<ParsingDiagnostic>(), diagnostics)
    }

    @Test
    fun conditionAndUnknownExpressionEntriesPreserveLetPatternGrammar() {
        val text = "let value <- source"
        for (kind in listOf(
            CjExprKind.IF_COND_EXPR, CjExprKind.WHILE_COND_EXPR,
            CjExprKind.EXPR_IN_IF_COND_TUPLE, CjExprKind.EXPR_IN_WHILE_COND_TUPLE, CjExprKind.UNKNOWN_EXPR,
        )) {
            assertParse(text, expressions(kind), listOf(root(CjNodeTypes.LET_EXPRESSION, text)))
        }
    }

    @Test
    fun annotationOnlyPrefixStillBelongsToIndexZeroRatherThanTheFirstLaterDeclaration() {
        val prefix = capturePrefix("@C func origin(): Unit {}")
        assertParse("func first() {}\nfunc second() {}", nodes(CjScopeKind.TOPLEVEL, prefix = prefix), listOf(
            root(CjNodeTypes.FUNC, "func first() {}"), root(CjNodeTypes.FUNC, "func second() {}"),
        ), expectedPrefixRoot = 0)
        assertParse("1\nfunc later() {}", nodes(CjScopeKind.TOPLEVEL, prefix = prefix), listOf(
            root(CjNodeTypes.INTEGER_CONSTANT, "1"), root(CjNodeTypes.FUNC, "func later() {}"),
        ), expectedPrefixRoot = null)
    }

    /** 前缀身份及位置由独立原始源码的完整解析捕获，不构造无来源的 modifier 或 annotation。 */
    private fun capturePrefix(text: String): CjMacroDeclarationPrefix {
        val sourceFile = CjInMemoryTextSourceFile("scoped-prefix-origin.cj", "test-origins/scoped-prefix-origin.cj", text)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val parsed = CangJieLightParser.parseBuffered(builder, sourceFile = sourceFile)
        equal(emptyList<ParsingDiagnostic>(), parsed.diagnostics)
        return parsed.file.declarationPrefixes.getValue(text.indexOf("func origin"))
    }

    private fun newFragment(text: String, seed: CjParserSeed): CjMacroCodeFragment =
        CjMacroCodeFragment(project, "fragment.cj", text, context = null, seed = seed)

    private fun seed(entry: CjMacroFragmentContext): CjParserSeed = createMacroFragmentParserSeed(
        SOURCE_FACTS, CjmpParsingContext.SyntaxOnly, entry,
    )

    private fun nodes(
        scope: CjScopeKind = CjScopeKind.FUNC_BODY,
        outerDeclIdent: String? = null,
        parent: CjMacroFragmentParent = CjMacroFragmentParent.OTHER,
        prefix: CjMacroDeclarationPrefix = CjMacroDeclarationPrefix.EMPTY,
    ): CjMacroFragmentContext.Nodes = CjMacroFragmentContext.Nodes(
        MacroParsingScope.Declaration(scope), parent, outerDeclIdent, prefix,
    )

    private fun expressions(
        kind: CjExprKind = CjExprKind.ALL,
        parent: CjMacroFragmentParent = CjMacroFragmentParent.OTHER,
    ): CjMacroFragmentContext.Nodes = CjMacroFragmentContext.Nodes(
        MacroParsingScope.Expression(kind), parent, null, CjMacroDeclarationPrefix.EMPTY,
    )

    private fun parameters(grammar: CjParameterGrammar = CjParameterGrammar.REGULAR_TYPED): CjMacroFragmentContext.ParameterSlice =
        CjMacroFragmentContext.ParameterSlice(grammar, "Owner")

    private fun annotations(outerDeclIdent: String? = null): CjMacroFragmentContext.Annotation =
        CjMacroFragmentContext.Annotation(outerDeclIdent)

    /** 两条入口分别与同一份独立期望比较，不能把一方的结果当作另一方的语言语义。 */
    private fun assertParse(
        text: String,
        entry: CjMacroFragmentContext,
        expectedRoots: List<Root>,
        expectedDiagnostics: List<ParsingDiagnostic> = emptyList(),
        expectedInoutRoots: Set<Int> = emptySet(),
        expectedPrefixRoot: Int? = null,
        hasInvalidRoot: Boolean = false,
    ): Parsed {
        val seed = seed(entry)
        val fragment = newFragment(text, seed)
        val expectedSpans = expectedSpans(text, expectedRoots)
        val contents = fragment.getContentElement()
        equal(expectedSpans, contents.roots.map { span(it.node) }, "PSI roots: $entry\n$text")
        truth(contents.roots.all { it.parent === contents }, "所有语言 root 都必须是 contents 的直接子节点")
        equal(CjNodeTypes.MACRO_FRAGMENT_CONTENTS, contents.node.elementType)
        equal(text, fragment.text)
        equal(0, fragment.node.startOffset)
        equal(text.length, fragment.node.textRange.endOffset)
        val actualSeed = assertPublishedParserSeed(seed, fragment)
        val sourceContext = present(actualSeed.sourceContext)
        val psiPublication = fragment.collectParsingDiagnosticPublication()
        assertLegacyParsingDiagnostics(expectedDiagnostics, psiPublication.diagnostics, sourceContext,
            "PSI diagnostics: $entry\n$text")
        assertCanonicalParsingDiagnostics(psiPublication.diagnostics, sourceContext, "PSI diagnostics: $entry\n$text")
        equal(SOURCE_FACTS, contents.fileFacts)
        equal(expectedInoutRoots, contents.parsingInfo.inoutRootIndices)
        equal(hasInvalidRoot, contents.parsingInfo.hasInvalidRoot)
        identical(actualSeed, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))

        val diagnostics = mutableListOf<ParsingDiagnostic>()
        val light = parseLight(text, actualSeed, diagnostics)
        equal(expectedSpans, light.roots.map { span(it, light.tree) }, "LT roots: $entry\n$text")
        equal(0, light.tree.root.startOffset)
        equal(text.length, light.tree.root.endOffset)
        equal(text, lightText(light.tree.root, light.tree))
        identical(actualSeed.lexicalInput, light.lexicalInput)
        equal(sourceContext, light.sourceContext)
        absent(light.actualInputSeed.diagnosticScope)
        equal(emptyList<Long>(), light.diagnosticScope.path)
        truth(light.diagnosticScope.rootIdentity != present(fragment.parsingDiagnosticScopeOrNull()).rootIdentity)
        assertLegacyParsingDiagnostics(expectedDiagnostics, diagnostics,
            sourceContext, "LT diagnostics: $entry\n$text")
        assertCanonicalParsingDiagnostics(diagnostics, sourceContext, "LT diagnostics: $entry\n$text")
        assertPublishedParsingDiagnosticParity(psiPublication.diagnostics, diagnostics, sourceContext, text)
        equal(SOURCE_FACTS, light.fileFacts)
        equal(expectedInoutRoots, light.parsingInfo.inoutRootIndices)
        equal(hasInvalidRoot, light.parsingInfo.hasInvalidRoot)
        if (entry is CjMacroFragmentContext.Nodes && entry.declarationPrefix != CjMacroDeclarationPrefix.EMPTY) {
            equal(expectedPrefixRoot, contents.parsingInfo.inheritedPrefixRootIndex)
            equal(expectedPrefixRoot, light.parsingInfo.inheritedPrefixRootIndex)
        }
        equal(contents.parsingInfo, light.parsingInfo)
        return Parsed(fragment, light)
    }

    private fun parseLight(
        text: String,
        seed: CjParserSeed,
        diagnostics: MutableList<ParsingDiagnostic>,
    ): ParsedCangJieMacroFragment = CangJieLightParser.parseMacroCodeFragment(
        PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text),
        seed,
        errorListener = { diagnostic -> diagnostics += diagnostic },
    )

    private fun root(type: IElementType, text: String): Root = Root(type, text)

    /** 期望文本只用于确定已知直接 root 的范围，不从生产 PSI 中挑选任何后代。 */
    private fun expectedSpans(text: String, roots: List<Root>): List<Span> {
        var nextOffset = 0
        return roots.map { expected ->
            val start = text.indexOf(expected.text, nextOffset)
            truth(start >= 0, "期望 root 不在输入的相应顺序位置：$expected\n$text")
            val end = start + expected.text.length
            nextOffset = end
            Span(expected.type, start, end, expected.text)
        }
    }

    private fun span(node: ASTNode): Span = Span(
        node.elementType, node.startOffset, node.startOffset + node.textLength, node.text,
    )

    private fun span(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Span = Span(
        node.tokenType, node.startOffset, node.endOffset, lightText(node, tree),
    )

    /** 使用实际 token 文本；每次借用的 LT 子节点数组在本层归还。 */
    private fun lightText(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): String {
        if (node is LighterASTTokenNode) return node.text.toString()
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get()
        try {
            return buildString {
                for (index in 0 until count) append(lightText(present(children?.get(index)), tree))
            }
        } finally {
            if (children != null) tree.disposeChildren(children, count)
        }
    }

    /** 这里仅核对已经确定的 root 内部结构，不以搜索后代代替 root 选择。 */
    private fun lightDescendants(
        node: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        type: IElementType,
    ): List<Span> {
        val result = mutableListOf<Span>()
        if (node.tokenType == type) result += span(node, tree)
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get()
        try {
            for (index in 0 until count) result += lightDescendants(present(children?.get(index)), tree, type)
        } finally {
            if (children != null) tree.disposeChildren(children, count)
        }
        return result
    }

    private fun at(text: String, token: String, message: String, fromIndex: Int = 0): ParsingDiagnostic {
        val start = text.indexOf(token, fromIndex)
        truth(start >= 0, "期望诊断 token 不在输入内：$token\n$text")
        return ParsingDiagnostic(start, start + token.length, message)
    }

    private fun applyOnEdt(change: () -> Unit) {
        val command = {
            CommandProcessor.getInstance().executeCommand(
                project,
                { ApplicationManager.getApplication().runWriteAction { change() } },
                "提交 scoped 宏片段重解析",
                null,
            )
        }
        if (EventQueue.isDispatchThread()) command()
        else try {
            EventQueue.invokeAndWait(command)
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }

    private data class Root(val type: IElementType, val text: String)
    private data class Span(val type: IElementType, val start: Int, val end: Int, val text: String)
    private data class Parsed(val psi: CjMacroCodeFragment, val light: ParsedCangJieMacroFragment)

    companion object {
        private val SOURCE_FACTS = ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE)
        private val TYPED_PARAMETER_GRAMMARS = listOf(
            CjParameterGrammar.REGULAR_TYPED,
            CjParameterGrammar.CLASS_PRIMARY,
            CjParameterGrammar.STRUCT_PRIMARY,
        )
        private val PARAMETER_GRAMMARS = CjParameterGrammar.entries
        private val COMMA_PARENTS = listOf(
            CjMacroFragmentParent.ARRAY_LITERAL,
            CjMacroFragmentParent.TUPLE_LITERAL,
            CjMacroFragmentParent.FUNCTION_ARGUMENT,
        )
        private const val IF_AVAILABLE = "@IfAvailable(level: 19, { => println(1) }, { => println(2) })"
        private const val IF_AVAILABLE_ARG_NAME = "@IfAvailable expect an argument name"
        private const val IF_AVAILABLE_LAMBDA = "@IfAvailable expect a literal lambda here"
    }
}
