package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.analysis.tests.runners.AbstractCfirPsiDiagnosticTest
import org.junit.jupiter.api.Test

/**
 * enum pattern 的 constructor 引用必须按官方语义解析：
 * 未知 constructor 名字报 unresolved reference，而非 PATTERN_NOT_MATCH。
 */
class MatchPatternConstructorTest : AbstractCfirPsiDiagnosticTest() {
    @Test
    fun testMatchPatternConstructor() {
        runTest("cfir/analysis-tests/testData/pattern/matchPatternConstructor.cj")
    }

    /**
     * 裸名字是别的 enum 的 constructor 时，目标只能在 subject enum 内定位：
     * 找不到报 unresolved reference，不能悄悄绑定到那个 enum 而吞掉诊断。
     */
    @Test
    fun testMatchPatternForeignEnumConstructor() {
        runTest("cfir/analysis-tests/testData/pattern/matchPatternForeignEnumConstructor.cj")
    }

    /**
     * subject 的 constructor 已知时，pattern 引用与泛型 enum 的 use-site constructor 是同一个：
     * 匹配的分支可达，其后的分支才是不可达分支。
     */
    @Test
    fun testMatchPatternKnownGenericConstructor() {
        runTest("cfir/analysis-tests/testData/pattern/matchPatternKnownGenericConstructor.cj")
    }

    /**
     * 限定名模式的 owner 按解析出的类型判定：指向同一 enum 的 typealias 合法，
     * owner 是别的 enum 或类型实参不同才是 pattern 不匹配。
     */
    @Test
    fun testMatchPatternQualifiedOwner() {
        runTest("cfir/analysis-tests/testData/pattern/matchPatternQualifiedOwner.cj")
    }
}
