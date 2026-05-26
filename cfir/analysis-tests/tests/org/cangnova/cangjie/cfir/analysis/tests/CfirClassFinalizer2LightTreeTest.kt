package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirLightTreeDiagnosticsTest
import org.junit.jupiter.api.Test

/**
 * 回归测试：finalizer 共享 function-like BODY_RESOLVE 入口后，
 * 仍需保持 `class_finalizer2.cj` 中 finalizer 作用域的既有诊断行为。
 */
class CfirClassFinalizer2LightTreeTest : AbstractCfirLightTreeDiagnosticsTest() {
    @Test
    fun testClassFinalizer2() {
        runTest("cfir/analysis-tests/testData/llt/class/class_finalizer/class_finalizer2.cj")
    }
}
