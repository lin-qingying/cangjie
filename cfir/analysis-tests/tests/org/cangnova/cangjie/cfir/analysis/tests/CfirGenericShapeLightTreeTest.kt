package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirLightTreeDiagnosticsTest
import org.junit.jupiter.api.Test

/**
 * P3 批次 1（泛型/函数形态/实例化族）诊断回归（LightTree 入口）：
 * 泛型构造器、泛型 operator 重载、抽象类实例化、数值转换实参。
 */
class CfirGenericShapeLightTreeTest : AbstractCfirLightTreeDiagnosticsTest() {
    @Test
    fun testGenericShape() {
        runTest("cfir/analysis-tests/testData/diagnostics/constructor/genericShape.cj")
    }
}
