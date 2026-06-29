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
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

@OptIn(CompilerConfiguration.Internals::class)
/**
 * 覆盖前端宏构造执行链路中的 splicer、executor、parser、降级和诊断行为。
 */
class FrontendMacroConstructionExecutionTest {
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

    /**
     * 验证 parser 不可用时 DEGRADED 模式会构造声明和参数占位符。
     */
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

    /**
     * 验证 builtin non-macro surface 在 stable splice 前完成脱糖。
     */
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
        return macroSurfaceFixture(listOf(surface))
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
            pre = buildPreMacroRawFiles(session, listOf(file), listOf(surfaces)),
        )
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
     * 该 surface 默认位于函数体块内，并携带表达式替换句柄，是宏执行测试中最常见的输入形态。
     */
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

    /**
     * 构造声明宏 surface。
     *
     * 声明 surface 的替换句柄指向给定函数声明，用于验证声明级 stable splice 目标。
     */
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

    /**
     * 构造参数宏 surface。
     *
     * 参数 surface 的替换句柄指向给定值参数，用于验证参数级宏展开结果不会误写到其他声明位置。
     */
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

    /**
     * 构造宏执行器返回片段中使用的 token 信息。
     */
    private fun tokenInfo(text: String): org.cangnova.cangjie.macro.TokenInfo {
        return org.cangnova.cangjie.macro.TokenInfo(0u.toUByte(), text)
    }

    /**
     * 构造 `@IfAvailable` 内建 surface。
     *
     * 该 surface 使用 branch token 表达脱糖后的主体片段，用于验证 builtin non-macro 不进入稳定拼接队列。
     */
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
            return MacroFragmentResult.Success(input.node, input.tokens, input.mode)
        }
    }

    /**
     * 固定附加 payload 的测试片段解析器。
     */
    private class StaticPayloadParser(
        /** 放入解析成功结果的静态 payload。 */
        private val payload: Any,
    ) : MacroFragmentParser {
        /**
         * 原样返回片段节点与 token，并携带固定 payload。
         */
        override fun parse(input: MacroFragmentInput): MacroFragmentResult {
            return MacroFragmentResult.Success(input.node, input.tokens, input.mode, payload)
        }
    }

    /**
     * 记录内建 non-macro 脱糖调用的测试脱糖器。
     */
    private class RecordingDesugarer : BuiltinNonMacroDesugarer {
        /** 已进入脱糖流程的内建 surface。 */
        val surfaces = mutableListOf<org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroSurface>()

        /**
         * 记录 surface 并将片段结果原样传回调用方。
         */
        override fun desugar(
            surface: org.cangnova.cangjie.cfir.resolve.providers.macro.BuiltinNonMacroSurface,
            fragment: MacroFragmentResult.Success,
        ): MacroFragmentResult? {
            surfaces += surface
            return fragment
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
        override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): List<CfirFile> {
            this.slots += slots
            return files
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
