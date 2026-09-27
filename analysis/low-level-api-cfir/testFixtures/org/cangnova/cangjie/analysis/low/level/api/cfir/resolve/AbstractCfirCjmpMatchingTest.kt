@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CaMutableTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.cjmpMappingStorage
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.test.services.TestServices
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
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByMainFile(mainFile: org.cangnova.cangjie.psi.CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val commonModule = testServices.cjTestModuleStructure.getModule("common")
        val specificModule = mainModule.caModule as CaMutableTestModule
        specificModule.directDependsOnDependencies += commonModule.caModule

        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val cfirFile = mainFile.getOrBuildCfirFile(resolutionFacade)
        val specificClass = cfirFile.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "Box" }
        val specificEnum = cfirFile.declarations.filterIsInstance<CfirEnum>().singleOrNull { it.name.asString() == "Choice" }
        val specificExtend = cfirFile.declarations.filterIsInstance<CfirExtend>().singleOrNull()
        val specificSession = specificClass.moduleData.session
        val mode = if (mainModule.name == "modeNone") CfirCjmpMode.NONE else CfirCjmpMode.SPECIFIC
        specificSession.register(
            CfirCjmpSettingsComponent::class,
            CfirCjmpSettingsComponent(explicitMode = mode),
        )

        if (mainModule.name == "firstMatch") {
            val sharedFunctions = cfirFile.declarations.filterIsInstance<CfirNamedFunction>()
                .filter { it.name.asString() == "shared" }
            assertEquals(2, sharedFunctions.size)
            val first = sharedFunctions[0]
            val second = sharedFunctions[1]

            second.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)

            val storage = specificSession.cjmpMappingStorage
            val firstCommon = storage.commonFor(first)
            assertNotNull(
                firstCommon,
                "first-fit declaration was not bound; mismatches=${storage.mismatchKindsFor(first)}, " +
                        "candidates=${storage.mismatchCandidatesFor(first)}, second=${storage.mismatchKindsFor(second)}",
            )
            val common = requireNotNull(firstCommon)
            assertEquals(null, storage.commonFor(second))
            assertEquals(listOf(first, second), storage.specificBindingsFor(common))
            assertTrue(CjmpMismatchKind.SECOND_BINDING in storage.mismatchKindsFor(second))
            return
        }

        if (mainModule.name == "memberFirstMatch") {
            val members = specificClass.declarations.filterIsInstance<CfirNamedFunction>()
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

        val specificIdentity = specificClass.declarations.filterIsInstance<CfirNamedFunction>().single()
        specificClass.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)
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
            assertTrue(specificClass.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            assertTrue(requiredSpecificExtend.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
            assertTrue(storage.isEmpty)
            assertEquals(null, storage.commonFor(specificClass))
            assertEquals(null, storage.commonFor(requiredSpecificExtend))
            return
        }

        assertEquals(CfirResolvePhase.CJMP_MATCHING, specificClass.resolvePhase)
        assertNotNull(storage.commonFor(specificClass) as? CfirClass)
        assertNotNull(
            storage.commonFor(specificIdentity) as? CfirNamedFunction,
            "specific member not bound; class=${storage.commonFor(specificClass)}, " +
                    "member mismatches=${storage.mismatchKindsFor(specificIdentity)}, " +
                    "candidates=${storage.mismatchCandidatesFor(specificIdentity)}, " +
                    "typeMap=${storage.typeParameterMappingFor(specificClass)}",
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
}
