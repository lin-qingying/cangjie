package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.parsingFileFacts
import org.cangnova.cangjie.cfir.declarations.publishParsingFileFacts
import org.cangnova.cangjie.parsing.CjmpFileRole
import org.cangnova.cangjie.parsing.ParsingFileFacts
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.hasInitialDefaultValue
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildFileCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirIfAvailableExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationReplaceCarrier
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallSite
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirReplaceHandle
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionCacheKey
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentInput
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentPayload
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroReplacementSlotType
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroParameterSignature
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroReplaceSlot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolution
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroStableSpliceResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroStableSplicer
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceContainerContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceScopeContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceSourceRange
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.bindMacroImports
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildMacroSymbolIndex
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroDiagnosticInfo
import org.cangnova.cangjie.macro.MacroDiagnosticSeverity
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.MacroLibraryLoadFailure
import org.cangnova.cangjie.macro.MacroLibraryLoadFailureKind
import org.cangnova.cangjie.macro.MacroLibraryLoadResult
import org.cangnova.cangjie.macro.SourcePosition
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CjParameterGrammar
import org.cangnova.cangjie.parsing.CjParserSourceContext
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.parsing.parsedFileFacts
import org.cangnova.cangjie.parsing.parsingSourceContextOrNull
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException

@OptIn(CompilerConfiguration.Internals::class)
/**
 * 覆盖前端宏构造执行链路中的 splicer、executor、parser、降级和诊断行为。
 */
class FrontendMacroConstructionExecutionTest {
    /** 真正源码的 IfAvailable 必须保留整段调用，重解析后在原 expression 槽写回两个分支。 */
    @Test
    fun sourceIfAvailableRetainsInvocationAndBothBranchesThroughRawConstruction() {
        val disposable = com.intellij.openapi.util.Disposer.newDisposable("ifavailable-source-construction")
        try {
            val project = org.cangnova.cangjie.CangJieCoreEnvironment.create(
                disposable, org.cangnova.cangjie.CangJieCoreEnvironmentMode.Production,
            ).projectEnvironment.project
            val source = """
                package sample
                func use(): Unit {
                    @IfAvailable(level: 19, {=> availableBranch() }, {=> unavailableBranch() })
                }
            """.trimIndent()

            for (lightTree in listOf(false, true)) {
                var executorCreations = 0
                val executor = RecordingExecutor()
                val session = productionParserSession()
                val preFile = if (lightTree) {
                    val sourceFile = CjInMemoryTextSourceFile("ifavailable.cj", "memory/ifavailable.cj", source)
                    org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir(session, session.cangjieScopeProvider)
                        .buildPreMacroCfirFile(source, sourceFile, source.toSourceLinesMapping())
                } else {
                    val psiFile = com.intellij.psi.PsiFileFactory.getInstance(project).createFileFromText(
                        "ifavailable.cj",
                        org.cangnova.cangjie.lang.CangJieFileType.INSTANCE,
                        source,
                    ) as org.cangnova.cangjie.psi.CjFile
                    org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder(
                        session,
                        annotationMetadata = CfirAnnotationMetadataBuilder(),
                    ).buildPreMacroCfirFile(psiFile)
                }
                val sourceSurface = preFile.surfaces.single() as MacroSurfaceExpr
                assertTrue(sourceSurface.capturedRawSyntax.orEmpty().startsWith("@IfAvailable("))
                val pre = buildPreMacroRawFiles(session, listOf(preFile))
                val configuration = CompilerConfiguration().apply {
                    put(org.cangnova.cangjie.config.CommonConfigurationKeys.USE_LIGHT_TREE, lightTree)
                    installDefaultMacroFragmentParserFactory(project)
                    macroExecutorFactory = MacroExecutorFactory {
                        executorCreations++
                        executor
                    }
                }
                val context = bindMacroImports(pre, buildMacroSymbolIndex(pre = pre))
                val classification = MacroDemandClassification.create(pre)
                classification.freezeFinal(macroArtifactDefinitions = context.symbolIndex.foreigns)
                assertTrue(classification.finalDecisions.single().resolution is MacroResolution.BuiltinNonMacro)

                val result = FrontendMacroConstructionService(configuration).expand(
                    pre = pre,
                    context = context,
                    classification = classification,
                    mode = MacroConstructionService.Mode.STRICT,
                )

                assertInstanceOf(MacroConstructionResult.Success::class.java, result, "LightTree=$lightTree")
                assertEquals(0, executorCreations, "IfAvailable must not initialize the user macro executor; LightTree=$lightTree")
                assertTrue(executor.calls.isEmpty(), "IfAvailable must not execute as a user macro; LightTree=$lightTree")
                assertEquals(
                    sourceSurface.capturedRawSyntax,
                    result.registry.generatedDisplayText.getValue(sourceSurface.surfaceId),
                )
                val function = preFile.cfirFile.declarations.filterIsInstance<CfirNamedFunction>().single()
                val expression = function.body!!.statements.single() as CfirIfAvailableExpression
                assertEquals(Name.identifier("level"), expression.conditionName)
                val conditionNameSource = checkNotNull(expression.conditionNameSource)
                val mappedConditionName = checkNotNull(session.macroExpansionRegistry)
                    .originSourceForGeneratedSource(conditionNameSource)
                assertEquals(source.indexOf("level: 19"), mappedConditionName?.startOffset)
                assertEquals(source.indexOf("level: 19") + "level".length, mappedConditionName?.endOffset)
                val offsetOnlyConditionName = CjOffsetsOnlySourceElement(
                    conditionNameSource.startOffset,
                    conditionNameSource.endOffset,
                    sourceAnchor = conditionNameSource,
                )
                val mappedOffsetOnlyConditionName = checkNotNull(session.macroExpansionRegistry)
                    .originSourceForGeneratedSource(offsetOnlyConditionName)
                assertEquals(source.indexOf("level: 19"), mappedOffsetOnlyConditionName?.startOffset)
                assertEquals(source.indexOf("level: 19") + "level".length, mappedOffsetOnlyConditionName?.endOffset)
                val thenBranch = expression.thenBranch as CfirAnonymousFunctionExpression
                val elseBranch = expression.elseBranch as CfirAnonymousFunctionExpression
                assertTrue(thenBranch.anonymousFunction.body!!.statements.isNotEmpty())
                assertTrue(elseBranch.anonymousFunction.body!!.statements.isNotEmpty())
            }
        } finally {
            com.intellij.openapi.util.Disposer.dispose(disposable)
        }
    }

    /** 声明位点仍按原声明 scope 重解析完整调用，并由真实声明槽拒绝表达式结果。 */
    @Test
    fun sourceIfAvailableDeclarationPreservesInvocationAndDeclarationSlotInPsiAndLightTree() {
        val disposable = com.intellij.openapi.util.Disposer.newDisposable("ifavailable-declaration-construction")
        try {
            val project = org.cangnova.cangjie.CangJieCoreEnvironment.create(
                disposable, org.cangnova.cangjie.CangJieCoreEnvironmentMode.Production,
            ).projectEnvironment.project
            val source = """
                package sample
                @IfAvailable(level: 19, {=> availableBranch() }, {=> unavailableBranch() })
                func use(): Unit {}
            """.trimIndent()

            for (lightTree in listOf(false, true)) {
                val session = productionParserSession()
                val preFile = if (lightTree) {
                    val sourceFile = CjInMemoryTextSourceFile("ifavailable-declaration.cj", "memory/ifavailable-declaration.cj", source)
                    org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir(session, session.cangjieScopeProvider)
                        .buildPreMacroCfirFile(source, sourceFile, source.toSourceLinesMapping())
                } else {
                    val psiFile = com.intellij.psi.PsiFileFactory.getInstance(project).createFileFromText(
                        "ifavailable-declaration.cj",
                        org.cangnova.cangjie.lang.CangJieFileType.INSTANCE,
                        source,
                    ) as org.cangnova.cangjie.psi.CjFile
                    org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder(
                        session,
                        annotationMetadata = CfirAnnotationMetadataBuilder(),
                    ).buildPreMacroCfirFile(psiFile)
                }
                assertEquals(1, preFile.surfaces.size, "LightTree=$lightTree")
                val surface = preFile.surfaces.single() as MacroSurfaceDecl
                assertTrue(surface.capturedRawSyntax.orEmpty().startsWith("@IfAvailable("))
                assertNull(surface.replaceHandle.annotationCarrier)
                assertEquals(
                    listOf("CfirMacroExpansionDeclarationImpl", "CfirNamedFunctionImpl"),
                    preFile.cfirFile.declarations.map { it::class.simpleName },
                    "LightTree=$lightTree",
                )
                val pre = buildPreMacroRawFiles(session, listOf(preFile))
                val context = bindMacroImports(pre, buildMacroSymbolIndex(pre = pre))
                val classification = MacroDemandClassification.create(pre)
                classification.freezeFinal(macroArtifactDefinitions = context.symbolIndex.foreigns)
                val decision = classification.finalDecisions.single()
                assertTrue(decision.resolution is MacroResolution.BuiltinNonMacro)
                assertEquals(MacroReplacementSlotType.DECLARATION, decision.slotType)

                var executorCreations = 0
                val parserInputs = mutableListOf<MacroFragmentInput>()
                val configuration = CompilerConfiguration().apply {
                    put(org.cangnova.cangjie.config.CommonConfigurationKeys.USE_LIGHT_TREE, lightTree)
                    installDefaultMacroFragmentParserFactory(project)
                    macroExecutorFactory = MacroExecutorFactory {
                        executorCreations++
                        RecordingExecutor()
                    }
                }
                val productionParserFactory = checkNotNull(configuration.macroFragmentParserFactory)
                configuration.macroFragmentParserFactory = MacroFragmentParserFactory { parserSession ->
                    val productionParser = productionParserFactory.create(parserSession)
                    object : MacroFragmentParser {
                        override fun parse(input: MacroFragmentInput): MacroFragmentResult {
                            parserInputs += input
                            return productionParser.parse(input)
                        }
                    }
                }

                val result = FrontendMacroConstructionService(configuration).expand(
                    pre = pre,
                    context = context,
                    classification = classification,
                    mode = MacroConstructionService.Mode.STRICT,
                )

                assertInstanceOf(MacroConstructionResult.Failed::class.java, result, "LightTree=$lightTree")
                assertEquals(0, executorCreations, "IfAvailable declaration recovery must not execute a user macro")
                val parsedTokenText = parserInputs.single().tokens.joinToString(separator = "") { it.text }
                assertTrue(parsedTokenText.startsWith("@IfAvailable"))
                assertTrue(parsedTokenText.contains("availableBranch"))
                assertTrue(parsedTokenText.contains("unavailableBranch"))
                assertFalse(parsedTokenText.contains("func"))
                assertTrue((result as MacroConstructionResult.Failed).registry.diagnostics.any {
                    it.kind == MacroConstructionDiagnostic.Kind.MACRO_EXPECT_DECLARATION
                })

                val successSession = productionParserSession()
                val successPreFile = if (lightTree) {
                    val sourceFile = CjInMemoryTextSourceFile("ifavailable-declaration.cj", "memory/ifavailable-declaration.cj", source)
                    org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir(successSession, successSession.cangjieScopeProvider)
                        .buildPreMacroCfirFile(source, sourceFile, source.toSourceLinesMapping())
                } else {
                    val psiFile = com.intellij.psi.PsiFileFactory.getInstance(project).createFileFromText(
                        "ifavailable-declaration.cj",
                        org.cangnova.cangjie.lang.CangJieFileType.INSTANCE,
                        source,
                    ) as org.cangnova.cangjie.psi.CjFile
                    org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder(
                        successSession,
                        annotationMetadata = CfirAnnotationMetadataBuilder(),
                    ).buildPreMacroCfirFile(psiFile)
                }
                val successPre = buildPreMacroRawFiles(successSession, listOf(successPreFile))
                val successContext = bindMacroImports(successPre, buildMacroSymbolIndex(pre = successPre))
                val successClassification = MacroDemandClassification.create(successPre)
                successClassification.freezeFinal(macroArtifactDefinitions = successContext.symbolIndex.foreigns)
                val generatedFunction = namedFunction(
                    moduleData = successPreFile.cfirFile.moduleData,
                    packageFqName = FqName("sample"),
                    name = "generated",
                )
                val successfulConfiguration = CompilerConfiguration().apply {
                    put(org.cangnova.cangjie.config.CommonConfigurationKeys.USE_LIGHT_TREE, lightTree)
                    macroExecutorFactory = MacroExecutorFactory {
                        executorCreations++
                        RecordingExecutor()
                    }
                    macroFragmentParserFactory = MacroFragmentParserFactory {
                        object : MacroFragmentParser {
                            override fun parse(input: MacroFragmentInput): MacroFragmentResult =
                                MacroFragmentResult.Success(
                                    input.node,
                                    input.tokens,
                                    input.mode,
                                    MacroFragmentPayload.single(generatedFunction),
                                )
                        }
                    }
                }
                val successfulResult = FrontendMacroConstructionService(successfulConfiguration).expand(
                    pre = successPre,
                    context = successContext,
                    classification = successClassification,
                    mode = MacroConstructionService.Mode.STRICT,
                )
                assertInstanceOf(MacroConstructionResult.Success::class.java, successfulResult, "LightTree=$lightTree")
                assertEquals(
                    listOf("generated", "use"),
                    successPreFile.cfirFile.declarations.filterIsInstance<CfirNamedFunction>().map { it.name.asString() },
                    "The expanded declaration must replace the real macro declaration slot; LightTree=$lightTree",
                )
                assertTrue(successPreFile.cfirFile.declarations.none { it is org.cangnova.cangjie.cfir.declarations.CfirMacroExpansionDeclaration })
                assertEquals(0, executorCreations, "IfAvailable declaration recovery must not execute a user macro")
            }
        } finally {
            com.intellij.openapi.util.Disposer.dispose(disposable)
        }
    }

    /** class body 中的独立声明宏调用在 PSI 与 LightTree 中指向同一真实成员槽。 */
    @Test
    fun sourceIfAvailableDeclarationSlotIsRetainedInsideClassBodyInPsiAndLightTree() {
        val disposable = com.intellij.openapi.util.Disposer.newDisposable("ifavailable-class-declaration-slot")
        try {
            val project = org.cangnova.cangjie.CangJieCoreEnvironment.create(
                disposable, org.cangnova.cangjie.CangJieCoreEnvironmentMode.Production,
            ).projectEnvironment.project
            val source = """
                package sample
                class Host {
                    @IfAvailable(level: 19, {=> availableBranch() }, {=> unavailableBranch() })
                }
            """.trimIndent()

            for (lightTree in listOf(false, true)) {
                val session = productionParserSession()
                val preFile = if (lightTree) {
                    val sourceFile = CjInMemoryTextSourceFile("ifavailable-member.cj", "memory/ifavailable-member.cj", source)
                    org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir(session, session.cangjieScopeProvider)
                        .buildPreMacroCfirFile(source, sourceFile, source.toSourceLinesMapping())
                } else {
                    val psiFile = com.intellij.psi.PsiFileFactory.getInstance(project).createFileFromText(
                        "ifavailable-member.cj",
                        org.cangnova.cangjie.lang.CangJieFileType.INSTANCE,
                        source,
                    ) as org.cangnova.cangjie.psi.CjFile
                    org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder(
                        session,
                        annotationMetadata = CfirAnnotationMetadataBuilder(),
                    ).buildPreMacroCfirFile(psiFile)
                }
                val host = preFile.cfirFile.declarations
                    .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirClass>()
                    .single()
                val invocationCarrier = host.declarations
                    .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirMacroExpansionDeclaration>()
                    .single()
                val surface = preFile.surfaces.single() as MacroSurfaceDecl

                assertSame(invocationCarrier, surface.replaceHandle.carrier, "LightTree=$lightTree")
                assertNull(surface.replaceHandle.annotationCarrier)
                assertTrue(surface.capturedRawSyntax.orEmpty().startsWith("@IfAvailable("))
            }
        } finally {
            com.intellij.openapi.util.Disposer.dispose(disposable)
        }
    }

