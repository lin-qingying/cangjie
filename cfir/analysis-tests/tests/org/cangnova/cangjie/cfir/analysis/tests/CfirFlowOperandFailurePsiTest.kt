package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirPsiDiagnosticTest
import org.junit.jupiter.api.Test

/**
 * flow 表达式解糖调用失败的诊断归属（官方 `ChkFlowExpr` 第二阶段，
 * `external/cangjie_compiler/src/Sema/TypeCheckExpr/BinaryExpr.cpp:1057-1070`）。
 */
class CfirFlowOperandFailurePsiTest : AbstractCfirPsiDiagnosticTest() {
    @Test
    fun testFlowOperandFailure() {
        runTest("cfir/analysis-tests/testData/diagnostics/general/flowOperandFailure.cj")
    }
}
