package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirReplaceHandle
import org.cangnova.cangjie.cfir.resolve.providers.macro.FinalMacroSurfaceDecision
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallSite
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallForestBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroForestEvaluator
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentInput
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFailurePolicy
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroReplaceSlot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroReplacementSlotType
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolution
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroStableSplicer
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceContainerContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceScopeContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceSourceRange
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.macro.MacroCallInfo
import org.cangnova.cangjie.macro.MacroExecutor
import org.cangnova.cangjie.macro.MacroExpansionResult
import org.cangnova.cangjie.macro.TokenInfo
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 宏 construction 架构边界守卫。
 *
 * 这些测试不验证具体宏语义，只防止旧 ordinary resolve / text patch 通道回流。
 */
class MacroConstructionArchitectureGuardTest {
    /**
     * 验证普通 CFIR resolve phase 枚举中不会重新引入 MACRO_EXPAND。
     */
    @Test
    fun resolvePhaseModelDoesNotContainMacroExpandPhase() {
        assertFalse(
            CfirResolvePhase.entries.any { it.name == "MACRO_EXPAND" },
            "Macro expansion must not be modeled as an ordinary CfirResolvePhase.",
        )

        val phaseSource = readRepoFile("cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirResolvePhase.kt")
        assertFalse(
            Regex("""\bMACRO_EXPAND\b""").containsMatchIn(phaseSource),
            "CfirResolvePhase source must not reintroduce MACRO_EXPAND.",
        )
    }

    /**
     * 验证 ordinary resolve 路径不再引用旧宏展开 phase 或 processor。
     */
    @Test
    fun ordinaryResolveSourcesDoNotReferenceRemovedMacroExpandPhase() {
        val forbiddenPatterns = listOf(
            Regex("""\bCfirResolvePhase\s*\.\s*MACRO_EXPAND\b"""),
            Regex("""\bMacroExpandAction\b"""),
            Regex("""\bCfirMacroExpandResolveProcessor\b"""),
        )
        val violations = sourceFilesUnder("cfir", "analysis", "compiler")
            .filterNot { it.fileName.toString() == "MacroConstructionArchitectureGuardTest.kt" }
            .flatMap { path ->
                val text = Files.readString(path, UTF_8)
                forbiddenPatterns
                    .filter { pattern -> pattern.containsMatchIn(text) }
                    .map { pattern -> root.relativize(path).toString() + ": " + pattern.pattern }
            }

        assertTrue(
            violations.isEmpty(),
            "Removed macro-expand resolve phase must not be referenced by ordinary resolve paths:\n" +
                violations.joinToString(separator = "\n"),
        )
    }

    /**
     * 验证 source provider 不暴露 recordFile 旁路 API。
     */
    @Test
    fun sourceProviderDoesNotExposeRecordFile() {
        val publicMethodNames = CfirProviderImpl::class.java.methods.mapTo(mutableSetOf()) { it.name }
        val declaredRecordFileMethods = CfirProviderImpl::class.java.declaredMethods.filter { it.name == "recordFile" }

        assertFalse(
            "recordFile" in publicMethodNames,
            "CfirProviderImpl must not expose public recordFile; use recordExpandedRawFilesOnce.",
        )
        assertTrue(
            declaredRecordFileMethods.isEmpty(),
            "CfirProviderImpl must not keep a direct recordFile side door.",
        )
    }

    /**
     * 验证 runResolution 入口不接受裸 CfirFile 列表。
     */
    @Test
    fun runResolutionDoesNotAcceptBareCfirFileList() {
        val analyseSource = readRepoFile("cfir/entrypoint/src/org/cangnova/cangjie/cfir/pipeline/analyse.kt")
        val bareListRunResolution = Regex(
            pattern = """fun\s+CfirSession\s*\.\s*runResolution\s*\([^)]*List\s*<\s*CfirFile\s*>""",
            options = setOf(RegexOption.DOT_MATCHES_ALL),
        )

        assertFalse(
            bareListRunResolution.containsMatchIn(analyseSource),
            "runResolution public API must accept RecordableRawCfirFiles, not bare List<CfirFile>.",
        )
    }