    /** 宏声明槽替换改变成员图，但不能改变宿主在原始源码中确定的默认实现事实。 */
    @Test
    fun macroDeclarationReplacementPreservesCommonHostDefaultFactInPsiAndLightTree() {
        val disposable = com.intellij.openapi.util.Disposer.newDisposable("common-host-macro-replacement")
        try {
            val project = org.cangnova.cangjie.CangJieCoreEnvironment.create(
                disposable, org.cangnova.cangjie.CangJieCoreEnvironmentMode.Production,
            ).projectEnvironment.project
            for (lightTree in listOf(false, true)) {
                for (hasDefault in listOf(false, true)) {
                    val initializer = if (hasDefault) " = 1" else ""
                    val source = """
                        package sample
                        public common class Host {
                            public common let value: Int64$initializer
                            @IfAvailable(level: 19, {=> availableBranch() }, {=> unavailableBranch() })
                        }
                    """.trimIndent()
                    val session = productionParserSession()
                    val preFile = if (lightTree) {
                        val sourceFile = CjInMemoryTextSourceFile("host.cj", "memory/host.cj", source)
                        org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir(session, session.cangjieScopeProvider)
                            .buildPreMacroCfirFile(source, sourceFile, source.toSourceLinesMapping())
                    } else {
                        val psiFile = com.intellij.psi.PsiFileFactory.getInstance(project).createFileFromText(
                            "host.cj", org.cangnova.cangjie.lang.CangJieFileType.INSTANCE, source,
                        ) as org.cangnova.cangjie.psi.CjFile
                        org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder(
                            session, annotationMetadata = CfirAnnotationMetadataBuilder(),
                        ).buildPreMacroCfirFile(psiFile)
                    }
                    val host = preFile.cfirFile.declarations
                        .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirClass>().single()
                    val before = host.cjmpHasCommonDefault()
                    assertEquals(hasDefault, before, "LightTree=$lightTree")
                    val carrier = host.declarations
                        .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirMacroExpansionDeclaration>().single()
                    val originalMembers = host.declarations.toList()
                    val generated = namedFunction(preFile.cfirFile.moduleData, FqName("sample"), "generated")
                    generated.replaceStatus(CfirDeclarationStatusImpl().apply { isCommon = true })
                    assertFalse(generated.cjmpHasCommonDefault())
                    val pre = buildPreMacroRawFiles(session, listOf(preFile))
                    val context = bindMacroImports(pre, buildMacroSymbolIndex(pre = pre))
                    val classification = MacroDemandClassification.create(pre)
                    classification.freezeFinal(macroArtifactDefinitions = context.symbolIndex.foreigns)
                    val configuration = CompilerConfiguration().apply {
                        put(org.cangnova.cangjie.config.CommonConfigurationKeys.USE_LIGHT_TREE, lightTree)
                        macroFragmentParserFactory = MacroFragmentParserFactory {
                            object : MacroFragmentParser {
                                override fun parse(input: MacroFragmentInput): MacroFragmentResult =
                                    MacroFragmentResult.Success(input.node, input.tokens, input.mode, MacroFragmentPayload.single(generated))
                            }
                        }
                    }
                    val result = FrontendMacroConstructionService(configuration).expand(
                        pre = pre, context = context, classification = classification,
                        mode = MacroConstructionService.Mode.STRICT,
                    )
                    assertInstanceOf(MacroConstructionResult.Success::class.java, result, "LightTree=$lightTree")
                    assertTrue(originalMembers.any { it === carrier })
                    assertTrue(host.declarations.none { it === carrier })
                    assertTrue(host.declarations.any { it === generated })
                    assertEquals(before, host.cjmpHasCommonDefault(), "LightTree=$lightTree, original=$hasDefault")
                }
            }
        } finally {
            com.intellij.openapi.util.Disposer.dispose(disposable)
        }
    }

