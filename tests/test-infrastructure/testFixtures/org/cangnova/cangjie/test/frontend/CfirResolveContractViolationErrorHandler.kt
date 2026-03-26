package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.test.WrappedException
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.services.artifactsProvider
import org.cangnova.cangjie.test.services.assertions
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.moduleStructure

class CfirResolveContractViolationErrorHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {
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
