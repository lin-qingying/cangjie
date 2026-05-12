package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirErrorFunction
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroDesugarer
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirReplaceHandle
import org.cangnova.cangjie.cfir.resolve.providers.macro.IfAvailableSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroReplaceSlot
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
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@OptIn(CompilerConfiguration.Internals::class)
class FrontendMacroConstructionExecutionTest {
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
            override fun parse(
                node: MacroCallNode,
                tokens: List<MacroSurfaceToken>,
                mode: MacroFragmentParser.Mode,
            ): MacroFragmentResult {
                return MacroFragmentResult.Success(
                    originNode = node,
                    tokens = tokens,
                    mode = mode,
                    payload = buildErrorExpression {
                        diagnostic = ConeSimpleDiagnostic("expanded macro payload")
                    },
                )
            }
        }
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory { executor }
            macroFragmentParserFactory = MacroFragmentParserFactory { parser }
        }

        val result = FrontendMacroConstructionService(configuration).expand(
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

    @Test
    fun defaultStableSplicerReplacesDeclarationCarrierWithParsedDeclarationPayload() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val original = namedFunction(moduleData, packageFqName, "original")
        val replacement = namedFunction(moduleData, packageFqName, "expanded")
        val file = fileWithDeclarations(moduleData, packageFqName, original)
        val surface = declarationSurface(
            surfaceId = 3100L,
            qualifiedName = FqName("macros.GenerateDeclaration"),
            packageFqName = packageFqName,
            carrier = original,
        )
        val pre = buildPreMacroRawFiles(session, listOf(file), listOf(listOf(surface)))
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory {
                RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo("func expanded() {}")), "func expanded() {}"))
            }
            macroFragmentParserFactory = MacroFragmentParserFactory {
                StaticPayloadParser(replacement)
            }
        }

        val result = FrontendMacroConstructionService(configuration).expand(
            pre = pre,
            context = contextWithArtifact(pre, "GenerateDeclaration"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertSame(replacement, file.declarations.single())
    }

    @Test
    fun defaultStableSplicerReplacesParameterCarrierWithParsedParameterPayload() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val functionSymbol = CfirNamedFunctionSymbol(CallableId(packageFqName, Name.identifier("useMacro")))
        val original = valueParameter(moduleData, packageFqName, "before", functionSymbol)
        val replacement = valueParameter(moduleData, packageFqName, "after", functionSymbol)
        val function = namedFunction(moduleData, packageFqName, "useMacro", functionSymbol, listOf(original))
        val file = fileWithDeclarations(moduleData, packageFqName, function)
        val surface = parameterSurface(
            surfaceId = 3101L,
            qualifiedName = FqName("macros.GenerateParameter"),
            packageFqName = packageFqName,
            carrier = original,
        )
        val pre = buildPreMacroRawFiles(session, listOf(file), listOf(listOf(surface)))
        val configuration = CompilerConfiguration().apply {
            macroExecutorFactory = MacroExecutorFactory {
                RecordingExecutor(MacroExpansionResult.Success(listOf(tokenInfo("after: Int")), "after: Int"))
            }
            macroFragmentParserFactory = MacroFragmentParserFactory {
                StaticPayloadParser(replacement)
            }
        }

        val result = FrontendMacroConstructionService(configuration).expand(
            pre = pre,
            context = contextWithArtifact(pre, "GenerateParameter"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertSame(replacement, function.valueParameters.single())
    }

    @Test
    fun degradedModeBuildsTypedDeclarationAndParameterPlaceholdersWhenParserIsUnavailable() {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val declarationCarrier = namedFunction(moduleData, packageFqName, "declarationCarrier")
        val functionSymbol = CfirNamedFunctionSymbol(CallableId(packageFqName, Name.identifier("parameterCarrier")))
        val parameterCarrier = valueParameter(moduleData, packageFqName, "before", functionSymbol)
        val parameterOwner = namedFunction(moduleData, packageFqName, "parameterCarrier", functionSymbol, listOf(parameterCarrier))
        val file = fileWithDeclarations(moduleData, packageFqName, declarationCarrier, parameterOwner)
        val surfaces = listOf(
            declarationSurface(3200L, FqName("macros.GenerateDeclaration"), packageFqName, declarationCarrier),
            parameterSurface(3201L, FqName("macros.GenerateParameter"), packageFqName, parameterCarrier),
        )
        val pre = buildPreMacroRawFiles(session, listOf(file), listOf(surfaces))

        val result = FrontendMacroConstructionService(CompilerConfiguration()).expand(
            pre = pre,
            context = bindMacroImports(pre, buildMacroSymbolIndex(pre)),
            mode = MacroConstructionService.Mode.DEGRADED,
        )

        assertTrue(result is MacroConstructionResult.Degraded)
        assertInstanceOf(CfirErrorFunction::class.java, file.declarations.first())
        val parameterPlaceholder = (file.declarations[1] as CfirNamedFunction).valueParameters.single()
        assertInstanceOf(CfirErrorExpression::class.java, parameterPlaceholder.defaultValue)
        assertEquals(
            surfaces.map { it.surfaceId }.toSet(),
            (result as MacroConstructionResult.Degraded).registry.placeholderOriginById.keys,
        )
    }

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

        val result = FrontendMacroConstructionService(configuration, splicer).expand(
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
                MacroSurfaceToken("left", 10, 14),
                MacroSurfaceToken(" ", 14, 15),
                MacroSurfaceToken("@ChildOne", 20, 29),
                MacroSurfaceToken("(", 29, 30),
                MacroSurfaceToken("one", 30, 33),
                MacroSurfaceToken(")", 33, 34),
                MacroSurfaceToken(" ", 34, 35),
                MacroSurfaceToken("middle", 35, 41),
                MacroSurfaceToken(" ", 41, 42),
                MacroSurfaceToken("@ChildTwo", 50, 59),
                MacroSurfaceToken("(", 59, 60),
                MacroSurfaceToken("two", 60, 63),
                MacroSurfaceToken(")", 63, 64),
                MacroSurfaceToken(" ", 64, 65),
                MacroSurfaceToken("right", 65, 70),
            ),
        )
        val firstChild = expressionSurface(
            surfaceId = 3301L,
            qualifiedName = FqName("macros.ChildOne"),
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 20, endOffset = 34),
        ).copy(
            inputTokens = listOf(MacroSurfaceToken("one", 30, 33)),
        )
        val secondChild = expressionSurface(
            surfaceId = 3302L,
            qualifiedName = FqName("macros.ChildTwo"),
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 50, endOffset = 64),
        ).copy(
            inputTokens = listOf(MacroSurfaceToken("two", 60, 63)),
        )
        val fixture = macroSurfaceFixture(listOf(parent, firstChild, secondChild))
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

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expand(
            pre = fixture.pre,
            context = contextWithArtifacts(fixture.pre, "Parent", "ChildOne", "ChildTwo"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertEquals(listOf("ChildOne", "ChildTwo", "Parent"), executor.calls.map { it.idName })
        assertEquals(
            listOf("left", " ", "expandedOne", " ", "middle", " ", "expandedTwo", " ", "right"),
            executor.calls.single { it.idName == "Parent" }.argTokens.map { it.value },
        )
    }

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
                MacroSurfaceToken("#[", 10, 12),
                MacroSurfaceToken("@Child", 20, 26),
                MacroSurfaceToken("(", 26, 27),
                MacroSurfaceToken("childArg", 27, 35),
                MacroSurfaceToken(")", 35, 36),
                MacroSurfaceToken("]", 36, 37),
            ),
            inputTokens = listOf(MacroSurfaceToken("arg", 50, 53)),
        )
        val child = expressionSurface(
            surfaceId = 3311L,
            qualifiedName = FqName("macros.Child"),
            packageFqName = packageFqName,
            sourceRange = MacroSurfaceSourceRange(source = null, startOffset = 20, endOffset = 36),
        ).copy(
            inputTokens = listOf(MacroSurfaceToken("childArg", 27, 35)),
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

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expand(
            pre = fixture.pre,
            context = contextWithArtifacts(fixture.pre, "Parent", "Child"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertEquals(listOf("Child", "Parent"), executor.calls.map { it.idName })
        assertEquals(
            listOf("#[", "expandedChild", "]"),
            executor.calls.single { it.idName == "Parent" }.attrTokens.map { it.value },
        )
        assertEquals(
            listOf("arg"),
            executor.calls.single { it.idName == "Parent" }.argTokens.map { it.value },
        )
    }

    private fun macroExpressionCarrierFixture(): MacroExpressionCarrierFixture {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val carrier = buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("macro construction carrier")
        }
        val surface = expressionSurface(
            surfaceId = 3000L,
            qualifiedName = FqName("macros.Generated"),
            packageFqName = packageFqName,
        ).copy(
            replaceHandle = CfirReplaceHandle(3000L, carrier),
        )
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
        val file = buildFile {
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.Error
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "sample.cj"
            packageDirective = buildPackageDirective {
                this.packageFqName = packageFqName
            }
            declarations += function
        }
        return MacroExpressionCarrierFixture(
            function = function,
            pre = buildPreMacroRawFiles(session, listOf(file), listOf(listOf(surface))),
        )
    }

    @Test
    fun builtinNonMacroSurfaceIsDesugaredBeforeStableSplice() {
        val surface = ifAvailableSurface()
        val fixture = macroSurfaceFixture(surface)
        val parser = RecordingParser()
        val desugarer = RecordingDesugarer()
        val splicer = RecordingSplicer()
        val configuration = CompilerConfiguration().apply {
            macroFragmentParserFactory = MacroFragmentParserFactory { parser }
        }

        val result = FrontendMacroConstructionService(
            configuration = configuration,
            stableSplicer = splicer,
            builtinNonMacroDesugarer = desugarer,
        ).expand(
            pre = fixture.pre,
            context = bindMacroImports(
                pre = fixture.pre,
                symbolIndex = buildMacroSymbolIndex(pre = fixture.pre),
            ),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertEquals(listOf("body"), parser.parsedTokens.single().map { it.text })
        assertSame(surface, desugarer.surfaces.single())
        assertTrue(splicer.slots.isEmpty())
        assertEquals(
            "body",
            (result as MacroConstructionResult.Success).registry.generatedDisplayText.getValue(surface.surfaceId),
        )
    }

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
        ).expand(
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

    private fun macroSurfaceFixture(surface: MacroSurface): MacroSurfaceFixture {
        return macroSurfaceFixture(listOf(surface))
    }

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
            }
        }
        return MacroSurfaceFixture(
            file = file,
            surface = surfaces.singleOrNull() ?: surfaces.first(),
            pre = buildPreMacroRawFiles(session, listOf(file), listOf(surfaces)),
        )
    }

    private fun contextWithArtifact(
        pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
        name: String,
    ) = contextWithArtifacts(pre, name)

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
        }
        this.declarations += declarations
    }

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

    private fun expressionSurface(
        surfaceId: Long,
        qualifiedName: FqName,
        packageFqName: FqName,
        sourceRange: MacroSurfaceSourceRange? = null,
    ): MacroSurfaceExpr = MacroSurfaceExpr(
        surfaceId = surfaceId,
        qualifiedName = qualifiedName,
        kind = MacroSurface.Kind.PLAIN,
        hasParenthesis = true,
        attrTokens = emptyList(),
        inputTokens = listOf(MacroSurfaceToken("arg", 0, 3)),
        sourceRange = sourceRange,
        scopeContext = MacroSurfaceScopeContext(packageFqName, null, null),
        modifiers = emptyList(),
        carriedAnnotations = emptyList(),
        capturedRawSyntax = "@${qualifiedName.asString()}(arg)",
        containerContext = MacroSurfaceContainerContext(
            outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY,
            isInsidePrimaryConstructor = false,
            isInsideEnumBody = false,
            isInsideBlock = true,
        ),
        replaceHandle = CfirReplaceHandle(surfaceId),
    )

    private fun declarationSurface(
        surfaceId: Long,
        qualifiedName: FqName,
        packageFqName: FqName,
        carrier: CfirNamedFunction,
    ): MacroSurfaceDecl = MacroSurfaceDecl(
        surfaceId = surfaceId,
        qualifiedName = qualifiedName,
        kind = MacroSurface.Kind.PLAIN,
        hasParenthesis = true,
        attrTokens = emptyList(),
        inputTokens = listOf(MacroSurfaceToken("decl", 0, 4)),
        sourceRange = null,
        scopeContext = MacroSurfaceScopeContext(packageFqName, null, null),
        modifiers = emptyList(),
        carriedAnnotations = emptyList(),
        capturedRawSyntax = "@${qualifiedName.asString()}",
        containerContext = MacroSurfaceContainerContext(
            outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL,
            isInsidePrimaryConstructor = false,
            isInsideEnumBody = false,
            isInsideBlock = false,
        ),
        replaceHandle = CfirReplaceHandle(surfaceId, carrier),
    )

    private fun parameterSurface(
        surfaceId: Long,
        qualifiedName: FqName,
        packageFqName: FqName,
        carrier: CfirValueParameter,
    ): MacroSurfaceParam = MacroSurfaceParam(
        surfaceId = surfaceId,
        qualifiedName = qualifiedName,
        kind = MacroSurface.Kind.PLAIN,
        hasParenthesis = true,
        attrTokens = emptyList(),
        inputTokens = listOf(MacroSurfaceToken("param", 0, 5)),
        sourceRange = null,
        scopeContext = MacroSurfaceScopeContext(packageFqName, null, null),
        modifiers = emptyList(),
        carriedAnnotations = emptyList(),
        capturedRawSyntax = "@${qualifiedName.asString()}",
        containerContext = MacroSurfaceContainerContext(
            outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.NONE,
            isInsidePrimaryConstructor = false,
            isInsideEnumBody = false,
            isInsideBlock = false,
        ),
        replaceHandle = CfirReplaceHandle(surfaceId, carrier),
    )

    private fun tokenInfo(text: String): org.cangnova.cangjie.macro.TokenInfo {
        return org.cangnova.cangjie.macro.TokenInfo(0u.toUByte(), text)
    }

    private fun ifAvailableSurface(): IfAvailableSurface = IfAvailableSurface(
        surfaceId = 3002L,
        qualifiedName = FqName.topLevel(Name.identifier("IfAvailable")),
        kind = MacroSurface.Kind.PLAIN,
        hasParenthesis = true,
        attrTokens = emptyList(),
        inputTokens = listOf(MacroSurfaceToken("condition", 0, 9)),
        sourceRange = null,
        scopeContext = MacroSurfaceScopeContext(FqName("sample"), null, null),
        modifiers = emptyList(),
        carriedAnnotations = emptyList(),
        capturedRawSyntax = "@IfAvailable(condition)",
        containerContext = MacroSurfaceContainerContext(
            outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL,
            isInsidePrimaryConstructor = false,
            isInsideEnumBody = false,
            isInsideBlock = false,
        ),
        replaceHandle = CfirReplaceHandle(3002L),
        branchTokens = listOf(MacroSurfaceToken("body", 0, 4)),
    )

    private data class MacroSurfaceFixture(
        val file: CfirFile,
        val surface: MacroSurface,
        val pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
    )

    private data class MacroExpressionCarrierFixture(
        val function: org.cangnova.cangjie.cfir.declarations.CfirNamedFunction,
        val pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
    )

    private class RecordingExecutor(
        private val result: MacroExpansionResult,
    ) : MacroExecutor {
        val calls = mutableListOf<MacroCallInfo>()

        override fun loadLibraries(libPaths: List<String>) {}
        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            this.calls += calls
            return calls.map { result }
        }
        override fun reset() {}
        override fun isAvailable(): Boolean = true
        override fun close() {}
    }

    private class RoutingExecutor(
        private val results: Map<String, MacroExpansionResult>,
    ) : MacroExecutor {
        val calls = mutableListOf<MacroCallInfo>()

        override fun loadLibraries(libPaths: List<String>) {}
        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            this.calls += calls
            return calls.map { call ->
                results[call.idName]
                    ?: MacroExpansionResult.Failure("No test macro result configured for `${call.idName}`.")
            }
        }
        override fun reset() {}
        override fun isAvailable(): Boolean = true
        override fun close() {}
    }

    private class RecordingParser : MacroFragmentParser {
        val parsedTokens = mutableListOf<List<MacroSurfaceToken>>()
        val modes = mutableListOf<MacroFragmentParser.Mode>()

        override fun parse(
            node: MacroCallNode,
            tokens: List<MacroSurfaceToken>,
            mode: MacroFragmentParser.Mode,
        ): MacroFragmentResult {
            parsedTokens += tokens
            modes += mode
            return MacroFragmentResult.Success(node, tokens, mode)
        }
    }

    private class StaticPayloadParser(
        private val payload: Any,
    ) : MacroFragmentParser {
        override fun parse(
            node: MacroCallNode,
            tokens: List<MacroSurfaceToken>,
            mode: MacroFragmentParser.Mode,
        ): MacroFragmentResult {
            return MacroFragmentResult.Success(node, tokens, mode, payload)
        }
    }

    private class RecordingDesugarer : BuiltinNonMacroDesugarer {
        val surfaces = mutableListOf<org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroSurface>()

        override fun desugar(
            surface: org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroSurface,
            fragment: MacroFragmentResult.Success,
        ): MacroFragmentResult? {
            surfaces += surface
            return fragment
        }
    }

    private class RecordingSplicer : MacroStableSplicer {
        val slots = mutableListOf<MacroReplaceSlot>()

        override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): List<CfirFile> {
            this.slots += slots
            return files
        }
    }

    private class TestModuleData(session: CfirSession) : CfirModuleData() {
        override val name: Name = Name.identifier("frontend-macro-construction-execution-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = true
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "frontend-macro-construction-execution-test"
        override val session: CfirSession = session

        init {
            bindSession(session)
        }
    }
}
