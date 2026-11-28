package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.PackageKind
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.declarations.parsingFileFacts
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.cfir.declarations.parserSettings
import org.cangnova.cangjie.cfir.declarations.replaceParsingSourceContext
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.pipeline.collectParsingDiagnosticPublication
import org.cangnova.cangjie.cfir.pipeline.reportParsingDiagnostics
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallForestBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentInput
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentPayload
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroParsingDiagnosticRecord
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroReplaceSlot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroStableSpliceResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroStableSplicer
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.bindMacroImports
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildMacroSymbolIndex
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.finalizeImplicitConstructorDeclarations
import org.cangnova.cangjie.cfir.resolve.providers.macro.materializePrimaryConstructorParameterFields
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroLibraryLoadResult
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.messages.CompilerMessageLocationWithRelatedLocations
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingWarning
import org.cangnova.cangjie.messages.WarningGroup
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CjModifierKind
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.junit.jupiter.api.Assertions.assertEquals as equalTo
import org.junit.jupiter.api.Assertions.assertFalse as falseValue
import org.junit.jupiter.api.Assertions.assertInstanceOf as instanceOf
import org.junit.jupiter.api.Assertions.assertNotSame as differentFrom
import org.junit.jupiter.api.Assertions.assertNull as nullValue
import org.junit.jupiter.api.Assertions.assertSame as sameAs
import org.junit.jupiter.api.Assertions.assertThrows as throwsType
import org.junit.jupiter.api.Assertions.assertTrue as trueValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CancellationException

/**
 * v1.1.3 的 modifier-warning-authority/mapping.md、native-v1.1.3.json，以及
 * 20261114-cjmp-parser-warning-edge-authority、20261114-cjmp-backslash-lexer-boundary-authority
 * 固定本类的语言期望：显式 sealed 冗余分支不受 ignoreScopeCheck 抑制，WARNING 不导致失败，
 * LEX ERROR 屏蔽整个单元的 PARSE 输出。成功只指 macro construction，不代替后续语义检查。
 * 所有 warning 均由生产 parser 产生；不构造 empty/duplicate attribute producer 或诊断 DTO。
 */
@OptIn(CompilerConfiguration.Internals::class, CfirImplementationDetail::class)
class MacroParsingWarningPublicationTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun successfulMacroWarningRetainsNativePayloadOrderAndSourceMapAfterSeal() = withParsers { project, lightTree ->
        val fixture = fixture(project, lightTree)
        val executor = OutputExecutor(listOf(WARNING_OUTPUT))
        val parsed = mutableListOf<MacroFragmentResult>()
        val configuration = configuration(project, lightTree, executor) { input, result ->
            sameAs(fixture.pre.files.single().annotationMetadata, input.annotationMetadata)
            nullValue(fixture.pre.session.macroExpansionRegistry)
            trueValue(collectParsingDiagnosticPublication(fixture.files).diagnostics.isEmpty())
            parsed += result
        }

        val result = instanceOf(MacroConstructionResult.Success::class.java, expand(fixture.pre, configuration))
        val fragment = instanceOf(MacroFragmentResult.Success::class.java, parsed.single())
        val original = fragment.diagnosticPublication.diagnostics.single()
        assertExplicitWarning(original)
        equalTo(0, original.startOffset)
        equalTo(4, original.endOffset)
        equalTo(1 to 1, present(original.mainRange).begin)
        equalTo(1 to 5, present(original.mainRange).end)
        equalTo(5, original.hints.single().startOffset)
        equalTo(11, original.hints.single().endOffset)
        equalTo(1 to 6, original.nativeHints.single().range.begin)
        equalTo(1 to 12, original.nativeHints.single().range.end)

        val registry = result.registry
        sameAs(registry, fixture.pre.session.macroExpansionRegistry)
        falseValue(registry.hasErrors)
        trueValue(registry.parsingFailures.isEmpty())
        trueValue(registry.diagnostics.isEmpty())
        val record = registry.parsingReports.single()
        sameAs(fragment.originNode, record.originNode)
        sameAs(fragment.diagnosticPublication, record.diagnosticPublication)
        sameAs(fragment.sourceMap, record.sourceMap)
        sameAs(fixture.files.single(), registry.originFileForSurface(record.originNode.surface.surfaceId))
        equalTo(Name.identifier("C"), fixture.files.single().declarations.filterIsInstance<CfirClass>().single().name)
        sameAs(fixture.files.single(), result.recordableFiles.files.single())
        // SEALED 可查询但不能再次封闭；验证报告不是在 seal 之前冒充最终产品。
        fixture.pre.files.single().annotationMetadata.snapshots
        throwsType(IllegalStateException::class.java) {
            fixture.pre.files.single().annotationMetadata.seal(fixture.files)
        }