    /**
     * 验证语义宏路径不再引用 text patch 宏展开实现。
     */
    @Test
    fun semanticMacroPathDoesNotReferenceTextPatchExpansion() {
        val forbiddenTokens = listOf(
            "DefaultMacro" + "Replacer",
            "DefaultMacro" + "Expander",
            "MacroCallInfo" + "Factory",
            "MacroPsiExpansion" + "Service",
            "expanded" + "Text",
            "text" + "Patch",
            "Text" + "Patch",
        )
        val violations = sourceFilesUnder("compiler", "cfir", "analysis", "tests")
            .filterNot { it.fileName.toString() == "MacroConstructionArchitectureGuardTest.kt" }
            .flatMap { path ->
                val text = Files.readString(path, UTF_8)
                forbiddenTokens
                    .filter { token -> token in text }
                    .map { token -> root.relativize(path).toString() + ": " + token }
            }

        assertTrue(
            violations.isEmpty(),
            "Text patch macro expansion must not be referenced by semantic paths:\n" +
                violations.joinToString(separator = "\n"),
        )
    }

    /**
     * 验证 source provider 注册只能走最终 registrar。
     */
    @Test
    fun sourceProviderRegistrationUsesFinalRegistrarOnly() {
        val forbiddenPatterns = listOf(
            Regex("""\.\s*recordFile\s*\("""),
            Regex("""fun\s+recordFile\s*\("""),
        )
        val violations = sourceFilesUnder("compiler", "cfir", "analysis", "tests")
            .filterNot { it.fileName.toString() == "MacroConstructionArchitectureGuardTest.kt" }
            .flatMap { path ->
                val text = Files.readString(path, UTF_8)
                forbiddenPatterns
                    .filter { pattern -> pattern.containsMatchIn(text) }
                    .map { pattern -> root.relativize(path).toString() + ": " + pattern.pattern }
            }

        assertTrue(
            violations.isEmpty(),
            "Source provider file registration must go through recordExpandedRawFilesOnce only:\n" +
                violations.joinToString(separator = "\n"),
            )
    }

