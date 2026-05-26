package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

/**
 * 对齐 Kotlin `AbstractFirLazyDeclarationResolveTest` 的 source 版 low-level 入口。
 */
abstract class AbstractCfirLazyDeclarationResolveTest : AbstractCfirLazyDeclarationResolveOverAllPhasesTest() {
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

abstract class AbstractCfirSourceLazyDeclarationResolveTest : AbstractCfirLazyDeclarationResolveTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
