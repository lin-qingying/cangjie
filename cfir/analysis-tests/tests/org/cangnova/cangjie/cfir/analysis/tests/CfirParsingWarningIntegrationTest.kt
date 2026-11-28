package org.cangnova.cangjie.cfir.analysis.tests

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.WarningLevel
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.declarations.parsingFileFacts
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.cfir.declarations.parserSettings
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory2
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters2
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirFromCjFiles
import org.cangnova.cangjie.cfir.pipeline.buildPreMacroRawCfirViaLightTree
import org.cangnova.cangjie.cfir.pipeline.reportParsingDiagnostics
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.ConfigurationPipelineArtifact
import org.cangnova.cangjie.frontend.pipeline.DefaultCfirFrontendPipelineArtifact
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticSettings
import org.cangnova.cangjie.messages.ParsingWarning
import org.cangnova.cangjie.messages.WarningGroup
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CjModifierKind
import org.cangnova.cangjie.parsing.CjParserSettings
import org.cangnova.cangjie.parsing.CjParsingSourceRange
import org.cangnova.cangjie.test.CfirParser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertEquals as equalTo
import org.junit.jupiter.api.Assertions.assertFalse as falseValue
import org.junit.jupiter.api.Assertions.assertNull as nullValue
import org.junit.jupiter.api.Assertions.assertNotSame as differentFrom
import org.junit.jupiter.api.Assertions.assertSame as sameAs
import org.junit.jupiter.api.Assertions.assertTrue as trueValue
import org.junit.jupiter.api.Assertions.assertArrayEquals as arrayEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * 固定 modifier-warning-authority/29 的 v1.1.3 SDK 事实，验收真实 raw 产品进入 typed collector 的契约。
 * 语法警告由 parser 保存；测试不手造 CFIR 文件、诊断上下文、library 声明或 warning 工厂实例。
 * 使用 JUnit 5 断言别名及 present 值断言，避免继承 JUnit 同名方法遮蔽返回值。
 */
