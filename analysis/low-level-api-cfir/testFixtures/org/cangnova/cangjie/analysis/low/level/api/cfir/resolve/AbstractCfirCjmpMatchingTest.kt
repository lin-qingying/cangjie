@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
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
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirCjmpDeclarationMatchFacts
import org.cangnova.cangjie.cfir.declarations.CfirCjmpDeclarationMatchResult
import org.cangnova.cangjie.cfir.declarations.cjmpMatchFacts
import org.cangnova.cangjie.cfir.declarations.cjmpMatchResult
import org.cangnova.cangjie.cfir.declarations.cjmpMatchResultForSpecific
import org.cangnova.cangjie.cfir.declarations.publishCjmpMatchResult
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpResolver
import org.cangnova.cangjie.cfir.resolve.transformers.CfirCjmpMatcherTransformer
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpCandidateDiagnostic
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirPhaseUpdater
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirGlobalResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.withCjmpDeclarationState
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCandidateTraversal
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchEvaluator
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchPlan
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchResult
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
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

        // 模式由模块工厂持有；验证 source/dependency session 都消费同一声明角色，不推进目标相位。
        moduleStructure.mainModules.forEach { module ->
            val mode = module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
            val facade = module.caModule.getResolutionFacade(module.caModule.project)
            val sourceSession = facade.sessionProvider.getResolvableSession(module.caModule)
            val dependencySession = LLCfirSessionCache.getInstance(module.caModule.project).getDependencySession(module.caModule)
            listOfNotNull(sourceSession, dependencySession).distinct().forEach { session ->
                assertEquals(mode, session.cjmpSettings.mode, "${module.name}: session 必须消费模块配置的 CJMP 模式")
            }
        }

        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val mode = mainModule.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single()
        val cfirFile = mainFile.getOrBuildCfirFile(resolutionFacade)
        val specificClass = cfirFile.declarations.filterIsInstance<CfirClass>().singleOrNull { it.name.asString() == "Box" }
        val specificEnum = cfirFile.declarations.filterIsInstance<CfirEnum>().singleOrNull { it.name.asString() == "Choice" }
        val specificExtend = cfirFile.declarations.filterIsInstance<CfirExtend>().singleOrNull()
        val specificSession = cfirFile.moduleData.session

        mainModule.testModule.directives[CfirDiagnosticsDirectives.CJMP_EXPECTED_COUNTERPART_COUNT]
            .singleOrNull()?.toInt()?.let { expectedCounterpartCount ->
                val ordinaryFunction = cfirFile.declarations.filterIsInstance<CfirNamedFunction>()
                    .single { it.symbol.name.asString() == "ordinary" }
                val ordinaryFunctionPhase = ordinaryFunction.resolvePhase
                assertTrue(ordinaryFunctionPhase < CfirResolvePhase.CJMP_MATCHING)
                assertNull(ordinaryFunction.symbol.cjmpMatchResultForSpecific())
                assertEquals(
                    ordinaryFunctionPhase,
                    ordinaryFunction.resolvePhase,
                    "reading CJMP results must not advance a declaration without the specific role",
                )

                val specificFunction = cfirFile.declarations.filterIsInstance<CfirNamedFunction>()
                    .single { it.status.isSpecific }
                assertTrue(
                    specificFunction.resolvePhase < CfirResolvePhase.CJMP_MATCHING,
                    "the fixture must begin before the target's CJMP_MATCHING request",
                )
                val unrelatedSpecificClass = requireNotNull(specificClass)
                assertTrue(unrelatedSpecificClass.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
                assertNull(unrelatedSpecificClass.cjmpMatchFacts)
                val accessorResult = specificFunction.symbol.cjmpMatchResultForSpecific()
                val accessorCounterparts = accessorResult?.commonSymbols.orEmpty()
                assertEquals(
                    CfirResolvePhase.CJMP_MATCHING,
                    specificFunction.resolvePhase,
                    "reading a declaration's counterparts must lazily resolve its CJMP phase root",
                )
                assertTrue(
                    unrelatedSpecificClass.resolvePhase < CfirResolvePhase.CJMP_MATCHING,
                    "reading one declaration's counterparts must not advance an unrelated declaration",
                )
                assertNull(unrelatedSpecificClass.cjmpMatchFacts)
                val facts = requireNotNull(specificFunction.cjmpMatchFacts)
                val plan = facts.plans.single() as CjmpMatchPlan.Declaration<*, *, *>
                assertEquals(CjmpCandidateTraversal.ALL_CANDIDATES, plan.traversal)

                val family = specificSession.cjmpFamilyAggregate.matchingGroupFor(specificFunction)
                val commonCounterparts = family.commonCounterpartsFor(specificFunction)
                assertEquals(expectedCounterpartCount, commonCounterparts.size)
                assertEquals(
                    commonCounterparts.map { it.symbol },
                    accessorCounterparts,
                    "the lazy declaration accessor must preserve the family's ordered common-parent results",
                )
                assertEquals(
                    refinementModules,
                    commonCounterparts.map { (it.moduleData as LLCfirModuleData).caModule },
                )
                val parentMappings = family.typeParameterMappingsFor(specificFunction)
                assertEquals(commonCounterparts, parentMappings.map { it.first })
                assertEquals(
                    List(expectedCounterpartCount) { listOf("T" to "U") },
                    parentMappings.map { (_, mapping) ->
                        mapping.entries.map { (common, specific) -> common.name.asString() to specific.name.asString() }
                    },
                )
                assertEquals(
                    parentMappings.map { (_, mapping) -> mapping },
                    accessorResult?.counterparts?.map { it.typeParameterMapping },
                    "the declaration accessor must expose each counterpart's type-parameter mapping",
                )

                val requiredSpecificClass = requireNotNull(specificClass)
                val classResult = requiredSpecificClass.symbol.cjmpMatchResultForSpecific()
                val classFamily = specificSession.cjmpFamilyAggregate.matchingGroupFor(requiredSpecificClass)
                val classCounterparts = classFamily.commonCounterpartsFor(requiredSpecificClass)
                assertEquals(expectedCounterpartCount, classCounterparts.size)
                assertEquals(
                    refinementModules,
                    classCounterparts.map { (it.moduleData as LLCfirModuleData).caModule },
                )
                assertEquals(
                    List(expectedCounterpartCount) { listOf("T" to "U") },
                    classFamily.typeParameterMappingsFor(requiredSpecificClass).map { (_, mapping) ->
                        mapping.entries.map { (common, specific) -> common.name.asString() to specific.name.asString() }
                    },
                )
                assertEquals(
                    classFamily.typeParameterMappingsFor(requiredSpecificClass).map { it.second },
                    classResult?.counterparts?.map { it.typeParameterMapping },
                )

                val specificIdentity = requiredSpecificClass.declarations.filterIsInstance<CfirNamedFunction>()
                    .single { it.status.isSpecific }
                val memberResult = specificIdentity.symbol.cjmpMatchResultForSpecific()
                val memberFamily = specificSession.cjmpFamilyAggregate.matchingGroupFor(specificIdentity)
                assertEquals(expectedCounterpartCount, memberFamily.commonCounterpartsFor(specificIdentity).size)
                assertEquals(
                    memberFamily.typeParameterMappingsFor(specificIdentity).map { it.second },
                    memberResult?.counterparts?.map { it.typeParameterMapping },
                )
                assertEquals(
                    List(expectedCounterpartCount) { listOf("T" to "U") },
                    memberFamily.typeParameterMappingsFor(specificIdentity).map { (_, mapping) ->
                        mapping.entries.map { (common, specific) -> common.name.asString() to specific.name.asString() }
                    },
                )
            }

        if (mainModule.name == "multipleCommonParentsSharedAncestor") {
            val requiredSpecificClass = requireNotNull(specificClass)
            requiredSpecificClass.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            val family = specificSession.cjmpFamilyAggregate.matchingGroupFor(requiredSpecificClass)
            val commonClass = family.commonCounterpartsFor(requiredSpecificClass).single() as CfirClass
            assertEquals("Sources of commonBase", (commonClass.moduleData as LLCfirModuleData).caModule.moduleDescription)
            assertEquals(
                listOf(listOf("T" to "U")),
                family.typeParameterMappingsFor(requiredSpecificClass).map { (_, mapping) ->
                    mapping.entries.map { (common, specific) -> common.name.asString() to specific.name.asString() }
                },
            )
            return
        }

        if (mainModule.name == "firstMatch" || mainModule.name == "parallelFirstMatch") {
            val sharedFunctions = cfirFile.declarations.filterIsInstance<CfirNamedFunction>()
                .filter { it.name.asString() == "shared" }
            assertEquals(2, sharedFunctions.size)
            if (mainModule.name == "parallelFirstMatch") {
                resolveTogether(sharedFunctions)
            } else {
                sharedFunctions[1].lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
                assertTrue(sharedFunctions[1].resolvePhase >= CfirResolvePhase.CJMP_MATCHING)
                assertTrue(sharedFunctions[0].resolvePhase < CfirResolvePhase.CJMP_MATCHING)
                assertNotNull(sharedFunctions[1].cjmpMatchFacts)
                assertNull(sharedFunctions[0].cjmpMatchFacts, "a target resolve must not publish its sibling's facts")
                val family = specificSession.cjmpFamilyAggregate.matchingGroupFor(sharedFunctions[1])
                val occupiedCommon = family.candidateDiagnosticsFor(sharedFunctions[1])
                    .filterIsInstance<CfirCjmpCandidateDiagnostic.Occupied>()
                    .single().commonCandidate
                assertEquals(listOf(sharedFunctions[0]), family.specificBindingsFor(occupiedCommon))
                assertNull(sharedFunctions[0].cjmpMatchFacts, "family evaluation must not publish a sibling root")
                sharedFunctions[0].lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            }
            val family = specificSession.cjmpFamilyAggregate.matchingGroupFor(sharedFunctions[1])
            assertFirstFit(sharedFunctions, family)
            val llState = matchState(sharedFunctions, family)

            CfirCjmpMatcherTransformer(specificSession, ScopeSession()).transformFile(cfirFile, null)
            assertTrue(sharedFunctions.all { it.cjmpMatchFacts != null })
            assertEquals(
                llState,
                matchState(sharedFunctions, family),
                "eager declaration publication must preserve the immutable family's first-fit result",
            )
            assertEagerPlanDumpMatchesFamily(sharedFunctions, family)
            return
        }

        if (mainModule.name == "firstFitCandidateDiagnostic") {
            val specificFunction = cfirFile.declarations.filterIsInstance<CfirNamedFunction>()
                .single { it.name.asString() == "select" }
            specificFunction.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)

            val family = specificSession.cjmpFamilyAggregate.matchingGroupFor(specificFunction)
            val common = family.commonFor(specificFunction)
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
                        "mismatches=${family.mismatchKindsFor(specificFunction)}",
            )
            assertEquals(refinementModules[1], (requireNotNull(common).moduleData as LLCfirModuleData).caModule)
            assertTrue(
                family.candidateDiagnosticsFor(specificFunction).any { diagnostic ->
                    diagnostic is CfirCjmpCandidateDiagnostic.ParameterMismatch &&
                            diagnostic.mismatch.kind == CjmpMismatchKind.PARAMETER_NAMES
                },
                "the LL first-fit run retains the first candidate's diagnostic after binding the second candidate",
            )
            val llState = matchState(listOf(specificFunction), family)

            CfirCjmpMatcherTransformer(specificSession, ScopeSession()).transformFile(cfirFile, null)
            assertEquals(
                llState,
                matchState(listOf(specificFunction), family),
                "eager declaration publication preserves the immutable candidate diagnostic and first-fit winner",
            )
            return
        }

        if (mainModule.name == "memberFirstMatch") {
            val members = requireNotNull(specificClass).declarations.filterIsInstance<CfirNamedFunction>()
                .filter { it.name.asString() == "shared" }
            assertEquals(3, members.size)
            members[2].lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)

            val family = specificSession.cjmpFamilyAggregate.matchingGroupFor(members[2])
            assertTrue(members[2].resolvePhase >= CfirResolvePhase.CJMP_MATCHING)
            assertTrue(members[0].resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            assertTrue(members[1].resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            assertNotNull(members[2].cjmpMatchFacts)
            assertNull(members[0].cjmpMatchFacts)
            assertNull(members[1].cjmpMatchFacts)
            val commonClass = family.commonFor(requireNotNull(specificClass)) as CfirClass
            val commonMembers = commonClass.declarations.filterIsInstance<CfirNamedFunction>()
                .filter { it.name.asString() == "shared" }
            assertEquals(2, commonMembers.size)
            assertEquals(listOf(members[0]), family.specificBindingsFor(commonMembers[0]))

            members[1].lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            assertTrue(members[0].resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            assertNotNull(members[1].cjmpMatchFacts)
            assertNull(members[0].cjmpMatchFacts)
            members[0].lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)

            assertSame(commonMembers[0], family.commonFor(members[0]))
            assertSame(commonMembers[1], family.commonFor(members[1]))
            assertEquals(null, family.commonFor(members[2]))
            assertTrue(family.mismatchKindsFor(members[1]).isEmpty())
            assertTrue(family.mismatchCandidatesFor(members[1]).isEmpty())
            assertTrue(
                family.candidateDiagnosticsFor(members[1]).isEmpty(),
                "a successful overload must not retain candidate failures from another overload",
            )
            assertEquals(listOf(members[0]), family.specificBindingsFor(commonMembers[0]))
            assertEquals(listOf(members[1]), family.specificBindingsFor(commonMembers[1]))
            assertTrue(CjmpMismatchKind.SECOND_BINDING in family.mismatchKindsFor(members[2]))
            assertTrue(family.candidateDiagnosticsFor(members[2]).any {
                it == CfirCjmpCandidateDiagnostic.Occupied(commonMembers[0], members[0])
            })

            val llState = matchState(members, family)
            CfirCjmpMatcherTransformer(specificSession, ScopeSession()).transformFile(cfirFile, null)
            assertEquals(llState, matchState(members, family), "eager declaration publication preserves ordered overload matching")
            assertEagerPlanDumpMatchesFamily(members, family)
            return
        }

        if (mainModule.name == "propertyMemberMatches") {
            val properties = requireNotNull(specificClass).declarations.filterIsInstance<CfirProperty>()
            assertEquals(1, properties.size)
            val specificProperty = properties.single()
            val specificGetter = requireNotNull(specificProperty.getter) { "fixture must declare a getter" }
            assertTrue(specificProperty.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            val failedPublication = IllegalStateException("injected owned-accessor publication failure")
            assertSame(
                failedPublication,
                assertThrows(IllegalStateException::class.java) {
                    withCjmpDeclarationState(specificProperty) {
                        specificGetter.publishCjmpMatchResult(
                            CfirCjmpDeclarationMatchResult(specificGetter.symbol, emptyList()),
                        )
                        throw failedPublication
                    }
                },
            )
            assertNull(specificGetter.cjmpMatchResult, "failed root publication must restore owned accessor results")
            val getterResult = specificGetter.symbol.cjmpMatchResultForSpecific()
            val getterCounterparts = getterResult?.counterparts?.map { it.commonSymbol }.orEmpty()
            assertEquals(
                CfirResolvePhase.CJMP_MATCHING,
                specificProperty.resolvePhase,
                "reading an accessor's result must resolve its owning property phase root",
            )
            specificProperty.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)

            val family = specificSession.cjmpFamilyAggregate.matchingGroupFor(specificProperty)
            val common = family.commonFor(specificProperty)
            assertNotNull(
                common,
                "specific property did not bind; mismatches=${family.mismatchKindsFor(specificProperty)}",
            )
            val commonProperty = common as CfirProperty

            // 官方 MatchCJMPProp（v1.1.3 CheckCJMP.cpp:1001-1010）对本体与访问器**分别**
            // TrySetSpecificImpl：getter 必须有自己独立的配对，而不是只有本体被绑定。
            val commonGetter = requireNotNull(commonProperty.getter) { "common fixture must declare a getter" }
            assertSame(
                commonGetter,
                family.commonFor(specificGetter),
                "the getter must be paired as an independent declaration",
            )
            assertEquals(listOf(commonGetter.symbol), getterCounterparts)
            assertEquals(
                listOf(commonGetter.symbol),
                getterResult?.counterparts?.map { it.commonSymbol },
                "the declaration-owned accessor result must retain the getter mapping record",
            )
            return
        }

        if (mainModule.name == "noCommonCandidates") {
            val requiredSpecificClass = requireNotNull(specificClass)
            requiredSpecificClass.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            val classFacts = requireNotNull(requiredSpecificClass.cjmpMatchFacts)
            assertEquals(
                false,
                CjmpMatchEvaluator.evaluate(classFacts.plans).matched[requiredSpecificClass.symbol],
                "a class with no common candidate must publish an explicit unmatched root fact",
            )

            val specificFunction = requiredSpecificClass.declarations.filterIsInstance<CfirNamedFunction>().single()
            specificFunction.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            val functionFacts = requireNotNull(specificFunction.cjmpMatchFacts)
            assertEquals(
                false,
                CjmpMatchEvaluator.evaluate(functionFacts.plans).matched[specificFunction.symbol],
                "a member whose owner has no common counterpart must publish its empty candidate result",
            )
            return
        }

        if (mainModule.name == "rootFactsFailureRetry") {
            val function = cfirFile.declarations.filterIsInstance<CfirNamedFunction>().single()
            function.lazyResolveToPhase(CfirResolvePhase.EXTENSIONS)
            val initialFunctionPhase = function.resolvePhase
            val initialParameterPhases = function.valueParameters.map { it.resolvePhase }
            val facts = CfirCjmpDeclarationMatchFacts(
                rootSymbol = function.symbol,
                ownedSymbols = listOf(function.symbol),
                plans = emptyList(),
                container = null,
                commonContainers = emptyList(),
            )
            val failure = IllegalStateException("injected failure after CJMP facts and signature phase update")
            val lockProvider = LLCfirGlobalResolveComponents.getInstance(specificSession).lockProvider
            assertSame(
                failure,
                assertThrows(IllegalStateException::class.java) {
                    lockProvider.withJumpingLock(
                        target = function,
                        phase = CfirResolvePhase.CJMP_MATCHING,
                        actionUnderLock = {
                            withCjmpDeclarationState(function) {
                                function.cjmpMatchFacts = facts
                                function.publishCjmpMatchResult(
                                    CfirCjmpDeclarationMatchResult(
                                        specificSymbol = function.symbol,
                                        counterparts = emptyList(),
                                    ),
                                )
                                LLCfirPhaseUpdater.updateDeclarationContent(function, CfirResolvePhase.CJMP_MATCHING)
                                throw failure
                            }
                        },
                        actionOnCycle = { error("the standalone publication failure must not be a resolve cycle") },
                    )
                },
            )
            assertNull(function.cjmpMatchFacts, "a failed publication transaction must restore the root facts")
            assertNull(function.cjmpMatchResult, "a failed publication transaction must restore the declaration result")
            assertEquals(initialFunctionPhase, function.resolvePhase, "the transaction must not advance the root phase")
            assertEquals(initialParameterPhases, function.valueParameters.map { it.resolvePhase })

            function.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            assertNotNull(function.cjmpMatchFacts, "a failed root transaction must permit a real retry")
            val matchResult = requireNotNull(function.cjmpMatchResult) {
                "a successful retry must publish the declaration-owned match result"
            }
            assertSame(function.symbol, matchResult.specificSymbol)
            assertEquals(
                specificSession.cjmpFamilyAggregate.matchingGroupFor(function)
                    .commonCounterpartsFor(function).map { it.symbol },
                matchResult.commonSymbols,
            )
            assertEquals(CfirResolvePhase.CJMP_MATCHING, function.resolvePhase)
            assertTrue(function.valueParameters.all { it.resolvePhase == CfirResolvePhase.CJMP_MATCHING })
            return
        }

        if (mainModule.name == "specificMemberMatchesCommon") {
            val requiredSpecificExtend = requireNotNull(specificExtend)
            val commonModule = moduleStructure.mainModules.single { it.name == "common" }
            val commonPsi = commonModule.cjFiles.single()
            val commonFile = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            val unrelatedCommonExtend = commonFile.declarations.filterIsInstance<CfirExtend>().single { extend ->
                extend.declarations.filterIsInstance<CfirNamedFunction>().any { it.name.asString() == "unrelated" }
            }
            requiredSpecificExtend.lazyResolveToPhase(CfirResolvePhase.EXTENSIONS)
            requiredSpecificExtend.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            assertTrue(
                unrelatedCommonExtend.resolvePhase < CfirResolvePhase.TYPES,
                "matching one extend must not advance common extends with a different structural target",
            )
        }

        val requiredSpecificClass = requireNotNull(specificClass) { "this LL fixture must contain Box" }
        val specificIdentity = requiredSpecificClass.declarations.filterIsInstance<CfirNamedFunction>().single()
        fun assertUnrelatedCommonExtendNotTypes(stage: String) {
            if (mainModule.name != "specificMemberMatchesCommon") return
            val commonModule = moduleStructure.mainModules.single { it.name == "common" }
            val commonPsi = commonModule.cjFiles.single()
            val commonFile = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            val unrelatedCommonExtend = commonFile.declarations.filterIsInstance<CfirExtend>().single { extend ->
                extend.declarations.filterIsInstance<CfirNamedFunction>().any { it.name.asString() == "unrelated" }
            }
            assertTrue(
                unrelatedCommonExtend.resolvePhase < CfirResolvePhase.TYPES,
                "$stage must not advance a common extend with a different structural target",
            )
        }
        requiredSpecificClass.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertUnrelatedCommonExtendNotTypes("matching the Box class")
        specificIdentity.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertUnrelatedCommonExtendNotTypes("matching the Box identity member")
        val requiredSpecificExtend = requireNotNull(specificExtend)
        requiredSpecificExtend.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        assertUnrelatedCommonExtendNotTypes("matching the Holder extend")
        val extendFamily = specificSession.cjmpFamilyAggregate.matchingGroupFor(requiredSpecificExtend)
        assertUnrelatedCommonExtendNotTypes("building one extend family")
        val specificExtendFunction = requiredSpecificExtend.declarations.filterIsInstance<CfirNamedFunction>().single()
        specificExtendFunction.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        val indexedSpecificExtend = specificSession.extendProvider.getContainingExtend(specificExtendFunction.symbol)
        val specificEnumConstructor = specificEnum?.declarations?.filterIsInstance<CfirEnumConstructor>()?.single()
        if (specificEnum != null && specificEnumConstructor != null) {
            specificEnum.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
            specificEnumConstructor.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
        }

        if (mode == CfirCjmpMode.NONE) {
            for (declaration in listOfNotNull(
                requiredSpecificClass,
                specificIdentity,
                requiredSpecificExtend,
                specificExtendFunction,
                specificEnum,
                specificEnumConstructor,
            )) {
                assertEquals(CfirResolvePhase.CJMP_MATCHING, declaration.resolvePhase)
                assertNull(declaration.cjmpMatchFacts)
            }
            return
        }

        val family = specificSession.cjmpFamilyAggregate.matchingGroupFor(requiredSpecificClass)
        if (mainModule.name == "specificMemberMatchesCommon") {
            val commonModule = moduleStructure.mainModules.single { it.name == "common" }
            val commonPsi = commonModule.cjFiles.single()
            val commonFile = commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
            val unrelatedCommonExtend = commonFile.declarations.filterIsInstance<CfirExtend>().single { extend ->
                extend.declarations.filterIsInstance<CfirNamedFunction>().any { it.name.asString() == "unrelated" }
            }
            assertTrue(
                unrelatedCommonExtend.resolvePhase < CfirResolvePhase.TYPES,
                "building one class family must not advance unrelated common extends in the package",
            )
        }

        assertEquals(CfirResolvePhase.CJMP_MATCHING, requiredSpecificClass.resolvePhase)
        assertTrue(family.commonCounterpartsFor(requiredSpecificClass).any { it is CfirClass })
        assertTrue(
            family.commonCounterpartsFor(specificIdentity).any { it is CfirNamedFunction },
            "specific member not bound; class=${family.commonCounterpartsFor(requiredSpecificClass)}, " +
                    "member mismatches=${family.mismatchKindsFor(specificIdentity)}, " +
                    "candidates=${family.mismatchCandidatesFor(specificIdentity)}, " +
                            "typeMaps=${family.typeParameterMappingsFor(requiredSpecificClass)}",
        )
        if (mainModule.name == "specificMemberMatchesCommon") {
            val resolutionScopeSession = resolutionFacade.getScopeSessionFor(specificSession)
            val rawScope = requiredSpecificClass.unsubstitutedScope(
                useSiteSession = specificSession,
                scopeSession = resolutionScopeSession,
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            val actualizedScope = requiredSpecificClass.unsubstitutedScope(
                useSiteSession = specificSession,
                scopeSession = resolutionScopeSession,
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.CJMP_MATCHING,
            )
            val replacedActualizedScope = requireNotNull(
                actualizedScope.withReplacedSessionOrNull(specificSession, ScopeSession()),
            )
            val commonOnlyName = Name.identifier("commonOnly")
            val rawMembers = mutableListOf<CfirNamedFunctionSymbol>()
            val actualizedMembers = mutableListOf<CfirNamedFunctionSymbol>()
            val replacedActualizedMembers = mutableListOf<CfirNamedFunctionSymbol>()
            val rawConstructors = mutableListOf<CfirConstructorSymbol>()
            rawScope.processFunctionsByName(commonOnlyName) { rawMembers += it }
            rawScope.processDeclaredConstructors { rawConstructors += it }
            actualizedScope.processFunctionsByName(commonOnlyName) { actualizedMembers += it }
            replacedActualizedScope.processFunctionsByName(commonOnlyName) { replacedActualizedMembers += it }
            val familyCommonOnly = family.membersFor(requiredSpecificClass)
                .filterIsInstance<CfirNamedFunction>()
                .single { it.name == commonOnlyName }
            assertTrue(rawMembers.isEmpty(), "the raw scope must not inject a common-only implementation")
            assertSame(familyCommonOnly.symbol, actualizedMembers.single().unwrapSubstitutionOverrides())
            assertSame(
                familyCommonOnly.symbol,
                replacedActualizedMembers.single().unwrapSubstitutionOverrides(),
                "replacing a use-site session must preserve the CJMP actualized member view",
            )
            assertEquals(1, rawConstructors.size, "the STATUS raw view must expose only physical specific constructors")

            val typeAlias = cfirFile.declarations.filterIsInstance<CfirTypeAlias>().single()
            typeAlias.lazyResolveToPhase(CfirResolvePhase.TYPES)
            val aliasConstructorScope = typeAlias.scopeProvider.getTypealiasConstructorScope(
                typeAlias,
                specificSession,
                resolutionScopeSession,
            )
            val aliasConstructors = mutableListOf<CfirConstructorSymbol>()
            aliasConstructorScope.processDeclaredConstructors { aliasConstructors += it }
            assertEquals(
                listOf(0, 1),
                aliasConstructors.map { requireNotNull(it.typeAliasConstructorInfo).originalConstructor.valueParameters.size }.sorted(),
                "specific typealias lookup must retain the actualized common constructor alongside its specific constructor",
            )
        }
        assertEquals(CfirResolvePhase.CJMP_MATCHING, requiredSpecificExtend.resolvePhase)
        assertTrue(extendFamily.commonCounterpartsFor(requiredSpecificExtend).any { it is CfirExtend })
        val commonExtends = extendFamily.commonCounterpartsFor(requiredSpecificExtend)
        assertEquals(2, commonExtends.size)
        assertEquals(listOf(1, 1), commonExtends.map { extendFamily.typeParameterMappingFor(requiredSpecificExtend, it).size })
        val extendFacts = requireNotNull(requiredSpecificExtend.cjmpMatchFacts)
        val extendPlan = extendFacts.plans.single() as CjmpMatchPlan.Declaration<*, *, *>
        assertEquals(
            extendFamily.commonCounterpartsFor(requiredSpecificExtend).map { it.symbol },
            extendPlan.candidates.filter { it.eligibility == CjmpMatchResult.Matched }.map { it.common },
            "the declaration-owned plan must preserve every successfully merged common extend in order",
        )
        assertTrue(
            extendFamily.commonCounterpartsFor(specificExtendFunction).any { it is CfirNamedFunction },
            "extend member not bound; ownerSame=${indexedSpecificExtend === requiredSpecificExtend}, " +
                    "owner=$indexedSpecificExtend, " +
                    "ownerPairs=${indexedSpecificExtend?.let(extendFamily::commonCounterpartsFor)}, " +
                    "extendPair=${extendFamily.commonCounterpartsFor(requiredSpecificExtend)}, " +
                    "parentTypeMaps=${extendFamily.typeParameterMappingsFor(requiredSpecificExtend)}, " +
                    "memberPhase=${specificExtendFunction.resolvePhase}, " +
                    "memberReturnType=${specificExtendFunction.returnTypeRef}, " +
                    "memberMismatches=${extendFamily.mismatchKindsFor(specificExtendFunction)}, " +
                    "memberCandidates=${extendFamily.mismatchCandidatesFor(specificExtendFunction)}, " +
                    "unmatched=${extendFamily.isUnmatched(specificExtendFunction)}",
        )
        assertTrue(family.typeParameterMappingsFor(specificIdentity).all { (_, mapping) -> mapping.size == 1 })
        assertTrue(extendFamily.typeParameterMappingsFor(specificExtendFunction).all { (_, mapping) -> mapping.size == 1 })
        val commonExtendFunctions = extendFamily.commonCounterpartsFor(specificExtendFunction)
            .filterIsInstance<CfirNamedFunction>()
        assertEquals(refinementModules.size, commonExtendFunctions.size)
        for (commonExtendFunction in commonExtendFunctions) {
            val commonExtendOwner = moduleStructure.mainModules
                .filter { it !== mainModule }
                .flatMap { commonModule ->
                    commonModule.cjFiles.map { commonPsi ->
                        commonPsi.getOrBuildCfirFile(commonPsi.getResolutionFacadeForTest())
                    }
                }
                .flatMap { commonFile -> commonFile.declarations.filterIsInstance<CfirExtend>() }
                .single { extend -> extend.declarations.any { it.symbol === commonExtendFunction.symbol } }
            val logicalExtendOwner = extendFamily.logicalOwnerFor(commonExtendFunction) as CfirExtend
            assertSame(requiredSpecificExtend, logicalExtendOwner)
            assertEquals(
                mapOf(commonExtendOwner.typeParameters.single() to logicalExtendOwner.typeParameters.single()),
                extendFamily.ownerTypeParameterMappingFor(commonExtendFunction),
                "actualizing each common extend member must map its physical type parameter to the merged specific owner",
            )
        }
        assertTrue(family.hasResolutionResult(specificIdentity))
        if (specificEnum != null && specificEnumConstructor != null) {
            val enumFamily = specificSession.cjmpFamilyAggregate.matchingGroupFor(specificEnum)
            assertTrue(enumFamily.commonCounterpartsFor(specificEnum).any { it is CfirEnum })
            assertTrue(enumFamily.commonCounterpartsFor(specificEnumConstructor).any { it is CfirEnumConstructor })
            assertTrue(enumFamily.hasResolutionResult(specificEnumConstructor))
        }
    }

    private fun assertFirstFit(
        specificFunctions: List<CfirNamedFunction>,
        family: CfirCjmpFamilySnapshot,
    ) {
        val first = specificFunctions[0]
        val second = specificFunctions[1]
        val common = family.commonFor(first)
        assertNotNull(
            common,
            "first-fit declaration was not bound; mismatches=${family.mismatchKindsFor(first)}, " +
                    "candidates=${family.mismatchCandidatesFor(first)}, second=${family.mismatchKindsFor(second)}",
        )
        assertEquals(null, family.commonFor(second))
        assertEquals(listOf(first), family.specificBindingsFor(requireNotNull(common)))
        assertTrue(CjmpMismatchKind.SECOND_BINDING in family.mismatchKindsFor(second))
        assertTrue(family.candidateDiagnosticsFor(second).any {
            it == CfirCjmpCandidateDiagnostic.Occupied(common, first)
        })
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
        family: CfirCjmpFamilySnapshot,
    ): List<MatchState> = specificFunctions.map { specific ->
        val common = family.commonFor(specific)
        MatchState(
            common = common,
            mismatchKinds = family.mismatchKindsFor(specific),
            mismatchCandidates = family.mismatchCandidatesFor(specific),
            candidateDiagnostics = family.candidateDiagnosticsFor(specific),
            reverseBindings = common?.let(family::specificBindingsFor).orEmpty(),
        )
    }

    private fun assertEagerPlanDumpMatchesFamily(
        specificFunctions: List<CfirNamedFunction>,
        family: CfirCjmpFamilySnapshot,
    ) {
        val specificSymbols = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<CfirBasedSymbol<*>, Boolean>(),
        )
        specificFunctions.forEach { specificSymbols += it.symbol }
        val familyPlans = family.matching.plans.filter { it.specific in specificSymbols }
        val eagerPlans = specificFunctions.flatMap { specific ->
            requireNotNull(specific.cjmpMatchFacts) {
                "eager matching must publish each overload's own immutable plan"
            }.plans
        }
        val familySeeds = family.matching.initialBindings.filter { it.specific in specificSymbols }

        assertEquals(familyPlans, eagerPlans, "eager and LL declaration plan dumps must preserve overload order and candidates")
        assertEquals(
            CjmpMatchEvaluator.evaluate(familyPlans, familySeeds),
            CjmpMatchEvaluator.evaluate(eagerPlans),
            "eager and LL declaration plan dumps must produce identical bindings, visits, and diagnostics",
        )
    }
}
