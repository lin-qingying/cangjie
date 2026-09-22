package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirPsiDiagnosticTest
import org.junit.jupiter.api.Test

class CfirInteropMutabilityPsiTest : AbstractCfirPsiDiagnosticTest() {
    @Test
    fun testInteropMutability() {
        runTest("cfir/analysis-tests/testData/diagnostics/constructor/interopMutabilityProbe.cj")
    }
}
