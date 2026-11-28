package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertFalse as falseValue
import kotlin.test.assertTrue as trueValue
import kotlin.test.assertNotNull as present
import kotlin.test.assertNotEquals as different
import kotlin.test.assertSame as identical
import kotlin.test.assertFailsWith as fails

/** 同一冻结输入的源身份与 target parser 的语义结果独立，不以 token 相同授予 owner 等价。 */
class CjLexicalSourceIdentityTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun sameInputPreservesTokenIdentityWhileContextsReturnDifferentAttributes() {
        val source = "public unsafe specific func wrong(): Unit {}"
        val input = CjLexicalSourceInput(CjParserSourceContext.complete(source).snapshot)
        val rejected = parse(input, CjmpParsingContext.Compilation(true, false, true))
            .declarationModifiers.values.single()
        val accepted = parse(input, CjmpParsingContext.Compilation(false, true, true))
            .declarationModifiers.values.single()
        equal(rejected.sourceToken, accepted.sourceToken)
        different(rejected.rootIdentity, accepted.rootIdentity)
        equal(emptySet(), rejected.attributes)
        trueValue(accepted.hasModifier(CjModifierKind.SPECIFIC))
        trueValue(accepted.hasModifier(CjModifierKind.UNSAFE))
        equal(CjmpModifierDeclarationKind.FUNCTION, accepted.declarationKind)
        equal(CjScopeKind.TOPLEVEL, accepted.scope)
        equal(CjTokens.FUNC_KEYWORD, input.tokens[accepted.sourceToken.ordinal].type)
    }

    @Test
    fun identicalTextInIndependentInputsHasDifferentSourceIdentities() {
        val source = "public func same(): Unit {}"
        val first = CjLexicalSourceInput(CjParserSourceContext.complete(source).snapshot)
        val second = CjLexicalSourceInput(CjParserSourceContext.complete(source).snapshot)
        val a = parse(first, CjmpParsingContext.SyntaxOnly).declarationModifiers.values.single()
        val b = parse(second, CjmpParsingContext.SyntaxOnly).declarationModifiers.values.single()
        equal(a.sourceToken.ordinal, b.sourceToken.ordinal)
        different(a.sourceToken.sourceIdentity, b.sourceToken.sourceIdentity)
        falseValue(first.contains(b.sourceToken))
        trueValue(first.contains(a.sourceToken))
    }

    @Test
    fun publishedFileSeedKeepsTheActualInputAndOriginalRegionEntry() {
        val file = load("lexicalFileSeed", NESTED)
        val region = present(file.parsedRoleRegion)
        val result = present(present(file.packageDirective).node.getCopyableUserData(PARSED_FILE_RESULT_KEY))
        val published = present(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        identical(result.input, region.entrySeed)
        identical(region.lexicalInput, published.lexicalInput)
        identical(result.sourceContext, published.sourceContext)
        identical(region.lexicalInput.source, present(published.sourceContext).snapshot)
        equal(result.sourceText, region.lexicalInput.source.text)
        equal(result.input.context, published.context)
        equal(result.input.cjmpContext, published.cjmpContext)
        equal(result.input.fileRole, published.fileRole)
    }

    @Test
    fun lazyChildUsesTheActualFileLexicalInputWithAnIndependentJournal() {
        val file = load("lexicalLazy", NESTED)
        val root = present(file.parsedRoleRegion)
        val host = file.declarations.filterIsInstance<CjNamedFunction>().single()
        val hostResult = present(host.node.declarationModifiersForOwnerOrNull())
        val body = present(host.bodyBlockExpression)
        falseValue(body.isParsed)
        identical(root.lexicalInput, present(body.parserSeedOrNull()).lexicalInput)
        body.firstChildNode
        val nested = body.statements.filterIsInstance<CjNamedFunction>().single()
        val nestedResult = present(nested.node.declarationModifiersForOwnerOrNull())
        different(hostResult.rootIdentity, nestedResult.rootIdentity)
        equal(hostResult.sourceToken.sourceIdentity, nestedResult.sourceToken.sourceIdentity)
        trueValue(nestedResult.sourceToken.ordinal > hostResult.sourceToken.ordinal)
        equal(CjScopeKind.FUNC_BODY, nestedResult.scope)
        identical(root.lexicalInput, present(present(nested.bodyBlockExpression).parserSeedOrNull()).lexicalInput)
        equal(nestedResult.sourceToken, present(nested.node.findChildByType(CjTokens.FUNC_KEYWORD)).parsedSourceTokenOrNull())
    }

    @Test
    fun copyingPsiKeepsTheInputIdentityAndIndependentChildResults() {
        val original = load("lexicalCopy", NESTED)
        val input = present(original.parsedRoleRegion).lexicalInput
        val copy = original.copy() as CjFile
        identical(input, present(copy.parsedRoleRegion).lexicalInput)
        val originalHost = original.declarations.filterIsInstance<CjNamedFunction>().single()
        val copiedHost = copy.declarations.filterIsInstance<CjNamedFunction>().single()
        equal(present(originalHost.node.declarationModifiersForOwnerOrNull()).sourceToken,
            present(copiedHost.node.declarationModifiersForOwnerOrNull()).sourceToken)
        val copiedBody = present(copiedHost.bodyBlockExpression)
        copiedBody.firstChildNode
        val nested = copiedBody.statements.filterIsInstance<CjNamedFunction>().single()
        identical(input, present(present(nested.bodyBlockExpression).parserSeedOrNull()).lexicalInput)
        falseValue(present(originalHost.bodyBlockExpression).isParsed)
        trueValue(input.contains(present(nested.node.declarationModifiersForOwnerOrNull()).sourceToken))
    }

    @Test
    fun rollbackReparseKeepsTheTokenButNeverReusesTheRoleEventIdentity() {
        val source = "specific func same(): Unit {}"
        val input = CjLexicalSourceInput(CjParserSourceContext.complete(source).snapshot)
        val builder = semantic(input)
        val parser = CangJieParsing.createForTopLevelNonLazy(builder,
            cjmpContext = CjmpParsingContext.Compilation(false, true, true))
        val probe = builder.mark()
        parser.parseFile()
        probe.rollbackTo()
        parser.parseFile()
        val data = builder.macroParsingJournal.freeze(0)
        val result = data.declarationModifiers.values.single()
        equal(input.tokenAt(source.indexOf("func")).id, result.sourceToken)
        trueValue(data.roleEvents.single().id.sequence > 0)
        trueValue(input.contains(result.sourceToken))
    }

    @Test
    fun realFileStubRetainsSourceIdentityGrammarAndScope() {
        val file = load("lexicalStub", NESTED)
        val root = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
        val results = present(root.cjmpRoleSnapshot).stubDeclarationModifiers.values
        val input = present(file.parsedRoleRegion).lexicalInput
        val host = file.declarations.filterIsInstance<CjNamedFunction>().single()
        val nested = present(host.bodyBlockExpression).statements.filterIsInstance<CjNamedFunction>().single()
        val owners = listOf(host, nested).map { present(it.node.declarationModifiersForOwnerOrNull()) }
        equal(owners.size, results.size)
        trueValue(results.all { input.contains(it.sourceToken) })
        trueValue(results.all { it.declarationKind == CjmpModifierDeclarationKind.FUNCTION })
        equal(setOf(CjScopeKind.TOPLEVEL, CjScopeKind.FUNC_BODY), results.map { it.scope }.toSet())
        // 当前 stub visitor 保存真实 host 和 nested 声明；每个结果仍对应其原 AST owner。
        equal(owners.associateBy { it.sourceToken }, results.associateBy { it.sourceToken })
    }

    @Test
    fun unicodeAndCrlfKeepTokenOrdinalsIndependentOfUtf16Coordinates() {
        val source = "// α\r\npublic func `Café`(): Unit {}"
        val input = CjLexicalSourceInput(CjParserSourceContext.complete(source).snapshot)
        val result = parse(input, CjmpParsingContext.SyntaxOnly).declarationModifiers.values.single()
        val token = input.tokens[result.sourceToken.ordinal]
        equal(CjTokens.FUNC_KEYWORD, token.type)
        equal(source.indexOf("func"), token.startOffset)
        different(token.startOffset, token.id.ordinal)
        equal(token.id.ordinal, input.boundaryAt(token.startOffset))
        equal(token.id.ordinal + 1, input.boundaryAt(token.endOffset))
        equal(input.tokens.size, input.boundaryAt(source.length))
        equal(2, input.source.nativePosition(token.startOffset).first)
    }

    @Test
    fun lexicalSnapshotRejectsMutationAndPositionsInsideTokens() {
        val input = CjLexicalSourceInput(CjParserSourceContext.complete("public func name(): Unit {}").snapshot)
        fails<UnsupportedOperationException> { (input.tokens as MutableList<CjLexicalSourceInput.Token>).clear() }
        val keyword = input.tokens.first { it.type == CjTokens.FUNC_KEYWORD }
        fails<IllegalStateException> { input.tokenAt(keyword.startOffset + 1) }
        fails<IllegalStateException> { input.boundaryAt(keyword.startOffset + 1) }
        fails<IllegalArgumentException> { input.boundaryAt(-1) }
    }

    private fun semantic(input: CjLexicalSourceInput): SemanticWhitespaceAwarePsiBuilderImpl =
        SemanticWhitespaceAwarePsiBuilderImpl(
            PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), input.source.text),
            CjParserSourceContext(input.source, 0), input,
        )

    private fun parse(input: CjLexicalSourceInput, context: CjmpParsingContext): CjPreparedParsingData {
        val builder = semantic(input)
        CangJieParsing.createForTopLevelNonLazy(builder, cjmpContext = context).parseFile()
        builder.lightTree
        return builder.macroParsingJournal.freeze(0)
    }

    private fun load(name: String, source: String): CjFile = (createPsiFile(name, source) as CjFile).also {
        it.cjmpParsingContext = CjmpParsingContext.SyntaxOnly
        it.node.firstChildNode
    }

    companion object {
        private val NESTED = "public func host(): Unit { unsafe func nested(): Unit {} }"
    }
}
