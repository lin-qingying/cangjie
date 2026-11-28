package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertFailsWith as assertFailure
import kotlin.test.assertFalse as assertValueFalse
import kotlin.test.assertNotEquals as assertValueNotEquals
import kotlin.test.assertNotSame as assertValueNotSame
import kotlin.test.assertNull as assertValueNull
import kotlin.test.assertSame as assertValueSame
import kotlin.test.assertTrue as assertValueTrue

/**
 * v34 同文本重解析保留实际冻结来源，编译语境变化仍产生独立的 journal、属性及诊断代次。
 * 官方 v1.1.3 ParseModifiers.cpp 的 CheckDeclModifiers 在 UNKNOWN_SCOPE 直接映射属性；
 * 真实作用域先调用 ParseCJMPDecl.cpp 的 CheckCJMPModifiers，拒绝时返回有效空属性集。
 * 词元身份只证明同一输入；不同 journal 的声明及事件不会因此取得 owner 等价。
 */
class CjReparseSourceIdentityTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun macroReparseRetainsActualInputWhenOriginalSeedHasNoSource() {
        val seed = macroSeed(COMMON_ONLY)
        assertValueNull(seed.sourceContext)
        assertValueNull(seed.lexicalInput)
        val (original, oldDiagnostics, transaction) = observeBufferedMacro(seed)
        val before = original.declarationModifiers.values.single()
        assertValueSame(seed, original.inputSeed)
        assertValueSame(original.sourceContext, original.actualInputSeed.sourceContext)
        assertValueSame(original.lexicalInput, original.actualInputSeed.lexicalInput)
        assertValueSame(original.sourceContext.snapshot, original.lexicalInput.source)
        assertDeclaration(before, original.lexicalInput, COMMON_ONLY, CjScopeKind.TOPLEVEL, emptySet())
        assertSpecificFileDiagnostic(oldDiagnostics)

