package org.cangnova.cangjie.parsing

import com.intellij.lang.impl.PsiBuilderImpl
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CjCanonicalParsingDiagnosticHint
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.CjParsingDiagnosticRange
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as equal
import kotlin.test.assertNotNull as present
import kotlin.test.assertNotSame as distinct
import kotlin.test.assertSame as identical
import kotlin.test.assertTrue as truth

/**
 * v1.1.3 ParserDiag.cpp:557–603 和 published-final-expectations/parameter-access-triple.cj：
 * final 只发布 public/private 与 protected/public；每条都先提示原 modifier，再提示参数的 let/var。
 * 原始偏移来自独立 fixture，原生坐标独立计算，不能由 collector 或实际诊断反推 expected。
 */
class CjPrimaryParameterDiagnosticPublicationTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun officialPrimaryAccessHintsAreCompleteInPsiAndLightTree() {
        for (fixture in fixtures()) {
            val file = sourceFile(fixture)
            val input = actualInput(file, fixture)
            val expected = completedExpected(fixture, input)
            equal(expected, file.collectParsingDiagnostics(), "PSI: ${fixture.name}")
            equal(expected, file.collectParsingDiagnostics(), "PSI repeated: ${fixture.name}")

            val parsed = CangJieLightParser.parseBuffered(sdkBuilder(fixture.text), lexicalInput = input)
            identical(input, parsed.file.lexicalInput)
            identical(input.source, parsed.file.sourceContext.snapshot)
            parsed.file.requireConsistentInput()
            equal(expected, parsed.diagnostics, "LightTree: ${fixture.name}")
        }
    }

    /** 先执行实际 parser 并回滚其 marker；最后只通过正式 buffered 入口完成新的发布事务。 */
    @Test
    fun realParserRollbackDoesNotRetainSpeculativePrimaryReports() {
        for (fixture in fixtures()) {
            val input = actualInput(sourceFile(fixture), fixture)
            val sdk = sdkBuilder(fixture.text)
            val semantic = SemanticWhitespaceAwarePsiBuilderImpl(sdk, CjParserSourceContext(input.source, 0), input)
            val speculation = semantic.mark()
            val abandonedRoot = semantic.macroParsingJournal.diagnosticScope.rootIdentity
            CangJieParsing.createForTopLevelNonLazy(semantic).parseFile()
            truth(semantic.eof(), fixture.name)
            speculation.rollbackTo()
            equal(0, sdk.currentOffset, "SDK cursor after rollback: ${fixture.name}")
            identical(input.source, semantic.macroParsingJournal.sourceContext.snapshot)

            val parsed = CangJieLightParser.parseBuffered(sdk, lexicalInput = input)
            identical(input, parsed.file.lexicalInput)
            parsed.file.requireConsistentInput()
            equal(completedExpected(fixture, input), parsed.diagnostics, "final after rollback: ${fixture.name}")

            // 三个真实 producer 仍存在；final 的两条不能掩盖 speculative 报告没有被回滚的问题。
            val reports = parsed.file.diagnosticPublication.reports
            equal(completedExpected(fixture, input, includeSuppressed = true), reports.map { it.diagnostic }, fixture.name)
            equal(3, reports.map { it.order }.toSet().size, fixture.name)
            truth(reports.none { it.order.rootIdentity == abandonedRoot }, fixture.name)
            truth(reports.all { it.order.rootIdentity == parsed.file.diagnosticScope.rootIdentity }, fixture.name)
        }
    }

    /** caller 输入使用官方 raw DTO；collector 仅生成实际发布值，expected 由独立范围算法完成。 */
    @Test
    fun callerHintListChangesCannotRewriteEitherPublishedPrimaryHint() {
        for (fixture in fixtures()) {
            val input = actualInput(sourceFile(fixture), fixture)
            val expected = completedExpected(fixture, input)
            val raw = rawExpected(fixture, includeSuppressed = true)
            val callerHints = raw.first().hints.toMutableList()
            val callerDiagnostic = raw.first().copy(hints = callerHints)
            val collector = CjParsingDiagnosticCollector(CjParserSourceContext(input.source, 0))
            collector.report(callerDiagnostic)
            callerHints.clear()
            callerHints += ParsingDiagnosticHint(0, 1, "caller changed the original hint list")
            identical(callerHints, callerDiagnostic.hints)
            collector.report(raw[1])
            collector.report(raw[2])

            val actual = collector.finish()
            equal(expected, actual, "published after caller mutation: ${fixture.name}")
            distinct(callerHints, actual.first().hints)
            equal(completedExpected(fixture, input, includeSuppressed = true),
                collector.publication.reports.map { it.diagnostic }, fixture.name)
            callerHints.clear()
            equal(expected, collector.publication.diagnostics, "publication remains frozen: ${fixture.name}")
        }
    }

    private fun sourceFile(fixture: Fixture): CjFile =
        CjPsiFactory(project, markGenerated = false).createFile(fixture.text)

    /** PSI/LT 使用同一个实际冻结输入，不能另造匿名快照或用诊断 source 作 expected 的来源。 */
    private fun actualInput(file: CjFile, fixture: Fixture): CjLexicalSourceInput {
        file.node.firstChildNode
        val seed = present(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        val input = present(seed.lexicalInput)
        identical(input.source, present(seed.sourceContext).snapshot)
        equal(fixture.text, input.source.text)
        identical(CjmpParsingContext.SyntaxOnly, file.cjmpParsingContext)
        return input
    }

    private fun sdkBuilder(text: String): PsiBuilderImpl =
        PsiBuilderImpl(null, null, CangJieParserDefinition(), CangJieLexer(), null, text, null, null)

    /** CONST/PRIVATE 等冲突表不参与这里的 expected 计算；三个 producer 的顺序按官方数组明确指定。 */
    private fun rawExpected(fixture: Fixture, includeSuppressed: Boolean = false): List<ParsingDiagnostic> {
        val privateRange = tokenRange(fixture.text, "private")
        val publicRange = tokenRange(fixture.text, "public")
        val protectedRange = tokenRange(fixture.text, "protected")
        val keywordStart = fixture.text.indexOf("${fixture.keyword} value: Int32")
        require(keywordStart >= 0)
        val keywordRange = keywordStart to keywordStart + fixture.keyword.length
        fun conflict(token: String, previous: String, main: Pair<Int, Int>, hint: Pair<Int, Int>): ParsingDiagnostic =
            ParsingDiagnostic(
                main.first, main.second,
                CangJieParsingBundle.message("parsing.error.parameter.conflicting.member.modifiers", token, previous),
                origin = ParsingDiagnostic.Origin.INPUT,
                category = ParsingDiagnostic.Category.PARSE,
                rangeKind = ParsingDiagnostic.RangeKind.RANGE,
                hints = listOf(
                    ParsingDiagnosticHint(hint.first, hint.second, CONFLICT_HINT),
                    ParsingDiagnosticHint(keywordRange.first, keywordRange.second, DECLARATION_HINT),
                ),
            )
        return buildList {
            add(conflict("public", "private", publicRange, privateRange))
            add(conflict("protected", "public", protectedRange, publicRange))
            if (includeSuppressed) add(conflict("protected", "private", protectedRange, privateRange))
        }
    }

    private fun completedExpected(
        fixture: Fixture, input: CjLexicalSourceInput, includeSuppressed: Boolean = false,
    ): List<ParsingDiagnostic> = rawExpected(fixture, includeSuppressed).map { raw ->
        fun range(start: Int, end: Int): CjParsingDiagnosticRange = CjParsingDiagnosticRange(
            input.source, nativePosition(fixture.text, start), nativePosition(fixture.text, end),
        )
        raw.copy(
            mainRange = range(raw.startOffset, raw.endOffset),
            nativeHints = raw.hints.map { hint ->
                CjCanonicalParsingDiagnosticHint(range(hint.startOffset, hint.endOffset), hint.label)
            },
        )
    }

    private fun tokenRange(text: String, token: String): Pair<Int, Int> {
        val start = text.indexOf(token)
        require(start >= 0 && text.lastIndexOf(token) == start)
        return start to start + token.length
    }

    /** 独立扫描 CR/LF/CRLF；中文 trivia 使 UTF-8 字节列与 UTF-16 偏移明确不同。 */
    private fun nativePosition(text: String, offset: Int): Pair<Int, Int> {
        require(offset in 0..text.length)
        var line = 1
        var lineStart = 0
        var index = 0
        while (index < offset) {
            if (text[index] == '\r' || text[index] == '\n') {
                val next = index + if (text[index] == '\r' && index + 1 < text.length && text[index + 1] == '\n') 2 else 1
                if (next > offset) break
                line++
                lineStart = next
                index = next
            } else {
                index++
            }
        }
        return line to text.substring(lineStart, offset).toByteArray(Charsets.UTF_8).size + 1
    }

    private fun fixtures(): List<Fixture> = buildList {
        for (kind in listOf("class", "struct")) {
            for (keyword in listOf("let", "var")) {
                add(Fixture("$kind/$keyword/plain", keyword,
                    "$kind Owner { Owner(private public protected $keyword value: Int32, let after!: Int32 = 2) { after } }\n"))
                add(Fixture("$kind/$keyword/triviaCRLF", keyword,
                    "$kind Owner {\r\n    Owner(private /* 中 */ public\r\n        protected $keyword value: Int32, let after!: Int32 = 2) { after }\r\n}\r\n"))
            }
        }
    }

    private data class Fixture(val name: String, val keyword: String, val text: String)

    private companion object {
        val CONFLICT_HINT = CangJieParsingBundle.message("parsing.error.hint.conflicted.modifier")
        val DECLARATION_HINT = CangJieParsingBundle.message("parsing.error.hint.on", "variable declaration")
    }
}