        val typed = DiagnosticsCollectorImpl()
        val publication = typed.reportParsingDiagnostics(fixture.files)
        val published = publication.diagnostics.single()
        assertExplicitWarning(published)
        sameAs(fragment.diagnosticPublication.reports.single().order, publication.reports.single().order)
        val sourceMap = present(fragment.sourceMap)
        val mappedMain = present(sourceMap.mapRange(original.startOffset, original.endOffset))
        val mappedHint = present(sourceMap.mapRange(original.hints.single().startOffset, original.hints.single().endOffset))
        equalTo(ONE_MACRO.indexOf('@'), mappedMain.startOffset)
        equalTo(ONE_MACRO.indexOf('}') + 1, mappedMain.endOffset)
        equalTo(mappedMain.startOffset, mappedHint.startOffset)
        equalTo(mappedMain.endOffset, mappedHint.endOffset)
        val host = present(fixture.files.single().parsingSourceContext).snapshot
        val mainRange = present(present(published.mainRange).sourceRange)
        val hintRange = present(published.nativeHints.single().range.sourceRange)
        sameAs(host, mainRange.source)
        sameAs(host, hintRange.source)
        equalTo(mappedMain.startOffset, mainRange.startOffset)
        equalTo(mappedMain.endOffset, mainRange.endOffset)
        equalTo(mappedHint.startOffset, hintRange.startOffset)
        equalTo(mappedHint.endOffset, hintRange.endOffset)
        equalTo(original.hints.single().label, published.nativeHints.single().label)
        val diagnostic = typed.diagnostics.single()
        sameAs(CfirErrors.REDUNDANT_MODIFIER, diagnostic.factory)
        sameAs(published, diagnostic.parsingDiagnostic)
        equalTo(published.message, diagnostic.renderMessage())
        equalTo(Severity.WARNING, diagnostic.severity)
        falseValue(typed.hasErrors)
        trueValue(typed.hasWarningsForWError)
        equalTo(1, executor.executionCount)
        equalTo(1, executor.closeCount)
    }

    /** 相同消息来自不同 surface 时仍各报一次，并保留两次真实 parser 的独立报告身份和宿主顺序。 */
    @Test
    fun successfulSiblingWarningsRetainBothProducerOrdersAndDistinctHostRanges() = withParsers { project, lightTree ->
        val fixture = fixture(project, lightTree, TWO_MACROS)
        val parsed = mutableListOf<MacroFragmentResult.Success>()
        val configuration = configuration(project, lightTree,
            OutputExecutor(listOf(WARNING_OUTPUT, "open sealed class D{}"))) { _, result ->
            parsed += instanceOf(MacroFragmentResult.Success::class.java, result)
        }
        val result = instanceOf(MacroConstructionResult.Success::class.java, expand(fixture.pre, configuration))
        equalTo(2, result.registry.parsingReports.size)
        val typed = DiagnosticsCollectorImpl()
        val publication = typed.reportParsingDiagnostics(fixture.files)
        equalTo(2, publication.diagnostics.size)
        equalTo(2, typed.diagnostics.size)
        val originalReports = parsed.flatMap { it.diagnosticPublication.reports }
        equalTo(2, originalReports.size)
        originalReports.zip(publication.reports).forEach { (original, mapped) -> sameAs(original.order, mapped.order) }
        equalTo(listOf(TWO_MACROS.indexOf("@Generate"), TWO_MACROS.lastIndexOf("@Generate")),
            publication.diagnostics.map { present(present(it.mainRange).sourceRange).startOffset })
        publication.diagnostics.forEach(::assertExplicitWarning)
        typed.diagnostics.forEach { sameAs(CfirErrors.REDUNDANT_MODIFIER, it.factory) }
        falseValue(typed.hasErrors)
        trueValue(typed.hasWarningsForWError)
    }

    /** PARSE 失败只终止 CFIR/metadata 提交；两种调用次序均须发布已经完成且正常 splice 的兄弟根。 */
    @Test
    fun siblingParseFailureRetainsSuccessfulWarningInBothOrdersAndConstructionModes() = withParsers { project, lightTree ->
        for (mode in MacroConstructionService.Mode.entries) {
            for (warningFirst in listOf(true, false)) {
                val fixture = fixture(project, lightTree, TWO_MACROS)
                trueValue(present(fixture.files.single().parsingDiagnosticPublication).reports.isEmpty(),
                    "宿主必须语法合法，失败只能来自宏输出")
                equalTo(2, MacroCallForestBuilder.build(fixture.pre.allSurfaces).roots.size)
                val outputs = if (warningFirst) listOf(WARNING_OUTPUT, PARSE_FAILURE_OUTPUT)
                    else listOf(PARSE_FAILURE_OUTPUT, WARNING_OUTPUT)
                val executor = OutputExecutor(outputs)
                val parsed = mutableListOf<MacroFragmentResult>()
                val configuration = configuration(project, lightTree, executor) { _, fragment ->
                    nullValue(fixture.pre.session.macroExpansionRegistry)
                    trueValue(collectParsingDiagnosticPublication(fixture.files).reports.isEmpty())
                    parsed += fragment
                }

                val failed = instanceOf(MacroConstructionResult.Failed::class.java, expand(fixture.pre, configuration, mode))
                equalTo(2, parsed.size, "PARSE 失败不能跳过独立兄弟根")
                val success = parsed.filterIsInstance<MacroFragmentResult.Success>().single()
                val failure = parsed.filterIsInstance<MacroFragmentResult.SyntaxFailure>().single()
                assertExplicitWarning(success.diagnosticPublication.diagnostics.single())
                assertMissingParameterList(failure.diagnostics.single())
                val registry = failed.registry
                sameAs(registry, fixture.pre.session.macroExpansionRegistry)
                sameAs(failure, registry.parsingFailures.single())
                trueValue(registry.hasErrors)
                trueValue(registry.diagnostics.isEmpty(), "原生 PARSE 不得重建为 construction 语义错误")
                trueValue(registry.parsingReports.isEmpty(), "metadata abort 后没有成功 CFIR 产品")
                assertAborted(fixture.pre)
                equalTo(Name.identifier("C"), fixture.files.single().declarations.filterIsInstance<CfirClass>().single().name,
                    "WARNING 根已经经过真实 stable splice")

                val records = registry.parsingDiagnosticRecords
                equalTo(2, records.size)
                equalTo(parsed.map { it.originNode }, records.map { it.originNode })
                trueValue(records[0].invocationSequence < records[1].invocationSequence)
                equalTo(if (warningFirst) listOf(false, true) else listOf(true, false), records.map { it.syntaxFailure })
                records.zip(parsed).forEach { (record, fragment) ->
                    sameAs(fragment.originNode, record.originNode)
                    sameAs(fragment.diagnosticPublication, record.diagnosticPublication)
                    sameAs(fixture.files.single(), record.originFile)
                    sameAs(fixture.files.single().parsingDiagnosticPublication, record.originDiagnosticPublication)
                    sameAs(fixture.files.single().parsingSourceContext, record.originSourceContext)
                    sameAs(if (fragment is MacroFragmentResult.Success) fragment.sourceMap else failure.sourceMap, record.sourceMap)
                }

                val typed = DiagnosticsCollectorImpl()
                val publication = typed.reportParsingDiagnostics(fixture.files)
                equalTo(2, publication.reports.size)
                equalTo(2, publication.diagnostics.size)
                equalTo(if (warningFirst) listOf(ParsingDiagnostic.Severity.WARNING, ParsingDiagnostic.Severity.ERROR)
                    else listOf(ParsingDiagnostic.Severity.ERROR, ParsingDiagnostic.Severity.WARNING),
                    publication.diagnostics.map { it.severity })
                equalTo(listOf(TWO_MACROS.indexOf("@Generate"), TWO_MACROS.lastIndexOf("@Generate")),
                    publication.diagnostics.map { present(present(it.mainRange).sourceRange).startOffset })
                records.zip(publication.reports).forEach { (record, report) -> assertMappedReports(record, listOf(report)) }
                assertExplicitWarning(publication.diagnostics.single { !it.isError })
                val warning = typed.diagnostics.single()
                sameAs(CfirErrors.REDUNDANT_MODIFIER, warning.factory)
                sameAs(publication.diagnostics.single { !it.isError }, warning.parsingDiagnostic)
                equalTo(Severity.WARNING, warning.severity)
                equalTo(warning.parsingDiagnostic?.message, warning.renderMessage())
                falseValue(typed.hasErrors, "原生 PARSE ERROR 保留独立 parser 通道")
                trueValue(typed.hasWarningsForWError)
                equalTo(2, executor.executionCount)
                equalTo(1, executor.closeCount)
            }
        }
    }

    /** 同一 CFIR 对象换代，包括 parser 返回前换代；成功 WARNING 的来源必须冻结在 begin 时。 */
    @Test
    fun successfulWarningIsFilteredAfterInPlaceRawParserGenerationReplacement() = withParsers { project, lightTree ->
        assertInPlaceGenerationReplacement(project, lightTree, syntaxFailure = false)
    }

    /** SyntaxFailure 同样属于原始 parser 产品，不能借可变 originFile 混入下一源码单元。 */
    @Test
    fun syntaxFailureIsFilteredAfterInPlaceRawParserGenerationReplacement() = withParsers { project, lightTree ->
        assertInPlaceGenerationReplacement(project, lightTree, syntaxFailure = true)
    }

    /** 宏 seed 的 ignoreScopeCheck 只屏蔽 interface sentinel，不能屏蔽上一用例的显式 sealed 分支。 */
    @Test
    fun macroInterfaceSentinelIsSuppressedWithoutFailingConstruction() = withParsers { project, lightTree ->
        val fixture = fixture(project, lightTree)
        val parsed = mutableListOf<MacroFragmentResult>()
        val configuration = configuration(project, lightTree, OutputExecutor(listOf("open interface I{}"))) { _, result ->
            parsed += result
        }
        val result = instanceOf(MacroConstructionResult.Success::class.java, expand(fixture.pre, configuration))
        val fragment = instanceOf(MacroFragmentResult.Success::class.java, parsed.single())
        trueValue(fragment.diagnosticPublication.diagnostics.isEmpty())
        trueValue(fragment.diagnosticPublication.reports.isEmpty())
        sameAs(fragment.diagnosticPublication, result.registry.parsingReports.single().diagnosticPublication)
        falseValue(result.registry.hasErrors)
        val typed = DiagnosticsCollectorImpl()
        trueValue(typed.reportParsingDiagnostics(fixture.files).diagnostics.isEmpty())
        trueValue(typed.diagnostics.isEmpty())
        falseValue(typed.hasWarningsForWError)
    }

    /** 第一片段实际 splice，后续 LEX 拒绝 CFIR 提交并屏蔽 PARSE 输出；WARNING 不得成为 committed 产品。 */
    @Test
    fun laterLexFailureAbortsAlreadySplicedWarningInBothConstructionModes() = withParsers { project, lightTree ->
        for (mode in MacroConstructionService.Mode.entries) {
            val fixture = fixture(project, lightTree, source = TWO_MACROS)
            val parsed = mutableListOf<MacroFragmentResult>()
            val executor = OutputExecutor(listOf(WARNING_OUTPUT, "\\"))
            val configuration = configuration(project, lightTree, executor) { _, result -> parsed += result }
            val result = instanceOf(MacroConstructionResult.Failed::class.java, expand(fixture.pre, configuration, mode))
            val successful = instanceOf(MacroFragmentResult.Success::class.java, parsed.first())
            assertExplicitWarning(successful.diagnosticPublication.diagnostics.single())
            equalTo(Name.identifier("C"), fixture.files.single().declarations.filterIsInstance<CfirClass>().single().name)
            val failure = result.registry.parsingFailures.single()
            sameAs(parsed.last(), failure)
            assertBackslash(failure.diagnostics.single())
            equalTo(ParsingDiagnostic.RangeKind.POINT, failure.diagnostics.single().rangeKind)
            equalTo(1 to 1, present(failure.diagnostics.single().mainRange).begin)
            equalTo(1 to 2, present(failure.diagnostics.single().mainRange).end)
            trueValue(result.registry.hasErrors)
            trueValue(result.registry.parsingReports.isEmpty())
            trueValue(result.registry.diagnostics.isEmpty())
            sameAs(result.registry, fixture.pre.session.macroExpansionRegistry)
            assertAborted(fixture.pre)
            val typed = DiagnosticsCollectorImpl()
            val publication = typed.reportParsingDiagnostics(fixture.files)
            assertBackslash(publication.diagnostics.single())
            trueValue(typed.diagnostics.isEmpty())
            falseValue(typed.hasWarningsForWError)
            equalTo(2, executor.executionCount)
            equalTo(1, executor.closeCount)
        }
    }

    /** 真实 parser 返回 WARNING 后，splicer 的不变量与取消异常必须原样传播并终止私有 metadata。 */
    @Test
    fun spliceExceptionsNeverPublishSuccessfulParserWarning() = withParsers { project, lightTree ->
        for (expected in listOf(IllegalStateException("splice invariant"), CancellationException("cancel splice"))) {
            val fixture = fixture(project, lightTree)
            val executor = OutputExecutor(listOf(WARNING_OUTPUT))
            val configuration = configuration(project, lightTree, executor)
            var spliceCount = 0
            val splicer = object : MacroStableSplicer {
                override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): MacroStableSpliceResult {
                    spliceCount++
                    sameAs(fixture.files.single(), files.single())
                    val fragment = instanceOf(MacroFragmentResult.Success::class.java, slots.single().fragment)
                    assertExplicitWarning(fragment.diagnosticPublication.diagnostics.single())
                    sameAs(fixture.pre.files.single().annotationMetadata, slots.single().annotationMetadata)
                    throw expected
                }
            }
            sameAs(expected, throwsType(RuntimeException::class.java) {
                expand(fixture.pre, configuration, MacroConstructionService.Mode.DEGRADED, splicer)
            })
            equalTo(1, spliceCount)
            equalTo(1, executor.executionCount)
            equalTo(1, executor.closeCount)
            nullValue(fixture.pre.session.macroExpansionRegistry)
            assertAborted(fixture.pre)
            val typed = DiagnosticsCollectorImpl()
            trueValue(typed.reportParsingDiagnostics(fixture.files).diagnostics.isEmpty())
            trueValue(typed.diagnostics.isEmpty())
        }
    }

    /** 用 parser 实际记录的 annotation 槽位制造 seal 不变量错误，不创建假的属性或 warning producer。 */
    @Test
    fun metadataSealFailureKeepsPostSpliceWarningUncommitted() = withParsers { project, lightTree ->
        val fixture = fixture(project, lightTree)
        val parsed = mutableListOf<MacroFragmentResult.Success>()
        val configuration = configuration(project, lightTree, OutputExecutor(listOf("@!SealTag $WARNING_OUTPUT"))) { input, result ->
            val fragment = instanceOf(MacroFragmentResult.Success::class.java, result)
            val owner = (fragment.payload.roots.single() as MacroFragmentPayload.Root.Declaration).declaration
            val annotation = owner.annotations.single()
            val slot = input.annotationMetadata.snapshots.single { it.owner === owner }
            sameAs(annotation, slot.originalAnnotation)
            // 故障注入：真实列表改变却不迁移 metadata；seal 必须拒绝，不能修补缺失槽位。
            owner.replaceAnnotations(emptyList())
            parsed += fragment
        }
        val failure = throwsType(IllegalStateException::class.java) { expand(fixture.pre, configuration) }
        equalTo("Final annotation slots differ from the recorded construction metadata.", failure.message)
        val fragment = parsed.single()
        assertExplicitWarning(fragment.diagnosticPublication.diagnostics.single())
        val registry = present(fixture.pre.session.macroExpansionRegistry)
        // registry 只在 splice、字段收尾后安装；此处看到它，确认失败发生于 seal 而非 parser/splicer。
        trueValue(registry.parsingReports.isEmpty())
        trueValue(registry.parsingFailures.isEmpty())
        trueValue(registry.parsingDiagnosticRecords.isEmpty(), "seal 异常没有正常返回 Failed，pending 不得发布")
        trueValue(registry.diagnostics.isEmpty())
        equalTo(Name.identifier("C"), fixture.files.single().declarations.filterIsInstance<CfirClass>().single().name)
        assertAborted(fixture.pre)
        val typed = DiagnosticsCollectorImpl()
        trueValue(typed.reportParsingDiagnostics(fixture.files).diagnostics.isEmpty())
        trueValue(typed.diagnostics.isEmpty())
    }

    /**
     * 同一逻辑 surface 的后续代次：保留真实新 raw carrier，只复用原 surface/handle id。
     * 此处直接驱动公共 registry stage + successOf 的生命周期；声明槽测试 splicer 只处理一个
     * 顶层声明的对象身份替换。语言解析、payload、source map、metadata seal 均使用生产组件。
     */
    @Test
    fun emptyGenerationReplacesCommittedWarningOnlyAfterSuccessfulMetadataSeal() = withParsers { project, lightTree ->
        val initial = fixture(project, lightTree)
        val first = instanceOf(MacroConstructionResult.Success::class.java,
            expand(initial.pre, configuration(project, lightTree, OutputExecutor(listOf(WARNING_OUTPUT)))))
        val registry = first.registry
        val previous = registry.parsingReports.single()
        val originalSurface = previous.originNode.surface
        val nextFile = buildRaw(project, lightTree, initial.pre.session, "invocation.cj", ONE_MACRO)
        val nextSurface = (nextFile.surfaces.single() as MacroSurfaceDecl).copy(
            surfaceId = originalSurface.surfaceId,
            replaceHandle = nextFile.surfaces.single().replaceHandle.copy(handleId = originalSurface.replaceHandle.handleId),
        )
        val rebound = PreMacroCfirFile(nextFile.cfirFile, listOf(nextSurface), nextFile.annotationMetadata,
            nextFile.parsingDiagnosticPublication, nextFile.parserSettings)
        val next = buildPreMacroRawFiles(initial.pre.session, listOf(rebound))
        registry.registerFileSurfaces(nextFile.cfirFile, listOf(nextSurface))
        val classification = MacroDemandClassification.create(next)
        classification.freezeFinal(macroArtifactDefinitions = definitions(next))
        val input = MacroFragmentInput(
            node = MacroCallForestBuilder.build(listOf(nextSurface)).roots.single(),
            tokens = listOf(MacroSurfaceToken("class C{}", 0, 9)),
            decision = classification.finalDecisions.single(),
            fileFacts = present(nextFile.cfirFile.parsingFileFacts),
            annotationMetadata = nextFile.annotationMetadata,
        )
        val parserConfiguration = CompilerConfiguration().apply {
            useLightTree = lightTree
            languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
            installDefaultMacroFragmentParserFactory(project)
        }
        registry.beginParsingInvocation(input.node)
        val empty = instanceOf(MacroFragmentResult.Success::class.java,
            present(parserConfiguration.macroFragmentParserFactory).create(next.session).parse(input))
        trueValue(empty.diagnosticPublication.reports.isEmpty())
        trueValue(empty.diagnosticPublication.diagnostics.isEmpty())
        val slot = MacroReplaceSlot(nextSurface.replaceHandle, nextSurface, empty, nextFile.annotationMetadata)
        val declarations = nextFile.cfirFile.declarations as MutableList<CfirDeclaration>
        val carrierIndex = declarations.indexOfFirst { it === slot.handle.carrier }
        trueValue(carrierIndex >= 0, "新代次必须命中 raw builder 的真实 carrier")
        val replacement = (empty.payload.roots.single() as MacroFragmentPayload.Root.Declaration).declaration
        declarations[carrierIndex] = replacement
        finalizeImplicitConstructorDeclarations(nextFile.cfirFile, nextFile.annotationMetadata)
        materializePrimaryConstructorParameterFields(nextFile.cfirFile, nextFile.annotationMetadata)
        registry.stageParsingReports(listOf(slot))

        sameAs(previous, registry.parsingReports.single())
        trueValue(collectParsingDiagnosticPublication(listOf(nextFile.cfirFile)).diagnostics.isEmpty())
        falseValue(collectParsingDiagnosticPublication(listOf(previous.originFile)).diagnostics.isEmpty())
        val committed = MacroConstructionService.successOf(next, listOf(nextFile.cfirFile), registry)
        sameAs(registry, committed.registry)
        val current = registry.parsingReports.single()
        sameAs(empty.originNode, current.originNode)
        sameAs(empty.diagnosticPublication, current.diagnosticPublication)
        sameAs(empty.sourceMap, current.sourceMap)
        sameAs(nextFile.cfirFile, registry.originFileForSurface(originalSurface.surfaceId))
        trueValue(previous.diagnosticPublication.diagnostics.isNotEmpty())
        val typed = DiagnosticsCollectorImpl()
        trueValue(typed.reportParsingDiagnostics(listOf(nextFile.cfirFile)).diagnostics.isEmpty())
        trueValue(typed.diagnostics.isEmpty())
        falseValue(typed.hasWarningsForWError)
        throwsType(IllegalStateException::class.java) { nextFile.annotationMetadata.seal(listOf(nextFile.cfirFile)) }
    }

    /** 正式 frontend 必须等宏 LEX 收口；源 WARNING 已在 executor 运行时发出也算失败。 */
    @Test
    fun frontendUnitLexGatePreventsEarlySourceWarningsAndTypedParseWarnings() {
        for (lightTree in listOf(false, true)) {
            for (separateFiles in listOf(false, true)) {
                val disposable = Disposer.newDisposable("macro-warning-unit-gate")
                try {
                    val directory = Files.createDirectories(tempDir.resolve("gate-$lightTree-$separateFiles"))
                    val invocation = "@Generate func original(): Unit {}\n"
                    val source = directory.resolve("invocation.cj")
                    Files.writeString(source, "package sample\nimport macros.*\n" +
                        (if (separateFiles) "" else "open interface I{}\n") + invocation, Charsets.UTF_8)
                    val messages = RecordingMessages()
                    val typed = DiagnosticsCollectorImpl()
                    val macroCjo = directory.resolve("macros.cjo")
                    CjoPackageWriter.write(macroCjo, CjoPackageMetadata(
                        fullPackageName = "macros", moduleName = "macros", kind = PackageKind.Macro,
                        declarations = listOf(CjoPackageDeclaration("Generate")),
                    ))
                    val library = Files.write(directory.resolve("macros.dll"), byteArrayOf(1))
                    var session: CfirSession? = null
                    val executor = OutputExecutor(listOf("\\")) {
                        trueValue(messages.entries.none { it.first == CompilerMessageSeverity.WARNING })
                        trueValue(typed.diagnostics.isEmpty(), "raw WARNING 必须延迟到全部宏构造结束")
                    }
                    val configuration = CompilerConfiguration().apply {
                        moduleName = "macroParsingWarningPublication"
                        languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
                        useLightTree = lightTree
                        messageCollector = messages
                        diagnosticsCollector = typed
                        addCangJieSourceRoot(source.toString())
                        if (separateFiles) {
                            val warningSource = Files.writeString(directory.resolve("warning.cj"),
                                "package sample\nopen interface I{}\n", Charsets.UTF_8)
                            addCangJieSourceRoot(warningSource.toString())
                        }
                        macroArtifactPackages = listOf(MacroArtifactPackage(
                            FqName("macros"), MacroArtifactPackage.Kind.MACRO, macroCjo.toString(), library.toString(),
                            origin = MacroArtifactPackage.Origin.EXTERNAL_PATH,
                        ))
                        macroExecutorFactory = MacroExecutorFactory { parserSession -> session = parserSession; executor }
                    }

                    nullValue(CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable)))
                    equalTo(1, executor.executionCount, messages.entries.toString())
                    equalTo(1, executor.closeCount)
                    trueValue(messages.hasErrors())
                    equalTo(listOf("unknown start of token: \\"), messages.entries.filter { it.first.isError }.map { it.second })
                    trueValue(messages.entries.none { it.first == CompilerMessageSeverity.WARNING })
                    trueValue(typed.diagnostics.isEmpty())
                    falseValue(typed.hasWarningsForWError)
                    val registry = present(present(session).macroExpansionRegistry)
                    assertBackslash(registry.parsingFailures.single().diagnostics.single())
                    trueValue(registry.parsingReports.isEmpty())
                    val surface = registry.parsingFailures.single().originNode.surface
                    val host = present(registry.originFileForSurface(surface.surfaceId))
                    if (!separateFiles) {
                        val raw = present(host.parsingDiagnosticPublication).diagnostics.single()
                        equalTo(ParsingWarning.RedundantModifier(CjModifierKind.OPEN,
                            ParsingWarning.Redundancy.InterfaceMember), raw.warning)
                        equalTo("interface member implies 'open'", raw.mainHint)
                    }
                    val replay = DiagnosticsCollectorImpl()
                    assertBackslash(replay.reportParsingDiagnostics(listOf(host)).diagnostics.single())
                    trueValue(replay.diagnostics.isEmpty())
                } finally {
                    Disposer.dispose(disposable)
                }
            }
        }
    }

    /** 正式单元收口保留源 WARNING 与宏 WARNING；合法 import/调用保证两个宏确实执行到 parser。 */
    @Test
    fun frontendParseOnlyFailurePublishesSourceAndSiblingMacroWarnings() {
        for (lightTree in listOf(false, true)) {
            for (mode in MacroConstructionService.Mode.entries) {
                for (warningFirst in listOf(true, false)) {
                    val disposable = Disposer.newDisposable("macro-warning-parse-only-unit")
                    try {
                        val directory = Files.createDirectories(tempDir.resolve("parse-$lightTree-$mode-$warningFirst"))
                        val text = "package sample\nimport macros.*\nopen interface I{}\n" +
                            "@Generate func first(): Unit {}\n@Generate func second(): Unit {}\n"
                        val source = Files.writeString(directory.resolve("invocation.cj"), text, Charsets.UTF_8)
                        val macroCjo = directory.resolve("macros.cjo")
                        CjoPackageWriter.write(macroCjo, CjoPackageMetadata(
                            fullPackageName = "macros", moduleName = "macros", kind = PackageKind.Macro,
                            declarations = listOf(CjoPackageDeclaration("Generate")),
                        ))
                        val library = Files.write(directory.resolve("macros.dll"), byteArrayOf(1))
                        val messages = RecordingMessages()
                        val typed = DiagnosticsCollectorImpl()
                        var session: CfirSession? = null
                        val outputs = if (warningFirst) listOf(WARNING_OUTPUT, PARSE_FAILURE_OUTPUT)
                            else listOf(PARSE_FAILURE_OUTPUT, WARNING_OUTPUT)
                        val executor = OutputExecutor(outputs) {
                            trueValue(messages.entries.none { it.first == CompilerMessageSeverity.WARNING })
                            trueValue(typed.diagnostics.isEmpty(), "源报告须等所有宏 parser 完成后统一发布")
                        }
                        val configuration = CompilerConfiguration().apply {
                            moduleName = "macroParsingWarningPublication"
                            languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
                            useLightTree = lightTree
                            macroConstructionMode = mode
                            messageCollector = messages
                            diagnosticsCollector = typed
                            addCangJieSourceRoot(source.toString())
                            macroArtifactPackages = listOf(MacroArtifactPackage(
                                FqName("macros"), MacroArtifactPackage.Kind.MACRO, macroCjo.toString(), library.toString(),
                                origin = MacroArtifactPackage.Origin.EXTERNAL_PATH,
                            ))
                            macroExecutorFactory = MacroExecutorFactory { parserSession -> session = parserSession; executor }
                        }

                        nullValue(CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable)))
                        equalTo(2, executor.executionCount, "两个宏必须实际执行，不能提前被宿主源码错误拒绝：${messages.entries}")
                        equalTo(1, executor.closeCount)
                        trueValue(messages.hasErrors())
                        val expectedMacroSeverities = if (warningFirst)
                            listOf(CompilerMessageSeverity.WARNING, CompilerMessageSeverity.ERROR)
                        else listOf(CompilerMessageSeverity.ERROR, CompilerMessageSeverity.WARNING)
                        equalTo(listOf(CompilerMessageSeverity.WARNING) + expectedMacroSeverities,
                            messages.entries.map { it.first }, messages.entries.toString())
                        equalTo(listOf(PARSE_FAILURE_MESSAGE), messages.entries.filter { it.first.isError }.map { it.second })
                        equalTo(listOf("redundant modifier: 'open'", "redundant modifier: 'open'"),
                            messages.entries.filter { it.first == CompilerMessageSeverity.WARNING }.map { it.second })
                        val locations = messages.locations.map(::present)
                        val warningIndex = if (warningFirst) 1 else 2
                        val errorIndex = if (warningFirst) 2 else 1
                        equalTo(listOf(3, 4, 5), locations.map { it.line })
                        equalTo(listOf(1, 1, 1), locations.map { it.column })
                        equalTo(3, locations[0].lineEnd)
                        equalTo(5, locations[0].columnEnd)
                        for (index in listOf(1, 2)) {
                            equalTo(locations[index].line, locations[index].lineEnd)
                            equalTo(present(locations[index].lineContent).length + 1, locations[index].columnEnd)
                        }
                        locations.forEach { location ->
                            equalTo(source.toAbsolutePath().normalize(), Path.of(location.path).toAbsolutePath().normalize())
                            equalTo(ParsingDiagnostic.Category.PARSE,
                                instanceOf(CompilerMessageLocationWithRelatedLocations::class.java, location).diagnosticCategory)
                        }
                        val sourceLocation = instanceOf(CompilerMessageLocationWithRelatedLocations::class.java, locations[0])
                        val macroLocation = instanceOf(CompilerMessageLocationWithRelatedLocations::class.java, locations[warningIndex])
                        equalTo("parse_redundant_modifier", sourceLocation.nativeDiagnosticKind)
                        equalTo("parse_redundant_modifier", macroLocation.nativeDiagnosticKind)
                        equalTo("interface member implies 'open'", sourceLocation.mainHint)
                        equalTo("'sealed' implies 'open'", macroLocation.mainHint)
                        equalTo(sourceLocation.notes, macroLocation.notes)
                        val hint = macroLocation.relatedLocations.single()
                        equalTo(locations[warningIndex].line to 1, hint.location.line to hint.location.column)
                        equalTo(locations[warningIndex].lineEnd to locations[warningIndex].columnEnd,
                            hint.location.lineEnd to hint.location.columnEnd)
                        equalTo(locations[warningIndex].path, hint.location.path)
                        nullValue(instanceOf(CompilerMessageLocationWithRelatedLocations::class.java,
                            locations[errorIndex]).nativeDiagnosticKind)

                        val registry = present(present(session).macroExpansionRegistry)
                        trueValue(registry.hasErrors)
                        trueValue(registry.diagnostics.isEmpty())
                        trueValue(registry.parsingReports.isEmpty(), "Failed 没有 metadata commit")
                        val records = registry.parsingDiagnosticRecords
                        equalTo(2, records.size)
                        trueValue(records[0].invocationSequence < records[1].invocationSequence)
                        equalTo(if (warningFirst) listOf(false, true) else listOf(true, false), records.map { it.syntaxFailure })
                        val host = records.first().originFile
                        records.forEach { record ->
                            sameAs(host, record.originFile)
                            sameAs(host.parsingSourceContext, record.originSourceContext)
                            sameAs(host.parsingDiagnosticPublication, record.originDiagnosticPublication)
                            val carrier = instanceOf(CfirDeclaration::class.java, record.originNode.surface.replaceHandle.carrier)
                            carrier.annotations.forEach { annotation ->
                                nullValue(present(session).annotationMetadataRegistry.snapshot(
                                    instanceOf(CfirAnnotationCall::class.java, annotation)),
                                    "Failed 不得向 session 索引提交 annotation metadata")
                            }
                        }
                        assertMissingParameterList(registry.parsingFailures.single().diagnostics.single())
                        val raw = present(host.parsingDiagnosticPublication)
                        equalTo(1, raw.diagnostics.size, "宿主只有真实 WARNING，没有 PARSE/LEX ERROR")
                        falseValue(raw.diagnostics.single().isError)
                        equalTo(ParsingWarning.RedundantModifier(CjModifierKind.OPEN,
                            ParsingWarning.Redundancy.InterfaceMember), raw.diagnostics.single().warning)
                        val replay = DiagnosticsCollectorImpl()
                        val publication = replay.reportParsingDiagnostics(listOf(host))
                        equalTo(3, publication.reports.size)
                        equalTo(3, publication.diagnostics.size)
                        sameAs(raw.reports.single().order, publication.reports.first().order)
                        records.zip(publication.reports.drop(1)).forEach { (record, report) -> assertMappedReports(record, listOf(report)) }
                        equalTo(2, typed.diagnostics.size)
                        equalTo(listOf(CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET, CfirErrors.REDUNDANT_MODIFIER),
                            typed.diagnostics.map { it.factory })
                        equalTo(publication.diagnostics.filter { !it.isError }, typed.diagnostics.map { it.parsingDiagnostic })
                        typed.diagnostics.forEach { diagnostic ->
                            equalTo(Severity.WARNING, diagnostic.severity)
                            equalTo(diagnostic.parsingDiagnostic?.message, diagnostic.renderMessage())
                            equalTo(WarningGroup.PARSER, present(present(diagnostic.parsingDiagnostic).warning).group)
                        }
                        falseValue(typed.hasErrors)
                        trueValue(typed.hasWarningsForWError)
                        trueValue(replay.hasWarningsForWError)
                    } finally {
                        Disposer.dispose(disposable)
                    }
                }
            }
        }
    }

    /**
     * 用更短且带真实 WARNING 的 raw 产品替换原对象的 parser 输入；旧 source map 在新快照上越界，
     * 因此必须先按冻结 publication/context 筛选，不能先规范化旧报告再决定是否属于本单元。
     */
    private fun assertInPlaceGenerationReplacement(project: Project, lightTree: Boolean, syntaxFailure: Boolean) {
        for (replaceDuringParse in listOf(false, true)) {
            val fixture = fixture(project, lightTree)
            val host = fixture.files.single()
            val originalContext = present(host.parsingSourceContext)
            val originalPublication = present(host.parsingDiagnosticPublication)
            val originalFacts = present(host.parsingFileFacts)
            val originalSettings = present(host.parserSettings)
            val replacement = buildRaw(project, lightTree, fixture.pre.session, "invocation.cj",
                "package sample\nopen interface I{}\n")
            val nextContext = present(replacement.cfirFile.parsingSourceContext)
            equalTo(originalFacts, replacement.cfirFile.parsingFileFacts)
            equalTo(originalSettings, replacement.parserSettings)
            differentFrom(originalContext, nextContext)
            differentFrom(originalPublication, replacement.parsingDiagnosticPublication)
            trueValue(nextContext.snapshot.text.length < present(fixture.pre.allSurfaces.single().sourceRange).endOffset)
            fun replaceGeneration() {
                host.replaceParsingSourceContext(originalContext, nextContext,
                    present(replacement.cfirFile.parsingFileFacts), replacement.parsingDiagnosticPublication, replacement.parserSettings)
                sameAs(nextContext, host.parsingSourceContext)
                sameAs(replacement.parsingDiagnosticPublication, host.parsingDiagnosticPublication)
                sameAs(originalFacts, host.parsingFileFacts)
                sameAs(originalSettings, host.parserSettings)
            }
            val parsed = mutableListOf<MacroFragmentResult>()
            val executor = OutputExecutor(listOf(if (syntaxFailure) PARSE_FAILURE_OUTPUT else WARNING_OUTPUT))
            val configuration = configuration(project, lightTree, executor) { _, fragment ->
                parsed += fragment
                if (replaceDuringParse) replaceGeneration()
            }
            val result = expand(fixture.pre, configuration)
            if (syntaxFailure) {
                instanceOf(MacroConstructionResult.Failed::class.java, result)
                assertMissingParameterList(instanceOf(MacroFragmentResult.SyntaxFailure::class.java, parsed.single()).diagnostics.single())
                assertAborted(fixture.pre)
                trueValue(result.registry.parsingReports.isEmpty())
            } else {
                instanceOf(MacroConstructionResult.Success::class.java, result)
                assertExplicitWarning(instanceOf(MacroFragmentResult.Success::class.java, parsed.single()).diagnosticPublication.diagnostics.single())
                fixture.pre.files.single().annotationMetadata.snapshots
                throwsType(IllegalStateException::class.java) { fixture.pre.files.single().annotationMetadata.seal(fixture.files) }
                equalTo(1, result.registry.parsingReports.size)
            }
            val record = result.registry.parsingDiagnosticRecords.single()
            sameAs(host, record.originFile)
            sameAs(originalContext, record.originSourceContext)
            sameAs(originalPublication, record.originDiagnosticPublication)
            sameAs(parsed.single().originNode, record.originNode)
            sameAs(parsed.single().diagnosticPublication, record.diagnosticPublication)
            equalTo(syntaxFailure, record.syntaxFailure)
            val fragment = parsed.single()
            sameAs(when (fragment) {
                is MacroFragmentResult.Success -> fragment.sourceMap
                is MacroFragmentResult.SyntaxFailure -> fragment.sourceMap
                else -> error("此用例只接受真实 Success/SyntaxFailure")
            }, record.sourceMap)
            val oldMapped = present(present(record.sourceMap).mapRange(
                record.diagnosticPublication.diagnostics.single().startOffset,
                record.diagnosticPublication.diagnostics.single().endOffset))
            equalTo(ONE_MACRO.indexOf('@'), oldMapped.startOffset)
            equalTo(ONE_MACRO.indexOf('}') + 1, oldMapped.endOffset)
            if (!replaceDuringParse) {
                assertMappedReports(record, collectParsingDiagnosticPublication(fixture.files).reports)
                replaceGeneration()
            }

            val typed = DiagnosticsCollectorImpl()
            val publication = typed.reportParsingDiagnostics(fixture.files)
            val current = publication.diagnostics.single()
            equalTo(1, publication.reports.size)
            sameAs(replacement.parsingDiagnosticPublication.reports.single().order, publication.reports.single().order)
            trueValue(publication.reports.none { it.order === record.diagnosticPublication.reports.single().order })
            sameAs(nextContext.snapshot, present(current.mainRange).source)
            sameAs(nextContext.snapshot, current.nativeHints.single().range.source)
            equalTo(ParsingWarning.RedundantModifier(CjModifierKind.OPEN,
                ParsingWarning.Redundancy.InterfaceMember), current.warning)
            equalTo(ParsingDiagnostic.Severity.WARNING, current.severity)
            equalTo(WarningGroup.PARSER, present(current.warning).group)
            sameAs(CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET, typed.diagnostics.single().factory)
            sameAs(current, typed.diagnostics.single().parsingDiagnostic)
            falseValue(typed.hasErrors)
            trueValue(typed.hasWarningsForWError)
            sameAs(record, result.registry.parsingDiagnosticRecords.single())
            sameAs(originalContext, record.originSourceContext)
            sameAs(originalPublication, record.originDiagnosticPublication)
            equalTo(1, executor.executionCount)
            equalTo(1, executor.closeCount)
        }
    }

    /** 映射只改变来源范围；原生 payload、producer 顺序及 source map 身份均保留。 */
    private fun assertMappedReports(record: MacroParsingDiagnosticRecord, reports: List<CjParsingDiagnosticReport>) {
        equalTo(record.diagnosticPublication.reports.size, reports.size)
        val context = record.originSourceContext
        val sourceMap = present(record.sourceMap)
        record.diagnosticPublication.reports.zip(reports).forEach { (originalReport, report) ->
            sameAs(originalReport.order, report.order)
            val original = originalReport.diagnostic
            val mapped = report.diagnostic
            equalTo(original.message, mapped.message)
            equalTo(original.category, mapped.category)
            equalTo(original.severity, mapped.severity)
            equalTo(original.warning, mapped.warning)
            equalTo(original.mainHint, mapped.mainHint)
            equalTo(original.notes, mapped.notes)
            val tokenRange = present(sourceMap.mapRange(original.startOffset, original.endOffset))
            val surfaceRange = present(record.originNode.surface.sourceRange)
            equalTo(surfaceRange.startOffset, tokenRange.startOffset)
            equalTo(surfaceRange.endOffset, tokenRange.endOffset)
            val expectedStart = if (record.syntaxFailure && original.isError) surfaceRange.startOffset else tokenRange.startOffset
            val expectedEnd = if (record.syntaxFailure && original.isError) surfaceRange.endOffset else tokenRange.endOffset
            val main = present(mapped.mainRange)
            val characters = present(main.sourceRange)
            sameAs(context.snapshot, main.source)
            sameAs(context.snapshot, characters.source)
            equalTo(expectedStart, mapped.startOffset)
            equalTo(expectedEnd, mapped.endOffset)
            equalTo(expectedStart, characters.startOffset)
            equalTo(expectedEnd, characters.endOffset)
            equalTo(context.snapshot.nativePosition(expectedStart), main.begin)
            equalTo(context.snapshot.nativePosition(expectedEnd), main.end)
            equalTo(original.hints.size, mapped.nativeHints.size)
            original.hints.zip(mapped.nativeHints).forEach { (hint, nativeHint) ->
                val hintRange = present(sourceMap.mapRange(hint.startOffset, hint.endOffset))
                val charactersHint = present(nativeHint.range.sourceRange)
                sameAs(context.snapshot, nativeHint.range.source)
                equalTo(hintRange.startOffset, charactersHint.startOffset)
                equalTo(hintRange.endOffset, charactersHint.endOffset)
                equalTo(context.snapshot.nativePosition(hintRange.startOffset), nativeHint.range.begin)
                equalTo(context.snapshot.nativePosition(hintRange.endOffset), nativeHint.range.end)
                equalTo(hint.label, nativeHint.label)
            }
        }
    }

    /** v1.1.3 ParseFuncParameters/parse_expected_lt_paren：缺列表、不消费正文左括号。 */
    private fun assertMissingParameterList(diagnostic: ParsingDiagnostic) {
        equalTo(ParsingDiagnostic.Category.PARSE, diagnostic.category)
        equalTo(ParsingDiagnostic.Severity.ERROR, diagnostic.severity)
        trueValue(diagnostic.isError)
        nullValue(diagnostic.warning)
        equalTo(PARSE_FAILURE_MESSAGE, diagnostic.message)
        equalTo(PARSE_FAILURE_OUTPUT.indexOf('{'), diagnostic.startOffset)
        equalTo(PARSE_FAILURE_OUTPUT.indexOf('{') + 1, diagnostic.endOffset)
        equalTo(1 to 13, present(diagnostic.mainRange).begin)
        equalTo(1 to 14, present(diagnostic.mainRange).end)
    }

    private fun assertExplicitWarning(diagnostic: ParsingDiagnostic) {
        equalTo(ParsingDiagnostic.Category.PARSE, diagnostic.category)
        equalTo(ParsingDiagnostic.Severity.WARNING, diagnostic.severity)
        falseValue(diagnostic.isError)
        equalTo(ParsingWarning.RedundantModifier(CjModifierKind.OPEN,
            ParsingWarning.Redundancy.Explicit(CjModifierKind.SEALED)), diagnostic.warning)
        equalTo("parse_redundant_modifier", present(diagnostic.warning).nativeKind)
        equalTo(WarningGroup.PARSER, present(diagnostic.warning).group)
        equalTo("redundant modifier: 'open'", diagnostic.message)
        equalTo("'sealed' implies 'open'", diagnostic.mainHint)
        equalTo(listOf("this warning can be suppressed by setting the compiler option `-Woff parser`"), diagnostic.notes)
    }

    private fun assertBackslash(diagnostic: ParsingDiagnostic) {
        equalTo(ParsingDiagnostic.Category.LEX, diagnostic.category)
        equalTo(ParsingDiagnostic.Severity.ERROR, diagnostic.severity)
        equalTo("unknown start of token: \\", diagnostic.message)
        nullValue(diagnostic.warning)
    }

    private fun assertAborted(pre: PreMacroRawBuildResult) {
        for (file in pre.files) {
            throwsType(IllegalStateException::class.java) { file.annotationMetadata.snapshots }
            throwsType(IllegalStateException::class.java) { file.annotationMetadata.seal(listOf(file.cfirFile)) }
        }
    }

    private fun withParsers(action: (Project, Boolean) -> Unit) {
        val disposable = Disposer.newDisposable("macro-parsing-warning-publication")
        try {
            val project = CangJieCoreEnvironment.create(disposable, CangJieCoreEnvironmentMode.Production).projectEnvironment.project
            for (lightTree in listOf(false, true)) action(project, lightTree)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun fixture(project: Project, lightTree: Boolean, source: String = ONE_MACRO): Fixture {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        session.register(CfirModuleData::class, TestModuleData(session))
        session.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        session.register(CfirLanguageSettingsComponent::class,
            CfirLanguageSettingsComponent(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)))
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent())
        return Fixture(buildPreMacroRawFiles(session, listOf(buildRaw(project, lightTree, session, "invocation.cj", source))))
    }

    private fun buildRaw(project: Project, lightTree: Boolean, session: CfirSession, name: String, text: String): PreMacroCfirFile =
        if (lightTree) {
            LightTree2Cfir(session, session.cangjieScopeProvider).buildPreMacroCfirFile(
                text, CjInMemoryTextSourceFile(name, "memory/$name", text), text.toSourceLinesMapping(),
            )
        } else {
            PsiRawCfirBuilder(session, annotationMetadata = CfirAnnotationMetadataBuilder())
                .buildPreMacroCfirFile(CjPsiFactory(project).createFile(name, text))
        }

    /** 包导出定义仅供真实分类/绑定，无动态库调用；所有 invocation、carrier、语法事实均来自 raw builder。 */
    private fun definitions(pre: PreMacroRawBuildResult): List<MacroDefinitionEntry> =
        pre.allSurfaces.map { present(it.qualifiedName).shortName() }.distinct().map { name ->
            MacroDefinitionEntry(
                packageFqName = FqName("macros"),
                name = name,
                source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
            )
        }

    private fun expand(
        pre: PreMacroRawBuildResult,
        configuration: CompilerConfiguration,
        mode: MacroConstructionService.Mode = MacroConstructionService.Mode.STRICT,
        splicer: MacroStableSplicer? = null,
    ): MacroConstructionResult {
        val definitions = definitions(pre)
        val context = bindMacroImports(pre, buildMacroSymbolIndex(pre = pre, macroArtifactDefinitions = definitions))
        val classification = MacroDemandClassification.create(pre)
        classification.freezeFinal(macroArtifactDefinitions = definitions)
        val service = if (splicer == null) FrontendMacroConstructionService(configuration)
            else FrontendMacroConstructionService(configuration, splicer)
        return service.expand(pre, context, classification, mode)
    }

    private fun configuration(
        project: Project,
        lightTree: Boolean,
        executor: OutputExecutor,
        observe: (MacroFragmentInput, MacroFragmentResult) -> Unit = { _, _ -> },
    ): CompilerConfiguration = CompilerConfiguration().apply {
        useLightTree = lightTree
        languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        macroExecutorFactory = MacroExecutorFactory { executor }
        installDefaultMacroFragmentParserFactory(project)
        val factory = present(macroFragmentParserFactory)
        macroFragmentParserFactory = MacroFragmentParserFactory { session ->
            val parser = factory.create(session)
            object : MacroFragmentParser {
                override fun parse(input: MacroFragmentInput): MacroFragmentResult = parser.parse(input).also { observe(input, it) }
            }
        }
    }

    private data class Fixture(val pre: PreMacroRawBuildResult) {
        val files: List<CfirFile> get() = pre.files.map { it.cfirFile }
    }

    /** 只注入宏输出的文本；生产 token-backed parser 负责真正的 lexer 复扫、诊断与 source map。 */
    private class OutputExecutor(
        private val outputs: List<String>,
        private val beforeExecute: () -> Unit = {},
    ) : MacroExecutor {
        var executionCount = 0
            private set
        var closeCount = 0
            private set

        override fun loadLibraries(libPaths: List<String>): MacroLibraryLoadResult = MacroLibraryLoadResult.Success(libPaths)
        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            beforeExecute()
            return calls.map {
                val text = outputs[executionCount++]
                MacroExpansionResult.Success(listOf(TokenInfo(0u.toUByte(), text)), text)
            }
        }
        override fun reset() = Unit
        override fun isAvailable(): Boolean = true
        override fun close() { closeCount++ }
    }

    private class RecordingMessages : MessageCollector {
        val entries = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        val locations = mutableListOf<CompilerMessageSourceLocation?>()
        override fun clear() {
            entries.clear()
            locations.clear()
        }
        override fun hasErrors(): Boolean = entries.any { it.first.isError }
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            entries += severity to message
            locations += location
        }
    }

    /** 沿用现有 frontend 宏执行测试的 session 装配，不引入依赖解析或语义 checker。 */
    private class TestModuleData(override val session: CfirSession) : CfirModuleData() {
        override val name: Name = Name.identifier("macro-parsing-warning-publication")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = targetPlatform.isCommon()
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "macro-parsing-warning-publication"

        init { bindSession(session) }
    }

    private fun <T : Any> present(value: T?): T {
        org.junit.jupiter.api.Assertions.assertNotNull(value)
        return checkNotNull(value)
    }

    private companion object {
        const val WARNING_OUTPUT = "open sealed class C{}"
        const val PARSE_FAILURE_OUTPUT = "func broken {}"
        const val PARSE_FAILURE_MESSAGE = "expected '(' or '<', found '{'"
        const val ONE_MACRO = "package sample\nimport macros.*\n@Generate func original(): Unit {}\n"
        const val TWO_MACROS = "package sample\nimport macros.*\n@Generate func first(): Unit {}\n@Generate func second(): Unit {}\n"
    }
}
