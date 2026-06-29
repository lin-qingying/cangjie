package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.renderer.CfirResolvePhaseRenderer
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 渲染整文件 CFIR 的 lazy resolve phase golden。
 */
abstract class AbstractWholeFileResolvePhaseTest : AbstractAnalysisApiBasedTest() {
    /**
     * 将整文件推进到 BODY_RESOLVE 并渲染 resolve phase golden。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val cfirFile = mainFile.getOrBuildCfirFile(resolutionFacade)
        cfirFile.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        val actual = CfirRenderer(resolvePhaseRenderer = CfirResolvePhaseRenderer()).renderElementAsString(cfirFile)
        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = ".lazy.resolve.txt")
    }
}

/**
 * source 配置下的整文件 resolve phase 测试基类。
 */
abstract class AbstractSourceWholeFileResolvePhaseTest : AbstractWholeFileResolvePhaseTest() {
    /**
     * 使用源码 low-level CFIR 测试配置。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