        val reparsed = transaction.reparse(SPECIFIC_ONLY, CjmpFileRole.NONE)
        assertConsumed(transaction)
        reparsed.consume { fragment, diagnostics ->
            assertReparsedMacro(original, fragment, SPECIFIC_ONLY)
            assertDeclaration(fragment.declarationModifiers.values.single(), original.lexicalInput,
                SPECIFIC_ONLY, CjScopeKind.TOPLEVEL, SPECIFIC_ATTRIBUTES)
            assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, original.declarationRoleChecks.values.single())
            assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, fragment.declarationRoleChecks.values.single())
            assertValueNotEquals(original.roleEvents.single().id, fragment.roleEvents.single().id)
            assertValueTrue(diagnostics.isEmpty())
        }
        assertConsumed(reparsed)
        assertValueSame(seed, original.inputSeed)
        assertValueNull(original.inputSeed.sourceContext)
        assertValueNull(original.inputSeed.lexicalInput)
        assertValueSame(before, original.declarationModifiers.values.single())
        assertSpecificFileDiagnostic(oldDiagnostics)
    }

    @Test
    fun forSameContextTransfersOneGenerationAndItsUnpublishedDiagnostics() {
        val seed = macroSeed(COMMON_ONLY)
        val (original, oldDiagnostics, transaction) = observeBufferedMacro(seed)
        assertSpecificFileDiagnostic(oldDiagnostics)

        val retained = transaction.forContext(COMMON_ONLY, CjmpFileRole.NONE)
        assertConsumed(transaction)
        retained.consume { fragment, diagnostics ->
            assertValueSame(original, fragment)
            assertValueSame(seed, fragment.inputSeed)
            assertValueSame(original.tree, fragment.tree)
            assertValueSame(original.lexicalInput, fragment.lexicalInput)
            assertValueSame(original.declarationModifiers, fragment.declarationModifiers)
            assertValueSame(original.roleEvents, fragment.roleEvents)
            assertValueEquals(oldDiagnostics, diagnostics)
            assertValueSame(oldDiagnostics.single(), diagnostics.single())
            assertValueEquals(CangJieLightDiagnosticOwnership.BUFFERED, fragment.diagnosticOwnership)
            assertDeclaration(fragment.declarationModifiers.values.single(), fragment.lexicalInput,
                COMMON_ONLY, CjScopeKind.TOPLEVEL, emptySet())
        }
        assertConsumed(retained)
    }

    @Test
    fun forDifferentContextDropsRejectedDiagnosticsAndRebuildsAttributes() {
        val (original, oldDiagnostics, transaction) = observeBufferedMacro(macroSeed(COMMON_ONLY))
        assertSpecificFileDiagnostic(oldDiagnostics)

        val accepted = transaction.forContext(SPECIFIC_ONLY, CjmpFileRole.NONE)
        assertConsumed(transaction)
        accepted.consume { fragment, diagnostics ->
            assertReparsedMacro(original, fragment, SPECIFIC_ONLY)
            assertValueNotEquals(original.roleEvents.single().id, fragment.roleEvents.single().id)
            assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, fragment.declarationRoleChecks.values.single())
            assertDeclaration(fragment.declarationModifiers.values.single(), original.lexicalInput,
                SPECIFIC_ONLY, CjScopeKind.TOPLEVEL, SPECIFIC_ATTRIBUTES)
            assertValueTrue(diagnostics.isEmpty())
        }
        assertConsumed(accepted)
        assertSpecificFileDiagnostic(oldDiagnostics)
        assertValueTrue(original.declarationModifiers.values.single().attributes.isEmpty())
    }

    @Test
    fun forDifferentContextCreatesNewDiagnosticsWhenAcceptedInputIsRejected() {
        val (original, oldDiagnostics, transaction) = observeBufferedMacro(macroSeed(SPECIFIC_ONLY))
        assertValueTrue(oldDiagnostics.isEmpty())
        assertDeclaration(original.declarationModifiers.values.single(), original.lexicalInput,
            SPECIFIC_ONLY, CjScopeKind.TOPLEVEL, SPECIFIC_ATTRIBUTES)

        val rejected = transaction.forContext(COMMON_ONLY, CjmpFileRole.NONE)
        assertConsumed(transaction)
        rejected.consume { fragment, diagnostics ->
            assertReparsedMacro(original, fragment, COMMON_ONLY)
            assertValueNotEquals(original.roleEvents.single().id, fragment.roleEvents.single().id)
            assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, fragment.declarationRoleChecks.values.single())
            assertDeclaration(fragment.declarationModifiers.values.single(), original.lexicalInput,
                COMMON_ONLY, CjScopeKind.TOPLEVEL, emptySet())
            assertSpecificFileDiagnostic(diagnostics)
        }
        assertConsumed(rejected)
        assertValueTrue(oldDiagnostics.isEmpty())
        assertValueEquals(SPECIFIC_ATTRIBUTES, original.declarationModifiers.values.single().attributes)
    }

    @Test
    fun unknownScopeReparseMapsAttributesWithoutCompilationFileRoleChecks() {
        val (original, oldDiagnostics, transaction) = observeBufferedMacro(
            macroSeed(COMMON_ONLY, CjScopeKind.UNKNOWN_SCOPE),
        )
        assertValueTrue(oldDiagnostics.isEmpty())
        assertValueTrue(original.roleEvents.isEmpty())
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, original.declarationRoleChecks.values.single())
        assertDeclaration(original.declarationModifiers.values.single(), original.lexicalInput,
            COMMON_ONLY, CjScopeKind.UNKNOWN_SCOPE, SPECIFIC_ATTRIBUTES)

        val reparsed = transaction.reparse(SPECIFIC_ONLY, CjmpFileRole.NONE)
        assertConsumed(transaction)
        reparsed.consume { fragment, diagnostics ->
            assertReparsedMacro(original, fragment, SPECIFIC_ONLY)
            assertValueTrue(fragment.roleEvents.isEmpty())
            assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, fragment.declarationRoleChecks.values.single())
            assertDeclaration(fragment.declarationModifiers.values.single(), original.lexicalInput,
                SPECIFIC_ONLY, CjScopeKind.UNKNOWN_SCOPE, SPECIFIC_ATTRIBUTES)
            assertValueTrue(diagnostics.isEmpty())
        }
        assertConsumed(reparsed)
    }

    @Test
    fun completeBufferedLightTreeRetainsLexicalInputAcrossCompilationContexts() {
        val sourceFile = CjInMemoryTextSourceFile("reparse.cj", "source-identity/reparse.cj", SOURCE)
        val original = CangJieLightParser.parseBuffered(
            builder(SOURCE), languageModuleName = "source_identity", cjmpContext = COMMON_ONLY, sourceFile = sourceFile,
        )
        val input = original.file.lexicalInput
        val before = original.file.declarationModifiers.values.single()
        assertSpecificFileDiagnostic(original.diagnostics)
        assertDeclaration(before, input, COMMON_ONLY, CjScopeKind.TOPLEVEL, emptySet())

        val reparsed = CangJieLightParser.parseBuffered(
            builder(SOURCE), languageModuleName = original.file.inputLanguageModuleName,
            sourceKind = original.file.fileFacts.sourceKind, cjmpContext = SPECIFIC_ONLY, lexicalInput = input,
        )
        val after = reparsed.file.declarationModifiers.values.single()
        for (file in listOf(original.file, reparsed.file)) {
            assertValueTrue(file.isCompleteFile)
            assertValueEquals(CangJieLightDiagnosticOwnership.BUFFERED, file.diagnosticOwnership)
            assertValueEquals("source_identity", file.inputLanguageModuleName)
            assertValueEquals(CjSourceKind.SOURCE, file.fileFacts.sourceKind)
            assertValueSame(input, file.lexicalInput)
            assertValueSame(input.source, file.sourceContext.snapshot)
            assertValueEquals(0, file.sourceContext.inputStart)
            assertValueEquals(SOURCE, file.sourceContext.snapshot.text)
            assertValueEquals(sourceFile.name, file.sourceContext.snapshot.fileName)
            assertValueEquals(sourceFile.path, file.sourceContext.snapshot.filePath)
            assertValueTrue(file.sourceTokens.values.all { input.contains(it) })
            assertValueTrue(file.sourceTokens.values.any { it == before.sourceToken })
        }
        assertValueSame(before.sourceToken, after.sourceToken)
        assertValueNotEquals(before.rootIdentity, after.rootIdentity)
        assertValueNotSame(original.file.tree, reparsed.file.tree)
        assertValueNotEquals(original.file.roleEvents.single().id, reparsed.file.roleEvents.single().id)
        assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, original.file.declarationRoleChecks.values.single())
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, reparsed.file.declarationRoleChecks.values.single())
        assertValueEquals(COMMON_ONLY, original.file.cjmpContext)
        assertValueEquals(SPECIFIC_ONLY, reparsed.file.cjmpContext)
        assertDeclaration(after, input, SPECIFIC_ONLY, CjScopeKind.TOPLEVEL, SPECIFIC_ATTRIBUTES)
        assertValueTrue(reparsed.diagnostics.isEmpty())
        original.claimForConstruction()
        reparsed.claimForConstruction()
        assertFailure<IllegalStateException> { original.claimForConstruction() }
        assertFailure<IllegalStateException> { reparsed.claimForConstruction() }
    }

    @Test
    fun completeBufferedLightTreeRequiresTheExactFrozenFullText() {
        val original = CangJieLightParser.parseBuffered(builder(SOURCE), cjmpContext = SPECIFIC_ONLY)
        val input = original.file.lexicalInput
        val token = original.file.declarationModifiers.values.single().sourceToken
        val sameLengthDifferentName = SOURCE.replace("retained", "replaced")
        assertValueEquals(SOURCE.length, sameLengthDifferentName.length)
        // 短文本仍是原快照的合法切片，但完整文件入口必须校验全文，不能仅接受这个切片。
        for (changedText in listOf(sameLengthDifferentName, SOURCE.removeSuffix("\r\n"),
            SOURCE.replace("\r\n", "\n"), SOURCE + "\n")) {
            assertFailure<IllegalArgumentException> {
                CangJieLightParser.parseBuffered(builder(changedText), cjmpContext = COMMON_ONLY, lexicalInput = input)
            }
        }
        assertValueSame(input, original.file.lexicalInput)
        assertValueEquals(SOURCE, input.source.text)
        assertValueSame(token, original.file.declarationModifiers.values.single().sourceToken)
        assertValueTrue(original.diagnostics.isEmpty())
        original.claimForConstruction()
        assertFailure<IllegalStateException> { original.claimForConstruction() }
    }

    /**
     * 先领取真实 buffered 结果供前后对照，再把同一片段及诊断交给新的单次消费凭证。
     * 使用测试可见的 internal 事务构造器，仅转交既有代次，不另建来源、树、journal 或诊断。
     */
    private fun observeBufferedMacro(seed: CjParserSeed): Triple<
        ParsedCangJieMacroFragment, List<ParsingDiagnostic>, CangJieLightMacroParseResult,
    > {
        val initial = CangJieLightParser.parseMacroCodeFragmentBuffered(builder(SOURCE), seed)
        val observed = initial.consume { fragment, diagnostics ->
            Triple(fragment, diagnostics, CangJieLightMacroParseResult(fragment, diagnostics))
        }
        assertConsumed(initial)
        return observed
    }

    /** 只比较同次结果的直接声明，不按偏移、声明名或后代搜索为另一棵树授予 owner。 */
    private fun assertReparsedMacro(
        original: ParsedCangJieMacroFragment,
        reparsed: ParsedCangJieMacroFragment,
        targetContext: CjmpParsingContext.Compilation,
    ) {
        assertValueNotSame(original, reparsed)
        assertValueNotSame(original.tree, reparsed.tree)
        assertValueSame(original.lexicalInput, reparsed.lexicalInput)
        assertValueSame(original.sourceContext, reparsed.sourceContext)
        assertValueSame(original.sourceContext.snapshot, reparsed.lexicalInput.source)
        assertValueEquals(SOURCE, original.inputText)
        assertValueEquals(original.inputText, reparsed.inputText)
        assertValueSame(original.inputSeed.context, reparsed.inputSeed.context)
        assertValueSame(original.inputSeed.macroFragment, reparsed.inputSeed.macroFragment)
        assertValueEquals(original.inputSeed.sourceKind, reparsed.inputSeed.sourceKind)
        assertValueEquals(original.inputSeed.languageModuleName, reparsed.inputSeed.languageModuleName)
        assertValueEquals(original.inputSeed.quotedSyntaxDepth, reparsed.inputSeed.quotedSyntaxDepth)
        assertValueEquals(targetContext, reparsed.inputSeed.cjmpContext)
        assertValueEquals(CjmpFileRole.NONE, reparsed.inputSeed.fileRole)
        assertValueEquals(targetContext, reparsed.parsingInfo.parsingContext)
        assertValueEquals(CjmpFileRole.NONE, reparsed.parsingInfo.entryFileRole)
        assertValueSame(original.lexicalInput, reparsed.inputSeed.lexicalInput)
        assertValueSame(original.sourceContext, reparsed.inputSeed.sourceContext)
        assertValueSame(reparsed.lexicalInput, reparsed.actualInputSeed.lexicalInput)
        assertValueEquals(CangJieLightDiagnosticOwnership.BUFFERED, reparsed.diagnosticOwnership)
        val before = original.declarationModifiers.values.single()
        val after = reparsed.declarationModifiers.values.single()
        assertValueSame(before.sourceToken, after.sourceToken)
        assertValueNotSame(before, after)
        assertValueNotEquals(before.rootIdentity, after.rootIdentity)
    }

    private fun assertDeclaration(
        declaration: CjParsedDeclarationModifiers,
        input: CjLexicalSourceInput,
        context: CjmpParsingContext,
        scope: CjScopeKind,
        attributes: Set<CjModifierKind>,
    ) {
        assertValueEquals(context, declaration.parsingContext)
        assertValueEquals(attributes, declaration.attributes)
        assertValueEquals(CjmpModifierDeclarationKind.FUNCTION, declaration.declarationKind)
        assertValueEquals(scope, declaration.scope)
        assertValueFalse(declaration.hasConstModifier)
        assertValueTrue(input.contains(declaration.sourceToken))
        val token = input.tokens[declaration.sourceToken.ordinal]
        assertValueSame(token.id, declaration.sourceToken)
        assertValueEquals(CjTokens.FUNC_KEYWORD, token.type)
    }

    /** 文件角色错误保留原文件归属，不能从同名消息或片段关键字位置补造诊断。 */
    private fun assertSpecificFileDiagnostic(diagnostics: List<ParsingDiagnostic>) {
        val diagnostic = diagnostics.single()
        assertValueEquals(ParsingDiagnostic.Origin.ORIGINAL_FILE, diagnostic.origin)
        assertValueEquals(CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file"),
            diagnostic.message)
    }

    /** 三个合法事务入口共享一次消费状态，不能重新发布已经领取或转交的代次。 */
    private fun assertConsumed(result: CangJieLightMacroParseResult) {
        assertFailure<IllegalStateException> { result.consume { _, _ -> } }
        assertFailure<IllegalStateException> { result.forContext(COMMON_ONLY, CjmpFileRole.NONE) }
        assertFailure<IllegalStateException> { result.reparse(SPECIFIC_ONLY, CjmpFileRole.NONE) }
    }

    private fun macroSeed(context: CjmpParsingContext, scope: CjScopeKind = CjScopeKind.TOPLEVEL): CjParserSeed =
        createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), context,
            CjMacroFragmentContext.Nodes(MacroParsingScope.Declaration(scope), CjMacroFragmentParent.OTHER, null,
                CjMacroDeclarationPrefix.EMPTY))

    private fun builder(source: String): PsiBuilder =
        PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)

    companion object {
        private const val SOURCE = "public unsafe specific func retained(): Unit {}\r\n"
        private val COMMON_ONLY = CjmpParsingContext.Compilation(compileCommon = true, compileSpecific = false, enabled = true)
        private val SPECIFIC_ONLY = CjmpParsingContext.Compilation(compileCommon = false, compileSpecific = true, enabled = true)
        private val SPECIFIC_ATTRIBUTES = setOf(CjModifierKind.PUBLIC, CjModifierKind.UNSAFE, CjModifierKind.SPECIFIC)
    }
}
