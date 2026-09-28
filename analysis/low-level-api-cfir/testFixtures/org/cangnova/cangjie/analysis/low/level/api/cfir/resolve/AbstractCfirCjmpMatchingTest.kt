@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CaMutableTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpResolver
import org.cangnova.cangjie.cfir.resolve.transformers.CfirCjmpMatcherTransformer
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpCandidateDiagnostic
import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cjmpMappingStorage
import org.cangnova.cangjie.cfir.session.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.test.services.TestServices
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * 验证 LL 的 CJMP_MATCHING 相位在真实 depends-on 模块图中写入 specific session 配对结果。
 *
 * 测试模块结构由 fixture 声明；在 LL 工程模块图上将 common 源模块设为 specific 的
 * `dependsOn` refinement 输入，再从 specific class 入口触发 lazy resolve。
 */
abstract class AbstractCfirCjmpMatchingTest : AbstractAnalysisApiBasedTest() {
    private data class MatchState(
        val common: CfirDeclaration?,
        val mismatchKinds: List<CjmpMismatchKind>,
        val mismatchCandidates: List<CfirDeclaration>,
        val candidateDiagnostics: List<CfirCjmpCandidateDiagnostic>,
        val reverseBindings: List<CfirDeclaration>,
    )

    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByMainFile(mainFile: org.cangnova.cangjie.psi.CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val moduleStructure = testServices.cjTestModuleStructure
        val specificModule = mainModule.caModule as CaMutableTestModule
        val refinementModules = mainModule.testModule.dependsOnDependencies.map { dependency ->
            moduleStructure.getModule(dependency.dependencyModuleName).moduleForDependency(dependency.kind)
        }
        assertEquals(refinementModules, specificModule.directDependsOnDependencies)

        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val cfirFile = mainFile.getOrBuildCfirFile(resolutionFacade)
        val specificClass = cfirFile.declarations.filterIsInstance<CfirClass>().singleOrNull { it.name.asString() == "Box" }
        val specificEnum = cfirFile.declarations.filterIsInstance<CfirEnum>().singleOrNull { it.name.asString() == "Choice" }
        val specificExtend = cfirFile.declarations.filterIsInstance<CfirExtend>().singleOrNull()
        val specificSession = cfirFile.moduleData.session
        val mode = if (mainModule.name == "modeNone") CfirCjmpMode.NONE else CfirCjmpMode.SPECIFIC
        specificSession.register(
            CfirCjmpSettingsComponent::class,
            CfirCjmpSettingsComponent(explicitMode = mode),
        )

        if (mainModule.name == "firstMatch" || mainModule.name == "parallelFirstMatch") {
            val sharedFunctions = cfirFile.declarations.filterIsInstance<CfirNamedFunction>()
                .filter { it.name.asString() == "shared" }
            assertEquals(2, sharedFunctions.size)
            val storage = specificSession.cjmpMappingStorage
            if (mainModule.name == "parallelFirstMatch") {
                resolveTogether(sharedFunctions)
            } else {
                sharedFunctions[1].lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            }
            assertFirstFit(sharedFunctions, storage)
            val llState = matchState(sharedFunctions, storage)

            storage.clear()
            CfirCjmpMatcherTransformer(specificSession, ScopeSession()).transformFile(cfirFile, null)
            assertEquals(
                llState,
                matchState(sharedFunctions, storage),
                "eager and LL matching preserve the same first-fit winner and mismatch evidence",
            )
            return
        }

        if (mainModule.name == "firstFitCandidateDiagnostic") {
            val specificFunction = cfirFile.declarations.filterIsInstance<CfirNamedFunction>()
                .single { it.name.asString() == "select" }
            specificFunction.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)

            val storage = specificSession.cjmpMappingStorage
            val common = storage.commonFor(specificFunction)
            val providerCandidates = specificSession.dependenciesSymbolProvider.getTopLevelCallableSymbols(
                specificFunction.symbol.callableId.packageName,
                specificFunction.symbol.callableId.callableName,
            )
            val commonCandidates = CfirCjmpResolver.findCommonCandidates(specificFunction, specificSession)
            assertEquals(
                refinementModules,
                commonCandidates.map { (it.moduleData as LLCfirModuleData).caModule },
                "the declared depends-on order must flow into the LL common-candidate provider; " +
                        "direct=${specificModule.directDependsOnDependencies}, " +
                        "refinement=${specificSession.moduleData.allRefinementDependencies.map { it.name.asString() }}, " +
                        "dependencies=${specificSession.moduleData.dependencies.map { it.name.asString() }}, " +
                        "provider=$providerCandidates",
            )
            assertNotNull(
                common,
                "the later compatible common candidate must bind; modules=${moduleStructure.mainModules.map { it.name }}, " +
                        "dependencies=${mainModule.testModule.dependsOnDependencies.map { it.dependencyModuleName }}, " +
                        "refinement=${specificSession.moduleData.allRefinementDependencies.map { it.name.asString() }}, " +
                        "provider=$providerCandidates, " +
                        "candidates=${CfirCjmpResolver.findCommonCandidates(specificFunction, specificSession)}, " +
                        "mismatches=${storage.mismatchKindsFor(specificFunction)}",
            )
            assertEquals(refinementModules[1], (requireNotNull(common).moduleData as LLCfirModuleData).caModule)
            assertTrue(storage.parameterMismatchesFor(specificFunction).isEmpty())
            assertTrue(
                storage.candidateDiagnosticsFor(specificFunction).any { diagnostic ->
                    diagnostic is CfirCjmpCandidateDiagnostic.ParameterMismatch &&
                            diagnostic.mismatch.kind == CjmpMismatchKind.PARAMETER_NAMES
                },
                "the LL first-fit run retains the first candidate's diagnostic after binding the second candidate",
            )
            val llState = matchState(listOf(specificFunction), storage)

            storage.clear()
            CfirCjmpMatcherTransformer(specificSession, ScopeSession()).transformFile(cfirFile, null)
            assertEquals(
                llState,
                matchState(listOf(specificFunction), storage),
                "eager and LL preserve the same candidate diagnostic and later first-fit winner",
            )
            return
        }

