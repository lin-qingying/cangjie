package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCjcLlTDiagnosticsConsistencyTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * 回归测试：`--no-prelude` 下 `arraylit5_error.cj` 必须稳定产出
 * `sema_core_object_not_found_when_no_prelude`，且不能在 checker 提交流水线中丢失。
 */
@Disabled("LLT 官方一致性测试不作为当前 CFIR analysis-tests 门禁执行")
class CjcLlTArraylit5ErrorTest : AbstractCjcLlTDiagnosticsConsistencyTest() {
    @Test
    /**
     * 执行 `arraylit5_error.cj` 的官方 cjc 一致性回归测试。
     */
    fun testArraylit5Error() {
        runTest("cfir/analysis-tests/testData/llt/array/arraylit5_error.cj")
    }
}
