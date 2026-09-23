package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirPsiDiagnosticTest
import org.junit.jupiter.api.Test

/**
 * flow 表达式操作数的裸 `this` / `super` 行为（官方 `ChkFlowExpr`，
 * `external/cangjie_compiler/src/Sema/TypeCheckExpr/BinaryExpr.cpp:1008-1075`）。
 */
class CfirFlowThisPsiTest : AbstractCfirPsiDiagnosticTest() {
    @Test
    fun testFlowThisProbe() {
        runTest("cfir/analysis-tests/testData/diagnostics/general/flowThisProbe.cj")
    }
}
