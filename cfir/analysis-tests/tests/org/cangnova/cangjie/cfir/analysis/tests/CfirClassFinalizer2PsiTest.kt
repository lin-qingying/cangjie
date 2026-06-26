package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirPsiDiagnosticTest
import org.junit.jupiter.api.Test

/**
 * 回归测试：PSI 入口下同样要保持 `class_finalizer2.cj`
 * 的 finalizer 作用域与诊断不回归。
 */
class CfirClassFinalizer2PsiTest : AbstractCfirPsiDiagnosticTest() {
    @Test
    /**
     * 执行 PSI finalizer 回归测试数据。
     */
    fun testClassFinalizer2() {
        runTest("cfir/analysis-tests/testData/llt/class/class_finalizer/class_finalizer2.cj")
    }
}
