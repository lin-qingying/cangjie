package org.cangnova.cangjie.parsing

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.messages.ParsingDiagnosticSettings
import org.cangnova.cangjie.messages.ParsingWarning
import org.cangnova.cangjie.messages.WarningGroup
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** 固定 modifier-warning-authority/29 的真实 SDK 输出；warning 不借 ERROR_ELEMENT 或 semantic checker 发布。 */
class ParsingWarningPublicationTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    /** open 在同时存在 abstract/sealed 时选择 sealed；public 和 open 分别保存各自 token 范围。 */
    @Test
    fun explicitSealedPrecedesAbstractAndBothWarningsKeepTheirNativeHints() {
        val priority = "package modifier_class_open_priority\n\nopen abstract sealed class C {}\n"
        assertBoth(priority, listOf(warning(priority, 3, 1, 5, CjModifierKind.OPEN,
            ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED), "'sealed' implies 'open'", 3, 15, 21)))
        val both = CLASS_SOURCE
        assertBoth(both, listOf(
            warning(both, 3, 1, 7, CjModifierKind.PUBLIC,
                ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED), "'sealed' implies 'public'", 3, 22, 28),
            warning(both, 3, 8, 12, CjModifierKind.OPEN,
                ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED), "'sealed' implies 'open'", 3, 22, 28),
        ))
    }

    /** interface 头的隐含 open 使用官方原文 interface member；显式 sealed 仍优先。 */
    @Test
    fun interfaceHeadOpenAndPublicUseTheActualHigherTokenOrInterfaceKeyword() {
        val implicit = "package modifier_interface_open_implicit\n\nopen interface I {}\n"
        assertBoth(implicit, listOf(warning(implicit, 3, 1, 5, CjModifierKind.OPEN,
            ParsingWarning.Redundancy.InterfaceMember, "interface member implies 'open'", 3, 6, 15)))
        val explicit = "package modifier_interface_open_sealed\n\nopen sealed interface I {}\n"
        assertBoth(explicit, listOf(warning(explicit, 3, 1, 5, CjModifierKind.OPEN,
            ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED), "'sealed' implies 'open'", 3, 6, 12)))
        val publicSource = "package modifier_interface_public_sealed\n\npublic sealed interface I {}\n"
        assertBoth(publicSource, listOf(warning(publicSource, 3, 1, 7, CjModifierKind.PUBLIC,
            ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED), "'sealed' implies 'public'", 3, 8, 14)))
    }

    /** abstract/default 方法和合法 property 的两个警告均归属成员 token，hint 指向 interface keyword。 */
    @Test
    fun interfaceAbstractDefaultAndPropertyMembersPublishBothModifierWarnings() {
        val sources = listOf(
            "package modifier_interface_function_abstract\n\ninterface I {\n    public open func f(): Unit\n}\n",
            "package modifier_interface_function_default\n\ninterface I {\n    public open func f(): Int64 { 7 }\n}\n",
            "package modifier_interface_property_valid\n\ninterface I {\n    public open prop p: Int64 { get() { 7 } }\n}\n",
        )
        for (source in sources) {
            assertBoth(source, listOf(
                warning(source, 4, 5, 11, CjModifierKind.PUBLIC,
                    ParsingWarning.Redundancy.InterfaceMember, "interface member implies 'public'", 3, 1, 10),
                warning(source, 4, 12, 16, CjModifierKind.OPEN,
                    ParsingWarning.Redundancy.InterfaceMember, "interface member implies 'open'", 3, 1, 10),
            ))
        }
    }

    @Test
    fun reversedMemberModifierOrderRetainsTheOfficialSourceOrder() {
        val source = "package modifier_interface_member_source_order\n\ninterface I {\n    open public func f(): Unit\n}\n"
        assertBoth(source, listOf(
            warning(source, 4, 5, 9, CjModifierKind.OPEN,
                ParsingWarning.Redundancy.InterfaceMember, "interface member implies 'open'", 3, 1, 10),
            warning(source, 4, 10, 16, CjModifierKind.PUBLIC,
                ParsingWarning.Redundancy.InterfaceMember, "interface member implies 'public'", 3, 1, 10),
        ))
    }

    /** 首 open 仍合法并产生 WARNING，重复 token 只产生 ERROR，不追加另一个冗余 warning。 */
    @Test
    fun duplicateTokenIsExcludedWithoutSuppressingTheFirstValidModifierWarning() {
        val source = "package modifier_duplicate_token\n\nopen open interface I {}\n"
        assertBoth(source, listOf(
            warning(source, 3, 1, 5, CjModifierKind.OPEN,
                ParsingWarning.Redundancy.InterfaceMember, "interface member implies 'open'", 3, 11, 20),
            diagnostic(source, 3, 6, 10, "duplicated modifier: 'open'",
                listOf(hint(source, 3, 1, 5, "previous one is here"))),
        ))
    }

    /** illegal token 和冲突双方不进入 warning 候选；排除后不能据最终属性补造 WARNING。 */
    @Test
    fun illegalAndConflictingModifiersCannotProduceRedundancyWarnings() {
        val illegal = "package modifier_illegal_target\n\nopen struct S {}\n"
        assertBoth(illegal, listOf(diagnostic(illegal, 3, 1, 5, "unexpected modifier 'open' on struct declaration",
            listOf(hint(illegal, 3, 6, 12, "on struct declaration")))))
        val conflict = "package modifier_conflict_filter\n\nopen private sealed class C {}\n"
        assertBoth(conflict, listOf(diagnostic(conflict, 3, 14, 20,
            "'sealed' and 'private' modifiers conflict on class declaration", listOf(
                hint(conflict, 3, 6, 13, "because it is conflicted with this"),
                hint(conflict, 3, 21, 26, "on class declaration"),
            ))))
    }

    @Test
    fun parserErrorKeepsWarningButLexicalErrorClosesTheWholeFileParserChannel() {
        val parsing = "package modifier_parse_error_keeps_warning\n\nopen interface I {}\nopen struct S {}\n"
        assertBoth(parsing, listOf(
            warning(parsing, 3, 1, 5, CjModifierKind.OPEN,
                ParsingWarning.Redundancy.InterfaceMember, "interface member implies 'open'", 3, 6, 15),
            diagnostic(parsing, 4, 1, 5, "unexpected modifier 'open' on struct declaration",
                listOf(hint(parsing, 4, 6, 12, "on struct declaration"))),
        ))
        val lexical = "package modifier_lex_error_gates_warning\n\nopen interface I {}\n\\\n"
        val point = offset(lexical, 4, 1)
        assertBoth(lexical, listOf(ParsingDiagnostic(point, point, "unknown start of token: \\",
            category = ParsingDiagnostic.Category.LEX, rangeKind = ParsingDiagnostic.RangeKind.POINT)))
    }

    /** unsafe 在已实际执行的 CJNATIVE control 不发 warning；无冗余来源也不制造 warning。 */
    @Test
    fun nativeUnsafeAndNonredundantControlsDoNotInventWarnings() {
        assertBoth("package modifier_unsafe_native_control\n\nunsafe func f(): Unit {}\n", emptyList())
        assertBoth("package modifier_nonredundant_valid\n\npublic open class C {}\npublic interface I {\n" +
            "    func f(): Unit\n    prop p: Int64 { get() { 7 } }\n}\npublic struct S {}\npublic func f(): Unit {}\n", emptyList())
    }

    /** 同一真实输入的 PSI 发布覆盖与 LT 调用配置等价；抑制只影响输出，保留完整 producer 报告。 */
    @Test
    fun psiPublicationOverrideAndLightInvocationSettingsPreserveReportsAndErrors() {
        val warningOnly = "package modifier_warn_off_parser_valid\n\npublic sealed open abstract class C {}\n"
        val invalid = "package modifier_parse_error_keeps_warning\n\nopen interface I {}\nopen struct S {}\n"
        val cases: List<Pair<String, List<ParsingDiagnostic>>> = listOf(
            warningOnly to emptyList(),
            invalid to listOf(diagnostic(invalid, 4, 1, 5, "unexpected modifier 'open' on struct declaration",
                listOf(hint(invalid, 4, 6, 12, "on struct declaration")))),
        )
        for ((source, expected) in cases) {
            val file = createPsiFile("warningSettings", source) as CjFile
            val original = file.collectParsingDiagnosticPublication()
            val seed = publishedParserSeed(file)
            val view = file.currentPsiSourceView()
            val root = file.node
            assertEquals(2, original.diagnostics.size)
            assertTrue(original.diagnostics.any { it.severity == ParsingDiagnostic.Severity.WARNING })
            assertEquals(expected.size, original.diagnostics.count { it.isError })

            for (diagnostics in listOf(
                ParsingDiagnosticSettings(disableWarnings = true),
                ParsingDiagnosticSettings(suppressedGroups = setOf(WarningGroup.PARSER)),
            )) {
                val overridden = file.collectParsingDiagnosticPublication(settings = diagnostics)
                val settings = seed.settings.copy(diagnostics = diagnostics)
                val light = CangJieLightParser.parseBuffered(CangJieLightParser.createBuilder(source),
                    languageModuleName = seed.languageModuleName, sourceKind = seed.sourceKind,
                    cjmpContext = seed.cjmpContext, lexicalInput = view.lexicalInput, settings = settings)

                assertLegacyParsingDiagnostics(expected, overridden.diagnostics, view.sourceContext)
                assertPublishedParsingDiagnosticParity(overridden.diagnostics, light.diagnostics, view.sourceContext)
                assertEquals(overridden.diagnostics, file.collectParsingDiagnostics(settings = diagnostics))
                assertEquals(settings, light.file.settings)
                assertSame(view.lexicalInput, light.file.lexicalInput)
                assertSame(view.sourceContext.snapshot, light.file.sourceContext.snapshot)
                assertEquals(original.reports, overridden.reports)
                assertEquals(original.reports.map { it.diagnostic }, light.file.diagnosticPublication.reports.map { it.diagnostic })

                val republished = file.collectParsingDiagnosticPublication()
                assertEquals(original.diagnostics, republished.diagnostics)
                assertEquals(original.reports, republished.reports)
                assertSame(seed, publishedParserSeed(file))
                assertSame(root, file.node)
                view.requireCurrent()
            }
        }
    }

    @Test
    fun warningOnlyPublicationDoesNotCreatePsiErrorsOrReportHasErrors() {
        val file = createPsiFile("warningOnly", "package modifier_interface_open_implicit\n\nopen interface I {}\n") as CjFile
        assertEquals(1, file.collectParsingDiagnostics().size)
        assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
        val messages = RecordingMessages()

        assertFalse(messages.reportSyntaxErrors(file))

        assertFalse(messages.hasErrors())
        assertEquals(listOf(CompilerMessageSeverity.WARNING), messages.entries.map { it.first })
        val listenerMessages = RecordingMessages()
        val builder = CangJieLightParser.createBuilder(file.text)
        val light = parseLightWithPsiInput(file, builder)
        light.diagnostics.forEach(listenerMessages.toCangJieParsingErrorListener()::onError)
        assertEquals(messages.entries.map { it.first to it.second }, listenerMessages.entries.map { it.first to it.second })
        assertFalse(listenerMessages.hasErrors())
    }

    /** warning 与词元锚点一起回滚，SDK marker 重解析后只保留新代次的一份报告。 */
    @Test
    fun speculativeWarningRollsBackWithTheMarkerAndCanBeReportedByTheReparse() {
        for (precede in listOf(false, true)) {
            val context = CjParserSourceContext.complete(CLASS_SOURCE)
            val builder = SemanticWhitespaceAwarePsiBuilderImpl(CangJieLightParser.createBuilder(CLASS_SOURCE), context)
            val root = builder.mark()
            val speculation = builder.mark()
            captureAndConsume(builder)
            val original = warning(CLASS_SOURCE, 3, 8, 12, CjModifierKind.OPEN,
                ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED), "'sealed' implies 'open'", 3, 22, 28)
            builder.macroParsingJournal.reportParsingWarning(original.startOffset, original)
            if (precede) {
                speculation.done(CjNodeTypes.MODIFIER_LIST)
                speculation.precede().rollbackTo()
            } else speculation.rollbackTo()
            captureAndConsume(builder)
            builder.macroParsingJournal.reportParsingWarning(original.startOffset, original)
            root.done(CjNodeTypes.CJ_FILE)
            builder.lightTree
            val data = builder.macroParsingJournal.freeze(0)
            val stored = data.parsingReports.values.flatten().single()
            val collector = CjParsingDiagnosticCollector(context)
            collector.restoreReport(stored.report)
            val result = collector.finish()

            assertEquals(1, result.size)
            assertEquals(original.warning, result.single().warning)
            assertEquals(original.hints, result.single().hints)
            assertEquals(3 to 8, result.single().mainRange?.begin)
            assertEquals(3 to 12, result.single().mainRange?.end)
        }
    }

    /** 仅移动真实 open 词元；主 warning 位移而 sealed hint 不动，报告身份不由新的位置顺序替代。 */
    @Test
    fun psiTokenMoveProjectsWarningAndHintIndependentlyWithoutReparsing() {
        val file = createPsiFile("movedWarning", CLASS_SOURCE) as CjFile
        val original = file.collectParsingDiagnosticPublication()
        assertEquals(2, original.diagnostics.size)
        val declaration = PsiTreeUtil.findChildrenOfType(file, CjClass::class.java).single()
        val modifiers = kotlin.test.assertNotNull(declaration.modifierList).node
        val openToken = kotlin.test.assertNotNull(modifiers.findChildByType(CjTokens.OPEN_KEYWORD))
        val separator = kotlin.test.assertNotNull(openToken.treeNext)
        assertEquals(" ", separator.text)
        val sourceRoot = file.node
        WriteCommandAction.runWriteCommandAction(project) {
            modifiers.removeChild(separator)
            modifiers.removeChild(openToken)
            modifiers.addChild(openToken, modifiers.firstChildNode)
            modifiers.addChild(separator, openToken.treeNext)
        }

        val moved = file.collectParsingDiagnosticPublication()

        assertSame(sourceRoot, file.node)
        assertSame(openToken, modifiers.findChildByType(CjTokens.OPEN_KEYWORD))
        assertEquals("open public abstract sealed", modifiers.text.trimEnd())
        assertEquals(original.reports.map { it.order }, moved.reports.map { it.order })
        assertEquals(original.diagnostics.map { it.warning }.toSet(), moved.diagnostics.map { it.warning }.toSet())
        assertEquals(listOf(CjModifierKind.OPEN, CjModifierKind.PUBLIC), moved.diagnostics.map {
            (kotlin.test.assertNotNull(it.warning) as ParsingWarning.RedundantModifier).modifier
        })
        assertEquals(listOf(3 to 1, 3 to 6), moved.diagnostics.map { it.mainRange?.begin })
        assertEquals(listOf(3 to 5, 3 to 12), moved.diagnostics.map { it.mainRange?.end })
        moved.diagnostics.forEach { diagnostic ->
            assertEquals(3 to 22, diagnostic.nativeHints.single().range.begin)
            assertEquals(3 to 28, diagnostic.nativeHints.single().range.end)
            assertEquals(file.text, diagnostic.mainRange?.source?.text)
            assertSame(diagnostic.mainRange?.source, diagnostic.nativeHints.single().range.source)
        }
    }

    private fun assertBoth(text: String, expected: List<ParsingDiagnostic>) {
        for (context in listOf(CjmpParsingContext.SyntaxOnly, CjmpParsingContext.Compilation(false, false, true))) {
            val file = createPsiFile("modifierWarning", text) as CjFile
            file.cjmpParsingContext = context
            val psi = file.collectParsingDiagnosticPublication().diagnostics
            val light = parseLightWithPsiInput(file, CangJieLightParser.createBuilder(text)).diagnostics
            val source = kotlin.test.assertNotNull(publishedParserSeed(file).sourceContext)
            assertLegacyParsingDiagnostics(expected, psi, source)
            assertLegacyParsingDiagnostics(expected, light, source)
            assertPublishedParsingDiagnosticParity(psi, light, source)
            expected.zip(psi).forEach { (raw, published) ->
                assertEquals(raw.severity, published.severity)
                assertEquals(raw.warning, published.warning)
                if (raw.severity == ParsingDiagnostic.Severity.WARNING) {
                    assertFalse(published.isError)
                    assertEquals(raw.mainHint, published.mainHint)
                    assertEquals(listOf(SUPPRESSION_NOTE), published.notes)
                    assertEquals("parse_redundant_modifier", published.warning?.nativeKind)
                    assertEquals(WarningGroup.PARSER, published.warning?.group)
                    assertNull(published.nativeHints.single().label)
                }
            }
        }
    }

    /** 捕获真实 SDK 词元后报告事务事实；不创建虚拟的 lexer token 或 warning 节点。 */
    private fun captureAndConsume(builder: SemanticWhitespaceAwarePsiBuilderImpl) {
        val journal = builder.macroParsingJournal
        while (!builder.eof()) {
            val start = builder.currentOffset
            val token = journal.lexicalInput.tokenAt(start)
            journal.captureSourceToken(start, token.endOffset, kotlin.test.assertNotNull(builder.tokenType))
            builder.advanceLexer()
        }
    }

    private fun warning(
        text: String, line: Int, begin: Int, end: Int, modifier: CjModifierKind,
        reason: ParsingWarning.Redundancy, mainHint: String, hintLine: Int, hintBegin: Int, hintEnd: Int,
    ): ParsingDiagnostic {
        val spelling = if (modifier == CjModifierKind.PUBLIC) "public" else "open"
        return ParsingDiagnostic(offset(text, line, begin), offset(text, line, end), "redundant modifier: '$spelling'",
            severity = ParsingDiagnostic.Severity.WARNING,
            warning = ParsingWarning.RedundantModifier(modifier, reason),
            mainHint = mainHint, notes = listOf(SUPPRESSION_NOTE),
            hints = listOf(ParsingDiagnosticHint(offset(text, hintLine, hintBegin), offset(text, hintLine, hintEnd))))
    }

    private fun diagnostic(text: String, line: Int, begin: Int, end: Int, message: String,
        hints: List<ParsingDiagnosticHint>): ParsingDiagnostic =
        ParsingDiagnostic(offset(text, line, begin), offset(text, line, end), message, hints = hints)

    private fun hint(text: String, line: Int, begin: Int, end: Int, label: String): ParsingDiagnosticHint =
        ParsingDiagnosticHint(offset(text, line, begin), offset(text, line, end), label)

    private fun offset(text: String, line: Int, column: Int): Int =
        text.lines().take(line - 1).sumOf { it.length + 1 } + column - 1

    private class RecordingMessages : MessageCollector {
        val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        override fun clear() = entries.clear()
        override fun hasErrors(): Boolean = entries.any { it.first.isError }
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            entries += severity to message
        }
    }

    private companion object {
        const val SUPPRESSION_NOTE = "this warning can be suppressed by setting the compiler option `-Woff parser`"
        const val CLASS_SOURCE = "package modifier_class_two_warnings_priority\n\npublic open abstract sealed class C {}\n"
    }
}
