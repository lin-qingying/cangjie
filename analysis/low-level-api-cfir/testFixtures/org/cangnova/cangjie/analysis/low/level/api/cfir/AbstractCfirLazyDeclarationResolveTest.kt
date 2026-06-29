package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

/**
 * 对齐 Kotlin `AbstractFirLazyDeclarationResolveTest` 的 source 版 low-level 入口。
 */
abstract class AbstractCfirLazyDeclarationResolveTest : AbstractCfirLazyDeclarationResolveOverAllPhasesTest() {
    /**
     * 根据测试指令选出目标 CFIR 声明，并渲染所有模块文件的阶段状态。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        doLazyResolveTest(
            cjFile = mainFile,
            testServices = testServices,
            outputRenderingMode = OutputRenderingMode.ALL_FILES_FROM_ALL_MODULES,
        ) { resolutionFacade ->
            findCfirDeclarationToResolve(mainFile, testServices, resolutionFacade)
        }
    }
}

/**
 * source 配置下的 lazy declaration resolve 测试基类。
 */
abstract class AbstractCfirSourceLazyDeclarationResolveTest : AbstractCfirLazyDeclarationResolveTest() {
    /**
     * 使用源码 low-level CFIR 测试配置。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
