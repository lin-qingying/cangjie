package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirLightTreeDiagnosticsTest
import org.junit.jupiter.api.Test

/**
 * LightTree 前端下 enum pattern constructor 的解析/诊断必须与 PSI 前端一致。
 */
class MatchPatternConstructorLightTreeTest : AbstractCfirLightTreeDiagnosticsTest() {
    @Test
    fun testMatchPatternConstructorLightTree() {
        runTest("cfir/analysis-tests/testData/pattern/matchPatternConstructor.cj")
    }

    /** 裸名字 enum pattern 的分类与定位在 LightTree 前端下与 PSI 前端一致。 */
    @Test
    fun testMatchPatternForeignEnumConstructorLightTree() {
        runTest("cfir/analysis-tests/testData/pattern/matchPatternForeignEnumConstructor.cj")
    }

    /** 已知 constructor 的不可达分支结论在 LightTree 前端下与 PSI 前端一致。 */
    @Test
    fun testMatchPatternKnownGenericConstructorLightTree() {
        runTest("cfir/analysis-tests/testData/pattern/matchPatternKnownGenericConstructor.cj")
    }

    /** 限定名模式的 owner 判定在 LightTree 前端下与 PSI 前端一致。 */
    @Test
    fun testMatchPatternQualifiedOwnerLightTree() {
        runTest("cfir/analysis-tests/testData/pattern/matchPatternQualifiedOwner.cj")
    }
}
