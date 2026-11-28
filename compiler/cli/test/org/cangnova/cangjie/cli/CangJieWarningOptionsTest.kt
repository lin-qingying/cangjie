package org.cangnova.cangjie.cli

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.declarations.parserSettings
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cli.common.arguments.parseCommandLineArguments
import org.cangnova.cangjie.config.*
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.arguments.Freezable
import org.cangnova.cangjie.frontend.pipeline.*
import org.cangnova.cangjie.messages.*
import org.cangnova.cangjie.phaser.CompilerPhase
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.PhaserState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * 固定 v1.1.3 OptionTable/WarningOptionMgr 的单值、别名与按序覆盖规则；普通 WARNING
 * 使用实际 PSI/LT parser。Werror 只检查内部 typed 计数，不添加官方不存在的命令行选项。
 */
@OptIn(CompilerConfiguration.Internals::class)
class CangJieWarningOptionsTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun allNativeSingleValuesAndAliasSpellingsRetainOrderedOccurrences() {
        for ((name, alias) in listOf("warn-off" to "-Woff", "warn-on" to "-Won")) {
            for (value in WarningGroup.commandLineValues) {
                for (tokens in listOf(listOf("--$name", value), listOf("--$name=$value"),
                    listOf(alias, value), listOf("$alias=$value"), listOf("$alias$value"))) {
                    val parsed = parseCommandLineArguments(tokens, Arguments())
                    assertFalse(parsed.hasErrors, "$tokens: ${parsed.issues}")
                    assertTrue(parsed.issues.isEmpty())
                    assertEquals(listOf(name to value), parsed.explicitArgumentOccurrences)
                    assertEquals(listOf(value), parsed.explicitArguments[name])
                    assertArrayEquals(arrayOf(value), if (name == "warn-off") parsed.arguments.warnOff else parsed.arguments.warnOn)
                }
            }
        }
    }

    @Test
    fun invalidValuesAndMissingValuesStopBeforeSourceCollection() {
        for (tokens in listOf(listOf("-Woff"), listOf("--warn-on"), listOf("-Woff=PARSER"),
            listOf("--warn-off=parser,semantics"), listOf("-Wonparse_redundant_modifier"),
            listOf("--warn-off=ungrouped"), listOf("--warn-on="), listOf("-Werror"),
            listOf("-nowarn"), listOf("--suppress-warnings"))) {
            val pipeline = ProbePipeline()
            val compiler = Compiler(pipeline)
            val result = compiler.exec(tokens, configuration())
            assertTrue(result.parsing.hasErrors, "$tokens")
            assertNull(result.frontendSucceeded)
            assertEquals(0, compiler.sourceCollections)
            assertEquals(0, pipeline.executions)
        }
    }

    @Test
    fun interleavedActionsWithIdenticalGroupedArraysProduceOppositeFlags() {
        val firstConfiguration = configuration()
        val secondConfiguration = configuration()
        val first = Compiler(ProbePipeline()).exec(listOf("-Woffall", "-Wonparser", "-Woffparser"), firstConfiguration)
        val second = Compiler(ProbePipeline()).exec(listOf("-Woffall", "-Woffparser", "-Wonparser"), secondConfiguration)
        assertEquals(true, first.frontendSucceeded)
        assertEquals(true, second.frontendSucceeded)
        assertArrayEquals(first.parsing.arguments.warnOff, second.parsing.arguments.warnOff)
        assertArrayEquals(first.parsing.arguments.warnOn, second.parsing.arguments.warnOn)
        assertEquals(first.parsing.explicitArguments, second.parsing.explicitArguments)
        assertNotEquals(first.parsing.explicitArgumentOccurrences, second.parsing.explicitArgumentOccurrences)
        assertTrue(firstConfiguration.parsingDiagnosticSettings.isSuppressed(WarningGroup.PARSER))
        assertFalse(secondConfiguration.parsingDiagnosticSettings.isSuppressed(WarningGroup.PARSER))
        assertTrue(secondConfiguration.parsingDiagnosticSettings.isSuppressed(WarningGroup.UNGROUPED))
        assertTrue(secondConfiguration.parsingDiagnosticSettings.isSuppressed(WarningGroup.DRIVER_ARG))
    }

    @Test
    fun driverWarningActionsPrecedePositionalInputsEvenWhenWrittenLast() {
        val source = Files.writeString(directory.resolve("driver.cj"), "package sample\n", Charsets.UTF_8)
        val unused = Files.writeString(directory.resolve("unused.txt"), "unused", Charsets.UTF_8)
        for ((options, suppressed) in listOf(emptyList<String>() to false, listOf("-Woffdriver-arg") to true,
            listOf("-Woffall", "-Wonparser") to true, listOf("-Woffdriver-arg", "-Wonall") to false)) {
            val configuration = configuration()
            val result = Compiler(ProbePipeline()).exec(listOf(source.toString(), unused.toString()) + options, configuration)
            assertEquals(true, result.frontendSucceeded)
            assertEquals(if (suppressed) 0 else 1, messages(configuration).entries.count { it.severity == CompilerMessageSeverity.WARNING })
            assertEquals(listOf(source.toString()), configuration.cangjieSourceRoots.map { it.path })
        }
    }

    @Test
    fun reusedConfigurationDoesNotInheritPreviousWarningGroupFlags() {
        val configuration = configuration()
        val compiler = Compiler(ProbePipeline())
        assertEquals(true, compiler.exec(listOf("-Woffall"), configuration).frontendSucceeded)
        assertEquals(WarningGroup.entries.toSet(), configuration.parsingDiagnosticSettings.suppressedGroups)
        assertEquals(true, compiler.exec(emptyList(), configuration).frontendSucceeded)
        assertTrue(configuration.parsingDiagnosticSettings.suppressedGroups.isEmpty())
        configuration.parsingDiagnosticSettings = ParsingDiagnosticSettings(disableWarnings = true)
        assertEquals(true, compiler.exec(listOf("-Wonall"), configuration).frontendSucceeded)
        assertTrue(configuration.parsingDiagnosticSettings.disableWarnings)
        assertTrue(configuration.parsingDiagnosticSettings.suppressedGroups.isEmpty())
    }

    @Test
    fun directPipelineRequiresExplicitOrderAndFrozenMetadataRetainsThatOrder() {
        val pipeline = ProbePipeline()
        val arguments = Arguments().apply { warnOff = arrayOf("all", "parser"); warnOn = arrayOf("parser") }
        assertFalse(pipeline.execute(arguments, configuration()))
        assertEquals(0, pipeline.executions)
        val occurrences = mutableListOf("warn-off" to "all", "warn-off" to "parser", "warn-on" to "parser")
        arguments.explicitArgumentOccurrences = occurrences
        occurrences.clear()
        arguments.freeze()
        assertThrows(IllegalStateException::class.java) { arguments.explicitArgumentOccurrences = emptyList() }
        assertThrows(UnsupportedOperationException::class.java) {
            (arguments.explicitArgumentOccurrences as MutableList<Pair<String, String>>).clear()
        }
        val copy = arguments.copyOf() as Arguments
        assertEquals(arguments.explicitArgumentOccurrences, copy.explicitArgumentOccurrences)
        val configuration = configuration()
        assertTrue(pipeline.execute(copy, configuration))
        assertFalse(configuration.parsingDiagnosticSettings.isSuppressed(WarningGroup.PARSER))
        assertTrue(configuration.parsingDiagnosticSettings.isSuppressed(WarningGroup.UNGROUPED))
    }

    @Test
    fun realPsiAndLightTreeCliPublishOnlyEnabledParserWarnings() {
        val cases = listOf(emptyList<String>() to true, listOf("-Woffparser") to false,
            listOf("-Woffall", "-Wonparser") to true, listOf("-Wonparser", "-Woffall") to false,
            listOf("-Woffparser", "-Wonall") to true, listOf("-Wonparser", "-Woffparser") to false)
        for (lightTree in listOf(false, true)) {
            for ((index, case) in cases.withIndex()) {
                val text = "package warning_native\nopen interface I{}\n"
                val source = Files.writeString(directory.resolve("native-$lightTree-$index.cj"), text, Charsets.UTF_8)
                withRealCompiler(lightTree) { compiler, pipeline, configuration ->
                    val result = compiler.exec(case.first + source.toString(), configuration)
                    assertEquals(true, result.frontendSucceeded, messages(configuration).entries.toString())
                    val collector = configuration.diagnosticsCollector as DiagnosticsCollectorImpl
                    assertFalse(collector.hasErrors)
                    assertEquals(case.second, collector.hasWarningsForWError)
                    assertEquals(if (case.second) 1 else 0, collector.diagnostics.size)
                    assertEquals(if (case.second) 1 else 0,
                        messages(configuration).entries.count { it.severity == CompilerMessageSeverity.WARNING })
                    val file = checkNotNull(pipeline.artifact).frontendOutput.outputs.single().fir.single()
                    assertEquals(configuration.parsingDiagnosticSettings, checkNotNull(file.parserSettings).diagnostics)
                    val raw = checkNotNull(file.parsingDiagnosticPublication)
                    assertEquals(1, raw.reports.size, "原始 producer 报告必须保留，即使调用关闭该组")
                    if (case.second) {
                        val typed = collector.diagnostics.single()
                        assertSame(CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET, typed.factory)
                        assertEquals(Severity.WARNING, typed.severity)
                        val parsing = checkNotNull(typed.parsingDiagnostic)
                        assertEquals("parse_redundant_modifier", checkNotNull(parsing.warning).nativeKind)
                        assertEquals(WarningGroup.PARSER, parsing.warning?.group)
                        assertEquals("redundant modifier: 'open'", parsing.message)
                        assertEquals("interface member implies 'open'", parsing.mainHint)
                        assertEquals(2 to 1, checkNotNull(parsing.mainRange).begin)
                        assertEquals(2 to 5, checkNotNull(parsing.mainRange).end)
                        val range = checkNotNull(checkNotNull(parsing.mainRange).sourceRange)
                        assertEquals(text.indexOf("open"), range.startOffset)
                        assertEquals(text.indexOf("open") + 4, range.endOffset)
                        assertSame(range.source, parsing.nativeHints.single().range.sourceRange?.source)
                        assertEquals(listOf("this warning can be suppressed by setting the compiler option `-Woff parser`"), parsing.notes)
                    }
                }
            }
        }
    }

    @Test
    fun parseErrorsKeepWarningsAndCrossFileLexErrorsCloseTheWholeParseUnit() {
        for (lightTree in listOf(false, true)) {
            val source = Files.writeString(directory.resolve("failure-$lightTree.cj"),
                "package warning_failure\nopen interface I{}\nfunc broken{}\n", Charsets.UTF_8)
            withRealCompiler(lightTree) { compiler, _, configuration ->
                assertEquals(false, compiler.exec(listOf(source.toString()), configuration).frontendSucceeded)
                val collector = configuration.diagnosticsCollector as DiagnosticsCollectorImpl
                assertEquals(1, collector.diagnostics.size)
                assertTrue(collector.hasWarningsForWError)
                assertTrue(messages(configuration).hasErrors())
                assertEquals(1, messages(configuration).entries.count { it.severity == CompilerMessageSeverity.WARNING })
            }
            val warningSource = Files.writeString(directory.resolve("warning-$lightTree.cj"),
                "package warning_failure\nopen interface I{}\n", Charsets.UTF_8)
            val lexicalSource = Files.writeString(directory.resolve("lex-$lightTree.cj"),
                "package warning_failure\n\\", Charsets.UTF_8)
            withRealCompiler(lightTree) { compiler, _, configuration ->
                assertEquals(false, compiler.exec(listOf(warningSource.toString(), lexicalSource.toString()), configuration).frontendSucceeded)
                val collector = configuration.diagnosticsCollector as DiagnosticsCollectorImpl
                assertTrue(collector.diagnostics.isEmpty())
                assertFalse(collector.hasWarningsForWError)
                assertTrue(messages(configuration).hasErrors())
                assertTrue(messages(configuration).entries.none { it.severity == CompilerMessageSeverity.WARNING })
                assertTrue(messages(configuration).entries.any { it.severity.isError && it.message == "unknown start of token: \\" })
            }
        }
    }

    private fun configuration(): CompilerConfiguration = CompilerConfiguration.create(messageCollector = Messages())

    /** help/version 型提前退出允许成功；任何退出信号都不能覆盖已经报告的真实 ERROR。 */
    @Test
    fun bothTerminationSignalsRespectErrorsAndPermitWarningOnlyExit() {
        for (successSignal in listOf(false, true)) {
            for (severity in listOf(CompilerMessageSeverity.INFO, CompilerMessageSeverity.WARNING, CompilerMessageSeverity.ERROR)) {
                val pipeline = object : AbstractFrontendPipeline<Arguments>() {
                    override fun createCompoundPhase(arguments: Arguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<Arguments>, *> =
                        object : PipelinePhase<ArgumentsPipelineArtifact<Arguments>, ArgumentsPipelineArtifact<Arguments>>(name = "earlyExit") {
                            override fun executePhase(input: ArgumentsPipelineArtifact<Arguments>): ArgumentsPipelineArtifact<Arguments>? {
                                input.configuration.messageCollector.report(severity, "early exit", null)
                                if (successSignal) throw SuccessfulPipelineExecutionException()
                                return null
                            }
                        }
                }
                val result = Compiler(pipeline).exec(emptyList(), configuration())
                assertEquals(!severity.isError, result.frontendSucceeded, "$successSignal/$severity")
            }
        }
    }

    private fun messages(configuration: CompilerConfiguration): Messages = configuration.messageCollector as Messages

    private fun withRealCompiler(lightTree: Boolean, action: (Compiler, RealPipeline, CompilerConfiguration) -> Unit) {
        val disposable = Disposer.newDisposable("native-warning-cli")
        try {
            val pipeline = RealPipeline(disposable)
            val configuration = configuration().apply { useLightTree = lightTree }
            action(Compiler(pipeline), pipeline, configuration)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private class Arguments : CommonCompilerArguments() {
        init { languageVersion = "1.1.3" }
        override fun copyOf(): Freezable = Arguments().also {
            it.languageVersion = languageVersion
            it.warnOff = warnOff.copyOf()
            it.warnOn = warnOn.copyOf()
            it.explicitArgumentOccurrences = explicitArgumentOccurrences
        }
    }

    /** 参数和路径处理使用生产 CLI；probe 只观测共享配置进入实际 frontend 相位的时点。 */
    private class Compiler(pipeline: AbstractFrontendPipeline<Arguments>) : CangJieFrontendCLICompiler<Arguments>(
        pipeline, legacyCjmpCompilationDriver = LegacyCjmpCompilationDriver { _, _, _, _ -> error("No legacy CHIR input") },
    ) {
        var sourceCollections = 0
        override fun createArguments(): Arguments = Arguments()
        override fun configureSourceArguments(freeArguments: List<String>, configuration: CompilerConfiguration): Boolean {
            sourceCollections++
            configuration.moduleName = "warning_cli"
            configuration.addCangJieSourceRoots(freeArguments)
            return true
        }
    }

    private class ProbePipeline : AbstractFrontendPipeline<Arguments>() {
        var executions = 0
        override fun createCompoundPhase(arguments: Arguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<Arguments>, *> =
            object : PipelinePhase<ArgumentsPipelineArtifact<Arguments>, ArgumentsPipelineArtifact<Arguments>>(name = "warningOptions") {
                override fun executePhase(input: ArgumentsPipelineArtifact<Arguments>): ArgumentsPipelineArtifact<Arguments> {
                    executions++
                    return input
                }
            }
    }

    private class RealPipeline(private val disposable: Disposable) : AbstractFrontendPipeline<Arguments>() {
        var artifact: DefaultCfirFrontendPipelineArtifact? = null
        override fun createCompoundPhase(arguments: Arguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<Arguments>, *> =
            object : CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<Arguments>, DefaultCfirFrontendPipelineArtifact> {
                override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: PipelineContext,
                    input: ArgumentsPipelineArtifact<Arguments>): DefaultCfirFrontendPipelineArtifact =
                    CfirFrontendPipelinePhase.invoke(phaseConfig, phaserState, context,
                        ConfigurationPipelineArtifact(input.configuration, disposable)).also { artifact = it }
                override fun getNamedSubphases(startDepth: Int) = CfirFrontendPipelinePhase.getNamedSubphases(startDepth)
            }
    }

    private data class Message(val severity: CompilerMessageSeverity, val message: String)
    private class Messages : MessageCollector {
        val entries = mutableListOf<Message>()
        override fun clear() = entries.clear()
        override fun hasErrors(): Boolean = entries.any { it.severity.isError }
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            entries += Message(severity, message)
        }
    }
}
