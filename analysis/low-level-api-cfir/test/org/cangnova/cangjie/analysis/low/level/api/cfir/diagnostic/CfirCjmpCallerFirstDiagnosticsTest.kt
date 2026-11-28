package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.plus
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.session.getContainingCjmpDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.cjmpMatchFacts
import org.cangnova.cangjie.cfir.declarations.cjmpMatchResult
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * P1/A2 回归：首个语义请求必须经过真实 LL 文件诊断入口，并最终绑定跨文件 specific 声明。
 *
 * 测试用例生命周期、模块图、参数注入复用 SourcePostSemaDiagnosticsTest 的执行基座。
 */
class CfirCjmpCallerFirstDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics/cjmpCallerFirst",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    override val additionalDirectives = super.additionalDirectives + CfirDiagnosticsDirectives

    @Test
    fun genericOuterConstraintMappingFirstRequest(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertGenericOuterConstraints(mainFile, testServices, emptyList())
    }

    @Test
    fun genericOuterConstraintStricterFirstRequest(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertGenericOuterConstraints(mainFile, testServices, listOf("CFIR_GENERIC_CONSTRAINT_NOT_LOOSER"))
    }

    /** E8：首次 LL 诊断请求产生完整 T/V -> U/W 映射；后置约束错误不删除已经建立的配对。 */
    private fun assertGenericOuterConstraints(mainFile: CjFile, testServices: TestServices, expectedDiagnostics: List<String>) {
        prepareCjmpParsingContexts(testServices)
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val mode = module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            val file = module.cjFiles.single()
            val rawFile = file.getOrBuildCfirFile(file.getResolutionFacadeForTest())
            assertEquals(mode, rawFile.moduleData.session.cjmpSettings.mode, "${module.name}: session 必须消费模块配置的 CJMP 模式")
        }
        val specificFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val specific = specificFile.declarations.filterIsInstance<CfirClass>().single()
            .declarations.filterIsInstance<CfirNamedFunction>().single()
        val commonPsi = testServices.cjTestModuleStructure.mainModules.single { it.name == "common" }.cjFiles.single()
        val commonFile = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
        val common = commonFile.declarations.filterIsInstance<CfirClass>().single()
            .declarations.filterIsInstance<CfirNamedFunction>().single()
        assertTrue(specific.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertTrue(specific.body is CfirLazyBlock)
        assertNull(specific.cjmpMatchResult)

        val facade = mainFile.getResolutionFacadeForTest()
        val diagnostics = mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(expectedDiagnostics, diagnostics.map { it.factoryName }.sorted())
        val family = specific.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(specific)
        assertSame(common.symbol, family.commonFor(specific)?.symbol)
        val mapping = checkNotNull(specific.cjmpMatchResult).counterparts.single().typeParameterMapping
            .map { (source, target) -> source.symbol.name.asString() to target.symbol.name.asString() }.toMap()
        assertEquals(mapOf("T" to "U", "V" to "W"), mapping)
        assertEquals(
            expectedDiagnostics,
            mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
                .map { it.factoryName }.sorted(),
        )
    }

    @Test
    fun commonDiagnosticsAreIsolatedBySpecificUseSite(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        prepareCjmpParsingContexts(testServices)
        assertEquals("unmatched", mainModule.name)
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val mode = module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            val sourceFile = module.cjFiles.first()
            val rawFile = sourceFile.getOrBuildCfirFile(sourceFile.getResolutionFacadeForTest())
            assertEquals(mode, rawFile.moduleData.session.cjmpSettings.mode, "${module.name}: session 必须消费模块配置的 CJMP 模式")
        }

        val commonModule = testServices.cjTestModuleStructure.mainModules
            .single { it.name == "common" }
        val commonFile = commonModule.cjFiles.single { it.name == "common.cj" }
        val matchedFile = testServices.cjTestModuleStructure.mainModules
            .single { it.name == "matched" }
            .cjFiles.single()
        val unmatchedFile = mainFile

        fun commonDiagnosticNames(
            sourceFile: CjFile,
            useSiteFile: CjFile,
            filter: DiagnosticCheckerFilter = DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
        ): List<String> = sourceFile
            .collectDiagnosticsForFile(
                useSiteFile.getResolutionFacadeForTest(),
                filter,
            )
            .also { diagnostics ->
                assertTrue(diagnostics.all { it.psiElement.containingFile === sourceFile })
            }
            .map { it.factoryName }
            .sorted()

        val unmatchedFirst = commonDiagnosticNames(commonFile, unmatchedFile)
        val matchedSecond = commonDiagnosticNames(commonFile, matchedFile)
        val matchedFirst = commonDiagnosticNames(commonFile, matchedFile)
        val unmatchedSecond = commonDiagnosticNames(commonFile, unmatchedFile)

        val commonErrorFile = commonModule.cjFiles.single { it.name == "commonError.cj" }
        val defaultCommonErrorDiagnostics = commonDiagnosticNames(commonErrorFile, unmatchedFile)
        val mixedFilters = listOf(
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS + DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS,
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS + DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS,
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS +
                DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS +
                DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS,
        )
        val mixedCommonErrorDiagnostics = mixedFilters.map { filter ->
            commonDiagnosticNames(commonErrorFile, unmatchedFile, filter)
        }

        assertEquals(listOf("CFIR_NOT_MATCHED"), unmatchedFirst)
        assertEquals(emptyList<String>(), matchedSecond)
        assertEquals(matchedSecond, matchedFirst)
        assertEquals(unmatchedFirst, unmatchedSecond)
        assertTrue("CFIR_UNRESOLVED_REFERENCE" in defaultCommonErrorDiagnostics)
        assertTrue(mixedCommonErrorDiagnostics.all { it == defaultCommonErrorDiagnostics })
    }

    @Test
    fun defaultArgumentCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertCallerFirst(mainFile, mainModule, testServices)
    }

    @Test
    fun shadowedCommonCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertCallerFirst(mainFile, mainModule, testServices)
    }

    /** 参数类型相同但名称不同不构成 counterpart；调用 common 参数名仍须解析到 common。 */
    @Test
    fun parameterNameMismatchKeepsCommonCallableCallerFirst(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        assertCallerFirst(
            mainFile,
            mainModule,
            testServices,
            calleeName = Name.identifier("choose"),
            expectedCommonDiagnostics = listOf("CFIR_NOT_MATCHED"),
            expectSpecificBinding = false,
            expectCommonCounterpart = false,
        )

        val callerFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val caller = callerFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val calls = mutableListOf<CfirFunctionCall>()
        requireNotNull(caller.body).accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                calls += functionCall
                functionCall.acceptChildren(this)
            }
        })

        val commonModule = testServices.cjTestModuleStructure.mainModules.single { module ->
            module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single() == CfirCjmpMode.COMMON
        }
        val commonFile = commonModule.cjFiles.single { it.name == "common.cj" }
        val common = commonFile.getOrBuildCfirFile(commonFile.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single()
        val resolved = calls.single().calleeReference as CfirResolvedNamedReference
        assertSame(common.symbol, resolved.resolvedSymbol, "the named call must retain the common declaration")

        val specificFile = mainModule.cjFiles.single { it.name == "callee.cj" }
        val specificDiagnostics = specificFile.collectDiagnosticsForFile(
            mainFile.getResolutionFacadeForTest(),
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS,
        )
        assertTrue(
            specificDiagnostics.any { it.factoryName == "CFIR_NOT_MATCHED" },
            "the specific declaration must remain unmatched: ${specificDiagnostics.map { it.factoryName }}",
        )
    }

    /** 调用者省略返回类型，首次 IMPLICIT_TYPES 消费默认参数时也必须按需配对。 */
    @Test
    fun defaultArgumentInferredCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertCallerFirst(mainFile, mainModule, testServices)
    }

    /** 失败候选已经转移的默认参数前缀必须进入最终 specific 调用的参数映射。 */
    @Test
    fun transferredDefaultPrefixCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertCallerFirst(
            mainFile,
            mainModule,
            testServices,
            calleeName = Name.identifier("choose"),
            expectTransferredDefaultPrefix = true,
        )
    }

    /** 调用者省略返回类型，函数体推断与普通 BODY_RESOLVE 使用相同的 specific 遮蔽语义。 */
    @Test
    fun shadowedCommonInferredCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertCallerFirst(mainFile, mainModule, testServices)
    }

    /** specific 返回类型初始为 Quest；调用方先触发匹配，callee 诊断随后执行推断后的协变复核。 */
    @Test
    fun inferredSpecificReturnPostCheckCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        prepareCjmpParsingContexts(testServices)
        val calleePsi = mainModule.cjFiles.single { it.name == "callee.cj" }
        val rawCalleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val rawCallee = rawCalleeFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "inferred" }
        assertTrue(rawCallee.returnTypeRef is CfirImplicitTypeRef, "the specific signature must begin in the unresolved Quest state")
        assertTrue(rawCallee.body is CfirLazyBlock, "preparing the LL test must not resolve the specific function body")
        assertTrue(rawCallee.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertNull(rawCallee.cjmpMatchFacts)

        assertCallerFirst(mainFile, mainModule, testServices, calleeName = Name.identifier("inferred"))

        val calleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val callee = calleeFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "inferred" }
        assertTrue(callee.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(callee.returnTypeRef !is CfirImplicitTypeRef, "the caller must force inference after the CJMP match")

        val facade = mainFile.getResolutionFacadeForTest()
        val diagnostics = calleePsi.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(listOf("CFIR_RETURN_TYPE_INCOMPATIBLE"), diagnostics.map { it.factoryName }.sorted())

        val family = calleeFile.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(callee)
        assertTrue(family.hasResolutionResult(callee), "post-checking must preserve the declaration's published match result")
        assertNotNull(family.commonFor(callee), "an incompatible inferred return type must not erase the first-fit pair")
    }

    /** 显式不兼容返回类型必须在 caller-first 预筛选中继续保留 common 候选。 */
    @Test
    fun explicitSpecificReturnMismatchKeepsCommonCallerFirst(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        prepareCjmpParsingContexts(testServices)
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val mode = module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            val sourceFile = module.cjFiles.first()
            val rawFile = sourceFile.getOrBuildCfirFile(sourceFile.getResolutionFacadeForTest())
            assertEquals(mode, rawFile.moduleData.session.cjmpSettings.mode, "${module.name}: session 必须消费模块配置的 CJMP 模式")
        }

        val calleePsi = mainModule.cjFiles.single { it.name == "callee.cj" }
        val specificFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val specific = specificFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "explicit" }
        val commonModule = testServices.cjTestModuleStructure.mainModules.single { it.name == "common" }
        val commonPsi = commonModule.cjFiles.single { it.name == "common.cj" }
        val commonFile = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
        val common = commonFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "explicit" }
        specific.lazyResolveToPhase(CfirResolvePhase.TYPES)
        common.lazyResolveToPhase(CfirResolvePhase.TYPES)
        assertTrue(specific.returnTypeRef.coneTypeOrNull != null)
        assertTrue(specific.resolvePhase < CfirResolvePhase.CJMP_MATCHING)

        val facade = mainFile.getResolutionFacadeForTest()
        val callerFile = mainFile.getOrBuildCfirFile(facade)
        val callerDiagnostics = mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertTrue(callerDiagnostics.all { it.psiElement.containingFile === mainFile })
        val caller = callerFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val calls = mutableListOf<CfirFunctionCall>()
        requireNotNull(caller.body).accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                calls += functionCall
                functionCall.acceptChildren(this)
            }
        })
        val reference = calls.single().calleeReference as? CfirResolvedNamedReference
        assertTrue(
            (reference?.resolvedSymbol?.cfir as? CfirNamedFunction)?.status?.isCommon == true,
            "an explicit return type mismatch must leave the common callable visible to the caller",
        )

        val commonDiagnostics = testServices.cjTestModuleStructure.mainModules
            .filter { it.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single() == CfirCjmpMode.COMMON }
            .flatMap { module ->
                module.cjFiles.flatMap { file ->
                    file.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
                }
            }
        assertEquals(listOf("CFIR_NOT_MATCHED"), commonDiagnostics.map { it.factoryName }.sorted())
        val family = specificFile.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(specific)
        assertTrue(family.hasResolutionResult(specific))
        assertNull(family.commonFor(specific))
    }

    /** alpha.66 必须等 specific 隐式返回类型稳定后才最终匹配；首次调用不能假定配对。 */
    @Test
    fun inferredSpecificReturnAlpha66CallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertInferredSpecificReturnVersionCallerFirst(
            mainFile,
            mainModule,
            testServices,
            expectedFinalMatch = false,
            expectedDiagnostic = "CFIR_NOT_MATCHED",
            expectedCommonDiagnostics = listOf("CFIR_NOT_MATCHED"),
        )
    }

    /** alpha.68 在配对后复核推断返回类型，并保留首轮发布的匹配关系。 */
    @Test
    fun inferredSpecificReturnAlpha68CallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertInferredSpecificReturnVersionCallerFirst(
            mainFile,
            mainModule,
            testServices,
            expectedFinalMatch = true,
            expectedDiagnostic = "CFIR_RETURN_TYPE_INCOMPATIBLE",
            expectedCommonDiagnostics = emptyList(),
        )
    }

    private fun assertInferredSpecificReturnVersionCallerFirst(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
        expectedFinalMatch: Boolean,
        expectedDiagnostic: String,
        expectedCommonDiagnostics: List<String>,
    ) {
        prepareCjmpParsingContexts(testServices)
        val calleePsi = mainModule.cjFiles.single { it.name == "callee.cj" }
        val rawCalleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val rawCallee = rawCalleeFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "inferred" }
        assertTrue(rawCallee.returnTypeRef is CfirImplicitTypeRef, "the specific signature must begin in the unresolved Quest state")
        assertTrue(rawCallee.body is CfirLazyBlock, "preparing the LL test must not resolve the specific function body")
        assertTrue(rawCallee.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertNull(rawCallee.cjmpMatchFacts)

        assertCallerFirst(
            mainFile,
            mainModule,
            testServices,
            calleeName = Name.identifier("inferred"),
            expectedCallType = ConePrimitiveType.BOOLEAN,
            expectFinalMatch = expectedFinalMatch,
            expectedCommonDiagnostics = expectedCommonDiagnostics,
            expectSpecificBinding = true,
            expectedDeferredMatching = !expectedFinalMatch,
        )

        val facade = mainFile.getResolutionFacadeForTest()
        val diagnostics = calleePsi.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(listOf(expectedDiagnostic), diagnostics.map { it.factoryName }.sorted())

        val calleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val callee = calleeFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "inferred" }
        assertTrue(callee.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES)
        assertTrue(callee.returnTypeRef !is CfirImplicitTypeRef, "callee diagnostics must infer the specific return type")
        if (!expectedFinalMatch) {
            assertTrue(callee.resolvePhase >= CfirResolvePhase.CJMP_FINAL_MATCHING)
        }
        val family = calleeFile.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(callee)
        assertTrue(family.hasResolutionResult(callee), "the family must publish its final compatibility result")
        assertEquals(expectedFinalMatch, family.commonFor(callee) != null)
    }

    /** common 默认值先转移给 specific；alpha.66 的返回类型不兼容要等隐式类型稳定后再定案。 */
    @Test
    fun inferredSpecificReturnDefaultBootstrapCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        prepareCjmpParsingContexts(testServices)
        val calleePsi = mainModule.cjFiles.single { it.name == "callee.cj" }
        val rawCalleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val rawCallee = rawCalleeFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "inferred" }
        assertTrue(rawCallee.returnTypeRef is CfirImplicitTypeRef, "the specific signature must begin in the unresolved Quest state")
        assertTrue(rawCallee.body is CfirLazyBlock, "preparing the LL test must not resolve the specific function body")

        assertCallerFirst(
            mainFile,
            mainModule,
            testServices,
            calleeName = Name.identifier("inferred"),
            expectedCallType = ConePrimitiveType.BOOLEAN,
            expectFinalMatch = false,
            expectedCommonDiagnostics = listOf("CFIR_NOT_MATCHED"),
            expectSpecificBinding = true,
            expectedDeferredMatching = true,
        )

        val facade = mainFile.getResolutionFacadeForTest()
        val diagnostics = calleePsi.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertTrue(
            diagnostics.any { it.factoryName == "CFIR_NOT_MATCHED" },
            "final alpha.66 matching must use the inferred Bool return type; diagnostics=${diagnostics.map { it.factoryName }}, phase=${rawCallee.resolvePhase}, result=${rawCallee.cjmpMatchResult}",
        )
        val calleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val callee = calleeFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "inferred" }
        assertTrue(callee.resolvePhase >= CfirResolvePhase.CJMP_FINAL_MATCHING)
        assertEquals(ConePrimitiveType.BOOLEAN, callee.returnTypeRef.coneTypeOrNull)
        val family = calleeFile.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(callee)
        assertTrue(family.hasResolutionResult(callee))
        assertNull(family.commonFor(callee))
    }

    /** common 侧检查必须覆盖尚未被调用的实现，且不得为此解析其函数体。 */
    @Test
    fun uncalledImplementationCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertCallerFirst(mainFile, mainModule, testServices, calleeName = Name.identifier("used"))
        val calleePsi = mainModule.cjFiles.single { it.name == "uncalled.cj" }
        val calleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val uncalled = calleeFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "unused" }
        val family = calleeFile.moduleData.session.cjmpFamilyAggregate.ensureFamilyComplete(calleeFile.packageDirective.packageFqName)
        assertTrue(family.hasResolutionResult(uncalled), "common diagnostics must complete the uncalled family")
        assertNotNull(family.commonFor(uncalled), "the uncalled implementation must be present in the completed family")
        assertTrue(uncalled.body is CfirLazyBlock, "common diagnostics must not resolve the uncalled implementation body")
    }

    /** 包级 FINAL 快照首次由 caller-first 查询触发时，必须先由 LL 最终匹配阶段稳定未调用实现签名。 */
    @Test
    fun inferredSpecificReturnPackageFamilyAlpha66CallerFirst(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
    ) {
        prepareCjmpParsingContexts(testServices)
        assertEquals("caller.cj", mainFile.name)
        assertEquals("caller.cj", mainModule.cjFiles.first().name)
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val mode = module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            val sourceFile = module.cjFiles.first()
            val rawFile = sourceFile.getOrBuildCfirFile(sourceFile.getResolutionFacadeForTest())
            assertEquals(mode, rawFile.moduleData.session.cjmpSettings.mode, "${module.name}: session 必须消费模块配置的 CJMP 模式")
        }

        val calleePsi = mainModule.cjFiles.single { it.name == "callee.cj" }
        val calleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val callee = calleeFile.declarations.filterIsInstance<CfirNamedFunction>().single { it.name.asString() == "inferred" }
        assertTrue(callee.moduleData.session.requiresDeferredCjmpFinalMatching())
        assertTrue(callee.returnTypeRef is CfirImplicitTypeRef, "the uncalled specific signature must begin unresolved")
        assertTrue(callee.body is CfirLazyBlock, "constructing the fixture must not resolve the uncalled specific body")
        assertTrue(callee.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertNull(callee.cjmpMatchFacts)
        assertNull(callee.cjmpMatchResult)

        // 这是首个语义请求。caller 不调用 inferred；包级 common 检查仍需完成该 specific family。
        val facade = mainFile.getResolutionFacadeForTest()
        val callerDiagnostics = mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertTrue(callerDiagnostics.none { it.factoryName == "CFIR_AMBIGUOUS_FUNCTION_CALL" })
        assertTrue(callerDiagnostics.none { it.factoryName == "CFIR_NOT_MATCHED" })
        val commonDiagnostics = testServices.cjTestModuleStructure.mainModules
            .filter { it.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single() == CfirCjmpMode.COMMON }
            .flatMap { commonModule ->
                commonModule.cjFiles.flatMap { commonFile ->
                    commonFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
                }
            }
        assertEquals(listOf("CFIR_NOT_MATCHED"), commonDiagnostics.map { it.factoryName }.sorted())
        assertTrue(callee.resolvePhase >= CfirResolvePhase.CJMP_FINAL_MATCHING)
        assertNotNull(callee.cjmpMatchResult, "caller diagnostics must publish the specific declaration's final result")
        assertEquals(ConePrimitiveType.BOOLEAN, callee.returnTypeRef.coneTypeOrNull)

        val packageName = calleeFile.packageDirective.packageFqName
        val family = calleeFile.moduleData.session.cjmpFamilyAggregate.ensureFamilyComplete(packageName)
        assertTrue(family.hasResolutionResult(callee))
        assertTrue(family.isUnmatched(callee), "alpha.66 must match only after the inferred Bool signature is stable")
        assertNull(family.commonFor(callee))
        assertTrue(CjmpMismatchKind.FUNCTION_TYPE in family.mismatchKindsFor(callee))

        val calleeDiagnostics = calleePsi.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(listOf("CFIR_NOT_MATCHED"), calleeDiagnostics.map { it.factoryName }.sorted())
        assertTrue(calleeDiagnostics.none { it.factoryName == "CFIR_RETURN_TYPE_INCOMPATIBLE" })
    }

    /**
     * 成员级完备性：未被调用的 specific **成员**同样必须完成配对。
     *
     * 官方 `CollectCJMPDecls`（CheckCJMP.cpp:679-693）对 nominal 体继续 `WALK_CHILDREN`，因此
     * `MatchCJMPDecls` 的 common 循环会对每个成员单独判定；家族集合必须递归到成员，否则
     * 未被调用的成员会被误报 `NOT_MATCHED`（cfir `testCjmpMultipleCommonExtends` 之外的成员级缺口）。
     */
    @Test
    fun uncalledMemberImplementationCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertCallerFirst(mainFile, mainModule, testServices, calleeName = Name.identifier("used"))

        val calleePsi = mainModule.cjFiles.single { it.name == "callee.cj" }
        val calleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val members = calleeFile.declarations.filterIsInstance<CfirExtend>()
            .flatMap { it.declarations }
            .filterIsInstance<CfirNamedFunction>()
        val resolved = members.single { it.name.asString() == "used" }
        val matchingGroup = calleeFile.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(resolved)
        assertTrue(matchingGroup.hasResolutionResult(resolved), "the immutable family result must include the called member")
        assertNotNull(matchingGroup.commonFor(resolved), "the immutable family result must bind the called member")

        val uncalled = members.single { it.name.asString() == "unused" }
        val family = calleeFile.moduleData.session.cjmpFamilyAggregate.ensureFamilyComplete(calleeFile.packageDirective.packageFqName)
        assertTrue(
            family.hasResolutionResult(uncalled),
            "common diagnostics must complete the uncalled member family",
        )
        assertNotNull(family.commonFor(uncalled), "the uncalled member must be present in the completed family")
        assertTrue(uncalled.body is CfirLazyBlock, "common diagnostics must not resolve the uncalled member body")
    }

    private fun assertCallerFirst(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
        calleeName: Name? = null,
        expectTransferredDefaultPrefix: Boolean = false,
        expectedCallType: ConePrimitiveType? = ConePrimitiveType.INT64,
        expectFinalMatch: Boolean = true,
        expectedCallerDiagnostics: List<String> = emptyList(),
        expectedCommonDiagnostics: List<String> = emptyList(),
        expectSpecificBinding: Boolean = true,
        expectCommonCounterpart: Boolean = true,
        expectedDeferredMatching: Boolean? = null,
    ) {
        prepareCjmpParsingContexts(testServices)
        // 在原有 raw 构建请求前检查各模块 session 的实际模式，不推进任何声明相位。
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val mode = module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            val sourceFile = module.cjFiles.first()
            val session = sourceFile.getResolutionFacadeForTest().useSiteCfirSession
            assertEquals(mode, session.cjmpSettings.mode, "${module.name}: session 必须消费模块配置的 CJMP 模式")
        }

        val facade = mainFile.getResolutionFacadeForTest()
        val callerFile = mainFile.getOrBuildCfirFile(facade)
        val calleeFunctions = mainModule.cjFiles.filter { it !== mainFile }.flatMap { calleePsi ->
            // 顶层函数与 nominal/extend 成员都要可见：成员级配对同样必须由 common 诊断按需触发。
            val declarations = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest()).declarations
            declarations.filterIsInstance<CfirNamedFunction>() +
                    declarations.filterIsInstance<CfirExtend>().flatMap { it.declarations.filterIsInstance<CfirNamedFunction>() }
        }
        val callee = calleeFunctions.single { calleeName == null || it.name == calleeName }
        for (function in calleeFunctions) {
            assertTrue(function.resolvePhase < CfirResolvePhase.CJMP_MATCHING, "every callee must start unmatched")
            assertNull(function.cjmpMatchFacts, "test preparation must not publish root facts")
        }
        expectedDeferredMatching?.let { expected ->
            val session = callee.moduleData.session
            assertTrue(
                session.isCjmpSpecificCompilationEnabled(),
                "CJMP must be enabled for caller-first analysis: version=${session.languageVersionSettings.languageVersion}, mode=${session.cjmpSettings.mode}, explicit=${session.cjmpSettings.explicitMode}",
            )
            assertEquals(expected, session.requiresDeferredCjmpFinalMatching())
            assertTrue(callee.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC))
        }

        // 第一个语义操作只请求调用者诊断：不能先解析被调函数、收集被调文件诊断，或全模块遍历。
        val diagnostics = mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertTrue(diagnostics.all { it.psiElement.containingFile === mainFile }) {
            "single-file diagnostics must stay on the caller file: ${diagnostics.map { it.psiElement.containingFile?.name to it.factoryName }}"
        }
        val commonDiagnostics = testServices.cjTestModuleStructure.mainModules
            .filter { it.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single() == CfirCjmpMode.COMMON }
            .flatMap { commonModule ->
                commonModule.cjFiles.flatMap { commonFile ->
                    commonFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
                }
            }
        assertEquals(expectedCommonDiagnostics.sorted(), commonDiagnostics.map { it.factoryName }.sorted())

        val caller = callerFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        val calls = mutableListOf<CfirFunctionCall>()
        requireNotNull(caller.body).accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                calls += functionCall
                functionCall.acceptChildren(this)
            }
        })
        val call = calls.single()
        val reference = call.calleeReference as? CfirResolvedNamedReference
        assertEquals(
            expectedCallerDiagnostics.sorted(),
            diagnostics.map { it.factoryName }.sorted(),
            "CJMP state after caller-first request: phase=${callee.resolvePhase}, counterparts=${callee.cjmpMatchResult?.counterparts?.map { it.commonSymbol }}, plans=${callee.cjmpMatchFacts?.plans}, ownerCommon=${callee.getContainingCjmpDeclaration()?.cjmpMatchFacts?.commonContainers?.map { it.symbol }}, callTarget=${reference?.resolvedSymbol?.cfir}, callType=${call.coneTypeOrNull}",
        )
        when {
            expectSpecificBinding -> assertSame(callee.symbol, reference?.resolvedSymbol, "caller must bind the specific declaration")
            expectedCallerDiagnostics.contains("CFIR_AMBIGUOUS_FUNCTION_CALL") ->
                assertNull(reference, "an ambiguous alpha.66 call must not publish a resolved winner")
            else -> assertTrue(
                (reference?.resolvedSymbol?.cfir as? CfirNamedFunction)?.status?.isCommon == true,
                "the applicable common default must bind the call",
            )
        }
        if (expectedCallType != null) {
            assertEquals(expectedCallType, call.coneTypeOrNull)
            assertEquals(expectedCallType, caller.returnTypeRef.coneTypeOrNull)
        }
        if (expectFinalMatch) {
            assertTrue(callee.resolvePhase >= CfirResolvePhase.CJMP_MATCHING)
            assertNotNull(callee.cjmpMatchFacts, "reading the target's CJMP result must publish its immutable root facts")
            val family = callee.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(callee)
            assertTrue(family.hasResolutionResult(callee), "the immutable family result must include the specific target")
            if (expectCommonCounterpart) {
                assertNotNull(family.commonFor(callee), "the caller-visible match must retain its common counterpart")
            } else {
                assertNull(family.commonFor(callee), "an unmatched specific must not publish a common counterpart")
            }
        } else {
            assertTrue(expectedDeferredMatching == true)
            assertTrue(callee.resolvePhase >= CfirResolvePhase.CJMP_FINAL_MATCHING)
            assertNotNull(callee.cjmpMatchFacts, "the stable alpha.66 signature must publish its declaration facts")
            assertNotNull(callee.cjmpMatchResult, "final matching must publish an unmatched result for the rejected signature")
            val family = callee.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(callee)
            assertTrue(family.hasResolutionResult(callee))
            assertNull(family.commonFor(callee), "alpha.66 must not retain a common counterpart after the inferred return mismatch")
        }
        if (expectTransferredDefaultPrefix) {
            val family = callee.moduleData.session.cjmpFamilyAggregate.matchingGroupFor(callee)
            assertTrue(
                family.defaults.getValue(callee.symbol).first().hasInitial,
                "the ordered family defaults must retain a prefix transferred before an earlier candidate failed",
            )
        }

        // 相同诊断请求再次执行，检查缓存读取也不改变目标；这里不做额外的 callee 预热。
        val repeatedDiagnostics = mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(expectedCallerDiagnostics.sorted(), repeatedDiagnostics.map { it.factoryName }.sorted())
        if (expectSpecificBinding) {
            assertSame(callee.symbol, (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol)
        }
    }

    /**
     * 测试解析器也必须在首次 AST 访问前拿到与编译模块一致的 CJMP 文法门。
     * 早期 alpha 的 `platform` 是版本化修饰符；若先用 SyntaxOnly 构造 PSI，后续 CFIR
     * 无法从 identifier 恢复声明角色。
     */
    private fun prepareCjmpParsingContexts(testServices: TestServices) {
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val mode = module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            val languageVersionSettings = module.testModule.languageVersionSettings ?: LanguageVersionSettingsImpl.DEFAULT
            val parsingContext = CfirCjmpSettingsComponent(explicitMode = mode)
                .toParsingContext(languageVersionSettings)
            module.cjFiles.forEach { it.cjmpParsingContext = parsingContext }
        }
    }
}
