package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IElementType
import com.intellij.util.io.AbstractStringEnumerator
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjmpRoleInputCompleteness
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.elements.CjFileStubBuilder
import org.cangnova.cangjie.psi.stubs.elements.CjmpFileRoleSnapshotCodec
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertFailsWith as fails
import kotlin.test.assertNotNull as present
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as yes

/** 实际词元、精确切片、不可变树输入与 live stub 发布分别证明，描述字段不能冒充 owner 凭证。 */
class CjParsingSourceInvariantTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun remappedHardKeywordCannotClaimAnIdentifierSourceToken() {
        val builder = builder("nope f(): Unit {}")
        equal(CjTokens.IDENTIFIER, builder.tokenType)
        builder.remapCurrentToken(CjTokens.FUNC_KEYWORD)
        fails<IllegalStateException> { CangJieLightParser.parseBuffered(builder) }
    }

    @Test
    fun aKeywordCaptureMustRetainItsActualSdkExtent() {
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            CangJieParserDefinition(), SplitFunctionKeywordLexer(), "func f(): Unit {}",
        )
        equal("fun", builder.tokenText)
        fails<IllegalStateException> { CangJieLightParser.parseBuffered(builder) }
    }

    @Test
    fun skippedWhitespaceCannotHideTheActualDeclarationTokenStream() {
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            CangJieParserDefinition(), WholeInputWhitespaceLexer(), "func f(): Unit {}",
        )
        // SDK 自动跳过伪装词元，不能因为不存在声明 anchor 而授予完整文件结果。
        equal(null, builder.tokenType)
        fails<IllegalStateException> { CangJieLightParser.parseBuffered(builder) }
    }

    @Test
    fun anAlreadyConsumedDeclarationCannotBePublishedAsAnEmptyFile() {
        val builder = builder("func f(): Unit {}")
        builder.advanceLexer()
        fails<IllegalStateException> { CangJieLightParser.parseBuffered(builder) }
    }

    @Test
    fun aDelayedRemapperCannotTurnTheVerifiedSourceIntoSkippedWhitespace() {
        val builder = builder("func f(): Unit {}")
        builder.setTokenTypeRemapper { _, _, _, _ -> TokenType.WHITE_SPACE }
        fails<IllegalStateException> { CangJieLightParser.parseBuffered(builder) }
    }

    @Test
    fun aDelayedContextualNameRemapperRetainsTheSameActualSource() {
        val builder = builder("enum E { | common }")
        builder.setTokenTypeRemapper { type, _, _, _ ->
            if (type == CjTokens.COMMON_KEYWORD) CjTokens.IDENTIFIER else type
        }
        val parsed = CangJieLightParser.parseBuffered(builder)
        yes(parsed.diagnostics.isEmpty())
        parsed.file.requireConsistentInput()
        parsed.claimForConstruction()
    }

    @Test
    fun aDelayedContextualModifierRemapperKeepsTheGrammarAttributePack() {
        val builder = builder("common func f(): Unit")
        builder.setTokenTypeRemapper { type, _, _, _ ->
            if (type == CjTokens.COMMON_KEYWORD) CjTokens.IDENTIFIER else type
        }
        val parsed = CangJieLightParser.parseBuffered(builder,
            cjmpContext = CjmpParsingContext.Compilation(true, false, true))
        yes(parsed.diagnostics.isEmpty())
        equal(setOf(CjModifierKind.COMMON), parsed.file.declarationModifiers.values.single().attributes)
        parsed.claimForConstruction()
    }

    @Test
    fun constLookaheadRetainsFunctionGrammarAfterPackageProbeRollbackAndRemapping() {
        for (source in listOf("const common func f(): Unit {}", "const /* prefix */ common\nfunc f(): Unit {}")) {
            val original = CangJieLightParser.parseBuffered(builder(source))
            val remappedBuilder = builder(source)
            remappedBuilder.setTokenTypeRemapper { type, _, _, _ ->
                if (type == CjTokens.COMMON_KEYWORD) CjTokens.IDENTIFIER else type
            }
            val remapped = CangJieLightParser.parseBuffered(remappedBuilder)
            val before = original.file.declarationModifiers.values.single()
            val after = remapped.file.declarationModifiers.values.single()
            equal(CjmpModifierDeclarationKind.FUNCTION, before.declarationKind)
            equal(before.declarationKind, after.declarationKind)
            equal(before.scope, after.scope)
            equal(before.attributes, after.attributes)
            yes(before.hasConstModifier && after.hasConstModifier)
            yes(original.diagnostics.isNotEmpty())
            equal(original.diagnostics.map { it.message }, remapped.diagnostics.map { it.message })
        }
    }

    @Test
    fun constContextualPrimaryConstructorNameKeepsItsActualGrammarWithAndWithoutRemapping() {
        for (source in listOf("class common { const common() {} }", "class common { const /* name */ common() {} }")) {
            for (remap in listOf(false, true)) {
                val input = builder(source)
                if (remap) input.setTokenTypeRemapper { type, _, _, _ ->
                    if (type == CjTokens.COMMON_KEYWORD) CjTokens.IDENTIFIER else type
                }
                val parsed = CangJieLightParser.parseBuffered(input)
                yes(parsed.diagnostics.isEmpty(), source)
                val constructor = parsed.file.declarationModifiers.values.single {
                    it.declarationKind == CjmpModifierDeclarationKind.PRIMARY_CONSTRUCTOR
                }
                equal(CjScopeKind.CLASS_BODY, constructor.scope)
                yes(constructor.hasConstModifier)
            }
        }
    }

    @Test
    fun contextualEnumNamesKeepTheirLexerIdentityAndConstructorScope() {
        // 官方 get 是普通标识符；common 是可在此文法作为名字使用的上下文关键字。
        for ((name, type) in listOf("get" to CjTokens.IDENTIFIER, "common" to CjTokens.COMMON_KEYWORD)) {
            for (bar in listOf("| ", "")) {
                val source = "enum E { $bar$name }"
                val parsed = CangJieLightParser.parseBuffered(builder(source))
                yes(parsed.diagnostics.isEmpty(), source)
                val file = load("enumSource$name${bar.length}", source)
                val enumNode = file.declarations.single().node
                val body = present(enumNode.findChildByType(CjNodeTypes.ENUM_BODY))
                val entry = generateSequence(body.firstChildNode) { it.treeNext }
                    .single { it.elementType == CjNodeTypes.ENUM_CONSTRUCTOR }
                val result = present(entry.declarationModifiersForOwnerOrNull())
                val input = present(file.parsedRoleRegion).lexicalInput
                equal(type, input.tokens[result.sourceToken.ordinal].type)
                equal(CjmpModifierDeclarationKind.ENUM_CONSTRUCTOR, result.declarationKind)
                equal(CjScopeKind.ENUM_CONSTRUCTOR, result.scope)
                equal(emptySet(), result.attributes)
                identical(result.sourceToken, present(entry.findChildByType(CjTokens.IDENTIFIER)).parsedSourceTokenOrNull())
            }
        }
    }

    @Test
    fun missingEnumNamesReportSyntaxWithoutMintingAClosingBraceIdentity() {
        for (source in listOf("enum E { | }", "enum E { | static }")) {
            val parsed = CangJieLightParser.parseBuffered(builder(source))
            yes(parsed.diagnostics.isNotEmpty(), source)
            yes(parsed.file.declarationModifiers.values.none { it.declarationKind == CjmpModifierDeclarationKind.ENUM_CONSTRUCTOR })
            val file = load("missingEnumSource${source.length}", source)
            val stub = CjFileStubBuilder().buildStubTree(file) as CangJieFileStub
            yes(present(stub.cjmpRoleSnapshot).stubDeclarationModifiers.values.none {
                it.declarationKind == CjmpModifierDeclarationKind.ENUM_CONSTRUCTOR
            })
        }
    }

    @Test
    fun sourceSlicesRejectTokenInteriorsAndDifferentLengths() {
        val input = CjLexicalSourceInput(CjParserSourceContext.complete("public func f(): Unit {}").snapshot)
        for ((start, end) in listOf(1 to 3, 0 to 3)) {
            val context = CjParserSourceContext(input.source, start, end)
            val slice = input.source.text.substring(start, end)
            fails<IllegalStateException> { SemanticWhitespaceAwarePsiBuilderImpl(builder(slice), context, input) }
        }
        val context = CjParserSourceContext(input.source, 0, 6)
        fails<IllegalArgumentException> { context.requireInput("publi") }
        fails<IllegalArgumentException> { context.requireInput("public ") }
        context.requireInput("public")
    }

    @Test
    fun lazySlotKeepsBothBoundariesAndCannotBeExpandedOrTruncated() {
        val file = load("boundedSlot", "func host(): Unit {} \nfunc after(): Unit {}")
        val host = file.declarations.filterIsInstance<CjNamedFunction>().first()
        val body = present(host.bodyBlockExpression)
        val seed = present(body.parserSeedOrNull())
        val context = present(seed.sourceContext)
        val input = present(seed.lexicalInput)
        equal(body.startOffset, context.inputStart)
        equal(body.startOffset + body.textLength, context.inputEnd)
        identical(present(file.parsedRoleRegion).lexicalInput, input)
        val source = body.chars.toString()
        SemanticWhitespaceAwarePsiBuilderImpl(builder(source), context, input)
        for (changed in listOf(source + " ", source.dropLast(1))) {
            fails<IllegalArgumentException> { SemanticWhitespaceAwarePsiBuilderImpl(builder(changed), context, input) }
        }
    }

    @Test
    fun everyLightParserEntryRejectsAnExistingMutableCallerBuilder() {
        val actions: List<(PsiBuilder) -> Unit> = listOf(
            { CangJieLightParser.parse(it) },
            { CangJieLightParser.parseBuffered(it) },
            { CangJieLightParser.parseAnnotationOnly(it) },
            { CangJieLightParser.parseMacroExpansion(it) },
            { CangJieLightParser.parseMacroCodeFragmentBuffered(it, macroSeed()) },
        )
        for (action in actions) {
            val input = StringBuilder("func f(): Unit {}")
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), input)
            identical(input, builder.originalText)
            fails<IllegalArgumentException> { action(builder) }
        }
    }

    @Test
    fun ownedBuilderFactoriesFreezeBeforeTheSdkReceivesTheText() {
        for (factory in listOf<(CharSequence) -> PsiBuilder>(
            { CangJieLightParser.createBuilder(it) },
            { CangJiePsiBuilder(CangJieParserDefinition(), CangJieLexer(), it) },
        )) {
            val input = StringBuilder("func retained(): Unit {}")
            val builder = factory(input)
            val parsed = CangJieLightParser.parseBuffered(builder)
            input.replace(input.indexOf("retained"), input.indexOf("retained") + 8, "replaced")
            equal("func retained(): Unit {}", builder.originalText)
            parsed.file.requireConsistentInput()
            equal("func retained(): Unit {}", parsed.file.tree.toString(parsed.file.tree.root).toString())
            parsed.claimForConstruction()
        }
    }

    @Test
    fun parserSeedCannotSilentlyDiscardAnUnrelatedLexicalInput() {
        val first = CjLexicalSourceInput(CjParserSourceContext.complete("func f(): Unit {}").snapshot)
        val second = CjLexicalSourceInput(CjParserSourceContext.complete(first.source.text).snapshot)
        fails<IllegalArgumentException> {
            macroSeed().copy(sourceContext = CjParserSourceContext(first.source, 0), lexicalInput = second)
        }
        fails<IllegalArgumentException> { macroSeed().copy(lexicalInput = first) }
    }

    @Test
    fun sourceStubRejectsAClonedAttributePackEvenWithTheSameIds() {
        val file = load("forgedAttributes", "func first(): Unit {}")
        val carrier = functionCarriers(file).single()
        val original = present(carrier.parsedDeclarationModifiersOrNull())
        val forged = CjParsedDeclarationModifiers(original.rootIdentity, original.parsingContext,
            setOf(CjModifierKind.UNSAFE), original.hasConstModifier, original.sourceToken, original.declarationKind, original.scope)
        carrier.putCopyableUserData(DECLARATION_MODIFIERS_KEY, forged)
        fails<IllegalStateException> { CjFileStubBuilder().buildStubTree(file) }
    }

    @Test
    fun sourceStubRejectsExchangingTwoGenuineOwnerResults() {
        val file = load("exchangedCaptures", "func first(): Unit {}\nfunc second(): Unit {}")
        val carriers = functionCarriers(file)
        val first = present(carriers[0].parsedDeclarationModifiersOrNull())
        val second = present(carriers[1].parsedDeclarationModifiersOrNull())
        equal(first.rootIdentity, second.rootIdentity)
        carriers[0].putCopyableUserData(DECLARATION_MODIFIERS_KEY, second)
        carriers[1].putCopyableUserData(DECLARATION_MODIFIERS_KEY, first)
        fails<IllegalStateException> { CjFileStubBuilder().buildStubTree(file) }
    }

    @Test
    fun realPsiCopyRetainsTheSourceDescriptorAndItsActualOwnerCaptures() {
        val source = load("copiedCaptures", "func host(): Unit { func nested(): Unit {} }")
        val first = CjFileStubBuilder().buildStubTree(source) as CangJieFileStub
        val copy = source.copy() as CjFile
        val second = CjFileStubBuilder().buildStubTree(copy) as CangJieFileStub
        val a = present(first.cjmpRoleSnapshot)
        val b = present(second.cjmpRoleSnapshot)
        equal(a.lexicalSource, b.lexicalSource)
        equal(a.stubDeclarationModifiers, b.stubDeclarationModifiers)
        yes(a.stubDeclarationModifiers.isNotEmpty())
    }

    @Test
    fun persistentDescriptionsValidateUuidBoundsAndUniqueStubReferences() {
        val snapshot = describedSnapshot()
        val descriptor = present(snapshot.lexicalSource)
        val good = describedModifiers(CjSourceTokenId(descriptor.sourceIdentity, 1))
        snapshot.withStubBindings(emptyMap(), mapOf(listOf(0) to good))
        for (token in listOf(CjSourceTokenId(UUID.randomUUID(), 1), CjSourceTokenId(descriptor.sourceIdentity, descriptor.tokenCount))) {
            fails<IllegalArgumentException> { snapshot.withStubBindings(emptyMap(), mapOf(listOf(0) to describedModifiers(token))) }
        }
        fails<IllegalArgumentException> {
            snapshot.withStubBindings(emptyMap(), mapOf(listOf(0) to good, listOf(1) to good))
        }
        val withoutDescriptor = CjmpFileRoleSyntaxSnapshot(CjmpParsingContext.SyntaxOnly, CjmpFileRole.NONE,
            CjmpRoleInputCompleteness.COMPLETE, emptyList())
        fails<IllegalArgumentException> { withoutDescriptor.withStubBindings(emptyMap(), mapOf(listOf(0) to good)) }
    }

    @Test
    fun codecRejectsAChangedRootUuidOrTokenCountBeforeReturningBindings() {
        val snapshot = describedSnapshot()
        val descriptor = present(snapshot.lexicalSource)
        val bound = snapshot.withStubBindings(emptyMap(), mapOf(listOf(0) to describedModifiers(CjSourceTokenId(descriptor.sourceIdentity, 1))))
        val bytes = ByteArrayOutputStream().also { output ->
            StubOutputStream(output, TestStrings()).use { CjmpFileRoleSnapshotCodec.write(it, bound) }
        }.toByteArray()
        val raw = ByteArrayInputStream(bytes)
        val input = StubInputStream(raw, TestStrings())
        yes(input.readBoolean())
        equal(7, input.readUnsignedByte())
        if (input.readBoolean()) input.readUTF()
        yes(input.readBoolean())
        val uuidOffset = bytes.size - raw.available()
        val changedUuid = bytes.copyOf().also { it[uuidOffset] = (it[uuidOffset].toInt() xor 1).toByte() }
        val zeroCount = bytes.copyOf().also { payload -> repeat(4) { payload[uuidOffset + 16 + it] = 0 } }
        for (payload in listOf(changedUuid, zeroCount)) {
            StubInputStream(ByteArrayInputStream(payload), TestStrings()).use {
                fails<IOException> { CjmpFileRoleSnapshotCodec.read(it) }
            }
        }
    }

    private fun load(name: String, source: String): CjFile = (createPsiFile(name, source) as CjFile).also {
        it.cjmpParsingContext = CjmpParsingContext.SyntaxOnly
        it.node.firstChildNode
    }

    private fun builder(source: String): PsiBuilder = CangJieLightParser.createBuilder(source)

    private fun functionCarriers(file: CjFile) = file.declarations.filterIsInstance<CjNamedFunction>()
        .map { present(it.node.findChildByType(CjTokens.FUNC_KEYWORD)) }

    private fun macroSeed(): CjParserSeed = createMacroFragmentParserSeed(
        ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), CjmpParsingContext.SyntaxOnly,
        CjMacroFragmentContext.Nodes(MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER,
            null, CjMacroDeclarationPrefix.EMPTY),
    )

    /** 模拟持久化描述，不是 live parser 发布的 owner 凭证。 */
    private fun describedSnapshot(): CjmpFileRoleSyntaxSnapshot = CjmpFileRoleSyntaxSnapshot(
        CjmpParsingContext.SyntaxOnly, CjmpFileRole.NONE, CjmpRoleInputCompleteness.COMPLETE, emptyList(),
        lexicalSource = CjLexicalSourceDescriptor(UUID.randomUUID(), 4),
    )

    private fun describedModifiers(token: CjSourceTokenId): CjParsedDeclarationModifiers = CjParsedDeclarationModifiers(
        UUID.randomUUID(), CjmpParsingContext.SyntaxOnly, emptySet(), false, token,
        CjmpModifierDeclarationKind.FUNCTION, CjScopeKind.TOPLEVEL,
    )

    /** 保留 FUNC 种类但把实际 SDK 词元拆成 fun/c，检验 extent 不能从关键字名字推导。 */
    private class SplitFunctionKeywordLexer : LexerBase() {
        private val rest = CangJieLexer()
        private var phase = 0
        private lateinit var lexerBuffer: CharSequence
        private var lexerEnd = 0
        override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
            require(startOffset == 0 && initialState == 0)
            lexerBuffer = buffer
            lexerEnd = endOffset
            phase = 0
            rest.start(buffer, 4, endOffset, 0)
        }
        override fun getState(): Int = 0
        override fun getTokenType(): IElementType? = when (phase) { 0 -> CjTokens.FUNC_KEYWORD; 1 -> TokenType.BAD_CHARACTER; else -> rest.tokenType }
        override fun getTokenStart(): Int = when (phase) { 0 -> 0; 1 -> 3; else -> rest.tokenStart }
        override fun getTokenEnd(): Int = when (phase) { 0 -> 3; 1 -> 4; else -> rest.tokenEnd }
        override fun advance() { if (phase < 2) phase++ else rest.advance() }
        override fun getBufferSequence(): CharSequence = lexerBuffer
        override fun getBufferEnd(): Int = lexerEnd
    }

    private class WholeInputWhitespaceLexer : LexerBase() {
        private lateinit var lexerBuffer: CharSequence
        private var lexerEnd = 0
        private var consumed = false
        override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
            require(startOffset == 0 && initialState == 0)
            lexerBuffer = buffer
            lexerEnd = endOffset
            consumed = false
        }
        override fun getState(): Int = 0
        override fun getTokenType(): IElementType? = if (consumed) null else TokenType.WHITE_SPACE
        override fun getTokenStart(): Int = if (consumed) lexerEnd else 0
        override fun getTokenEnd(): Int = lexerEnd
        override fun advance() { consumed = true }
        override fun getBufferSequence(): CharSequence = lexerBuffer
        override fun getBufferEnd(): Int = lexerEnd
    }

    private class TestStrings : AbstractStringEnumerator {
        private val values = mutableListOf<String>()
        override fun enumerate(value: String?): Int = if (value == null) 0 else {
            val index = values.indexOf(value)
            if (index >= 0) index + 1 else { values += value; values.size }
        }
        override fun valueOf(idx: Int): String? = if (idx == 0) null else values[idx - 1]
        override fun markCorrupted(): Unit = error("Unexpected persistent storage operation")
        override fun close(): Unit = error("Unexpected persistent storage operation")
        override fun isDirty(): Boolean = error("Unexpected persistent storage operation")
        override fun force(): Unit = error("Unexpected persistent storage operation")
    }
}
