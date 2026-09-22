package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirPsiDiagnosticTest
import org.junit.jupiter.api.Test

class CfirDeclShapePsiTest : AbstractCfirPsiDiagnosticTest() {
    @Test
    fun testDeclShapeProbe() {
        runTest("cfir/analysis-tests/testData/diagnostics/general/declShapeProbe.cj")
    }

    @Test
    fun testMultipleMainProbe() {
        runTest("cfir/analysis-tests/testData/diagnostics/general/multipleMainProbe.cj")
    }
}