    /** 使用生产 parser，防止宏重新解析绕过文件角色和函数签名语法。 */
    @Test
    fun productionMacroParserRejectsInvalidDeclarationSyntaxInPsiAndLightTree() {
        val disposable = com.intellij.openapi.util.Disposer.newDisposable("macro-parser-syntax")
        try {
            val project = org.cangnova.cangjie.CangJieCoreEnvironment.create(
                disposable, org.cangnova.cangjie.CangJieCoreEnvironmentMode.Production,
            ).projectEnvironment.project
            val originalSourceText = "package sample\n@GenerateDeclaration func original(): Unit {}\n"
            val originalSourceFile = CjPsiFactory(project).createFile("macroInvocation.cj", originalSourceText)
            val invocationRange = MacroSurfaceSourceRange(
                originalSourceFile.toCjPsiSourceElement(),
                originalSourceText.indexOf('@'),
                originalSourceText.lastIndexOf('}') + 1,
            )
            val cases = listOf(
                Triple("common func generated(): Unit", org.cangnova.cangjie.cfir.session.CfirCjmpMode.NONE,
                    org.cangnova.cangjie.messages.CangJieParsingBundle.message("parsing.error.cjmp.common.in.non.common.file")),
                Triple("specific func generated(): Unit", org.cangnova.cangjie.cfir.session.CfirCjmpMode.NONE,
                    org.cangnova.cangjie.messages.CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file")),
                Triple("common func generated()", org.cangnova.cangjie.cfir.session.CfirCjmpMode.COMMON,
                    org.cangnova.cangjie.messages.CangJieParsingBundle.message("parsing.error.cjmp.function.return.type.required", "common")),
                Triple("common let (a, b) = (1, 2)", org.cangnova.cangjie.cfir.session.CfirCjmpMode.COMMON,
                    org.cangnova.cangjie.messages.CangJieParsingBundle.message("parsing.error.cjmp.pattern.declaration", "tuple", "common")),
                Triple("common let Some(x) = value", org.cangnova.cangjie.cfir.session.CfirCjmpMode.COMMON,
                    org.cangnova.cangjie.messages.CangJieParsingBundle.message("parsing.error.cjmp.pattern.declaration", "enum", "common")),
                Triple("common let _ = value", org.cangnova.cangjie.cfir.session.CfirCjmpMode.COMMON,
                    org.cangnova.cangjie.messages.CangJieParsingBundle.message("parsing.error.cjmp.pattern.declaration", "wildcard", "common")),
            )
            for (lightTree in listOf(false, true)) {
                for ((text, cjmpMode, expected) in cases) {
                    val session = object : CfirSession(CfirSession.Kind.Source) {}
                    val moduleData = TestModuleData(session)
                    session.register(CfirModuleData::class, moduleData)
                    session.register(org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider::class,
                        org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider())
                    session.register(org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent::class,
                        org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent(org.cangnova.cangjie.LanguageVersionSettingsImpl.DEFAULT))
                    session.register(org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent::class,
                        org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent(explicitMode = cjmpMode))
                    val packageName = FqName("sample")
                    val original = namedFunction(moduleData, packageName, "original")
                    val file = fileWithParsedSource(moduleData, packageName, originalSourceFile, original)
                    assertEquals(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), file.parsingFileFacts)
                    val surface = declarationSurface(3101L, FqName("macros.GenerateDeclaration"), packageName, original)
                        .copy(sourceRange = invocationRange)
                    val pre = buildPreMacroRawFiles(session, listOf(PreMacroCfirFile(file, listOf(surface), CfirAnnotationMetadataBuilder())))
                    val configuration = CompilerConfiguration().apply {
                        put(org.cangnova.cangjie.config.CommonConfigurationKeys.USE_LIGHT_TREE, lightTree)
                        macroExecutorFactory = MacroExecutorFactory {
                            RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo(text)), text))
                        }
                        installDefaultMacroFragmentParserFactory(project)
                    }
                    val result = FrontendMacroConstructionService(configuration).expandWithClassification(
                        pre, contextWithArtifact(pre, "GenerateDeclaration"), MacroConstructionService.Mode.STRICT,
                    )
                    assertInstanceOf(MacroConstructionResult.Failed::class.java, result, "$lightTree: $text")
                    assertTrue(result.registry.diagnostics.isEmpty(), "${result.registry.diagnostics}")
                    val failure = result.registry.parsingFailures.single()
                    assertEquals(text, failure.parsedText)
                    assertTrue(failure.diagnostics.any { it.message == expected }, "$lightTree: ${failure.diagnostics}")
                    val expectedOrigin = if (cjmpMode == org.cangnova.cangjie.cfir.session.CfirCjmpMode.NONE)
                        org.cangnova.cangjie.messages.ParsingDiagnostic.Origin.ORIGINAL_FILE
                    else org.cangnova.cangjie.messages.ParsingDiagnostic.Origin.INPUT
                    val expectedDiagnostic = failure.diagnostics.single { it.message == expected }
                    assertEquals(expectedOrigin, expectedDiagnostic.origin)
                    if (expectedOrigin == org.cangnova.cangjie.messages.ParsingDiagnostic.Origin.INPUT) {
                        val mapped = checkNotNull(failure.sourceMap).mapRange(
                            expectedDiagnostic.startOffset,
                            expectedDiagnostic.endOffset,
                        )
                        assertEquals(invocationRange.startOffset, mapped?.startOffset, "Generated text is not original source: $lightTree")
                        assertEquals(invocationRange.endOffset, mapped?.endOffset, "Generated text is not original source: $lightTree")
                    }
                    assertSame(original, file.declarations.single())
                }
            }
        } finally {
            com.intellij.openapi.util.Disposer.dispose(disposable)
        }
    }

    /** 文件角色依实际 parser 顺序传递；失败解析已识别的角色也保留，不同文件之间不串状态。 */
    @Test
    fun productionMacroParserMaintainsOriginalFileTransactionAcrossFragments() {
        val disposable = com.intellij.openapi.util.Disposer.newDisposable("macro-parser-file-transaction")
        try {
            val project = org.cangnova.cangjie.CangJieCoreEnvironment.create(
                disposable, org.cangnova.cangjie.CangJieCoreEnvironmentMode.Production,
            ).projectEnvironment.project
            for (lightTree in listOf(false, true)) for (mode in MacroConstructionService.Mode.entries) {
                for (separateFiles in listOf(false, true)) for (brokenFirst in listOf(false, true)) {
                    for (firstRole in listOf(CjmpFileRole.COMMON, CjmpFileRole.SPECIFIC)) for (version in listOf(
                        org.cangnova.cangjie.LanguageVersion.CANGJIE_1_1_3,
                        org.cangnova.cangjie.LanguageVersion.CANGJIE_1_2_0_ALPHA_19,
                        org.cangnova.cangjie.LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
                    )) {
                        val mixedAllowed = version >= org.cangnova.cangjie.LanguageVersion.CANGJIE_1_2_0_ALPHA_20
                        val session = productionParserSession(version)
                        val moduleData = session.moduleData
                        val packageName = FqName("sample")
                        val originals = listOf("first", "second").map { namedFunction(moduleData, packageName, it) }
                        val fileOwners = if (separateFiles) originals.map { listOf(it) } else listOf(originals)
                        val files = fileOwners.map { owners ->
                            val sourceText = "package sample\n" + owners.joinToString("\n") {
                                "@macros.GenerateDeclaration func ${it.name.asString()}(): Unit {}"
                            } + "\n"
                            val sourceFile = CjPsiFactory(project, markGenerated = false).createFile("sample.cj", sourceText)
                            sourceFile.cjmpParsingContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
                            fileWithParsedSource(moduleData, packageName, sourceFile, *owners.toTypedArray())
                        }
                        files.forEach { assertEquals(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), it.parsingFileFacts) }
                        val surfaces = originals.mapIndexed { index, original ->
                            declarationSurface(3201L + index, FqName("macros.GenerateDeclaration"), packageName, original)
                        }
                        val pre = buildPreMacroRawFiles(session, files.mapIndexed { index, file ->
                            PreMacroCfirFile(
                                file,
                                if (separateFiles) listOf(surfaces[index]) else surfaces,
                                CfirAnnotationMetadataBuilder(),
                            )
                        })
                        val opposite = if (firstRole == CjmpFileRole.COMMON) CjmpFileRole.SPECIFIC else CjmpFileRole.COMMON
                        val texts = listOf(
                            "${firstRole.name.lowercase()} func firstGenerated()" + if (brokenFirst) "" else ": Unit {}",
                            "${opposite.name.lowercase()} func secondGenerated(): Unit {}",
                        ).iterator()
                        val delegate = RecordingExecutor()
                        val executor = object : MacroExecutor by delegate {
                            override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> = calls.map {
                                val text = texts.next()
                                MacroExpansionResult.Success(listOf(tokenInfo(text)), text)
                            }
                        }
                        val configuration = CompilerConfiguration().apply {
                            put(org.cangnova.cangjie.config.CommonConfigurationKeys.USE_LIGHT_TREE, lightTree)
                            macroExecutorFactory = MacroExecutorFactory { executor }
                            installDefaultMacroFragmentParserFactory(project)
                        }
                        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
                            pre, contextWithArtifact(pre, "GenerateDeclaration"), mode,
                        )
                        val label = "$version/$lightTree/$mode/separate=$separateFiles/broken=$brokenFirst/$firstRole"
                        assertTrue(result.registry.diagnostics.isEmpty(), "$label: ${result.registry.diagnostics}")
                        val mixed = org.cangnova.cangjie.messages.CangJieParsingBundle.message("parsing.error.cjmp.common.and.specific.in.same.file")
                        assertEquals(!separateFiles && !mixedAllowed, result.registry.parsingFailures.any { failure ->
                            failure.diagnostics.any { it.message == mixed }
                        }, label)
                        result.registry.parsingFailures.flatMap { it.diagnostics }.filter { it.message == mixed }.forEach {
                            assertEquals(org.cangnova.cangjie.messages.ParsingDiagnostic.Origin.ORIGINAL_FILE, it.origin, label)
                        }
                        // 查询返回原文件的当前事务快照；同文件的两个 surface 都看到后续角色更新。
                        assertEquals(if (!separateFiles && mixedAllowed) CjmpFileRole.BOTH else firstRole,
                            result.registry.parsingFactsForSurface(surfaces.first().surfaceId)?.cjmpRole, label)
                        assertEquals(if (separateFiles) opposite else if (mixedAllowed) CjmpFileRole.BOTH else firstRole,
                            result.registry.parsingFactsForSurface(surfaces.last().surfaceId)?.cjmpRole, label)
                        files.forEach { assertEquals(CjmpFileRole.NONE, it.parsingFileFacts?.cjmpRole, label) }
                        if (brokenFirst || (!separateFiles && !mixedAllowed)) assertInstanceOf(MacroConstructionResult.Failed::class.java, result, label)
                        else assertInstanceOf(MacroConstructionResult.Success::class.java, result, label)
                    }
                }
            }
        } finally {
            com.intellij.openapi.util.Disposer.dispose(disposable)
        }
    }

    /** 宏声明重解析保留 `.cj.d` 无体规则和 raw 状态；宿主角色也须直接继承。 */
    @Test
    fun productionMacroParserInheritsDeclarationSourceKindAndExistingRole() {
        val disposable = com.intellij.openapi.util.Disposer.newDisposable("macro-parser-origin-kind")
        try {
            val project = org.cangnova.cangjie.CangJieCoreEnvironment.create(
                disposable, org.cangnova.cangjie.CangJieCoreEnvironmentMode.Production,
            ).projectEnvironment.project
            for (lightTree in listOf(false, true)) for (sourceKind in listOf(CjSourceKind.SOURCE, CjSourceKind.DECLARATION)) {
                for (initialRole in listOf(CjmpFileRole.NONE, CjmpFileRole.COMMON, CjmpFileRole.SPECIFIC)) {
                    val session = productionParserSession()
                    val packageName = FqName("sample")
                    val original = namedFunction(session.moduleData, packageName, "original")
                    val originalFacts = ParsingFileFacts(sourceKind, initialRole)
                    // 真实 viewProvider 的 FileType 决定文件种类；普通 CjPsiFactory 固定创建 SOURCE。
                    val sourceFileName = if (sourceKind == CjSourceKind.DECLARATION) "sample.cj.d" else "sample.cj"
                    val body = if (sourceKind == CjSourceKind.SOURCE && initialRole != CjmpFileRole.COMMON) " {}" else ""
                    val sourceFile = com.intellij.psi.PsiFileFactory.getInstance(project).createFileFromText(
                        sourceFileName,
                        if (sourceKind == CjSourceKind.DECLARATION) org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
                        else org.cangnova.cangjie.lang.CangJieFileType.INSTANCE,
                        "package sample\n@macros.GenerateDeclaration func original(): Unit$body\n",
                        com.intellij.util.LocalTimeCounter.currentTime(), false, false,
                    ) as org.cangnova.cangjie.psi.CjFile
                    assertNull(sourceFile.parsingSourceContextOrNull(), "入口角色必须在首次解析前安装")
                    sourceFile.cjmpParsingContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
                        .copy(initialFileRole = initialRole)
                    val file = fileWithParsedSource(session.moduleData, packageName, sourceFile, original)
                    assertEquals(originalFacts, file.parsingFileFacts)
                    val surface = declarationSurface(3301L, FqName("macros.GenerateDeclaration"), packageName, original)
                    val pre = buildPreMacroRawFiles(session, listOf(PreMacroCfirFile(file, listOf(surface), CfirAnnotationMetadataBuilder())))
                    val text = if (initialRole == CjmpFileRole.NONE) "func generated(): Unit"
                        else "${if (initialRole == CjmpFileRole.COMMON) "specific" else "common"} func generated(): Unit {}"
                    val configuration = CompilerConfiguration().apply {
                        put(org.cangnova.cangjie.config.CommonConfigurationKeys.USE_LIGHT_TREE, lightTree)
                        macroExecutorFactory = MacroExecutorFactory {
                            RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo(text)), text))
                        }
                        installDefaultMacroFragmentParserFactory(project)
                    }
                    val result = FrontendMacroConstructionService(configuration).expandWithClassification(
                        pre, contextWithArtifact(pre, "GenerateDeclaration"), MacroConstructionService.Mode.STRICT,
                    )
                    val label = "$lightTree/$sourceKind/$initialRole"
                    assertTrue(result.registry.diagnostics.isEmpty(), "$label: ${result.registry.diagnostics}")
                    assertEquals(originalFacts, result.registry.parsingFactsForSurface(surface.surfaceId), label)
                    assertEquals(originalFacts, file.parsingFileFacts, label)
                    if (initialRole != CjmpFileRole.NONE) {
                        assertInstanceOf(MacroConstructionResult.Failed::class.java, result, label)
                        val mixed = org.cangnova.cangjie.messages.CangJieParsingBundle.message("parsing.error.cjmp.common.and.specific.in.same.file")
                        assertTrue(result.registry.parsingFailures.single().diagnostics.any { it.message == mixed }, label)
                    } else if (sourceKind == CjSourceKind.DECLARATION) {
                        assertInstanceOf(MacroConstructionResult.Success::class.java, result, label)
                        val generated = (result as MacroConstructionResult.Success).recordableFiles.files.single().declarations.single() as CfirNamedFunction
                        assertEquals("generated", generated.name.asString(), label)
                        assertNotEquals(org.cangnova.cangjie.descriptors.Modality.ABSTRACT, generated.status.modality, label)
                    } else {
                        assertInstanceOf(MacroConstructionResult.Failed::class.java, result, label)
                        assertTrue(result.registry.parsingFailures.isNotEmpty(), label)
                    }
                }
            }
        } finally {
            com.intellij.openapi.util.Disposer.dispose(disposable)
        }
    }

    /**
     * 真实 PSI 提供这份宿主文件的输入、来源和已发布文法事实；声明仍是测试显式构造的
     * 合成 CFIR owner，保留 replace handle 与原断言所检查的对象身份，不声称其由 PSI raw 构建生成。
     */
    private fun fileWithParsedSource(
        moduleData: CfirModuleData,
        packageFqName: FqName,
        parsedSource: org.cangnova.cangjie.psi.CjFile,
        vararg owners: CfirNamedFunction,
    ): CfirFile {
        val root = parsedSource.node
        // 根正文必须实际展开，来源查询本身不会触发 parser。
        root.firstChildNode
        val sourceContext = checkNotNull(parsedSource.parsingSourceContextOrNull()) {
            "The fixture's actual PSI parser must publish its source context."
        }
        sourceContext.requireInput(root.chars)
        check(parsedSource.packageFqName == packageFqName)
        val facts = checkNotNull(parsedSource.parsedFileFacts)
        return buildFileCopy(fileWithDeclarations(moduleData, packageFqName, *owners)) {
            symbol = CfirFileSymbol()
            name = parsedSource.name
            source = parsedSource.toCjPsiSourceElement()
            sourceFile = CjPsiSourceFile(parsedSource)
            sourceFileLinesMapping = root.chars.toString().toSourceLinesMapping()
        }.also { it.publishParsingFileFacts(facts, sourceContext) }
    }

    /** 测试调用同时启用两个独立编译谓词，保留真实 parser/version/raw-builder 装配。 */
    private fun productionParserSession(
        version: org.cangnova.cangjie.LanguageVersion = org.cangnova.cangjie.LanguageVersion.LATEST_STABLE,
    ): CfirSession {
        val languageVersionSettings = org.cangnova.cangjie.LanguageVersionSettingsImpl(version)
        val commonPartInputs = (
            org.cangnova.cangjie.cfir.entrypoint.configuration.CjmpCommonPartInputResolver.resolve(
                commonPartCjoPaths = listOf("common.cjo"),
                commonPartChirPaths = emptyList(),
                languageVersionSettings = languageVersionSettings,
            ) as org.cangnova.cangjie.cfir.entrypoint.configuration.CjmpCommonPartInputResolution.Valid
        ).inputs
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        session.register(CfirModuleData::class, TestModuleData(session))
        session.register(org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider::class,
            org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider())
        session.register(org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent::class,
            org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent(languageVersionSettings))
        session.register(org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent::class,
            org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent(
                commonPartInputs = commonPartInputs,
                isChirOutput = true,
            ))
        return session
    }

    /** STRICT 与 DEGRADED 均不能拼接 parser 已拒绝的宏产物。 */
    @Test
    fun parserSyntaxFailureRejectsConstructionWithoutSemanticDiagnosticInEitherMode() {
        for (mode in MacroConstructionService.Mode.entries) {
            val fixture = macroExpressionCarrierFixture()
            val original = fixture.function.body!!.statements.single()
            val configuration = CompilerConfiguration().apply {
                macroExecutorFactory = MacroExecutorFactory {
                    RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo("invalid")), "invalid"))
                }
                macroFragmentParserFactory = MacroFragmentParserFactory {
                    object : MacroFragmentParser {
                        override fun parse(input: MacroFragmentInput): MacroFragmentResult {
                            val parsedText = "invalid"
                            val collector = CjParsingDiagnosticCollector(CjParserSourceContext.complete(parsedText))
                            collector.report(ParsingDiagnostic(0, 7, "Invalid macro syntax"))
                            collector.finish()
                            return MacroFragmentResult.SyntaxFailure(
                                input.node, parsedText, collector.publication,
                            )
                        }
                    }
                }
            }
            val result = FrontendMacroConstructionService(configuration).expandWithClassification(
                pre = fixture.pre,
                context = contextWithArtifact(fixture.pre, "Generated"),
                mode = mode,
            )
            assertInstanceOf(MacroConstructionResult.Failed::class.java, result)
            assertEquals(1, result.registry.parsingFailures.size)
            assertTrue(result.registry.diagnostics.isEmpty(), "${result.registry.diagnostics}")
            assertSame(original, fixture.function.body!!.statements.single())
        }
    }

    /** 合法语法产生的 roots 仍须匹配最终替换槽，避免把形状错误拖到 splicer 内部异常。 */
    @Test
    fun parserRootShapeFailureRejectsConstructionBeforeSplicing() {
        val fixture = macroExpressionCarrierFixture()
        val original = fixture.function.body!!.statements.single()
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory {
                RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo("expanded")), "expanded"))
            }
            macroFragmentParserFactory = MacroFragmentParserFactory {
                object : MacroFragmentParser {
                    override fun parse(input: MacroFragmentInput): MacroFragmentResult = MacroFragmentResult.Success(
                        originNode = input.node,
                        tokens = input.tokens,
                        mode = input.mode,
                        payload = MacroFragmentPayload.EMPTY,
                    )
                }
            }
        }

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = fixture.pre,
            context = contextWithArtifact(fixture.pre, "Generated"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        val registry = (result as? MacroConstructionResult.Failed)?.registry
            ?: error("A fragment without an expression root must fail construction, got ${result::class.simpleName}")
        val shapeFailure = registry.diagnostics.singleOrNull {
            it.kind == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Kind.MACRO_EXPANSION_FAILED
        }
        assertTrue(shapeFailure?.message?.contains("exactly one expression root") == true)
        assertSame(original, fixture.function.body!!.statements.single())
    }

    /** DEGRADED 仅处理结构化编译失败；parser/splicer 不变量与取消必须清理后原样抛出。 */
    @Test
    fun invariantAndCancellationFailuresAbortMetadataAndCloseExecutorWithoutDegrading() {
        for (failInParser in listOf(true, false)) {
            for (expected in listOf(IllegalStateException("metadata invariant"), CancellationException("cancel construction"))) {
                val fixture = macroExpressionCarrierFixture()
                val preFile = fixture.pre.files.single()
                val metadata = preFile.annotationMetadata
                val original = fixture.function.body!!.statements.single()
                val annotationSource = MetadataFixtureSource("@!FailureTag")
                val annotation = buildAnnotationCall {
                    source = annotationSource
                    typeRef = buildImplicitTypeRef()
                    annotationSourceName = "FailureTag"
                    sourceModuleName = "sample"
                    forcedCustom = true
                    isCompileTimeVisible = true
                    argumentList = buildArgumentList()
                    calleeReference = buildNamedReference { name = Name.identifier("FailureTag") }
                    containingDeclarationSymbol = fixture.function.symbol
                }
                fixture.function.replaceAnnotations(listOf(annotation))
                metadata.record(CfirAnnotationSlotSnapshot(
                    owner = fixture.function,
                    annotationIndex = 0,
                    originalAnnotation = annotation,
                    rawSyntax = "@!FailureTag",
                    forcedCustom = true,
                    isCompileTimeVisible = true,
                    annotationSource = annotationSource,
                    qualifiedName = FqName("sample.FailureTag"),
                    argumentText = null,
                    tokens = listOf(MacroSurfaceToken("@!FailureTag", 0, 12)),
                    callSite = MacroCallSite.DECLARATION,
                ))
                val published = fixture.pre.session.ensureAnnotationMetadataRegistry()
                val delegate = RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo("42")), "42"))
                var closeCalls = 0
                var parserCalls = 0
                var spliceCalls = 0
                val executor = object : MacroExecutor by delegate {
                    override fun close() {
                        closeCalls++
                        delegate.close()
                    }
                }
                val configuration = CompilerConfiguration().apply {
                    macroExecutorFactory = MacroExecutorFactory { executor }
                    macroFragmentParserFactory = MacroFragmentParserFactory {
                        object : MacroFragmentParser {
                            override fun parse(input: MacroFragmentInput): MacroFragmentResult {
                                parserCalls++
                                assertSame(metadata, input.annotationMetadata)
                                if (failInParser) throw expected
                                return MacroFragmentResult.Success(
                                    input.node, input.tokens, input.mode,
                                    MacroFragmentPayload.single(buildErrorExpression { diagnostic = ConeSimpleDiagnostic("replacement") }),
                                )
                            }
                        }
                    }
                }
                val splicer = object : MacroStableSplicer {
                    override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): MacroStableSpliceResult {
                        spliceCalls++
                        assertSame(metadata, slots.single().annotationMetadata)
                        throw expected
                    }
                }
                val thrown = assertThrows(RuntimeException::class.java) {
                    FrontendMacroConstructionService(configuration, splicer).expandWithClassification(
                        fixture.pre, contextWithArtifact(fixture.pre, "Generated"), MacroConstructionService.Mode.DEGRADED,
                    )
                }
                assertSame(expected, thrown)
                assertEquals(1, delegate.calls.size)
                assertEquals(1, parserCalls)
                assertEquals(if (failInParser) 0 else 1, spliceCalls)
                assertEquals(1, closeCalls)
                assertSame(original, fixture.function.body!!.statements.single())
                // SEALED 仍可只读，ABORTED 禁止查询；此处明确验证异常路径已 abort。
                assertThrows(IllegalStateException::class.java) { metadata.snapshot(annotation) }
                assertThrows(IllegalStateException::class.java) { metadata.seal(listOf(preFile.cfirFile)) }
                assertNull(published.snapshot(annotation))
            }
        }
    }

    /**
     * 验证默认稳定 splicer 会用解析出的表达式 payload 替换表达式 carrier。
     */
    @Test
    fun defaultStableSplicerReplacesExpressionMacroCarrierWithParsedExpressionPayload() {
        val fixture = macroExpressionCarrierFixture()
        val executor = RecordingExecutor(
            MacroExpansionResult.Success(
                tokens = listOf(org.cangnova.cangjie.macro.TokenInfo(0u.toUByte(), "expanded")),
                "expanded",
            ),
        )
        val parser = object : MacroFragmentParser {
            override fun parse(input: MacroFragmentInput): MacroFragmentResult {
                return MacroFragmentResult.Success(
                    originNode = input.node,
                    tokens = input.tokens,
                    mode = input.mode,
                    payload = MacroFragmentPayload.single(buildErrorExpression {
                        diagnostic = ConeSimpleDiagnostic("expanded macro payload")
                    }),
                )
            }
        }
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { parser }
        }

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = fixture.pre,
            context = bindMacroImports(
                pre = fixture.pre,
                symbolIndex = buildMacroSymbolIndex(
                    pre = fixture.pre,
                    macroArtifactDefinitions = listOf(
                        MacroDefinitionEntry(
                            packageFqName = FqName("macros"),
                            name = Name.identifier("Generated"),
                            source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        ),
                    ),
                ),
            ),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        val statement = fixture.function.body!!.statements.single()
        assertInstanceOf(CfirErrorExpression::class.java, statement)
    }

    /** 空的 `[]` 仍选择 attribute wrapper，并通过 executor 协议保留属性存在标志。 */
    @Test
    fun emptyAttributeDelimiterUsesAttributeMacroWrapper() {
        val fixture = macroExpressionCarrierFixture(hasAttr = true)
        val pre = fixture.pre
        val executor = RecordingExecutor(
            MacroExpansionResult.Success(
                tokens = listOf(org.cangnova.cangjie.macro.TokenInfo(0u.toUByte(), "expanded")),
                "expanded",
            ),
        )
        val parser = object : MacroFragmentParser {
            override fun parse(input: MacroFragmentInput): MacroFragmentResult = MacroFragmentResult.Success(
                originNode = input.node,
                tokens = input.tokens,
                mode = input.mode,
                payload = MacroFragmentPayload.single(buildErrorExpression {
                    diagnostic = ConeSimpleDiagnostic("expanded macro payload")
                }),
            )
        }
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { parser }
        }

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = pre,
            context = contextWithArtifact(pre, "Generated"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        val call = executor.calls.single()
        assertTrue(call.hasAttrs)
        assertTrue(call.methodName.startsWith("macroCall_a_"))
    }

    /**
     * 验证默认稳定 splicer 会用解析出的声明 payload 替换声明 carrier。
     */
    @Test
    fun defaultStableSplicerReplacesDeclarationCarrierWithParsedDeclarationPayload() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val original = namedFunction(moduleData, packageFqName, "original")
        val replacement = namedFunction(moduleData, packageFqName, "expanded")
        val siblingReplacement = namedFunction(moduleData, packageFqName, "expandedSibling")
        val metadata = CfirAnnotationMetadataBuilder()
        fun annotation(annotationName: String, context: CfirBasedSymbol<*> = replacement.symbol): CfirAnnotationCall = buildAnnotationCall {
            source = MetadataFixtureSource("@!$annotationName")
            typeRef = buildImplicitTypeRef()
            annotationSourceName = annotationName
            sourceModuleName = "sample"
            forcedCustom = true
            isCompileTimeVisible = true
            argumentList = buildArgumentList()
            calleeReference = buildNamedReference { name = Name.identifier(annotationName) }
            containingDeclarationSymbol = context
        }
        fun record(owner: CfirDeclaration, annotation: CfirAnnotationCall): CfirAnnotationReplaceCarrier {
            val rawSyntax = "@!${annotation.annotationSourceName}"
            return metadata.record(CfirAnnotationSlotSnapshot(
                owner = owner,
                annotationIndex = owner.annotations.indexOfFirst { it === annotation },
                originalAnnotation = annotation,
                rawSyntax = rawSyntax,
                forcedCustom = true,
                isCompileTimeVisible = true,
                annotationSource = requireNotNull(annotation.source),
                qualifiedName = FqName("sample.${annotation.annotationSourceName}"),
                argumentText = null,
                tokens = listOf(MacroSurfaceToken(rawSyntax, 0, rawSyntax.length)),
                callSite = MacroCallSite.DECLARATION,
            ))
        }
        val originalAnnotation = annotation("Tag", original.symbol).also { original.replaceAnnotations(listOf(it)) }
        val originalCarrier = record(original, originalAnnotation)
        val duplicatedFragmentAnnotation = annotation("Tag")
        val retainedFragmentAnnotation = annotation("Other")
        replacement.replaceAnnotations(listOf(duplicatedFragmentAnnotation, retainedFragmentAnnotation))
        val duplicateCarrier = record(replacement, duplicatedFragmentAnnotation)
        val retainedCarrier = record(replacement, retainedFragmentAnnotation)
        val reparsedAnnotation = annotation("Tag")
        val file = fileWithDeclarations(moduleData, packageFqName, original)
        val surface = declarationSurface(
            surfaceId = 3100L,
            qualifiedName = FqName("macros.GenerateDeclaration"),
            packageFqName = packageFqName,
            carrier = original,
        )
        val annotationSurface = surface.copy(
            surfaceId = 3101L,
            qualifiedName = FqName("Tag"),
            isQualifiedName = false,
            kind = MacroSurface.Kind.FORCED,
            hasParenthesis = false,
            inputTokens = emptyList(),
            capturedRawSyntax = "@!Tag",
            replaceHandle = CfirReplaceHandle(
                3101L,
                ownerSnapshot = surface.replaceHandle.ownerSnapshot,
                annotationCarrier = originalCarrier,
            ),
        )
        val pre = buildPreMacroRawFiles(session, listOf(PreMacroCfirFile(file, listOf(surface, annotationSurface), metadata)))
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory {
                RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo("func expanded() {}")), "func expanded() {}"))
            }
            macroFragmentParserFactory = MacroFragmentParserFactory {
                object : MacroFragmentParser {
                    override fun parse(input: MacroFragmentInput): MacroFragmentResult {
                        assertSame(metadata, input.annotationMetadata)
                        return if (input.decision.parserMode == MacroFragmentParser.Mode.CUSTOM_ANNOTATION) {
                            MacroFragmentResult.CustomAnnotation(input.node, reparsedAnnotation, input.tokens)
                        } else {
                            MacroFragmentResult.Success(
                                input.node,
                                input.tokens,
                                input.decision.parserMode,
                                MacroFragmentPayload(
                                    listOf(
                                        MacroFragmentPayload.Root.Declaration(0, replacement),
                                        MacroFragmentPayload.Root.Declaration(1, siblingReplacement),
                                    ),
                                    MacroFragmentPayload.EMPTY.parsingInfo,
                                ),
                            )
                        }
                    }
                }
            }
        }

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = pre,
            context = contextWithArtifact(pre, "GenerateDeclaration"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success, "Construction failed: $result; diagnostics=${result.registry.diagnostics}")
        assertEquals(listOf(replacement, siblingReplacement), file.declarations)
        assertEquals(listOf(reparsedAnnotation, retainedFragmentAnnotation), replacement.annotations)
        assertEquals(CfirAnnotationReplaceCarrier(replacement, 0, reparsedAnnotation), metadata.currentSlot(originalCarrier))
        assertEquals(CfirAnnotationReplaceCarrier(replacement, 1, retainedFragmentAnnotation), metadata.currentSlot(retainedCarrier))
        assertNull(metadata.currentSlot(duplicateCarrier))
        assertSame(duplicatedFragmentAnnotation, metadata.snapshot(duplicateCarrier)?.originalAnnotation)
        val published = session.ensureAnnotationMetadataRegistry()
        published.publishAll((result as MacroConstructionResult.Success).recordableFiles.annotationMetadata)
        assertNull(published.snapshot(originalAnnotation))
        assertNull(published.snapshot(duplicatedFragmentAnnotation))
        assertSame(replacement, published.snapshot(reparsedAnnotation)?.owner)
        assertEquals(0, published.snapshot(reparsedAnnotation)?.annotationIndex)
        assertSame(replacement, published.snapshot(retainedFragmentAnnotation)?.owner)
        assertEquals(1, published.snapshot(retainedFragmentAnnotation)?.annotationIndex)
    }

    /**
     * 父声明宏重新解析已展开的子宏 token 后，稳定 splice 不能再查找已脱离 final CFIR 的旧子载体。
     */
    @Test
    fun parentDeclarationExpansionConsumesNestedMacroCarriers() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val originalMember = namedFunction(moduleData, packageFqName, "originalMember")
        val expandedMember = namedFunction(moduleData, packageFqName, "expandedMember")
        val originalClass = namedClass(moduleData, packageFqName, "Outer", listOf(originalMember))
        val expandedClass = namedClass(moduleData, packageFqName, "Outer", listOf(expandedMember))
        val file = fileWithDeclarations(moduleData, packageFqName, originalClass)
        val parentSurfaceId = 3108L
        val childSurfaceId = 3109L
        val parentSurface = declarationSurface(
            surfaceId = parentSurfaceId,
            qualifiedName = FqName("macros.ReplaceClass"),
            packageFqName = packageFqName,
            carrier = originalClass,
        ).copy(
            inputTokens = listOf(MacroSurfaceToken("@ReplaceMember", 20, 30)),
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 0, endOffset = 100),
        )
        val childSurface = declarationSurface(
            surfaceId = childSurfaceId,
            qualifiedName = FqName("macros.ReplaceMember"),
            packageFqName = packageFqName,
            carrier = originalMember,
        ).copy(
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 20, endOffset = 30),
        )
        val pre = buildPreMacroRawFiles(
            session = session,
            files = listOf(PreMacroCfirFile(file, listOf(parentSurface, childSurface), CfirAnnotationMetadataBuilder())),
        )
        val parser = object : MacroFragmentParser {
            override fun parse(input: MacroFragmentInput): MacroFragmentResult {
                val payload = when (input.node.surface.surfaceId) {
                    parentSurfaceId -> MacroFragmentPayload.single(expandedClass)
                    childSurfaceId -> MacroFragmentPayload.single(expandedMember)
                    else -> error("Unexpected macro surface ${input.node.surface.surfaceId}")
                }
                return MacroFragmentResult.Success(input.node, input.tokens, input.mode, payload)
            }
        }
        val executor = RecordingExecutor(
            MacroExpansionResult.Success(listOf(tokenInfo("expanded")), "expanded"),
        )
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { parser }
        }

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = pre,
            context = contextWithArtifacts(pre, "ReplaceClass", "ReplaceMember"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success, "Construction failed: $result; diagnostics=${result.registry.diagnostics}")
        assertSame(expandedClass, file.declarations.single())
        assertSame(expandedMember, (file.declarations.single() as CfirClass).declarations.single())
        assertEquals(2, executor.calls.size)
    }

    /**
     * 验证默认稳定 splicer 会用解析出的参数 payload 替换参数 carrier。
     */
    @Test
    fun defaultStableSplicerReplacesParameterCarrierWithParsedParameterPayload() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val functionSymbol = CfirNamedFunctionSymbol(CallableId(packageFqName, Name.identifier("useMacro")))
        val original = valueParameter(moduleData, packageFqName, "before", functionSymbol)
        val replacement = valueParameter(moduleData, packageFqName, "after", functionSymbol)
        val siblingReplacement = valueParameter(moduleData, packageFqName, "afterSibling", functionSymbol)
        val function = namedFunction(moduleData, packageFqName, "useMacro", functionSymbol, listOf(original))
        val file = fileWithDeclarations(moduleData, packageFqName, function)
        val surface = parameterSurface(
            surfaceId = 3101L,
            qualifiedName = FqName("macros.GenerateParameter"),
            packageFqName = packageFqName,
            carrier = original,
        )
        val host = CjInMemoryTextSourceFile(
            "sample.cj", "testdata/sample.cj",
            "package sample\nfunc useMacro(@macros.GenerateParameter(before: Int64)) {}\n",
        )
        val pre = buildPreMacroRawFiles(session, listOf(freezeFixtureParserInput(file, host, listOf(surface))))
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory {
                RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo("after: Int")), "after: Int"))
            }
            macroFragmentParserFactory = MacroFragmentParserFactory {
                object : MacroFragmentParser {
                    override fun parse(input: MacroFragmentInput): MacroFragmentResult = MacroFragmentResult.Success(
                        input.node,
                        input.tokens,
                        input.mode,
                        MacroFragmentPayload(
                            listOf(
                                MacroFragmentPayload.Root.Parameter(0, replacement),
                                MacroFragmentPayload.Root.Parameter(1, siblingReplacement),
                            ),
                            MacroFragmentPayload.EMPTY.parsingInfo,
                        ),
                    )
                }
            }
        }

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = pre,
            context = contextWithArtifact(pre, "GenerateParameter"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        val parameterReplacement = result.registry.appliedDeclarationReplacements.single()
        assertSame(original, parameterReplacement.carrier)
        assertEquals(listOf(replacement, siblingReplacement), parameterReplacement.replacements)
        assertEquals(surface.surfaceId, parameterReplacement.surfaceId)
        assertEquals(listOf(replacement, siblingReplacement), function.valueParameters)
    }

    /**
     * 验证局部变量初始化器中的表达式 carrier 也能被稳定替换。
     */
    @Test
    fun defaultStableSplicerReplacesExpressionCarrierInsideLocalVariableInitializer() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val carrier = buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("macro construction carrier")
        }
        val replacement = buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("expanded macro payload")
        }
        val local = patternVariable(
            moduleData = moduleData,
            packageFqName = packageFqName,
            name = "value",
            initializer = carrier,
        )
        val function = namedFunction(moduleData, packageFqName, "useMacro").apply {
            replaceBody(buildBlock { statements += local })
        }
        val file = fileWithDeclarations(moduleData, packageFqName, function)
        val surface = expressionSurface(
            surfaceId = 3102L,
            qualifiedName = FqName("macros").child(Name.identifier("Generated")),
            packageFqName = packageFqName,
        ).copy(
            replaceHandle = CfirReplaceHandle(3102L, functionBodyMacroOwnerSnapshot(packageFqName, function), carrier),
        )
        val pre = buildPreMacroRawFiles(session, listOf(PreMacroCfirFile(file, listOf(surface), CfirAnnotationMetadataBuilder())))
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory {
                RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo("expanded")), "expanded"))
            }
            macroFragmentParserFactory = MacroFragmentParserFactory {
                StaticPayloadParser(replacement)
            }
        }

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = pre,
            context = contextWithArtifact(pre, "Generated"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertSame(replacement, local.initializer)
    }

    /**
     * 验证 DEGRADED 模式会为局部变量初始化器构造 typed expression 占位符。
     */
    @Test
    fun degradedModeBuildsTypedExpressionPlaceholderInsideLocalVariableInitializer() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val carrier = buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("macro construction carrier")
        }
        val local = patternVariable(
            moduleData = moduleData,
            packageFqName = packageFqName,
            name = "value",
            initializer = carrier,
        )
        val function = namedFunction(moduleData, packageFqName, "useMacro").apply {
            replaceBody(buildBlock { statements += local })
        }
        val file = fileWithDeclarations(moduleData, packageFqName, function)
        val surface = expressionSurface(
            surfaceId = 3103L,
            qualifiedName = FqName("macros").child(Name.identifier("Generated")),
            packageFqName = packageFqName,
        ).copy(
            replaceHandle = CfirReplaceHandle(3103L, functionBodyMacroOwnerSnapshot(packageFqName, function), carrier),
        )
        val pre = buildPreMacroRawFiles(session, listOf(PreMacroCfirFile(file, listOf(surface), CfirAnnotationMetadataBuilder())))

        val result = FrontendMacroConstructionService(CompilerConfiguration()).expandWithClassification(
            pre = pre,
            context = bindMacroImports(pre, buildMacroSymbolIndex(pre)),
            mode = MacroConstructionService.Mode.DEGRADED,
        )

        assertTrue(result is MacroConstructionResult.Degraded)
        val initializer = local.initializer
        assertInstanceOf(CfirErrorExpression::class.java, initializer)
        assertTrue(initializer !== carrier)
        assertEquals(
            "Macro call `@macros.Generated(arg)` was not expanded during macro construction.",
            (initializer as CfirErrorExpression).diagnostic.reason,
        )
    }

    /**
     * 验证 parser 不可用时 DEGRADED 模式保留声明 carrier，并为参数构造类型化占位符。
     */
    @Test
    fun degradedModePreservesDeclarationAndBuildsParameterPlaceholderWhenParserIsUnavailable() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val declarationCarrier = namedFunction(moduleData, packageFqName, "declarationCarrier")
        val functionSymbol = CfirNamedFunctionSymbol(CallableId(packageFqName, Name.identifier("parameterCarrier")))
        val parameterCarrier = valueParameter(moduleData, packageFqName, "before", functionSymbol)
        val parameterOwner = namedFunction(moduleData, packageFqName, "parameterCarrier", functionSymbol, listOf(parameterCarrier))
        val metadata = CfirAnnotationMetadataBuilder()
        fun recordAnnotation(owner: CfirDeclaration, context: CfirBasedSymbol<*>, annotationName: String): CfirAnnotationCall {
            val rawSyntax = "@!$annotationName[42]"
            val annotationSource = MetadataFixtureSource(rawSyntax)
            val annotation = buildAnnotationCall {
                source = annotationSource
                typeRef = buildImplicitTypeRef()
                annotationSourceName = annotationName
                sourceModuleName = "sample"
                forcedCustom = true
                isCompileTimeVisible = true
                argumentList = buildArgumentList()
                calleeReference = buildNamedReference { name = Name.identifier(annotationName) }
                containingDeclarationSymbol = context
            }
            owner.replaceAnnotations(listOf(annotation))
            metadata.record(CfirAnnotationSlotSnapshot(
                owner = owner,
                annotationIndex = 0,
                originalAnnotation = annotation,
                rawSyntax = rawSyntax,
                forcedCustom = true,
                isCompileTimeVisible = true,
                annotationSource = annotationSource,
                qualifiedName = FqName("sample.$annotationName"),
                argumentText = "[42]",
                tokens = listOf(MacroSurfaceToken(rawSyntax, 0, rawSyntax.length)),
                callSite = if (owner is CfirValueParameter) MacroCallSite.PARAMETER else MacroCallSite.DECLARATION,
            ))
            return annotation
        }
        val declarationAnnotation = recordAnnotation(declarationCarrier, declarationCarrier.symbol, "DeclarationTag")
        val parameterAnnotation = recordAnnotation(parameterCarrier, functionSymbol, "ParameterTag")
        val declarationMetadata = requireNotNull(metadata.snapshot(declarationAnnotation))
        val parameterMetadata = requireNotNull(metadata.snapshot(parameterAnnotation))
        val publishedMetadata = session.ensureAnnotationMetadataRegistry()
        val file = fileWithDeclarations(moduleData, packageFqName, declarationCarrier, parameterOwner)
        val surfaces = listOf(
            declarationSurface(3200L, FqName("macros.GenerateDeclaration"), packageFqName, declarationCarrier),
            parameterSurface(3201L, FqName("macros.GenerateParameter"), packageFqName, parameterCarrier),
        )
        val pre = buildPreMacroRawFiles(session, listOf(PreMacroCfirFile(file, surfaces, metadata)))

        val result = FrontendMacroConstructionService(CompilerConfiguration()).expandWithClassification(
            pre = pre,
            context = bindMacroImports(pre, buildMacroSymbolIndex(pre)),
            mode = MacroConstructionService.Mode.DEGRADED,
        )

        assertTrue(result is MacroConstructionResult.Degraded)
        assertSame(declarationCarrier, file.declarations.first())
        assertInstanceOf(CfirNamedFunction::class.java, file.declarations.first())
        val parameterPlaceholder = (file.declarations[1] as CfirNamedFunction).valueParameters.single()
        assertInstanceOf(CfirErrorExpression::class.java, parameterPlaceholder.defaultValue)
        assertFalse(parameterPlaceholder.hasInitialDefaultValue)
        assertEquals(
            setOf(surfaces.last().surfaceId),
            (result as MacroConstructionResult.Degraded).registry.placeholderOriginById.keys,
        )
        assertNotSame(parameterCarrier, parameterPlaceholder)
        assertNotSame(parameterCarrier.attributes, parameterPlaceholder.attributes)
        assertSame(declarationAnnotation, declarationCarrier.annotations.single())
        assertSame(parameterAnnotation, parameterPlaceholder.annotations.single())
        assertSame(functionSymbol, parameterAnnotation.containingDeclarationSymbol)
        assertNull(publishedMetadata.snapshot(declarationAnnotation), "封闭产品不能在显式 publish 前进入 session 索引")
        assertNull(publishedMetadata.snapshot(parameterAnnotation))
        publishedMetadata.publishAll(result.recordableFiles.annotationMetadata)
        assertEquals(declarationMetadata, publishedMetadata.snapshot(declarationAnnotation))
        assertEquals(parameterMetadata.copy(owner = parameterPlaceholder), publishedMetadata.snapshot(parameterAnnotation))
        assertSame(declarationCarrier, publishedMetadata.snapshot(declarationAnnotation)?.owner)
        assertSame(parameterPlaceholder, publishedMetadata.snapshot(parameterAnnotation)?.owner)
    }

    /**
     * 验证已解析的宏会经 executor、fragment parser 和 stable splicer 完整流转。
     */
    @Test
    fun resolvedMacroUsesExecutorFragmentParserAndStableSplicer() {
        val fixture = macroSurfaceFixture(
            surface = expressionSurface(
                surfaceId = 3001L,
                qualifiedName = FqName("macros.Generated"),
                packageFqName = FqName("sample"),
            ),
        )
        val executor = RecordingExecutor(
            MacroExpansionResult.Success(
                tokens = listOf(org.cangnova.cangjie.macro.TokenInfo(0u.toUByte(), "42")),
                "42",
            ),
        )
        val parser = RecordingParser()
        val splicer = RecordingSplicer()
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { parser }
        }

        val result = FrontendMacroConstructionService(configuration, splicer).expandWithClassification(
            pre = fixture.pre,
            context = bindMacroImports(
                pre = fixture.pre,
                symbolIndex = buildMacroSymbolIndex(
                    pre = fixture.pre,
                    macroArtifactDefinitions = listOf(
                        MacroDefinitionEntry(
                            packageFqName = FqName("macros"),
                            name = Name.identifier("Generated"),
                            source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        ),
                    ),
                ),
            ),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertEquals("Generated", executor.calls.single().idName)
        assertEquals(listOf("42"), parser.parsedTokens.single().map { it.text })
        assertEquals(MacroFragmentParser.Mode.EXPRESSION, parser.modes.single())
        assertSame(fixture.surface, splicer.slots.single().origin)
        assertTrue((result as MacroConstructionResult.Success).registry.diagnostics.isEmpty())
    }

    /**
     * 固定源码、FQN、范围和展开文本，验证真实 construction 的缓存仍区分解析来源。
     * 输入 surface 与输出注解分别改变模块来源，避免用其它维度的偶然变化掩盖遗漏。
     */
    @Test
    fun constructionCacheSeparatesParserContextForIdenticalSourceAndSurface() {
        val packageFqName = FqName("sample")
        val macroFqName = FqName("macros.Generated")
        val sourceText = "package sample\nimport macros.Generated\n@Generated func original() {}\n"
        val expandedText = "@ConstSafe func expanded() {}"
        val sourceRange = MacroSurfaceSourceRange(null, sourceText.indexOf("@Generated"), sourceText.length)

        fun constructCacheKey(
            parserModule: String,
            qualified: Boolean,
            annotationModule: String = "",
        ): MacroExpansionCacheKey {
            val session = object : CfirSession(CfirSession.Kind.Source) {}
            val moduleData = TestModuleData(session)
            session.register(CfirModuleData::class, moduleData)
            val original = namedFunction(moduleData, packageFqName, "original")
            val replacement = namedFunction(moduleData, packageFqName, "expanded")
            val outputAnnotation = buildAnnotationCall {
                typeRef = buildImplicitTypeRef()
                annotationSourceName = "ConstSafe"
                sourceModuleName = annotationModule
                argumentList = buildArgumentList()
                calleeReference = buildNamedReference { name = Name.identifier("ConstSafe") }
                containingDeclarationSymbol = replacement.symbol
            }
            replacement.replaceAnnotations(listOf(outputAnnotation))
            val file = buildFileCopy(fileWithDeclarations(moduleData, packageFqName, original)) {
                symbol = CfirFileSymbol()
                sourceFile = CjInMemoryTextSourceFile("sample.cj", "testdata/sample.cj", sourceText)
                sourceFileLinesMapping = sourceText.toSourceLinesMapping()
                imports += buildImport {
                    importedFqName = macroFqName
                    isAllUnder = false
                }
            }
            val originalSurface = declarationSurface(
                surfaceId = 3090L,
                qualifiedName = macroFqName,
                packageFqName = packageFqName,
                carrier = original,
            )
            val surface = originalSurface.copy(
                sourceRange = sourceRange,
                capturedRawSyntax = "@Generated func original() {}",
                isQualifiedName = qualified,
                scopeContext = originalSurface.scopeContext.copy(sourceModuleName = parserModule),
            )
            val pre = buildPreMacroRawFiles(session, listOf(PreMacroCfirFile(file, listOf(surface), CfirAnnotationMetadataBuilder())))
            val executor = RecordingExecutor(
                MacroExpansionResult.Success(listOf(tokenInfo(expandedText)), expandedText),
            )
            val configuration = CompilerConfiguration().apply {
                macroExecutorFactory = MacroExecutorFactory { executor }
                macroFragmentParserFactory = MacroFragmentParserFactory {
                    StaticPayloadParser(replacement)
                }
            }

            val result = FrontendMacroConstructionService(configuration).expandWithClassification(
                pre = pre,
                context = contextWithArtifact(pre, "Generated"),
                mode = MacroConstructionService.Mode.STRICT,
            )

            assertTrue(result is MacroConstructionResult.Success, "Construction failed: $result; diagnostics=${result.registry.diagnostics}")
            assertTrue(result.registry.diagnostics.isEmpty())
            assertEquals("Generated", executor.calls.single().idName)
            assertSame(replacement, file.declarations.single())
            assertSame(outputAnnotation, replacement.annotations.single())
            return result.registry.cacheKeys.values.single()
        }

        val original = constructCacheKey(parserModule = "", qualified = false)
        assertEquals(org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer.VERSION, original.tokenScannerVersion)
        assertNotEquals(original.stableHash(), original.copy(tokenScannerVersion = 2).stableHash())
        assertEquals(MacroFragmentParser.VERSION, original.fragmentParserVersion)
        assertNotEquals(original.stableHash(), original.copy(fragmentParserVersion = 30).stableHash())
        val differentModule = constructCacheKey(parserModule = "std", qualified = false)
        val differentQualification = constructCacheKey(parserModule = "", qualified = true)
        val differentAnnotationModule = constructCacheKey(parserModule = "", qualified = false, annotationModule = "std")

        assertEquals(MacroConstructionService.ALGORITHM_VERSION, original.constructionAlgorithmVersion)
        assertEquals(original, constructCacheKey(parserModule = "", qualified = false))
        for (changed in listOf(differentModule, differentQualification, differentAnnotationModule)) {
            assertEquals(original.sourceContentHash, changed.sourceContentHash)
            assertEquals(original.fileIdentity, changed.fileIdentity)
            assertEquals(original.importsHash, changed.importsHash)
            assertEquals(original.modulePackageIdentity, changed.modulePackageIdentity)
            assertNotEquals(original.stableHash(), changed.stableHash())
        }
        assertNotEquals(original.macroSurfaceRangesHash, differentModule.macroSurfaceRangesHash)
        assertNotEquals(original.macroSurfaceRangesHash, differentQualification.macroSurfaceRangesHash)
        assertEquals(original.runtimeFingerprint, differentModule.runtimeFingerprint)
        assertEquals(original.runtimeFingerprint, differentQualification.runtimeFingerprint)
        assertEquals(original.macroSurfaceRangesHash, differentAnnotationModule.macroSurfaceRangesHash)
        assertNotEquals(original.runtimeFingerprint, differentAnnotationModule.runtimeFingerprint)
    }

    /**
     * 不同源码文件的 offset 坐标系彼此独立，范围相同的宏调用不能被构造成父子节点。
     */
    @Test
    fun macroForestKeepsEqualRangesFromDifferentFilesIndependent() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val firstFile = fileWithDeclarations(moduleData, packageFqName)
        val secondFile = fileWithDeclarations(moduleData, packageFqName)
        val macroFqName = FqName("macros.Generated")
        val firstSurface = expressionSurface(
            surfaceId = 3010L,
            qualifiedName = macroFqName,
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(
                source = null,
                startOffset = 0,
                endOffset = 20,
            ),
        )
        val secondSurface = expressionSurface(
            surfaceId = 3011L,
            qualifiedName = macroFqName,
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(
                source = null,
                startOffset = 0,
                endOffset = 20,
            ),
        )
        val pre = buildPreMacroRawFiles(
            session = session,
            files = listOf(
                PreMacroCfirFile(firstFile, listOf(firstSurface), CfirAnnotationMetadataBuilder()),
                PreMacroCfirFile(secondFile, listOf(secondSurface), CfirAnnotationMetadataBuilder()),
            ),
        )
        val executor = RecordingExecutor(
            MacroExpansionResult.Success(
                tokens = listOf(org.cangnova.cangjie.macro.TokenInfo(0u.toUByte(), "42")),
                expandedText = "42",
            ),
        )
        val parser = RecordingParser()
        val splicer = RecordingSplicer()
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { parser }
        }

        val result = FrontendMacroConstructionService(configuration, splicer).expandWithClassification(
            pre = pre,
            context = bindMacroImports(
                pre = pre,
                symbolIndex = buildMacroSymbolIndex(
                    pre = pre,
                    macroArtifactDefinitions = listOf(
                        MacroDefinitionEntry(
                            packageFqName = FqName("macros"),
                            name = Name.identifier("Generated"),
                            source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        ),
                    ),
                ),
            ),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertEquals(2, executor.calls.size)
        assertEquals(listOf(3010L, 3011L), splicer.slots.map { it.origin.surfaceId })
        assertEquals(2, parser.parsedTokens.size)
        assertTrue((result as MacroConstructionResult.Success).registry.diagnostics.isEmpty())
    }

    /**
     * 验证重导出宏使用 executable identity 构造 executor 调用信息。
     */
    @Test
    fun reexportedMacroUsesExecutableIdentityForExecutorCallInfo() {
        val fixture = macroSurfaceFixture(
            surface = expressionSurface(
                surfaceId = 3003L,
                qualifiedName = FqName("macros.VisibleDerive"),
                packageFqName = FqName("sample"),
            ),
        )
        val executor = RecordingExecutor(
            MacroExpansionResult.Success(
                tokens = listOf(org.cangnova.cangjie.macro.TokenInfo(0u.toUByte(), "42")),
                "42",
            ),
        )
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
        }

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expandWithClassification(
            pre = fixture.pre,
            context = bindMacroImports(
                pre = fixture.pre,
                symbolIndex = buildMacroSymbolIndex(
                    pre = fixture.pre,
                    macroArtifactDefinitions = listOf(
                        MacroDefinitionEntry(
                            packageFqName = FqName("macros"),
                            name = Name.identifier("VisibleDerive"),
                            executablePackageFqName = FqName("upstream.deriving"),
                            executableName = Name.identifier("Derive"),
                            source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                            libPath = "impl-macro.dylib",
                        ),
                    ),
                ),
            ),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        val call = executor.calls.single()
        assertEquals("VisibleDerive", call.idName)
        assertEquals("upstream.deriving", call.packageName)
        assertEquals("macroCall_c_Derive_upstream_deriving", call.methodName)
        assertEquals("impl-macro.dylib", call.libPath)
    }

    /**
     * 验证父宏参数只替换直接子宏 surface 范围。
     */
    @Test
    fun parentMacroArgumentsReplaceOnlyDirectChildSurfaceRanges() {
        val packageFqName = FqName("sample")
        val parent = expressionSurface(
            surfaceId = 3300L,
            qualifiedName = FqName("macros.Parent"),
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 0, endOffset = 100),
        ).copy(
            inputTokens = listOf(
                MacroSurfaceToken("left", 10, 14, isSourceToken = true),
                MacroSurfaceToken(" ", 14, 15, isSourceToken = true),
                MacroSurfaceToken("@ChildOne", 20, 29, isSourceToken = true),
                MacroSurfaceToken("(", 29, 30, isSourceToken = true),
                MacroSurfaceToken("one", 30, 33, isSourceToken = true),
                MacroSurfaceToken(")", 33, 34, isSourceToken = true),
                MacroSurfaceToken(" ", 34, 35, isSourceToken = true),
                MacroSurfaceToken("middle", 35, 41, isSourceToken = true),
                MacroSurfaceToken(" ", 41, 42, isSourceToken = true),
                MacroSurfaceToken("@ChildTwo", 50, 59, isSourceToken = true),
                MacroSurfaceToken("(", 59, 60, isSourceToken = true),
                MacroSurfaceToken("two", 60, 63, isSourceToken = true),
                MacroSurfaceToken(")", 63, 64, isSourceToken = true),
                MacroSurfaceToken(" ", 64, 65, isSourceToken = true),
                MacroSurfaceToken("right", 65, 70, isSourceToken = true),
            ),
        )
        val firstChild = expressionSurface(
            surfaceId = 3301L,
            qualifiedName = FqName("macros.ChildOne"),
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 20, endOffset = 34),
        ).copy(
            inputTokens = listOf(MacroSurfaceToken("one", 30, 33, isSourceToken = true)),
        )
        val secondChild = expressionSurface(
            surfaceId = 3302L,
            qualifiedName = FqName("macros.ChildTwo"),
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 50, endOffset = 64),
        ).copy(
            inputTokens = listOf(MacroSurfaceToken("two", 60, 63, isSourceToken = true)),
        )
        val originalText = " ".repeat(10) + "left" + " ".repeat(6) + "@ChildOne(one)" +
            " middle" + " ".repeat(9) + "@ChildTwo(two) right" + " ".repeat(30)
        val fixture = macroSurfaceFixture(listOf(parent, firstChild, secondChild), sourceText = originalText)
        val executor = RoutingExecutor(
            results = mapOf(
                "ChildOne" to MacroExpansionResult.Success(listOf(tokenInfo("expandedOne")), "expandedOne"),
                "ChildTwo" to MacroExpansionResult.Success(listOf(tokenInfo("expandedTwo")), "expandedTwo"),
                "Parent" to MacroExpansionResult.Success(listOf(tokenInfo("expandedParent")), "expandedParent"),
            ),
        )
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
        }

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expandWithClassification(
            pre = fixture.pre,
            context = contextWithArtifacts(fixture.pre, "Parent", "ChildOne", "ChildTwo"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertEquals(listOf("ChildOne", "ChildTwo", "Parent"), executor.calls.map { it.idName })
        assertEquals(
            listOf("left", "expandedOne", "middle", "expandedTwo", "right"),
            executor.calls.single { it.idName == "Parent" }.argTokens.map { it.value },
        )
        val parentArguments = executor.calls.single { it.idName == "Parent" }.argTokens
        assertEquals(SourcePosition(line = 1, column = 11), parentArguments.first().begin)
        assertEquals(SourcePosition(line = 1, column = 21), parentArguments.single { it.value == "expandedOne" }.begin)
        assertEquals(SourcePosition(line = 1, column = 35), parentArguments.single { it.value == "expandedOne" }.end)
        assertEquals(SourcePosition(line = 1, column = 51), parentArguments.single { it.value == "expandedTwo" }.begin)
        assertEquals(SourcePosition(line = 1, column = 65), parentArguments.single { it.value == "expandedTwo" }.end)
    }

    /**
     * 验证父宏属性 token 会替换直接子宏 surface 范围。
     */
    @Test
    fun parentMacroAttributesReplaceDirectChildSurfaceRange() {
        val packageFqName = FqName("sample")
        val parent = expressionSurface(
            surfaceId = 3310L,
            qualifiedName = FqName("macros.Parent"),
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 0, endOffset = 80),
        ).copy(
            attrTokens = listOf(
                MacroSurfaceToken("#[", 10, 12, isSourceToken = true),
                MacroSurfaceToken("@Child", 20, 26, isSourceToken = true),
                MacroSurfaceToken("(", 26, 27, isSourceToken = true),
                MacroSurfaceToken("childArg", 27, 35, isSourceToken = true),
                MacroSurfaceToken(")", 35, 36, isSourceToken = true),
                MacroSurfaceToken("]", 36, 37, isSourceToken = true),
            ),
            inputTokens = listOf(MacroSurfaceToken("arg", 50, 53, isSourceToken = true)),
        )
        val child = expressionSurface(
            surfaceId = 3311L,
            qualifiedName = FqName("macros.Child"),
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 20, endOffset = 36),
        ).copy(
            inputTokens = listOf(MacroSurfaceToken("childArg", 27, 35, isSourceToken = true)),
        )
        val fixture = macroSurfaceFixture(listOf(parent, child))
        val executor = RoutingExecutor(
            results = mapOf(
                "Child" to MacroExpansionResult.Success(listOf(tokenInfo("expandedChild")), "expandedChild"),
                "Parent" to MacroExpansionResult.Success(listOf(tokenInfo("expandedParent")), "expandedParent"),
            ),
        )
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
        }

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expandWithClassification(
            pre = fixture.pre,
            context = contextWithArtifacts(fixture.pre, "Parent", "Child"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertEquals(listOf("Child", "Parent"), executor.calls.map { it.idName })
        assertEquals(
            listOf("#", "[", "expandedChild", "]"),
            executor.calls.single { it.idName == "Parent" }.attrTokens.map { it.value },
        )
        assertEquals(
            listOf("arg"),
            executor.calls.single { it.idName == "Parent" }.argTokens.map { it.value },
        )
    }

    // ============================================================
    // Batch 5 独立用例（PLAN.md §12 Batch 5）
    // 与 cfir/analysis-tests/testData/diagnostics2/macro/ 下的
    // alias_conflict / lib_path / executor_unavailable /
    // forced_kind_mismatch / plain_attr_overload 5 个 .cj 规范对齐：
    // 这里在程序级别断言 FrontendMacroConstructionService 真正按
    // baseline §4 / §8 产出对应的 MacroConstructionDiagnostic.Kind。
    // ============================================================

    /**
     * 验证 executor 不可用会产生诊断并在 DEGRADED 模式下降级。
     */
    @Test
    fun batch5_executorUnavailableYieldsExecutorUnavailableDiagnosticAndDegradedMode() {
        val surface = expressionSurface(
            surfaceId = 4001L,
            qualifiedName = FqName("macros.Reformat"),
            packageFqName = FqName("sample"),
        )
        val fixture = macroSurfaceFixture(surface)
        val configuration = CompilerConfiguration().apply {
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
            // 显式不可用执行器使测试独立于本机是否安装了可用 SDK。
            macroExecutorFactory = MacroExecutorFactory {
                object : MacroExecutor by RecordingExecutor() {
                    override fun isAvailable(): Boolean = false
                }
            }
        }

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = fixture.pre,
            context = contextWithArtifact(fixture.pre, "Reformat"),
            mode = MacroConstructionService.Mode.DEGRADED,
        )

        val registry = (result as? MacroConstructionResult.Degraded)?.registry
            ?: (result as? MacroConstructionResult.Failed)?.registry
            ?: error("Expected Degraded/Failed when executor is unavailable, got ${result::class.simpleName}")
        assertTrue(
            registry.diagnostics.any {
                it.kind == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Kind.MACRO_EXECUTOR_UNAVAILABLE
            },
            "Missing MACRO_EXECUTOR_UNAVAILABLE diagnostic; got: ${registry.diagnostics.map { it.kind }}",
        )
    }

    /**
     * 验证不支持 forced kind 的宏被 `@!` 调用时报告匹配错误。
     */
    @Test
    fun batch5_forcedKindOnNonForcedMacroReportsAtexclMismatch() {
        val surface = expressionSurface(
            surfaceId = 4002L,
            qualifiedName = FqName("macros.PlainOnly"),
            packageFqName = FqName("sample"),
        ).copy(kind = MacroSurface.Kind.FORCED)
        val fixture = macroSurfaceFixture(surface)
        val configuration = CompilerConfiguration().apply {
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
            macroExecutorFactory = MacroExecutorFactory { RecordingExecutor() }
        }

        val context = bindMacroImports(
            pre = fixture.pre,
            symbolIndex = buildMacroSymbolIndex(
                pre = fixture.pre,
                macroArtifactDefinitions = listOf(
                    MacroDefinitionEntry(
                        packageFqName = FqName("macros"),
                        name = Name.identifier("PlainOnly"),
                        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        supportsForcedKind = false,
                    ),
                ),
            ),
        )
        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = fixture.pre,
            context = context,
            mode = MacroConstructionService.Mode.STRICT,
        )

        val registry = (result as? MacroConstructionResult.Failed)?.registry
            ?: error("Forced-kind mismatch must be Failed in STRICT mode, got ${result::class.simpleName}")
        val mismatch = registry.diagnostics.singleOrNull {
            it.kind == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Kind.MACRO_EXPAND_ATEXCL
        }
        assertTrue(mismatch != null, "Expected a MACRO_EXPAND_ATEXCL diagnostic for unsupported forced macro")
        assertTrue(
            mismatch!!.message.contains("@!"),
            "Diagnostic message should mention `@!` invocation: ${mismatch.message}",
        )
    }

    /**
     * 验证 artifact libPath 会流入 executor 动态库加载调用。
     */
    @Test
    fun batch5_libPathFlowsThroughExecutorLoadLibraries() {
        val fixture = macroExpressionCarrierFixture(macroName = "WithLib")
        val executor = LibPathRecordingExecutor(
            MacroExpansionResult.Success(emptyList(), "42"),
        )
        val parser = object : MacroFragmentParser {
            override fun parse(input: MacroFragmentInput): MacroFragmentResult = MacroFragmentResult.Success(
                originNode = input.node,
                tokens = input.tokens,
                mode = input.mode,
                payload = MacroFragmentPayload.single(buildErrorExpression {
                    diagnostic = ConeSimpleDiagnostic("expanded WithLib payload")
                }),
            )
        }
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { parser }
        }

        val context = bindMacroImports(
            pre = fixture.pre,
            symbolIndex = buildMacroSymbolIndex(
                pre = fixture.pre,
                macroArtifactDefinitions = listOf(
                    MacroDefinitionEntry(
                        packageFqName = FqName("macros"),
                        name = Name.identifier("WithLib"),
                        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        libPath = "stub.dylib",
                    ),
                ),
            ),
        )
        FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = fixture.pre,
            context = context,
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(
            executor.loadedLibPaths.contains("stub.dylib"),
            "Macro construction must call executor.loadLibraries with the lib path from MacroDefinitionEntry: " +
            "${executor.loadedLibPaths}",
        )
    }

    /**
     * 验证动态库加载失败会报告 cannot-open-lib 并跳过 execute。
     */
    @Test
    fun batch5_loadLibraryFailureReportsCannotOpenLibAndSkipsExecute() {
        val fixture = macroExpressionCarrierFixture(macroName = "WithLib")
        val executor = LoadFailingExecutor(
            MacroLibraryLoadFailure(
                libPath = "broken.dylib",
                kind = MacroLibraryLoadFailureKind.CANNOT_OPEN_LIB,
                message = "open-lib failed: broken.dylib",
            )
        )
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
        }

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expandWithClassification(
            pre = fixture.pre,
            context = bindMacroImports(
                pre = fixture.pre,
                symbolIndex = buildMacroSymbolIndex(
                    pre = fixture.pre,
                    macroArtifactDefinitions = listOf(
                        MacroDefinitionEntry(
                            packageFqName = FqName("macros"),
                            name = Name.identifier("WithLib"),
                            source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                            libPath = "broken.dylib",
                        ),
                    ),
                ),
            ),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertEquals(0, executor.executeCallCount, "Executor.execute must not run after DefLib/loadLibraries failure.")
        val registry = (result as? MacroConstructionResult.Failed)?.registry
            ?: error("Load failure must drive Failed in STRICT mode, got ${result::class.simpleName}")
        val diagnostic = registry.diagnostics.single()
        assertEquals(
            org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Kind.MACRO_CANNOT_OPEN_LIB,
            diagnostic.kind,
        )
        assertEquals("broken.dylib", diagnostic.macroLibraryPath)
        assertEquals(
            org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Origin.EXECUTOR,
            diagnostic.diagnosticOrigin,
        )
    }

    /**
     * 验证 diag report warning 会记录但不阻断 strict expansion。
     */
    @Test
    fun batch5_diagReportWarningIsRecordedWithoutBlockingStrictExpansion() {
        val fixture = macroExpressionCarrierFixture(macroName = "Warn")
        val executor = RecordingExecutor(
            MacroExpansionResult.Success(
                tokens = listOf(tokenInfo("42")),
                "42",
                diagnostics = listOf(
                    MacroDiagnosticInfo(
                        severity = MacroDiagnosticSeverity.WARNING,
                        message = "macro warning",
                        hint = "check generated token",
                        begin = SourcePosition(line = 2, column = 3),
                        end = SourcePosition(line = 2, column = 5),
                    )
                ),
            ),
        )
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
        }

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expandWithClassification(
            pre = fixture.pre,
            context = contextWithArtifact(fixture.pre, "Warn"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        val registry = (result as? MacroConstructionResult.Success)?.registry
            ?: error("WARNING diagReport must not block strict expansion, got ${result::class.simpleName}")
        val diagnostic = registry.diagnostics.single()
        assertEquals(org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Severity.WARNING, diagnostic.severity)
        assertEquals(org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Origin.DIAG_REPORT, diagnostic.diagnosticOrigin)
        assertEquals("check generated token", diagnostic.hint)
        assertEquals(2, diagnostic.tokenRangeBeginLine)
        assertEquals(3, diagnostic.tokenRangeBeginColumn)
        assertEquals(2, diagnostic.tokenRangeEndLine)
        assertEquals(5, diagnostic.tokenRangeEndColumn)
    }

    /**
     * 验证 diag report error 会在记录后阻断 strict expansion。
     */
    @Test
    fun batch5_diagReportErrorBlocksStrictExpansionAfterBeingRecorded() {
        val fixture = macroExpressionCarrierFixture(macroName = "Error")
        val executor = RecordingExecutor(
            MacroExpansionResult.Success(
                tokens = listOf(tokenInfo("42")),
                "42",
                diagnostics = listOf(
                    MacroDiagnosticInfo(
                        severity = MacroDiagnosticSeverity.ERROR,
                        message = "macro error",
                        begin = SourcePosition(line = 4, column = 1),
                        end = SourcePosition(line = 4, column = 7),
                    )
                ),
            ),
        )
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
        }

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expandWithClassification(
            pre = fixture.pre,
            context = contextWithArtifact(fixture.pre, "Error"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        val registry = (result as? MacroConstructionResult.Failed)?.registry
            ?: error("ERROR diagReport must block strict expansion, got ${result::class.simpleName}")
        val diagnostic = registry.diagnostics.single()
        assertEquals(org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Severity.ERROR, diagnostic.severity)
        assertEquals(org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Origin.DIAG_REPORT, diagnostic.diagnosticOrigin)
        assertEquals("macro error", diagnostic.message)
        assertEquals(4, diagnostic.tokenRangeBeginLine)
        assertEquals(1, diagnostic.tokenRangeBeginColumn)
        assertEquals(4, diagnostic.tokenRangeEndLine)
        assertEquals(7, diagnostic.tokenRangeEndColumn)
    }

    /**
     * 验证 executor failure 结果中的 diag report 会先于失败诊断记录。
     */
    @Test
    fun batch5_failureResultDiagReportIsRecordedBeforeExecutorFailure() {
        val fixture = macroExpressionCarrierFixture(macroName = "FailWithDiag")
        val executor = RecordingExecutor(
            MacroExpansionResult.Failure(
                message = "executor expansion failed",
                kind = org.cangnova.cangjie.macro.MacroExpansionFailureKind.EXPAND_FAILED,
                diagnostics = listOf(
                    MacroDiagnosticInfo(
                        severity = MacroDiagnosticSeverity.WARNING,
                        message = "warning before failure",
                        hint = "emitted by macro library",
                        begin = SourcePosition(line = 8, column = 2),
                        end = SourcePosition(line = 8, column = 9),
                    )
                ),
            )
        )
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
        }

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expandWithClassification(
            pre = fixture.pre,
            context = contextWithArtifact(fixture.pre, "FailWithDiag"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        val registry = (result as? MacroConstructionResult.Failed)?.registry
            ?: error("Executor failure must block strict expansion, got ${result::class.simpleName}")
        val diagReport = registry.diagnostics.singleOrNull {
            it.diagnosticOrigin == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Origin.DIAG_REPORT
        } ?: error("Failure result diagnostics must be preserved as DIAG_REPORT: ${registry.diagnostics}")
        val executorFailure = registry.diagnostics.singleOrNull {
            it.kind == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Kind.MACRO_EXPAND_FAILED
        } ?: error("Executor failure kind must be preserved: ${registry.diagnostics}")
        assertEquals(org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Severity.WARNING, diagReport.severity)
        assertEquals("warning before failure", diagReport.message)
        assertEquals("emitted by macro library", diagReport.hint)
        assertEquals(8, diagReport.tokenRangeBeginLine)
        assertEquals(2, diagReport.tokenRangeBeginColumn)
        assertEquals("executor expansion failed", executorFailure.message)
    }

    /**
     * 验证宏 import alias conflict 会从 bindMacroImports 进入构造诊断。
     */
    @Test
    fun batch5_aliasConflictIsReportedFromBindMacroImports() {
        // 真实构造两条 import 指向不同 fqn 但绑同一短名 `Bar`，
        // 让 bindMacroImports 自然产出 aliasConflicts，再走 expand 转 MACRO_ALIAS_CONFLICT。
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val fooFq = FqName("macros.foo").child(Name.identifier("A"))
        val bazFq = FqName("macros.baz").child(Name.identifier("B"))
        val sharedAlias = Name.identifier("Bar")
        val firstImport = org.cangnova.cangjie.cfir.declarations.builder.buildImport {
            this.importedFqName = fooFq
            this.isAllUnder = false
            this.aliasName = sharedAlias
        }
        val secondImport = org.cangnova.cangjie.cfir.declarations.builder.buildImport {
            this.importedFqName = bazFq
            this.isAllUnder = false
            this.aliasName = sharedAlias
        }
        val file = buildFile {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "alias.cj"
            packageDirective = buildPackageDirective {
                this.packageFqName = packageFqName
                isMacroPackage = false
            }
            imports += firstImport
            imports += secondImport
        }
        val pre = buildPreMacroRawFiles(session, listOf(PreMacroCfirFile(file, annotationMetadata = CfirAnnotationMetadataBuilder())))
        val context = bindMacroImports(
            pre = pre,
            symbolIndex = buildMacroSymbolIndex(
                pre = pre,
                macroArtifactDefinitions = listOf(
                    MacroDefinitionEntry(
                        packageFqName = FqName("macros.foo"),
                        name = Name.identifier("A"),
                        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                    ),
                    MacroDefinitionEntry(
                        packageFqName = FqName("macros.baz"),
                        name = Name.identifier("B"),
                        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                    ),
                ),
            ),
        )
        val configuration = CompilerConfiguration().apply {
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
        }
        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = pre,
            context = context,
            mode = MacroConstructionService.Mode.STRICT,
        )

        val registry = (result as? MacroConstructionResult.Failed)?.registry
            ?: error("Alias conflict must drive Failed in STRICT mode, got ${result::class.simpleName}")
        val conflict = registry.diagnostics.singleOrNull {
            it.kind == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Kind.MACRO_ALIAS_CONFLICT
        }
        assertTrue(conflict != null, "Expected MACRO_ALIAS_CONFLICT diagnostic")
        assertEquals(sharedAlias, conflict!!.relatedName)
        assertEquals(setOf(fooFq, bazFq), conflict.relatedTargets.toSet())
    }

    /**
     * 验证不支持 plain attr 重载的宏会报告 plain macro mismatch。
     */
    @Test
    fun batch5_plainAttrWithoutOverloadSupportReportsPlainMacroMismatch() {
        val surface = expressionSurface(
            surfaceId = 4005L,
            qualifiedName = FqName("macros.PlainOnly"),
            packageFqName = FqName("sample"),
        ).copy(hasParenthesis = false)
        val fixture = macroSurfaceFixture(surface)
        val executor = LibPathRecordingExecutor(MacroExpansionResult.Success(emptyList(), ""))
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { RecordingParser() }
        }

        val context = bindMacroImports(
            pre = fixture.pre,
            symbolIndex = buildMacroSymbolIndex(
                pre = fixture.pre,
                macroArtifactDefinitions = listOf(
                    MacroDefinitionEntry(
                        packageFqName = FqName("macros"),
                        name = Name.identifier("PlainOnly"),
                        source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                        supportsPlainAttrOverload = false,
                    ),
                ),
            ),
        )
        val result = FrontendMacroConstructionService(
            configuration = configuration,
            stableSplicer = RecordingSplicer(),
        ).expandWithClassification(
            pre = fixture.pre,
            context = context,
            mode = MacroConstructionService.Mode.STRICT,
        )

        val registry = (result as? MacroConstructionResult.Failed)?.registry
            ?: error("Plain-attr mismatch must be Failed in STRICT mode, got ${result::class.simpleName}")
        val mismatch = registry.diagnostics.singleOrNull {
            it.kind == org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic.Kind.MACRO_EXPECT_PLAIN_MACRO
        }
        assertTrue(mismatch != null, "Expected MACRO_EXPECT_PLAIN_MACRO for unsupported plain-attr overload")
        assertTrue(executor.calls.isEmpty(), "Plain-attr mismatch must not invoke executor: ${executor.calls}")
    }

    /**
     * 构造表达式宏执行测试使用的载体函数与预处理结果。
     *
     * 该 fixture 将待替换的错误表达式放入函数体中，并把同一表达式登记为宏 surface，
     * 使测试能够验证 stable splice 是否精确替换原始 CFIR 载体。
     */
    private fun macroExpressionCarrierFixture(
        macroName: String = "Generated",
        hasAttr: Boolean = false,
    ): MacroExpressionCarrierFixture {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val carrier = buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("macro construction carrier")
        }
        val function = buildNamedFunction {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirNamedFunctionSymbol(CallableId(packageFqName, Name.identifier("useMacro")))
            name = Name.identifier("useMacro")
            isMut = false
            body = buildBlock {
                statements += carrier
            }
        }
        val rawSyntax = "@macros.$macroName" + (if (hasAttr) "[]" else "") + "(arg)"
        val sourceText = "package sample\nfunc useMacro() { $rawSyntax }\n"
        val sourceFile = CjInMemoryTextSourceFile("sample.cj", "testdata/sample.cj", sourceText)
        val start = sourceText.indexOf(rawSyntax)
        val surface = expressionSurface(
            surfaceId = 3000L,
            qualifiedName = FqName("macros").child(Name.identifier(macroName)),
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(null, start, start + rawSyntax.length),
        ).copy(
            hasAttr = hasAttr,
            capturedRawSyntax = rawSyntax,
            replaceHandle = CfirReplaceHandle(3000L, functionBodyMacroOwnerSnapshot(packageFqName, function), carrier),
        )
        val file = buildFile {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = sourceFile.name
            this.sourceFile = sourceFile
            sourceFileLinesMapping = sourceText.toSourceLinesMapping()
            packageDirective = buildPackageDirective {
                this.packageFqName = packageFqName
                isMacroPackage = false
            }
            declarations += function
        }
        return MacroExpressionCarrierFixture(
            function = function,
            pre = buildPreMacroRawFiles(session, listOf(freezeFixtureParserInput(file, sourceFile, listOf(surface)))),
        )
    }

    /**
     * 验证 builtin sourceFile/sourceLine 使用宿主文件元数据。
     */
    @Test
    fun builtinSourceFileAndSourceLineUseHostFileMetadata() {
        val sourceText = "package sample\n\nlet value = @sourceLine()\nlet file = @sourceFile()\n"
        val lineSurface = expressionSurface(
            surfaceId = 3003L,
            qualifiedName = FqName.topLevel(Name.identifier("sourceLine")),
            packageFqName = FqName("sample"),
            sourceRange = MacroSurfaceSourceRange(
                source = null,
                startOffset = sourceText.indexOf("@sourceLine"),
                endOffset = sourceText.indexOf("@sourceLine") + "@sourceLine()".length,
            ),
        )
        val fileSurface = expressionSurface(
            surfaceId = 3004L,
            qualifiedName = FqName.topLevel(Name.identifier("sourceFile")),
            packageFqName = FqName("sample"),
            sourceRange = MacroSurfaceSourceRange(
                source = null,
                startOffset = sourceText.indexOf("@sourceFile"),
                endOffset = sourceText.indexOf("@sourceFile") + "@sourceFile()".length,
            ),
        )
        val fixture = macroSurfaceFixture(
            surfaces = listOf(lineSurface, fileSurface),
            sourceText = sourceText,
            sourceFileName = "builtin-macro.cj",
        )
        val parser = RecordingParser()
        val configuration = CompilerConfiguration().apply {
            macroFragmentParserFactory = MacroFragmentParserFactory { parser }
        }

        val result = FrontendMacroConstructionService(
            configuration = configuration,
            stableSplicer = RecordingSplicer(),
        ).expandWithClassification(
            pre = fixture.pre,
            context = bindMacroImports(
                pre = fixture.pre,
                symbolIndex = buildMacroSymbolIndex(pre = fixture.pre),
            ),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertEquals(listOf("3"), parser.parsedTokens[0].map { it.text })
        assertEquals(listOf("\"builtin-macro.cj\""), parser.parsedTokens[1].map { it.text })
    }

    /**
     * 基于单个宏 surface 构造默认源码文件 fixture。
     */
    private fun macroSurfaceFixture(surface: MacroSurface): MacroSurfaceFixture {
        val sourceText = "package ${surface.scopeContext.packageFqName}\n" +
                "func useMacro() { ${surface.capturedRawSyntax} }\n"
        return macroSurfaceFixture(listOf(surface), sourceText)
    }

    /**
     * 构造带指定宏 surface 集合的预宏原始文件 fixture。
     *
     * @param surfaces 需要登记到预处理结果中的宏 surface，至少应包含一个元素。
     * @param sourceText 宿主源码文本，用于 sourceLine/sourceFile 等内建宏定位测试。
     * @param sourceFileName 宿主虚拟源码文件名。
     */
    private fun macroSurfaceFixture(
        surfaces: List<MacroSurface>,
        sourceText: String = "",
        sourceFileName: String = "sample.cj",
    ): MacroSurfaceFixture {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val sourceFile = CjInMemoryTextSourceFile(sourceFileName, "testdata/$sourceFileName", sourceText)
        val file = buildFile {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = sourceFileName
            this.sourceFile = sourceFile
            this.sourceFileLinesMapping = sourceText.toSourceLinesMapping()
            packageDirective = buildPackageDirective {
                packageFqName = surfaces.firstOrNull()?.scopeContext?.packageFqName ?: FqName.ROOT
                isMacroPackage = false
            }
        }
        return MacroSurfaceFixture(
            file = file,
            surface = surfaces.singleOrNull() ?: surfaces.first(),
            pre = buildPreMacroRawFiles(session, listOf(freezeFixtureParserInput(file, sourceFile, surfaces))),
        )
    }

    private fun freezeFixtureParserInput(
        file: CfirFile,
        sourceFile: CjInMemoryTextSourceFile,
        surfaces: List<MacroSurface>,
        metadata: CfirAnnotationMetadataBuilder = CfirAnnotationMetadataBuilder(),
    ): PreMacroCfirFile {
        val disposable = com.intellij.openapi.util.Disposer.newDisposable("macro-execution-host")
        try {
            org.cangnova.cangjie.CangJieCoreEnvironment.create(
                disposable, org.cangnova.cangjie.CangJieCoreEnvironmentMode.Production,
            )
            val parsed = CangJieLightParser.parse(CangJieLightParser.createBuilder(sourceFile.text), sourceFile = sourceFile)
            parsed.requireConsistentInput()
            file.publishParsingFileFacts(parsed.fileFacts, parsed.sourceContext)
            return PreMacroCfirFile(file, surfaces, metadata, parsed.diagnosticPublication, parsed.settings)
        } finally {
            com.intellij.openapi.util.Disposer.dispose(disposable)
        }
    }

    /**
     * 构造只包含单个宏工件定义的宏解析上下文。
     */
    private fun contextWithArtifact(
        pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
        name: String,
    ) = contextWithArtifacts(pre, name)

    /**
     * 构造包含多个宏工件定义的宏解析上下文。
     *
     * 这些定义均使用 `macros` 包和 `MACRO_ARTIFACT` 来源，用于模拟编译产物中可执行宏的导出表。
     */
    private fun contextWithArtifacts(
        pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
        vararg names: String,
    ) = bindMacroImports(
        pre = pre,
        symbolIndex = buildMacroSymbolIndex(
            pre = pre,
            macroArtifactDefinitions = names.map { name ->
                MacroDefinitionEntry(
                    packageFqName = FqName("macros"),
                    name = Name.identifier(name),
                    source = MacroDefinitionEntry.Source.MACRO_ARTIFACT,
                )
            },
        ),
    )

    /**
     * 构造包含指定声明的 CFIR 文件。
     *
     * 该辅助函数用于搭建宏载体周围的最小文件结构，保持模块数据、包名和 RAW_CFIR 阶段一致。
     */
    private fun fileWithDeclarations(
        moduleData: CfirModuleData,
        packageFqName: FqName,
        vararg declarations: org.cangnova.cangjie.cfir.declarations.CfirDeclaration,
    ): CfirFile = buildFile {
        this.moduleData = moduleData
        resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Synthetic.Error
        attributes = CfirDeclarationAttributes.EMPTY
        symbol = CfirFileSymbol()
        name = "sample.cj"
        packageDirective = buildPackageDirective {
            this.packageFqName = packageFqName
            isMacroPackage = false
        }
        this.declarations += declarations
    }

    /**
     * 构造测试用命名函数声明。
     *
     * 返回的函数处于 RAW_CFIR 阶段，并允许调用方注入符号和值参数以覆盖声明宏、参数宏等场景。
     */
    private fun namedFunction(
        moduleData: CfirModuleData,
        packageFqName: FqName,
        name: String,
        symbol: CfirNamedFunctionSymbol = CfirNamedFunctionSymbol(CallableId(packageFqName, Name.identifier(name))),
        valueParameters: List<CfirValueParameter> = emptyList(),
    ): CfirNamedFunction = buildNamedFunction {
        this.moduleData = moduleData
        resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Synthetic.Error
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false
        dispatchReceiverType = null
        status = CfirDeclarationStatusImpl()
        returnTypeRef = buildImplicitTypeRef()
        this.symbol = symbol
        this.name = Name.identifier(name)
        isMut = false
        this.valueParameters += valueParameters
    }

    /** metadata fixture 使用独立语义来源，不伪造 PSI 或借宿主源码推导注解语义。 */
    private class MetadataFixtureSource(text: String) : CjBinarySourceElement(text, null, text)

    /** 构造带可替换成员列表的测试 class 宏载体。 */
    private fun namedClass(
        moduleData: CfirModuleData,
        packageFqName: FqName,
        name: String,
        declarations: List<CfirDeclaration>,
    ): CfirClass = buildClass {
        this.moduleData = moduleData
        resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Synthetic.Error
        attributes = CfirDeclarationAttributes.EMPTY
        scopeProvider = CfirCangJieScopeProvider()
        status = CfirDeclarationStatusImpl()
        symbol = CfirClassSymbol(ClassId(packageFqName, Name.identifier(name)))
        this.name = Name.identifier(name)
        this.declarations += declarations
    }

    /**
     * 构造测试用值参数声明。
     *
     * 参数符号和所属函数符号会显式绑定，便于参数宏 surface 验证 replace handle 的目标声明。
     */
    private fun valueParameter(
        moduleData: CfirModuleData,
        packageFqName: FqName,
        name: String,
        containingSymbol: CfirNamedFunctionSymbol,
    ): CfirValueParameter = buildValueParameter {
        this.moduleData = moduleData
        resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Synthetic.Error
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false
        isNamed = true
        status = CfirDeclarationStatusImpl.DEFAULT
        returnTypeRef = buildImplicitTypeRef()
        symbol = CfirValueParameterSymbol(CallableId(packageFqName, Name.identifier(name)))
        this.name = Name.identifier(name)
        containingDeclarationSymbol = containingSymbol
    }

    /**
     * 构造测试用局部 pattern variable。
     *
     * 可选 initializer 用于覆盖表达式宏出现在局部变量初始化器内部时的替换与降级路径。
     */
    private fun patternVariable(
        moduleData: CfirModuleData,
        packageFqName: FqName,
        name: String,
        initializer: CfirExpression?,
    ): CfirPatternVariable = buildPatternVariable {
        this.moduleData = moduleData
        resolvePhase = CfirResolvePhase.RAW_CFIR
        origin = CfirDeclarationOrigin.Synthetic.Error
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true
        dispatchReceiverType = null
        deprecationsProvider = EmptyDeprecationsProvider
        status = CfirDeclarationStatusImpl.DEFAULT
        this.initializer = initializer
        isVar = false
        symbol = CfirPatternVariableSymbol(CallableId(packageFqName, Name.identifier(name)))
        returnTypeRef = buildImplicitTypeRef()
        pattern = buildWildcardPattern()
    }

    /**
     * 构造表达式宏 surface。
     *
     * 纯 token/mock 调用没有 CFIR 宿主，快照显式采用顶层语境；实际函数体 fixture 替换句柄时提供真实宿主快照。
     * qualifiedName 参数直接作为 capturedRawSyntax 中的源码拼写，并非包名补全后的内部名称。
     */
    private fun expressionSurface(
        surfaceId: Long,
        qualifiedName: FqName,
        packageFqName: FqName,
        sourceRange: MacroSurfaceSourceRange? = null,
    ): MacroSurfaceExpr = MacroSurfaceExpr(
        surfaceId = surfaceId,
        qualifiedName = qualifiedName,
        isQualifiedName = '.' in qualifiedName.asString(),
        kind = MacroSurface.Kind.PLAIN,
        parsingFacts = org.cangnova.cangjie.parsing.MacroParsingFacts(
            org.cangnova.cangjie.parsing.MacroParsingScope.Expression(
                org.cangnova.cangjie.parsing.CjExprKind.UNKNOWN_EXPR,
            ),
            org.cangnova.cangjie.parsing.CjMacroFragmentParent.OTHER,
            null,
        ),
        hasParenthesis = true,
        attrTokens = emptyList(),
        hasAttr = false,
        inputTokens = listOf(MacroSurfaceToken("arg", 0, 3, isSourceToken = true)),
        sourceRange = sourceRange,
        scopeContext = MacroSurfaceScopeContext(
            packageFqName = packageFqName,
            sourceModuleName = BuiltInAnnotationRegistry.sourceModuleName(packageFqName),
            enclosingClassFqName = null,
            enclosingFunctionName = null,
        ),
        modifiers = emptyList(),
        carriedAnnotations = emptyList(),
        capturedRawSyntax = "@${qualifiedName.asString()}(arg)",
        containerContext = MacroSurfaceContainerContext(
            outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY,
            isInsidePrimaryConstructor = false,
            isInsideEnumBody = false,
            isInsideBlock = true,
        ),
        replaceHandle = CfirReplaceHandle(surfaceId, topLevelMacroOwnerSnapshot(packageFqName)),
    )

    /**
     * 构造声明宏 surface。
     *
     * 声明 surface 的替换句柄指向给定函数声明，用于验证声明级 stable splice 目标。
     * qualifiedName 参数直接作为 capturedRawSyntax 中的源码拼写，并非包名补全后的内部名称。
     */
    private fun declarationSurface(
        surfaceId: Long,
        qualifiedName: FqName,
        packageFqName: FqName,
        carrier: CfirDeclaration,
    ): MacroSurfaceDecl = MacroSurfaceDecl(
        surfaceId = surfaceId,
        qualifiedName = qualifiedName,
        isQualifiedName = '.' in qualifiedName.asString(),
        kind = MacroSurface.Kind.PLAIN,
        parsingFacts = org.cangnova.cangjie.parsing.MacroParsingFacts(
            org.cangnova.cangjie.parsing.MacroParsingScope.Declaration(
                org.cangnova.cangjie.parsing.CjScopeKind.TOPLEVEL,
            ),
            org.cangnova.cangjie.parsing.CjMacroFragmentParent.OTHER,
            null,
        ),
        hasParenthesis = true,
        attrTokens = emptyList(),
        hasAttr = false,
        inputTokens = listOf(MacroSurfaceToken("decl", 0, 4, isSourceToken = true)),
        sourceRange = null,
        scopeContext = MacroSurfaceScopeContext(
            packageFqName = packageFqName,
            sourceModuleName = BuiltInAnnotationRegistry.sourceModuleName(packageFqName),
            enclosingClassFqName = null,
            enclosingFunctionName = null,
        ),
        modifiers = emptyList(),
        carriedAnnotations = emptyList(),
        capturedRawSyntax = "@${qualifiedName.asString()}",
        containerContext = MacroSurfaceContainerContext(
            outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL,
            isInsidePrimaryConstructor = false,
            isInsideEnumBody = false,
            isInsideBlock = false,
        ),
        replaceHandle = CfirReplaceHandle(surfaceId, topLevelMacroOwnerSnapshot(packageFqName), carrier),
    )

    /**
     * 构造参数宏 surface。
     *
     * 参数 surface 的替换句柄指向给定值参数，用于验证参数级宏展开结果不会误写到其他声明位置。
     * qualifiedName 参数直接作为 capturedRawSyntax 中的源码拼写，并非包名补全后的内部名称。
     */
    private fun parameterSurface(
        surfaceId: Long,
        qualifiedName: FqName,
        packageFqName: FqName,
        carrier: CfirValueParameter,
    ): MacroSurfaceParam = MacroSurfaceParam(
        surfaceId = surfaceId,
        qualifiedName = qualifiedName,
        isQualifiedName = '.' in qualifiedName.asString(),
        kind = MacroSurface.Kind.PLAIN,
        parsingFacts = org.cangnova.cangjie.parsing.MacroParsingFacts(
            org.cangnova.cangjie.parsing.MacroParsingScope.Declaration(
                org.cangnova.cangjie.parsing.CjScopeKind.PRIMARY_CONSTRUCTOR_FUNC_PARAM,
            ),
            org.cangnova.cangjie.parsing.CjMacroFragmentParent.FUNCTION_PARAMETER_LIST,
            null,
        ),
        hasParenthesis = true,
        attrTokens = emptyList(),
        hasAttr = false,
        inputTokens = listOf(MacroSurfaceToken("param", 0, 5, isSourceToken = true)),
        sourceRange = null,
        scopeContext = MacroSurfaceScopeContext(
            packageFqName = packageFqName,
            sourceModuleName = BuiltInAnnotationRegistry.sourceModuleName(packageFqName),
            enclosingClassFqName = null,
            enclosingFunctionName = null,
        ),
        modifiers = emptyList(),
        carriedAnnotations = emptyList(),
        capturedRawSyntax = "@${qualifiedName.asString()}",
        containerContext = MacroSurfaceContainerContext(
            outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.NONE,
            isInsidePrimaryConstructor = false,
            isInsideEnumBody = false,
            isInsideBlock = false,
        ),
        replaceHandle = CfirReplaceHandle(
            surfaceId,
            ownerSnapshot = MacroRawOwnerSnapshot(
                packageFqName = packageFqName,
                // 顶层函数的参数先于函数体解析；签名 owner 独立于函数体的容器及 return target。
                containerSymbols = emptyList(),
                dispatchReceiverType = null,
                inLocalContext = false,
                functionTarget = null,
                signature = MacroParameterSignature.Declaration(
                    carrier.containingDeclarationSymbol,
                    CjParameterGrammar.REGULAR_TYPED,
                ),
                nominalSyntax = null,
                capturedTypeParameters = emptyList(),
                constructedClasses = emptyList(),
            ),
            carrier = carrier,
        ),
    )

    /**
     * 构造宏执行器返回片段中使用的 token 信息。
     */
    private fun tokenInfo(text: String): org.cangnova.cangjie.macro.TokenInfo {
        return org.cangnova.cangjie.macro.TokenInfo(0u.toUByte(), text)
    }

    /**
     * 宏 surface 测试 fixture。
     */
    private data class MacroSurfaceFixture(
        /** 承载 surface 的 CFIR 文件。 */
        val file: CfirFile,
        /** 测试关注的主宏 surface。 */
        val surface: MacroSurface,
        /** 包含文件与 surface 索引的预宏原始构建结果。 */
        val pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
    )

    /**
     * 表达式宏载体 fixture。
     */
    private data class MacroExpressionCarrierFixture(
        /** 包含宏载体表达式的宿主函数。 */
        val function: org.cangnova.cangjie.cfir.declarations.CfirNamedFunction,
        /** 已登记表达式宏 surface 的预宏原始构建结果。 */
        val pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
    )

    /**
     * 记录宏执行调用的测试执行器。
     *
     * 所有调用都会返回同一个展开结果，用于检查调用参数与稳定拼接链路。
     */
    private class RecordingExecutor(
        /** 每次宏调用返回的固定展开结果。 */
        private val result: MacroExpansionResult = MacroExpansionResult.Success(emptyList(), ""),
    ) : MacroExecutor {
        /** 执行器接收到的宏调用列表。 */
        val calls = mutableListOf<MacroCallInfo>()

        /**
         * 记录库加载成功，并保持传入路径的顺序。
         */
        override fun loadLibraries(libPaths: List<String>) = org.cangnova.cangjie.macro.MacroLibraryLoadResult.Success(libPaths.toList())

        /**
         * 记录本轮宏调用并为每个调用返回固定结果。
         */
        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            this.calls += calls
            return calls.map { result }
        }

        /**
         * 测试执行器没有跨轮次状态需要清空。
         */
        override fun reset() {}

        /**
         * 测试执行器始终可用。
         */
        override fun isAvailable(): Boolean = true

        /**
         * 测试执行器不持有外部资源。
         */
        override fun close() {}
    }

    /**
     * 同时记录库路径和宏调用的测试执行器。
     */
    private class LibPathRecordingExecutor(
        /** 每个宏调用返回的固定展开结果。 */
        private val result: MacroExpansionResult,
    ) : MacroExecutor {
        /** 执行器接收到的宏调用列表。 */
        val calls = mutableListOf<MacroCallInfo>()
        /** 宏服务请求加载的库路径。 */
        val loadedLibPaths = mutableListOf<String>()

        /**
         * 记录库路径并返回加载成功。
         */
        override fun loadLibraries(libPaths: List<String>): org.cangnova.cangjie.macro.MacroLibraryLoadResult {
            loadedLibPaths += libPaths
            return org.cangnova.cangjie.macro.MacroLibraryLoadResult.Success(libPaths.toList())
        }

        /**
         * 记录本轮宏调用并为每个调用返回固定结果。
         */
        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            this.calls += calls
            return calls.map { result }
        }

        /**
         * 测试执行器没有跨轮次状态需要清空。
         */
        override fun reset() {}

        /**
         * 测试执行器始终可用。
         */
        override fun isAvailable(): Boolean = true

        /**
         * 测试执行器不持有外部资源。
         */
        override fun close() {}
    }

    /**
     * 模拟宏库加载失败的测试执行器。
     */
    private class LoadFailingExecutor(
        /** 返回给宏服务的库加载失败信息。 */
        private val failure: MacroLibraryLoadFailure,
    ) : MacroExecutor {
        /** 统计宏执行方法是否被错误调用。 */
        var executeCallCount: Int = 0

        /**
         * 固定返回库加载失败。
         */
        override fun loadLibraries(libPaths: List<String>): MacroLibraryLoadResult {
            return MacroLibraryLoadResult.Failure(listOf(failure))
        }

        /**
         * 记录执行次数；正确路径下加载失败后不应进入该方法。
         */
        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            executeCallCount += 1
            return calls.map { MacroExpansionResult.Success(emptyList(), "") }
        }

        /**
         * 测试执行器没有跨轮次状态需要清空。
         */
        override fun reset() {}

        /**
         * 测试执行器始终可用，使测试聚焦库加载失败。
         */
        override fun isAvailable(): Boolean = true

        /**
         * 测试执行器不持有外部资源。
         */
        override fun close() {}
    }

    /**
     * 按宏标识分发表达式结果的测试执行器。
     */
    private class RoutingExecutor(
        /** 宏标识到展开结果的映射表。 */
        private val results: Map<String, MacroExpansionResult>,
    ) : MacroExecutor {
        /** 执行器接收到的宏调用列表。 */
        val calls = mutableListOf<MacroCallInfo>()

        /**
         * 记录库加载成功，并保持传入路径的顺序。
         */
        override fun loadLibraries(libPaths: List<String>) = org.cangnova.cangjie.macro.MacroLibraryLoadResult.Success(libPaths.toList())

        /**
         * 根据调用中的宏标识返回对应展开结果。
         */
        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            this.calls += calls
            return calls.map { call ->
                results[call.idName]
                    ?: MacroExpansionResult.Failure("No test macro result configured for `${call.idName}`.")
            }
        }

        /**
         * 测试执行器没有跨轮次状态需要清空。
         */
        override fun reset() {}

        /**
         * 测试执行器始终可用。
         */
        override fun isAvailable(): Boolean = true

        /**
         * 测试执行器不持有外部资源。
         */
        override fun close() {}
    }

    /**
     * 记录宏片段解析输入的测试解析器。
     */
    private class RecordingParser : MacroFragmentParser {
        /** 每次解析收到的 token 列表。 */
        val parsedTokens = mutableListOf<List<MacroSurfaceToken>>()
        /** 每次解析使用的解析模式。 */
        val modes = mutableListOf<MacroFragmentParser.Mode>()

        /**
         * 记录输入并原样返回成功解析结果。
         */
        override fun parse(input: MacroFragmentInput): MacroFragmentResult {
            parsedTokens += input.tokens
            modes += input.mode
            check(input.decision.slotType == MacroReplacementSlotType.EXPRESSION) {
                "RecordingParser only models expression-fragment roots."
            }
            val payload = MacroFragmentPayload.single(buildErrorExpression {
                diagnostic = ConeSimpleDiagnostic("recorded expression fragment")
            })
            return MacroFragmentResult.Success(input.node, input.tokens, input.mode, payload)
        }
    }

    /**
     * 固定附加 payload 的测试片段解析器。
     */
    private class StaticPayloadParser(
        /** 放入解析成功结果的静态 payload。 */
        private val payload: org.cangnova.cangjie.cfir.CfirElement,
    ) : MacroFragmentParser {
        /**
         * 原样返回片段节点与 token，并携带固定 payload。
         */
        override fun parse(input: MacroFragmentInput): MacroFragmentResult {
            return MacroFragmentResult.Success(input.node, input.tokens, input.mode, MacroFragmentPayload.single(payload))
        }
    }

    /**
     * 记录 stable splice 槽位的测试拼接器。
     */
    private class RecordingSplicer : MacroStableSplicer {
        /** 服务提交给 stable splicer 的替换槽位。 */
        val slots = mutableListOf<MacroReplaceSlot>()

        /**
         * 记录槽位并返回原文件列表，避免测试关注点扩散到真实拼接实现。
         */
        override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): MacroStableSpliceResult {
            this.slots += slots
            return MacroStableSpliceResult(files, emptyList())
        }
    }

    /**
     * 使用当前预宏结果创建需求分类，并调用宏构造服务执行展开。
     *
     * 该扩展函数将测试中的上下文绑定与分类冻结逻辑集中起来，保证每个用例走同一条服务入口。
     */
    private fun FrontendMacroConstructionService.expandWithClassification(
        pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
        context: org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext,
        mode: MacroConstructionService.Mode,
    ): MacroConstructionResult {
        val classification = MacroDemandClassification.create(pre)
        classification.freezeFinal(macroArtifactDefinitions = context.symbolIndex.foreigns)
        return expand(pre, context, classification, mode)
    }

    /**
     * 宏构造执行测试使用的最小模块数据。
     */
    private class TestModuleData(session: CfirSession) : CfirModuleData() {
        /** 测试模块名称。 */
        override val name: Name = Name.identifier("frontend-macro-construction-execution-test")
        /** 测试模块不声明普通依赖。 */
        override val dependencies: List<CfirModuleData> = emptyList()
        /** 测试模块不声明 refinement 依赖。 */
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        /** 测试模块不声明传递 refinement 依赖。 */
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        /** 使用默认仓颉目标平台。 */
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        /** 使用默认 CFIR 平台标记。 */
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        /** 是否为 common 模块由目标平台判定。 */
        override val isCommon: Boolean = targetPlatform.isCommon()
        /** 测试模块不携带额外能力。 */
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        /** 稳定模块名与测试类名保持一致。 */
        override val stableModuleName: String = "frontend-macro-construction-execution-test"
        /** 绑定到该模块数据的 CFIR session。 */
        override val session: CfirSession = session

        init {
            bindSession(session)
        }
    }
}
