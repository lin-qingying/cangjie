package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCjcLlTDiagnosticsConsistencyTest
import org.junit.jupiter.api.Test

/**
 * 回归测试：`--no-prelude` 下 `arraylit5_error.cj` 必须稳定产出
 * `sema_core_object_not_found_when_no_prelude`，且不能在 checker 提交流水线中丢失。
 */
class CjcLlTArraylit5ErrorTest : AbstractCjcLlTDiagnosticsConsistencyTest() {
    @Test
    fun testArraylit5Error() {
        runTest("cfir/analysis-tests/testData/llt/array/arraylit5_error.cj")
    }
}
