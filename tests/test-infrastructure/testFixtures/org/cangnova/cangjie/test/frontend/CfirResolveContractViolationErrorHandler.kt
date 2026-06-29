package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.test.WrappedException
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.services.artifactsProvider
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.moduleStructure

/**
 * 表示 `CfirResolveContractViolationErrorHandler`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirResolveContractViolationErrorHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    /**
     * 执行 `check` 对应的CFIR 前端测试流程，维持测试框架的阶段契约。
     */
    override fun check(failedAssertions: List<WrappedException>) {
        val artifactsProvider = testServices.artifactsProvider
        val exceptions = buildList {
            for (module in testServices.moduleStructure.modules) {
                val output = artifactsProvider.getArtifactSafe(module, FrontendKinds.CFIR) as? CfirOutputArtifact ?: continue
                for (part in output.partsForDependsOnModules) {
                    val lazyResolver = part.session.lazyDeclarationResolver as? CfirCompilerLazyDeclarationResolverWithPhaseChecking ?: continue
                    addAll(lazyResolver.getContractViolationExceptions())
                }
            }
        }

        if (exceptions.isNotEmpty()) {
            testServices.assertions.failAll(exceptions)
        }
    }
}