@OptIn(CompilerConfiguration.Internals::class)
class CfirParsingWarningIntegrationTest : AbstractCfirAnalysisTestCase() {
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun explicitModifierWarningsKeepFactoryIdentityNativePayloadAndWErrorState() {
        for (parser in CfirParser.entries) {
            val files = buildRawFiles(parser, listOf(InputSource("explicit.cj", CLASS_SOURCE)))
            val stored = publication(files.single())
            val collector = DiagnosticsCollectorImpl()

            val published = collector.reportParsingDiagnostics(files)

            equalTo(stored.reports, published.reports)
            equalTo(stored.diagnostics, published.diagnostics)
            equalTo(2, collector.diagnostics.size)
            val publicWarning = published.diagnostics[0]
            val openWarning = published.diagnostics[1]
            assertNativeWarning(publicWarning, CLASS_SOURCE, CjModifierKind.PUBLIC,
                ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED),
                3, 1, 7, "'sealed' implies 'public'", 3, 22, 28)
            assertNativeWarning(openWarning, CLASS_SOURCE, CjModifierKind.OPEN,
                ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED),
                3, 8, 12, "'sealed' implies 'open'", 3, 22, 28)
            assertTypedWarning(collector.diagnostics[0], publicWarning, CfirErrors.REDUNDANT_MODIFIER,
                CjTokens.PUBLIC_KEYWORD, CjTokens.SEALED_KEYWORD)
            assertTypedWarning(collector.diagnostics[1], openWarning, CfirErrors.REDUNDANT_MODIFIER,
                CjTokens.OPEN_KEYWORD, CjTokens.SEALED_KEYWORD)
            falseValue(collector.hasErrors)
            trueValue(collector.hasWarningsForWError)
            sameAs(stored, files.single().parsingDiagnosticPublication)
        }
    }

    /** interface 头也保留官方 interface member 文案；头和成员的 hint 均指向真实 interface token。 */
    @Test
    fun interfaceHeadAndMembersKeepTheImplicitTargetFactoryAndNativeHints() {
        for (parser in CfirParser.entries) {
            val head = buildRawFiles(parser, listOf(InputSource("head.cj", INTERFACE_HEAD_SOURCE)))
            val headCollector = DiagnosticsCollectorImpl()
            val headWarning = headCollector.reportParsingDiagnostics(head).diagnostics.single()
            assertNativeWarning(headWarning, INTERFACE_HEAD_SOURCE, CjModifierKind.OPEN,
                ParsingWarning.Redundancy.InterfaceMember,
                3, 1, 5, "interface member implies 'open'", 3, 6, 15)
            assertTypedWarning(headCollector.diagnostics.single(), headWarning, CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET,
                CjTokens.OPEN_KEYWORD, "interface member")

            val members = buildRawFiles(parser, listOf(InputSource("members.cj", INTERFACE_MEMBER_SOURCE)))
            val memberCollector = DiagnosticsCollectorImpl()
            val warnings = memberCollector.reportParsingDiagnostics(members).diagnostics
            equalTo(2, warnings.size)
            equalTo(2, memberCollector.diagnostics.size)
            assertNativeWarning(warnings[0], INTERFACE_MEMBER_SOURCE, CjModifierKind.PUBLIC,
                ParsingWarning.Redundancy.InterfaceMember,
                4, 5, 11, "interface member implies 'public'", 3, 1, 10)
            assertNativeWarning(warnings[1], INTERFACE_MEMBER_SOURCE, CjModifierKind.OPEN,
                ParsingWarning.Redundancy.InterfaceMember,
                4, 12, 16, "interface member implies 'open'", 3, 1, 10)
            assertTypedWarning(memberCollector.diagnostics[0], warnings[0], CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET,
                CjTokens.PUBLIC_KEYWORD, "interface member")
            assertTypedWarning(memberCollector.diagnostics[1], warnings[1], CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET,
                CjTokens.OPEN_KEYWORD, "interface member")
            falseValue(headCollector.hasErrors)
            falseValue(memberCollector.hasErrors)
            trueValue(headCollector.hasWarningsForWError)
            trueValue(memberCollector.hasWarningsForWError)
        }
    }

    /** factory 等级覆盖属于分析配置；原生 parser WARNING 的身份、载荷和 Werror 计数保持独立。 */
    @Test
    fun languageWarningLevelOverridesCannotRewriteOrSuppressNativeParserSeverity() {
        for (parser in CfirParser.entries) {
            for (level in WarningLevel.entries) {
                val levels = mapOf(
                    CfirErrors.REDUNDANT_MODIFIER.name to level,
                    CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET.name to level,
                )
                val files = buildRawFiles(parser, listOf(
                    InputSource("explicit.cj", CLASS_SOURCE),
                    InputSource("implicit.cj", INTERFACE_HEAD_SOURCE),
                ), warningLevels = levels)
                val reports = files.flatMap { publication(it).reports }
                val collector = DiagnosticsCollectorImpl()

                val published = collector.reportParsingDiagnostics(files)

                equalTo(3, published.diagnostics.size)
                equalTo(3, collector.diagnostics.size)
                equalTo(reports, published.reports)
                equalTo(setOf(CfirErrors.REDUNDANT_MODIFIER, CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET),
                    collector.diagnostics.map { it.factory }.toSet())
                collector.diagnostics.forEach { typed ->
                    equalTo(levels, typed.context.languageVersionSettings.getFlag(AnalysisFlags.warningLevels))
                    equalTo(Severity.WARNING, typed.severity)
                    val original = present(typed.parsingDiagnostic)
                    equalTo(ParsingDiagnostic.Severity.WARNING, original.severity)
                    sameAs(original, reports.single { it.diagnostic === original }.diagnostic)
                    equalTo(original.message, typed.renderMessage())
                }
                falseValue(collector.hasErrors)
                trueValue(collector.hasWarningsForWError)
            }
        }
    }

    @Test
    fun parserGroupPublicationOverrideSuppressesTypedWarningsWithoutLosingProducerReports() {
        val suppressed = ParsingDiagnosticSettings(suppressedGroups = setOf(WarningGroup.PARSER))
        for (parser in CfirParser.entries) {
            val files = buildRawFiles(parser, listOf(InputSource("suppression.cj", CLASS_SOURCE)))
            val stored = publication(files.single())
            val collector = DiagnosticsCollectorImpl()

            val published = collector.reportParsingDiagnostics(files, settings = suppressed)

            trueValue(published.diagnostics.isEmpty())
            equalTo(stored.reports, published.reports)
            trueValue(collector.diagnostics.isEmpty())
            falseValue(collector.hasErrors)
            falseValue(collector.hasWarningsForWError)
            equalTo(2, stored.diagnostics.size)
            sameAs(stored, files.single().parsingDiagnosticPublication)

            val enabledCollector = DiagnosticsCollectorImpl()
            val enabled = enabledCollector.reportParsingDiagnostics(files)
            equalTo(stored.diagnostics, enabled.diagnostics)
            equalTo(stored.reports, enabled.reports)
            equalTo(2, enabledCollector.diagnostics.size)
            falseValue(enabledCollector.hasErrors)
            trueValue(enabledCollector.hasWarningsForWError)
        }
    }

    /** raw 调用策略随文件属性保存；本次发布覆盖可以重放原报告，不能为此修改 CFIR 属性。 */
    @Test
    fun rawInvocationSuppressionIsRetainedByFileAttributesAndHonoredByDefaultTypedReporting() {
        val settings = CjParserSettings(LanguageVersion.CANGJIE_1_1_3,
            diagnostics = ParsingDiagnosticSettings(suppressedGroups = setOf(WarningGroup.PARSER)))
        for (parser in CfirParser.entries) {
            val files = buildRawFiles(parser, listOf(InputSource("suppressedRaw.cj", CLASS_SOURCE)), settings = settings)
            val file = files.single()
            val stored = publication(file)
            equalTo(settings, file.parserSettings)
            trueValue(stored.diagnostics.isEmpty())
            equalTo(2, stored.reports.size)
            trueValue(stored.reports.all { it.diagnostic.severity == ParsingDiagnostic.Severity.WARNING })
            val collector = DiagnosticsCollectorImpl()

            val published = collector.reportParsingDiagnostics(files)

            trueValue(published.diagnostics.isEmpty())
            equalTo(stored.reports, published.reports)
            trueValue(collector.diagnostics.isEmpty())
            falseValue(collector.hasErrors)
            falseValue(collector.hasWarningsForWError)
            val enabledCollector = DiagnosticsCollectorImpl()
            val enabled = enabledCollector.reportParsingDiagnostics(files, settings = ParsingDiagnosticSettings())
            equalTo(2, enabled.diagnostics.size)
            equalTo(2, enabledCollector.diagnostics.size)
            equalTo(stored.reports, enabled.reports)
            falseValue(enabledCollector.hasErrors)
            trueValue(enabledCollector.hasWarningsForWError)
            sameAs(stored, file.parsingDiagnosticPublication)
            equalTo(settings, file.parserSettings)
        }
    }

    /** LEX 门由整个编译单元的完整报告决定；另一个文件的合法 WARNING 不能先送入 typed collector。 */
    @Test
    fun lexicalErrorInAnotherRawFileGatesTypedWarningsAcrossTheCompilationUnit() {
        for (parser in CfirParser.entries) {
            val files = buildRawFiles(parser, listOf(
                InputSource("warning.cj", LEX_GATE_WARNING_SOURCE),
                InputSource("lex.cj", LEX_GATE_ERROR_SOURCE),
            ))
            val warningFile = files[0]
            val warningReports = publication(warningFile)
            equalTo(1, warningReports.diagnostics.size)
            equalTo(ParsingDiagnostic.Severity.WARNING, warningReports.diagnostics.single().severity)
            val reports = files.flatMap { publication(it).reports }
            val collector = DiagnosticsCollectorImpl()

            val published = collector.reportParsingDiagnostics(files)

            val lexical = published.diagnostics.single()
            equalTo(ParsingDiagnostic.Category.LEX, lexical.category)
            trueValue(lexical.isError)
            equalTo("unknown start of token: \\", lexical.message)
            equalTo(3 to 1, lexical.mainRange?.begin)
            equalTo(3 to 2, lexical.mainRange?.end)
            equalTo(LEX_GATE_ERROR_SOURCE, lexical.mainRange?.source?.text)
            nullValue(lexical.warning)
            equalTo(reports, published.reports)
            trueValue(collector.diagnostics.isEmpty())
            falseValue(collector.hasWarningsForWError)
            sameAs(warningReports, warningFile.parsingDiagnosticPublication)
        }
    }

    @Test
    fun psiCjoConsumptionDoesNotReplayTheLibrarySourceWarning() {
        assertCjoWarningOwnership(CfirParser.Psi)
    }

    @Test
    fun lightTreeCjoConsumptionDoesNotReplayTheLibrarySourceWarning() {
        assertCjoWarningOwnership(CfirParser.LightTree)
    }

    /** 正式 public raw 入口建立 CFIR 产品；pre 包装和文件属性必须持有同一次完整 parser publication。 */
    private fun buildRawFiles(
        parser: CfirParser,
        inputs: List<InputSource>,
        settings: CjParserSettings = CjParserSettings(LanguageVersion.CANGJIE_1_1_3),
        warningLevels: Map<String, WarningLevel> = emptyMap(),
    ): List<CfirFile> {
        val session = createTestSession()
        session.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(
            LanguageVersionSettingsImpl(settings.languageVersion,
                analysisFlags = mapOf(AnalysisFlags.warningLevels to warningLevels)),
        ))
        val pre = when (parser) {
            CfirParser.Psi -> session.buildPreMacroRawCfirFromCjFiles(
                inputs.map { createCjFile(it.name.removeSuffix(".cj"), it.text) }, parserSettings = settings)
            CfirParser.LightTree -> session.buildPreMacroRawCfirViaLightTree(
                inputs.map { CjInMemoryTextSourceFile(it.name, null, it.text) }, parserSettings = settings)
        }
        sameAs(session, pre.session)
        equalTo(inputs.size, pre.files.size)
        return pre.files.zip(inputs).map { (built, input) ->
            val file = built.cfirFile
            sameAs(session, file.moduleData.session)
            sameAs(built.parsingDiagnosticPublication, file.parsingDiagnosticPublication)
            equalTo(settings, built.parserSettings)
            equalTo(settings, file.parserSettings)
            equalTo(CjSourceKind.SOURCE, present(file.parsingFileFacts).sourceKind)
            equalTo(input.text, present(file.parsingSourceContext).snapshot.text)
            file
        }
    }

    private fun publication(file: CfirFile): CjParsingDiagnosticPublication =
        present(file.parsingDiagnosticPublication)

    /** 独立核对 SDK 的 ASCII 行列和提示；不调用 parser/collector 的坐标或 warning 选择算法生成期望。 */
    private fun assertNativeWarning(
        diagnostic: ParsingDiagnostic,
        text: String,
        modifier: CjModifierKind,
        reason: ParsingWarning.Redundancy,
        line: Int,
        begin: Int,
        end: Int,
        mainHint: String,
        hintLine: Int,
        hintBegin: Int,
        hintEnd: Int,
    ) {
        val spelling = if (modifier == CjModifierKind.PUBLIC) "public" else "open"
        equalTo("redundant modifier: '$spelling'", diagnostic.message)
        equalTo(ParsingDiagnostic.Severity.WARNING, diagnostic.severity)
        falseValue(diagnostic.isError)
        equalTo(ParsingDiagnostic.Category.PARSE, diagnostic.category)
        equalTo(ParsingWarning.RedundantModifier(modifier, reason), diagnostic.warning)
        equalTo("parse_redundant_modifier", diagnostic.warning?.nativeKind)
        equalTo(WarningGroup.PARSER, diagnostic.warning?.group)
        equalTo(mainHint, diagnostic.mainHint)
        equalTo(listOf(SUPPRESSION_NOTE), diagnostic.notes)
        val range = present(diagnostic.mainRange)
        equalTo(text, range.source.text)
        equalTo(line to begin, range.begin)
        equalTo(line to end, range.end)
        equalTo(offset(text, line, begin), diagnostic.startOffset)
        equalTo(offset(text, line, end), diagnostic.endOffset)
        equalTo(CjParsingSourceRange(range.source, diagnostic.startOffset, diagnostic.endOffset), range.sourceRange)
        val hint = diagnostic.hints.single()
        equalTo(offset(text, hintLine, hintBegin), hint.startOffset)
        equalTo(offset(text, hintLine, hintEnd), hint.endOffset)
        nullValue(hint.label)
        val nativeHint = diagnostic.nativeHints.single()
        sameAs(range.source, nativeHint.range.source)
        equalTo(hintLine to hintBegin, nativeHint.range.begin)
        equalTo(hintLine to hintEnd, nativeHint.range.end)
        equalTo(CjParsingSourceRange(range.source, hint.startOffset, hint.endOffset), nativeHint.range.sourceRange)
        nullValue(nativeHint.label)
    }

    /** 参数期望由既有工厂的泛型契约约束，原生报告必须作为同一个载荷进入 typed 诊断。 */
    private fun <A, B> assertTypedWarning(
        diagnostic: CjDiagnostic,
        parsing: ParsingDiagnostic,
        factory: CjDiagnosticFactory2<A, B>,
        modifier: A,
        target: B,
    ) {
        sameAs(factory, diagnostic.factory)
        equalTo(Severity.WARNING, diagnostic.severity)
        equalTo(parsing.message, diagnostic.renderMessage())
        sameAs(parsing, diagnostic.parsingDiagnostic)
        sameAs(parsing, diagnostic.context.parsingDiagnostic)
        equalTo(TextRange(parsing.startOffset, parsing.endOffset), diagnostic.firstRange)
        val arguments = org.junit.jupiter.api.Assertions.assertInstanceOf(CjDiagnosticWithParameters2::class.java, diagnostic)
        equalTo(modifier, arguments.a)
        equalTo(target, arguments.b)
    }

    /** 实际 SOURCE 写出 CJO，再由独立 consumer session 加载；两个消费对照的原文均有 SDK 终态证据。 */
    private fun assertCjoWarningOwnership(parser: CfirParser) {
        val root = Files.createDirectories(tempDir.resolve(parser.name))
        val disposable = Disposer.newDisposable("typed-parser-warning-cjo")
        try {
            val output = root.resolve("out")
            val producer = compileFrontend(parser, root.resolve("library"), CJO_LIBRARY_SOURCE, disposable) {
                cjoOutputDirectory = output.toString()
            }
            val producerFile = producer.artifact.frontendOutput.outputs.single().fir.single()
            val producerWarning = producer.diagnostics.diagnostics.single()
            val nativeProducerWarning = publication(producerFile).diagnostics.single()
            assertNativeWarning(nativeProducerWarning, CJO_LIBRARY_SOURCE, CjModifierKind.PUBLIC,
                ParsingWarning.Redundancy.InterfaceMember,
                4, 5, 11, "interface member implies 'public'", 3, 8, 17)
            assertTypedWarning(producerWarning, nativeProducerWarning, CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET,
                CjTokens.PUBLIC_KEYWORD, "interface member")
            falseValue(producer.diagnostics.hasErrors)
            trueValue(producer.diagnostics.hasWarningsForWError)
            val cjo = output.resolve(CjoConstants.packageNameToPath("modifier_cjo_library"))
            trueValue(Files.isRegularFile(cjo), "CJO 必须由真实 frontend writer 生成")
            val bytes = Files.readAllBytes(cjo)

            val consumer = compileFrontend(parser, root.resolve("consumer"), CJO_CONSUMER_SOURCE, disposable) {
                addClasspathRoot(output.toString())
            }
            trueValue(consumer.diagnostics.diagnostics.isEmpty())
            falseValue(consumer.diagnostics.hasErrors)
            falseValue(consumer.diagnostics.hasWarningsForWError)
            val consumerOutput = consumer.artifact.frontendOutput.outputs.single()
            val consumerFile = consumerOutput.fir.single()
            trueValue(publication(consumerFile).diagnostics.isEmpty())
            trueValue(publication(consumerFile).reports.isEmpty())
            differentFrom(producer.artifact.frontendOutput.outputs.single().session, consumerOutput.session)
            val classId = ClassId(FqName("modifier_cjo_library"), Name.identifier("I"))
            val loaded = present(consumerOutput.session.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId)).cfir
            equalTo(CfirDeclarationOrigin.Library, loaded.origin)
            val sourceOwner = producerFile.declarations.filterIsInstance<CfirInterface>().single()
            differentFrom(sourceOwner.symbol, loaded.symbol)
            equalTo(cjo.toAbsolutePath().normalize(),
                Path.of(present(loaded.cjoDeclarationOrigin).packagePath).toAbsolutePath().normalize())
            val use = consumerFile.declarations.filterIsInstance<CfirNamedFunction>().single()
            val parameterType = org.junit.jupiter.api.Assertions.assertInstanceOf(ConeClassLikeType::class.java,
                use.valueParameters.single().returnTypeRef.coneTypeOrNull)
            equalTo(classId, parameterType.lookupTag.classId)
            val replayCollector = DiagnosticsCollectorImpl()
            trueValue(replayCollector.reportParsingDiagnostics(consumerOutput.fir).diagnostics.isEmpty())
            trueValue(replayCollector.diagnostics.isEmpty())
            falseValue(replayCollector.hasWarningsForWError)

            val local = compileFrontend(parser, root.resolve("local"), CJO_LOCAL_WARNING_SOURCE, disposable) {
                addClasspathRoot(output.toString())
            }
            val localFile = local.artifact.frontendOutput.outputs.single().fir.single()
            val localWarning = local.diagnostics.diagnostics.single()
            val nativeLocalWarning = publication(localFile).diagnostics.single()
            assertNativeWarning(nativeLocalWarning, CJO_LOCAL_WARNING_SOURCE, CjModifierKind.PUBLIC,
                ParsingWarning.Redundancy.InterfaceMember,
                6, 5, 11, "interface member implies 'public'", 5, 8, 17)
            assertTypedWarning(localWarning, nativeLocalWarning, CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET,
                CjTokens.PUBLIC_KEYWORD, "interface member")
            falseValue(local.diagnostics.hasErrors)
            trueValue(local.diagnostics.hasWarningsForWError)
            arrayEqualTo(bytes, Files.readAllBytes(cjo))
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /** 沿用已有 CJO 测试的完整 frontend 配置，运行时临时输入与仓库 fixture 分离。 */
    private fun compileFrontend(
        parser: CfirParser,
        sourceRoot: Path,
        text: String,
        disposable: Disposable,
        configure: CompilerConfiguration.() -> Unit,
    ): Compilation {
        val source = Files.writeString(Files.createDirectories(sourceRoot).resolve("source.cj"), text, Charsets.UTF_8)
        val collector = DiagnosticsCollectorImpl()
        val messages = RecordingMessages()
        val configuration = CompilerConfiguration().apply {
            moduleName = "parsingWarningIntegration"
            languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
            diagnosticsCollector = collector
            messageCollector = messages
            useLightTree = parser == CfirParser.LightTree
            addCangJieSourceRoot(source.toString())
            configure()
        }
        val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
        falseValue(messages.hasErrors(), messages.entries.joinToString("\n"))
        falseValue(collector.hasErrors, collector.diagnostics.joinToString("\n") { "${it.factoryName}: ${it.renderMessage()}" })
        return Compilation(present(artifact, "正式 frontend 必须完成 SOURCE/CJO 对照"), collector)
    }

    private data class InputSource(val name: String, val text: String)
    private data class Compilation(val artifact: DefaultCfirFrontendPipelineArtifact, val diagnostics: DiagnosticsCollectorImpl)

    private class RecordingMessages : MessageCollector {
        val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        override fun clear() = entries.clear()
        override fun hasErrors(): Boolean = entries.any { it.first.isError }
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            entries += severity to message
        }
    }

    private fun offset(text: String, line: Int, column: Int): Int =
        text.lines().take(line - 1).sumOf { it.length + 1 } + column - 1

    private companion object {
        const val SUPPRESSION_NOTE = "this warning can be suppressed by setting the compiler option `-Woff parser`"
        const val CLASS_SOURCE = "package modifier_class_two_warnings_priority\n\npublic open abstract sealed class C {}\n"
        const val INTERFACE_HEAD_SOURCE = "package modifier_interface_open_implicit\n\nopen interface I {}\n"
        const val INTERFACE_MEMBER_SOURCE = "package modifier_interface_function_abstract\n\ninterface I {\n    public open func f(): Unit\n}\n"
        const val LEX_GATE_WARNING_SOURCE = "package modifier_lex_unit_gate\n\nopen interface I {}\n"
        const val LEX_GATE_ERROR_SOURCE = "package modifier_lex_unit_gate\n\n\\\n"
        const val CJO_LIBRARY_SOURCE = "package modifier_cjo_library\n\npublic interface I {\n    public func f(): Unit\n}\n"
        const val CJO_CONSUMER_SOURCE = "package modifier_cjo_client\n\nimport modifier_cjo_library.I\n\npublic func use(value: I): Unit { value.f() }\n"
        const val CJO_LOCAL_WARNING_SOURCE = "package modifier_cjo_local\n\nimport modifier_cjo_library.I\n\npublic interface J <: I {\n    public func g(): Unit\n}\n"
    }
    private fun <T : Any> present(value: T?, message: String? = null): T {
        org.junit.jupiter.api.Assertions.assertNotNull(value, message)
        return checkNotNull(value)
    }

}