    /**
     * 验证 PSI 和 LightTree raw builder 覆盖所有宏 surface 形态。
     */
    @Test
    fun rawBuildersCoverAllMacroSurfaceShapes() {
        val requiredMacroSurfaceConstructions = listOf(
            "MacroSurfaceDecl" to Regex("""\bMacroSurfaceDecl\s*\("""),
            "MacroSurfaceParam" to Regex("""\bMacroSurfaceParam\s*\("""),
            "MacroSurfaceExpr" to Regex("""\bMacroSurfaceExpr\s*\("""),
        )
        val builtinNonMacroSurfaceCoverage = listOf(
            "IfAvailableSurface" to Regex("""\bIfAvailableSurface\s*\("""),
            "BuiltinNonMacroSurface" to Regex("""\bis\s+BuiltinNonMacroSurface\b"""),
        )
        val builderSourceGroups = listOf(
            BuilderSourceGroup(
                displayName = "PSI raw builder",
                relativePaths = listOf(
                    "cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt",
                ),
            ),
            BuilderSourceGroup(
                displayName = "LightTree raw builder",
                relativePaths = listOf(
                    "cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt",
                    "cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirExpressionBuilder.kt",
                ),
            ),
        )

        val violations = builderSourceGroups.flatMap { group ->
            val source = group.relativePaths.joinToString(separator = "\n") { readRepoFile(it) }
            buildList {
                for ((surfaceName, constructionPattern) in requiredMacroSurfaceConstructions) {
                    if (!constructionPattern.containsMatchIn(source)) {
                        add("${group.displayName}: missing explicit $surfaceName construction")
                    }
                }

                val hasBuiltinNonMacroCoverage = builtinNonMacroSurfaceCoverage.any { (_, pattern) ->
                    pattern.containsMatchIn(source)
                }
                if (!hasBuiltinNonMacroCoverage) {
                    add(
                        "${group.displayName}: missing explicit IfAvailableSurface construction " +
                            "or BuiltinNonMacroSurface branch",
                    )
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "PSI and LightTree raw builders must not diverge in macro surface coverage:\n" +
            violations.joinToString(separator = "\n"),
        )
    }

    /**
     * 验证表达式宏 raw builder 不再创建旧 CfirMacroExpression carrier。
     */
    @Test
    fun rawExpressionMacroBuildersDoNotCreateLegacyCfirMacroExpressionCarrier() {
        val builderSources = listOf(
            "cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt",
            "cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirExpressionBuilder.kt",
        ).associateWith(::readRepoFile)
        val violations = builderSources.flatMap { (path, source) ->
            val convertMacroExpression = source.substringAfter("convertMacroExpression", "")
                .substringBefore("convertCasePattern", missingDelimiterValue = source.substringAfter("convertMacroExpression", ""))
                .substringBefore("// ===== 辅助方法", missingDelimiterValue = source.substringAfter("convertMacroExpression", ""))
            buildList {
                if ("buildMacroExpression" in convertMacroExpression) {
                    add("$path: convertMacroExpression must not build legacy CfirMacroExpression carrier")
                }
                if ("CfirReplaceHandle(handleId = surfaceId, carrier = carrier)" !in convertMacroExpression) {
                    add("$path: convertMacroExpression must attach the typed carrier to CfirReplaceHandle")
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Expression macro raw builders must produce construction-only typed carriers:\n" +
                violations.joinToString(separator = "\n"),
        )
    }

    /**
     * 验证前端稳定 splicer 使用 replace handle carrier 身份匹配。
     */
    @Test
    fun frontendStableSplicerUsesReplaceHandleCarrierIdentity() {
        val source = readRepoFile("compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt")

        assertTrue(
            "IdentityHashMap<CfirExpression, MacroReplaceSlot>" in source,
            "Stable splicer must match typed expression carriers by object identity, not source offset.",
        )
        assertTrue(
            "slot.handle.carrier" in source && "transformErrorExpression" in source,
            "Stable splicer must consume CfirReplaceHandle.carrier before legacy macro-expression fallback.",
        )
    }

    /**
     * 验证宏构造 API 支持 forest、executor、fragment parser 和 splice 主链路。
     */
    @Test
    fun macroConstructionApiSupportsExecutableForestExecutorFragmentSpliceChain() {
        val surface = macroSurface(
            surfaceId = 1L,
            name = "Outer",
            startOffset = 0,
            endOffset = 20,
            inputTokens = listOf(MacroSurfaceToken("inner", 0, 5, "IDENTIFIER")),
        )
        val forest = MacroCallForestBuilder.build(listOf(surface))
        val executor = RecordingMacroExecutor(
            resultTokens = listOf(TokenInfo(kind = 1.toUByte(), value = "expanded")),
        )
        val parser = object : MacroFragmentParser {
            override fun parse(input: MacroFragmentInput): MacroFragmentResult =
                MacroFragmentResult.Success(input.node, input.tokens, input.mode)
        }
        val splicer = object : MacroStableSplicer {
            val slots = mutableListOf<MacroReplaceSlot>()

            override fun applySlices(
                files: List<org.cangnova.cangjie.cfir.declarations.CfirFile>,
                slots: List<MacroReplaceSlot>,
            ): List<org.cangnova.cangjie.cfir.declarations.CfirFile> {
                this.slots += slots
                return files
            }
        }

        val evaluatorResults = MacroForestEvaluator(maxIterations = 1).evaluate(
            forest = forest,
            expand = { node, childResults ->
                val result = executor.execute(
                    listOf(
                        MacroCallInfo(
                            idName = node.surface.qualifiedName!!.shortName().asString(),
                            methodName = "call",
                            packageName = node.surface.scopeContext.packageFqName.asString(),
                            argTokens = node.surface.inputTokens.map { TokenInfo(kind = 1.toUByte(), value = it.text) },
                            parentNames = node.parentNames,
                        )
                    )
                ).single() as MacroExpansionResult.Success
                result.tokens.mapIndexed { index, token ->
                    MacroSurfaceToken(
                        text = token.value,
                        startOffset = index,
                        endOffset = index + token.value.length,
                        kindName = token.kind.toString(),
                    )
                } + childResults.values.flatten()
            },
        )
        val rootNode = forest.roots.single()
        val fragment = parser.parse(
            MacroFragmentInput(
                node = rootNode,
                tokens = evaluatorResults.getValue(rootNode),
                decision = expressionDecision(surface),
            ),
        )
        splicer.applySlices(
            files = emptyList(),
            slots = listOf(MacroReplaceSlot(surface.replaceHandle, surface, fragment)),
        )

        assertEquals(listOf("Outer"), executor.executedCalls.map { it.idName })
        assertTrue(fragment is MacroFragmentResult.Success)
        assertEquals(surface.replaceHandle, splicer.slots.single().handle)
    }

    /**
     * 验证前端宏构造服务主流程仍显式串联 forest/evaluator/executor/parser/splicer。
     */
    @Test
    fun frontendMacroConstructionServiceMainFlowReferencesForestExecutorFragmentAndSplice() {
        val source = readRepoFile("compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt")
        val requiredFlowTokens = listOf(
            "MacroCallForestBuilder",
            "MacroForestEvaluator",
            "macroExecutorFactory",
            "MacroFragmentParser",
            "MacroStableSplicer",
        )
        val missing = requiredFlowTokens.filterNot { it in source }

        assertTrue(
            missing.isEmpty(),
            "FrontendMacroConstructionService must wire forest/evaluator/executor/fragment/splice in its main flow; " +
                "missing: ${missing.joinToString()}",
        )
    }

    /**
     * 验证内建 sourceFile/sourceLine 宏使用真实源文件元数据。
     */
    @Test
    fun frontendMacroConstructionServiceDoesNotUseDebugSourceForBuiltinMacroPosition() {
        val source = readRepoFile("compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt")

        assertFalse(
            "getElementTextInContextForDebug" in source,
            "sourceFile builtin macro must use host file metadata, not debug source text.",
        )
        assertFalse(
            Regex("""val\s+line\s*=\s*surface\.sourceRange\?\.startOffset""").containsMatchIn(source),
            "sourceLine builtin macro must not expose raw source offset as a line number.",
        )
        assertTrue(
            "sourceFileLinesMapping?.getLineByOffset" in source,
            "sourceLine builtin macro must map source offset through CfirFile.sourceFileLinesMapping.",
        )
    }

    /**
     * 验证前端宏构造默认使用真实仓颉内建非宏 desugarer。
     */
    @Test
    fun frontendMacroConstructionServiceUsesRealBuiltinNonMacroDesugarerByDefault() {
        val source = readRepoFile("compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt")

        assertTrue(
            "CangJieBuiltinNonMacroDesugarer" in source,
            "Frontend macro construction must default to the Cangjie builtin non-macro desugarer.",
        )
        assertFalse(
            Regex("""builtinNonMacroDesugarer\s*:\s*BuiltinNonMacroDesugarer\s*=\s*IdentityBuiltinNonMacroDesugarer""")
                .containsMatchIn(source),
            "Frontend macro construction must not default @IfAvailable handling to identity desugar.",
        )
    }

    /**
     * 验证宏构造诊断携带 alias conflict 的结构化 payload。
     */
    @Test
    fun macroConstructionDiagnosticsUseStructuredAliasConflictPayload() {
        val apiSource = readRepoFile("cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroConstructionApi.kt")
        val frontendSource = readRepoFile("compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt")
        val collectorSource = readRepoFile(
            "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/collectors/components/MacroConstructionDiagnosticCollectorComponent.kt",
        )
        val factorySource = readRepoFile(
            "cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/collectors/components/DiagnosticComponentsFactory.kt",
        )

        assertTrue(
            "relatedName: Name?" in apiSource && "relatedTargets: List<FqName>" in apiSource,
            "MacroConstructionDiagnostic must carry structured name/target payload for non-surface diagnostics.",
        )
        assertTrue(
            "relatedName = conflict.alias" in frontendSource && "relatedTargets = conflict.targets" in frontendSource,
            "Alias conflict reporting must populate structured diagnostic payload.",
        )
        assertTrue(
            "diagnostic.relatedTargets" in collectorSource && "diagnostic.relatedName" in collectorSource,
            "Checker collector must report alias conflict from structured payload, not empty target lists.",
        )
        assertTrue(
            "MacroConstructionDiagnosticCollectorComponent(session, reporter)" in factorySource,
            "Macro construction diagnostics must be registered in ordinary checker component factory.",
        )
        assertTrue(
            "checkAndCommitReportsOn(source, data, commitEverything = true)" in collectorSource,
            "Macro construction diagnostics must be committed at original macro/import site immediately.",
        )
    }

    /**
     * 验证 ordinary checker 诊断通过宏 registry 重映射到原始宏位置。
     */
    @Test
    fun ordinaryCheckerDiagnosticsAreRemappedThroughMacroRegistry() {
        val apiSource = readRepoFile("cfir/providers/src/org/cangnova/cangjie/cfir/resolve/providers/macro/MacroConstructionApi.kt")
        val frontendSource = readRepoFile("compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt")
        val analyseSource = readRepoFile("cfir/entrypoint/src/org/cangnova/cangjie/cfir/pipeline/analyse.kt")
        val pendingReporterSource = readRepoFile(
            "common/diagnostics/src/org/cangnova/cangjie/cfir/diagnostics/impl/PendingDiagnosticsReporterImpl.kt",
        )
        val llCollectorSource = readRepoFile(
            "analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/diagnostics/FileStructureElementDiagnosticsCollector.kt",
        )
        val llReporterSource = readRepoFile(
            "analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/diagnostics/LLCfirDiagnosticReporter.kt",
        )

        assertTrue(
            "registerGeneratedCfirElement" in apiSource &&
                "originSourceForGeneratedSource" in apiSource,
            "Macro registry must map generated CFIR source elements back to original macro surfaces.",
        )
        assertTrue(
            "registry.registerGeneratedCfirElement" in frontendSource,
            "Successful macro splice must register generated payload sources before ordinary checkers run.",
        )
        assertTrue(
            "sourceMapper = { source -> registry?.originSourceForGeneratedSource(source) }" in analyseSource,
            "runCheckers must pass macro source remapping into PendingDiagnosticsReporterImpl.",
        )
        assertTrue(
            "remapSourceIfNeeded" in pendingReporterSource &&
                "CjOffsetsOnlyDiagnosticWithParameters" in pendingReporterSource,
            "Pending diagnostics must rewrite ordinary checker diagnostics to mapped macro sources.",
        )
        assertTrue(
            "session.macroExpansionRegistry?.originSourceForGeneratedSource(source)" in llCollectorSource,
            "Low-level analysis diagnostics must use the same macro registry source remap as CLI checkers.",
        )
        assertTrue(
            "toPsiDiagnosticAt" in llReporterSource &&
                "sourceMapper(currentElement)" in llReporterSource,
            "Low-level analysis reporter must remap generated diagnostic sources before committing PSI diagnostics.",
        )
    }

    /**
     * 验证 resolve/check 主流程保持构造先于 provider 注册和 ordinary resolve。
     */
    @Test
    fun resolveAndCheckMainFlowKeepsConstructionBeforeProviderRegistrationAndOrdinaryResolve() {
        val source = readRepoFile("cfir/entrypoint/src/org/cangnova/cangjie/cfir/pipeline/analyse.kt")
        val symbolIndexIndex = source.indexOf("val symbolIndex = buildMacroSymbolIndex(")
        val artifactDefinitionsIndex = source.indexOf("macroArtifactDefinitions = macroArtifactDefinitions")
        val bindIndex = source.indexOf("val context = bindMacroImports(pre, symbolIndex)")
        val preDiagnosticsErrorGateIndex = source.indexOf("preConstructionDiagnostics.any")
        val expandIndex = source.indexOf("val result = constructionService.expand(")
        val preDiagnosticsPassIndex = source.indexOf("preConstructionDiagnostics = preConstructionDiagnostics")
        val recordIndex = source.indexOf("recordExpandedRawFilesOnce(provider, recordable, result.registry)")
        val resolveIndex = source.indexOf("val output = resolveAndCheckCfir(session, recordable, diagnosticsCollector)")

        assertTrue(
            source.indexOf("fun resolveAndCheckCfirAfterConstruction(") >= 0,
            "resolveAndCheckCfirAfterConstruction entrypoint must exist.",
        )
        assertTrue(
            listOf(
                symbolIndexIndex,
                artifactDefinitionsIndex,
                bindIndex,
                preDiagnosticsErrorGateIndex,
                expandIndex,
                preDiagnosticsPassIndex,
                recordIndex,
                resolveIndex,
            ).all { it >= 0 },
            "resolveAndCheckCfirAfterConstruction must spell out artifact-aware symbol-index -> bind -> strict error diagnostics gate -> expand with diagnostics -> record -> resolve.",
        )
        assertTrue(
            symbolIndexIndex < artifactDefinitionsIndex &&
                artifactDefinitionsIndex < bindIndex &&
                bindIndex < preDiagnosticsErrorGateIndex &&
                preDiagnosticsErrorGateIndex < expandIndex &&
                expandIndex < preDiagnosticsPassIndex &&
                preDiagnosticsPassIndex < recordIndex &&
                recordIndex < resolveIndex,
            "Macro artifact diagnostics and construction must happen before source-provider registration and ordinary resolve.",
        )
    }

    /**
     * 验证 token 重评估会迭代到稳定状态。
     */
    @Test
    fun tokenReEvaluatorReachesFixedPoint() {
        val initial = listOf(MacroSurfaceToken(text = "foo  42", startOffset = 0, endOffset = 7))
        var calls = 0
        val tokenizer: (List<MacroSurfaceToken>) -> List<MacroSurfaceToken> = { tokens ->
            calls += 1
            when (calls) {
                1 -> listOf(
                    MacroSurfaceToken("foo", 0, 3, "IDENTIFIER"),
                    MacroSurfaceToken("  ", 3, 5, "WS"),
                    MacroSurfaceToken("42", 5, 7, "INT"),
                )
                else -> tokens
            }
        }
        val stable = org.cangnova.cangjie.cfir.resolve.providers.macro.MacroTokenReEvaluator
            .reTokenizeUntilStable(initial, tokenizer, maxIterations = 4)
        assertEquals(3, stable.size, "Token re-evaluation must split the joined sequence on the first pass.")
        assertEquals(2, calls, "Stable iteration must verify with one extra tokenizer call after the first split.")
    }

    /**
     * 验证 token-backed fragment parser 在 reparse 失败时返回 Failure。
     */
    @Test
    fun tokenBackedFragmentParserReportsFailureWhenReparseReturnsNull() {
        val surface = macroSurface(
            surfaceId = 99L,
            name = "NoReparse",
            startOffset = 0,
            endOffset = 5,
            inputTokens = listOf(MacroSurfaceToken("x", 0, 1, "IDENTIFIER")),
        )
        val node = MacroCallForestBuilder.build(listOf(surface)).roots.single()
        val parser = org.cangnova.cangjie.cfir.resolve.providers.macro.TokenBackedMacroFragmentParser(
            reparse = { _, _ -> null },
            reTokenize = { tokens -> tokens },
        )
        val result = parser.parse(
            MacroFragmentInput(
                node = node,
                tokens = surface.inputTokens,
                decision = expressionDecision(surface),
            ),
        )
        assertTrue(
            result is MacroFragmentResult.Failure,
            "TokenBackedMacroFragmentParser must surface Failure when reparse returns null instead of silently passing.",
        )
    }

    /**
     * 验证宏展开 cache key 暴露 baseline 要求的十三个维度。
     */
    @Test
    fun macroExpansionCacheKeyExposesThirteenDimensions() {
        val properties = org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionCacheKey::class
            .java.declaredFields
            .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .map { it.name }
        assertTrue(
            properties.size >= 13,
            "MacroExpansionCacheKey must keep at least 13 dimensions (baseline §11); actual: $properties",
        )
        assertEquals(
            13,
            org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionCacheKey.DIMENSION_COUNT,
            "MacroExpansionCacheKey.DIMENSION_COUNT must equal the documented baseline §11 dimension list.",
        )
    }

    /**
     * 验证前端宏构造服务会为每个文件注册 cache key。
     */
    @Test
    fun frontendMacroConstructionServiceRegistersCacheKeyPerFile() {
        val source = readRepoFile("compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpandPhase.kt")
        assertTrue(
            "registerCacheKeys(pre, context, expandedFiles, registry)" in source,
            "FrontendMacroConstructionService must compute per-file cache keys after splice completes.",
        )
        assertTrue(
            "MacroConstructionService.ALGORITHM_VERSION" in source &&
                "MacroPayloadTokenizer.VERSION" in source &&
                "MacroFragmentParser.VERSION" in source &&
                "MacroBuiltinRegistries.VERSION" in source,
            "Cache key construction must reference all four versioning constants (algorithm/scanner/parser/builtin).",
        )
        assertTrue(
            "configuration.languageVersionSettings" in source,
            "Cache key must derive SDK signature from CompilerConfiguration.languageVersionSettings.",
        )
        assertTrue(
            "macroRuntimeLoaderEnv" in source &&
                "macroTargetPlatform" in source &&
                "entry.executableFqName" in source &&
                "entry.artifactSignature" in source &&
                "entry.dynamicLibHash" in source &&
                "entry.dependenciesArtifactHash" in source &&
                "entry.resolverAlgorithmVersion" in source,
            "Cache key must include executable fqName, macro artifact signature, dylib/BCHIR hashes, target platform, loader env, and resolver algorithm version.",
        )
    }

    /**
     * 验证前端管线在 artifact resolver 前运行 expansion-demand 宏包编译。
     */
    @Test
    fun frontendPipelineRunsExpansionDemandedMacroPackageCompilationBeforeArtifactResolution() {
        val pipelineSource = readRepoFile("compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/CfirFrontendPipelinePhase.kt")
        val source = readRepoFile("compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/MacroExpansionArtifactPreparation.kt")
        val preIndex = pipelineSource.indexOf("val sessionPreResults = sessionsWithSources.map")
        val classificationIndex = pipelineSource.indexOf("val classifications = sessionPreResults.map")
        val preparationCallIndex = pipelineSource.indexOf("prepareMacroArtifactDefinitionsForExpansion(configuration, classifications)")
        val demandSurfacesIndex = source.indexOf("val demandSurfacesByPackage = collectMacroExpansionPackageDemandSurfaces(classifications)")
        val demandIndex = source.indexOf("val demandedMacroPackages = demandSurfacesByPackage.keys")
        val locatorIndex = source.indexOf("val artifactLocator = MacroArtifactLocator(configuration.macroSdkHome)")
        val initialLocateIndex = source.indexOf("val initialArtifacts = artifactLocator.locate")
        val compileIndex = source.indexOf("val macroCompilation = compileRequiredMacroSourcePackages(")
        val relocalizeIndex = source.indexOf("val locatedArtifacts = artifactLocator.locate")
        val resolverIndex = source.indexOf("MacroArtifactResolver().resolve")
        val outputSearchIndex = source.indexOf("searchRoots = macroArtifactSearchRoots(configuration) + macroCompilation.artifactSearchPaths")
        val explicitArtifactsIndex = source.indexOf("explicitArtifacts = configuration.macroArtifactPackages,")
        val diagnosticsIndex = source.indexOf("diagnostics = attachDemandSurfaceOrigins(")
        val diagnosticSourcesIndex = source.indexOf("diagnostics = macroCompilation.diagnostics + artifactResolution.diagnostics")

        assertTrue(
            listOf(
                preIndex,
                classificationIndex,
                preparationCallIndex,
                demandSurfacesIndex,
                demandIndex,
                locatorIndex,
                initialLocateIndex,
                compileIndex,
                relocalizeIndex,
                resolverIndex,
                outputSearchIndex,
                explicitArtifactsIndex,
                diagnosticsIndex,
                diagnosticSourcesIndex,
            ).all { it >= 0 },
            "Frontend phase must derive macro package demand, locate artifacts, compile missing same-project sources, and re-locate compiled artifacts before resolver.",
        )
        assertTrue(
            preIndex < classificationIndex &&
                classificationIndex < preparationCallIndex &&
                demandSurfacesIndex < demandIndex &&
                demandIndex < locatorIndex &&
                locatorIndex < initialLocateIndex &&
                initialLocateIndex < compileIndex &&
                compileIndex < relocalizeIndex &&
                relocalizeIndex < resolverIndex &&
                outputSearchIndex < resolverIndex &&
                explicitArtifactsIndex < resolverIndex &&
                resolverIndex < diagnosticsIndex &&
                diagnosticsIndex < diagnosticSourcesIndex,
            "Macro package compilation must be driven by expansion demand; compiled outputs must be re-discovered by the locator before resolver validation.",
        )
    }

    /**
     * 从仓库根目录读取指定源码文件。
     */
    private fun readRepoFile(relativePath: String): String =
        Files.readString(root.resolve(relativePath), UTF_8)

    /**
     * 收集指定顶层目录下的 Kotlin 源文件。
     */
    private fun sourceFilesUnder(vararg topLevelDirs: String): List<Path> =
        topLevelDirs.flatMap { dir ->
            val start = root.resolve(dir)
            if (!Files.exists(start)) return@flatMap emptyList()

            val stream = Files.walk(start)
            try {
                stream
                    .filter { Files.isRegularFile(it) }
                    .filter { it.toString().endsWith(".kt") || it.toString().endsWith(".kts") }
                    .filter { !isGeneratedOrBuildOutput(it) }
                    .toList()
            } finally {
                stream.close()
            }
        }

    /**
     * 判断路径是否位于生成目录或构建输出目录。
     */
    private fun isGeneratedOrBuildOutput(path: Path): Boolean {
        val relative = root.relativize(path)
        return relative.any { part ->
            part.toString() in setOf("bin", "build", ".gradle", "out")
        }
    }

    /**
     * 构造测试用表达式宏 surface。
     */
    private fun macroSurface(
        surfaceId: Long,
        name: String,
        startOffset: Int,
        endOffset: Int,
        inputTokens: List<MacroSurfaceToken>,
    ): MacroSurfaceExpr {
        val packageFqName = FqName("sample")
        return MacroSurfaceExpr(
            surfaceId = surfaceId,
            qualifiedName = FqName.topLevel(Name.identifier(name)),
            kind = MacroSurface.Kind.PLAIN,
            hasParenthesis = true,
            attrTokens = emptyList(),
            inputTokens = inputTokens,
            sourceRange = MacroSurfaceSourceRange(null, startOffset, endOffset),
            scopeContext = MacroSurfaceScopeContext(
                packageFqName = packageFqName,
                enclosingClassFqName = null,
                enclosingFunctionName = Name.identifier("useMacro"),
            ),
            modifiers = emptyList(),
            carriedAnnotations = emptyList(),
            capturedRawSyntax = "@$name()",
            containerContext = MacroSurfaceContainerContext(
                outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY,
                isInsidePrimaryConstructor = false,
                isInsideEnumBody = false,
                isInsideBlock = true,
            ),
            replaceHandle = CfirReplaceHandle(surfaceId),
        )
    }

    /**
     * 记录执行调用并返回固定 token 的宏执行器。
     */
    private class RecordingMacroExecutor(
        /**
         * 每次执行返回的 token 列表。
         */
        private val resultTokens: List<TokenInfo>,
    ) : MacroExecutor {
        /**
         * 已执行的宏调用记录。
         */
        val executedCalls: MutableList<MacroCallInfo> = mutableListOf()

        /**
         * 测试执行器假定动态库加载成功。
         */
        override fun loadLibraries(libPaths: List<String>) =
            org.cangnova.cangjie.macro.MacroLibraryLoadResult.Success(libPaths.toList())

        /**
         * 记录调用并返回固定成功结果。
         */
        override fun execute(calls: List<MacroCallInfo>): List<MacroExpansionResult> {
            executedCalls += calls
            return calls.map {
                MacroExpansionResult.Success(
                    tokens = resultTokens,
                    expandedText = resultTokens.joinToString(separator = "") { token -> token.value },
                )
            }
        }

        /**
         * 测试执行器无状态重置。
         */
        override fun reset() {}

        /**
         * 测试执行器始终可用。
         */
        override fun isAvailable(): Boolean = true

        /**
         * 测试执行器关闭时不释放资源。
         */
        override fun close() {}
    }

    /**
     * raw builder 源码组。
     */
    private data class BuilderSourceGroup(
        /**
         * 用于错误消息展示的分组名。
         */
        val displayName: String,
        /**
         * 参与该分组检查的源码相对路径。
         */
        val relativePaths: List<String>,
    )

    /**
     * 构造表达式宏 surface 对应的最终决策。
     */
    private fun expressionDecision(surface: MacroSurface): FinalMacroSurfaceDecision =
        FinalMacroSurfaceDecision(
            surface = surface,
            callSite = MacroCallSite.EXPRESSION,
            slotType = MacroReplacementSlotType.EXPRESSION,
            annotationCarrier = null,
            resolution = MacroResolution.CustomAnnotation(surface.qualifiedName?.shortName() ?: Name.identifier("Unknown")),
            parserMode = MacroFragmentParser.Mode.EXPRESSION,
            localConstruction = true,
            executorRequired = false,
            externalPackageDemand = null,
            failurePolicy = MacroFailurePolicy.STRICT,
            blockedDiagnostic = null,
        )

    /**
     * 测试类共享的仓库根。
     */
    private companion object {
        /**
         * 当前 checkout 根目录。
         */
        val root: Path = findRepoRoot()

        /**
         * 从当前工作目录向上查找仓库根。
         */
        private fun findRepoRoot(): Path {
            var current = Paths.get("").toAbsolutePath()
            while (current.parent != null) {
                if (Files.exists(current.resolve("settings.gradle.kts"))) {
                    return current
                }
                current = current.parent
            }
            error("Cannot locate repository root from ${Paths.get("").toAbsolutePath()}")
        }
    }
}