        if (mainModule.name == "memberFirstMatch") {
            val members = requireNotNull(specificClass).declarations.filterIsInstance<CfirNamedFunction>()
                .filter { it.name.asString() == "shared" }
            assertEquals(2, members.size)
            members[1].lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)

            val storage = specificSession.cjmpMappingStorage
            val common = storage.commonFor(members[0])
            assertNotNull(common, "first member did not bind; mismatches=${storage.mismatchKindsFor(members[0])}")
            assertEquals(null, storage.commonFor(members[1]))
            assertEquals(listOf(members[0], members[1]), storage.specificBindingsFor(requireNotNull(common)))
            assertTrue(CjmpMismatchKind.SECOND_BINDING in storage.mismatchKindsFor(members[1]))
            return
        }

        val requiredSpecificClass = requireNotNull(specificClass) { "this LL fixture must contain Box" }
        val specificIdentity = requiredSpecificClass.declarations.filterIsInstance<CfirNamedFunction>().single()
        requiredSpecificClass.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        specificIdentity.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        val requiredSpecificExtend = requireNotNull(specificExtend)
        requiredSpecificExtend.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        val specificExtendFunction = requiredSpecificExtend.declarations.filterIsInstance<CfirNamedFunction>().single()
        specificExtendFunction.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        val indexedSpecificExtend = specificSession.extendProvider.getContainingExtend(specificExtendFunction.symbol)
        val specificEnumConstructor = specificEnum?.declarations?.filterIsInstance<CfirEnumConstructor>()?.single()
        if (specificEnum != null && specificEnumConstructor != null) {
            specificEnum.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            specificEnumConstructor.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        }

        val storage = specificSession.cjmpMappingStorage
        if (mode == CfirCjmpMode.NONE) {
            assertTrue(requiredSpecificClass.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            assertTrue(requiredSpecificExtend.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            assertTrue(storage.isEmpty)
            assertEquals(null, storage.commonFor(requiredSpecificClass))
            assertEquals(null, storage.commonFor(requiredSpecificExtend))
            return
        }

        assertEquals(CfirResolvePhase.CJMP_MATCHING, requiredSpecificClass.resolvePhase)
        assertNotNull(storage.commonFor(requiredSpecificClass) as? CfirClass)
        assertNotNull(
            storage.commonFor(specificIdentity) as? CfirNamedFunction,
            "specific member not bound; class=${storage.commonFor(requiredSpecificClass)}, " +
                    "member mismatches=${storage.mismatchKindsFor(specificIdentity)}, " +
                    "candidates=${storage.mismatchCandidatesFor(specificIdentity)}, " +
                    "typeMap=${storage.typeParameterMappingFor(requiredSpecificClass)}",
        )
        assertEquals(CfirResolvePhase.CJMP_MATCHING, requiredSpecificExtend.resolvePhase)
        assertNotNull(storage.commonFor(requiredSpecificExtend) as? CfirExtend)
        assertEquals(2, storage.commonCounterpartsFor(requiredSpecificExtend).size)
        assertNotNull(
            storage.commonFor(specificExtendFunction) as? CfirNamedFunction,
            "extend member not bound; ownerSame=${indexedSpecificExtend === requiredSpecificExtend}, " +
                    "owner=$indexedSpecificExtend, " +
                    "ownerPair=${indexedSpecificExtend?.let(storage::commonFor)}, " +
                    "extendPair=${storage.commonFor(requiredSpecificExtend)}, " +
                    "parentTypeMap=${storage.typeParameterMappingFor(requiredSpecificExtend)}, " +
                    "memberPhase=${specificExtendFunction.resolvePhase}, " +
                    "memberReturnType=${specificExtendFunction.returnTypeRef}, " +
                    "memberMismatches=${storage.mismatchKindsFor(specificExtendFunction)}, " +
                    "memberCandidates=${storage.mismatchCandidatesFor(specificExtendFunction)}, " +
                    "unmatched=${storage.isUnmatched(specificExtendFunction)}",
        )
        assertEquals(1, storage.typeParameterMappingFor(specificIdentity).size)
        assertEquals(2, storage.typeParameterMappingFor(specificExtendFunction).size)
        assertTrue(storage.hasResolutionResult(specificIdentity))
        if (specificEnum != null && specificEnumConstructor != null) {
            assertNotNull(storage.commonFor(specificEnum) as? CfirEnum)
            assertNotNull(storage.commonFor(specificEnumConstructor) as? CfirEnumConstructor)
            assertTrue(storage.hasResolutionResult(specificEnumConstructor))
        }
    }

    private fun assertFirstFit(
        specificFunctions: List<CfirNamedFunction>,
        storage: CfirCjmpMappingStorage,
    ) {
        val first = specificFunctions[0]
        val second = specificFunctions[1]
        val common = storage.commonFor(first)
        assertNotNull(
            common,
            "first-fit declaration was not bound; mismatches=${storage.mismatchKindsFor(first)}, " +
                    "candidates=${storage.mismatchCandidatesFor(first)}, second=${storage.mismatchKindsFor(second)}",
        )
        assertEquals(null, storage.commonFor(second))
        assertEquals(listOf(first, second), storage.specificBindingsFor(requireNotNull(common)))
        assertTrue(CjmpMismatchKind.SECOND_BINDING in storage.mismatchKindsFor(second))
    }

    /** Two simultaneous LL target requests must observe the same ordered declaration group. */
    private fun resolveTogether(specificFunctions: List<CfirNamedFunction>) {
        val ready = CountDownLatch(specificFunctions.size)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(specificFunctions.size)
        try {
            val futures = specificFunctions.map { function ->
                executor.submit {
                    ready.countDown()
                    check(start.await(30, TimeUnit.SECONDS)) { "parallel LL start barrier timed out" }
                    ApplicationManager.getApplication().runReadAction {
                        function.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
                    }
                }
            }
            assertTrue(ready.await(30, TimeUnit.SECONDS), "parallel LL workers did not reach the start barrier")
            start.countDown()
            futures.forEach { it.get(60, TimeUnit.SECONDS) }
        } finally {
            start.countDown()
            executor.shutdownNow()
            check(executor.awaitTermination(30, TimeUnit.SECONDS)) { "parallel LL workers did not terminate" }
        }
    }

    private fun matchState(
        specificFunctions: List<CfirNamedFunction>,
        storage: CfirCjmpMappingStorage,
    ): List<MatchState> = specificFunctions.map { specific ->
        val common = storage.commonFor(specific)
        MatchState(
            common = common,
            mismatchKinds = storage.mismatchKindsFor(specific),
            mismatchCandidates = storage.mismatchCandidatesFor(specific),
            candidateDiagnostics = storage.candidateDiagnosticsFor(specific),
            reverseBindings = common?.let(storage::specificBindingsFor).orEmpty(),
        )
    }
}
