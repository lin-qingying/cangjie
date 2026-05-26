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
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroDesugarer
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirReplaceHandle
import org.cangnova.cangjie.cfir.resolve.providers.macro.IfAvailableSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDefinitionEntry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentInput
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
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.source.toSourceLinesMapping
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
            override fun parse(input: MacroFragmentInput): MacroFragmentResult {
                return MacroFragmentResult.Success(
                    originNode = input.node,
                    tokens = input.tokens,
                    mode = input.mode,
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

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
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

        val result = FrontendMacroConstructionService(configuration).expandWithClassification(
            pre = pre,
            context = contextWithArtifact(pre, "GenerateParameter"),
            mode = MacroConstructionService.Mode.STRICT,
        )

        assertTrue(result is MacroConstructionResult.Success)
        assertSame(replacement, function.valueParameters.single())
    }

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
            replaceHandle = CfirReplaceHandle(3102L, carrier),
        )
        val pre = buildPreMacroRawFiles(session, listOf(file), listOf(listOf(surface)))
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
            replaceHandle = CfirReplaceHandle(3103L, carrier),
        )
        val pre = buildPreMacroRawFiles(session, listOf(file), listOf(listOf(surface)))

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

        val result = FrontendMacroConstructionService(CompilerConfiguration()).expandWithClassification(
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

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expandWithClassification(
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

        val result = FrontendMacroConstructionService(configuration, RecordingSplicer()).expandWithClassification(
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

    // ============================================================
    // Batch 5 独立用例（PLAN.md §12 Batch 5）
    // 与 cfir/analysis-tests/testData/diagnostics2/macro/ 下的
    // alias_conflict / lib_path / executor_unavailable /
    // forced_kind_mismatch / plain_attr_overload 5 个 .cj 规范对齐：
    // 这里在程序级别断言 FrontendMacroConstructionService 真正按
    // baseline §4 / §8 产出对应的 MacroConstructionDiagnostic.Kind。
    // ============================================================

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
            // 不注入 macroExecutorFactory → executor 不可用
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
                payload = buildErrorExpression {
                    diagnostic = ConeSimpleDiagnostic("expanded WithLib payload")
                },
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
        val pre = buildPreMacroRawFiles(session, listOf(file), listOf(emptyList()))
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

    private fun macroExpressionCarrierFixture(macroName: String = "Generated"): MacroExpressionCarrierFixture {
        val session = object : CfirSession(CfirSession.Kind.Source) {}
        val moduleData = TestModuleData(session)
        session.register(CfirModuleData::class, moduleData)
        val packageFqName = FqName("sample")
        val carrier = buildErrorExpression {
            diagnostic = ConeSimpleDiagnostic("macro construction carrier")
        }
        val surface = expressionSurface(
            surfaceId = 3000L,
            qualifiedName = FqName("macros").child(Name.identifier(macroName)),
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
                isMacroPackage = false
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
        ).expandWithClassification(
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
                isMacroPackage = false
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
            isMacroPackage = false
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
        private val result: MacroExpansionResult = MacroExpansionResult.Success(emptyList(), ""),
    ) : MacroExecutor {
        val calls = mutableListOf<MacroCallInfo>()

        override fun loadLibraries(libPaths: List<String>) = org.cangnova.cangjie.macro.MacroLibraryLoadResult.Success(libPaths.toList())
        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            this.calls += calls
            return calls.map { result }
        }
        override fun reset() {}
        override fun isAvailable(): Boolean = true
        override fun close() {}
    }

    private class LibPathRecordingExecutor(
        private val result: MacroExpansionResult,
    ) : MacroExecutor {
        val calls = mutableListOf<MacroCallInfo>()
        val loadedLibPaths = mutableListOf<String>()

        override fun loadLibraries(libPaths: List<String>): org.cangnova.cangjie.macro.MacroLibraryLoadResult {
            loadedLibPaths += libPaths
            return org.cangnova.cangjie.macro.MacroLibraryLoadResult.Success(libPaths.toList())
        }

        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            this.calls += calls
            return calls.map { result }
        }

        override fun reset() {}
        override fun isAvailable(): Boolean = true
        override fun close() {}
    }

    private class LoadFailingExecutor(
        private val failure: MacroLibraryLoadFailure,
    ) : MacroExecutor {
        var executeCallCount: Int = 0

        override fun loadLibraries(libPaths: List<String>): MacroLibraryLoadResult {
            return MacroLibraryLoadResult.Failure(listOf(failure))
        }

        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            executeCallCount += 1
            return calls.map { MacroExpansionResult.Success(emptyList(), "") }
        }

        override fun reset() {}
        override fun isAvailable(): Boolean = true
        override fun close() {}
    }

    private class RoutingExecutor(
        private val results: Map<String, MacroExpansionResult>,
    ) : MacroExecutor {
        val calls = mutableListOf<MacroCallInfo>()

        override fun loadLibraries(libPaths: List<String>) = org.cangnova.cangjie.macro.MacroLibraryLoadResult.Success(libPaths.toList())
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

        override fun parse(input: MacroFragmentInput): MacroFragmentResult {
            parsedTokens += input.tokens
            modes += input.mode
            return MacroFragmentResult.Success(input.node, input.tokens, input.mode)
        }
    }

    private class StaticPayloadParser(
        private val payload: Any,
    ) : MacroFragmentParser {
        override fun parse(input: MacroFragmentInput): MacroFragmentResult {
            return MacroFragmentResult.Success(input.node, input.tokens, input.mode, payload)
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

    private fun FrontendMacroConstructionService.expandWithClassification(
        pre: org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult,
        context: org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext,
        mode: MacroConstructionService.Mode,
    ): MacroConstructionResult {
        val classification = MacroDemandClassification.create(pre)
        classification.freezeFinal(macroArtifactDefinitions = context.symbolIndex.foreigns)
        return expand(pre, context, classification, mode)
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
