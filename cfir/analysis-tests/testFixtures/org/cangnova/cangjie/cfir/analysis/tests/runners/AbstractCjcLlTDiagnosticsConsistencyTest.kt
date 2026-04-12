package org.cangnova.cangjie.cfir.analysis.tests.runners

import org.cangnova.cangjie.cfir.analysis.tests.services.CfirCjcLlTDiagnosticsChecker
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.directives.DiagnosticsDirectives

/**
 * LLT 诊断一致性测试基类（CFIR LightTree vs CJC）。
 *
 * 设计说明：
 * 1. 继承 [AbstractCfirLightTreeDiagnosticsTest]，保证走“标准诊断测试全管线”；
 * 2. 通过默认 `DIAGNOSTICS` 指令关闭 inline 诊断渲染，避免要求 `<!...!>` 标注；
 * 3. 真实 CFIR vs CJC 对比由 [CfirCjcLlTDiagnosticsChecker] 在 after-analysis 阶段完成。
 */
abstract class AbstractCjcLlTDiagnosticsConsistencyTest : AbstractCfirLightTreeDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(builder)

        // 关闭所有内联诊断渲染，保留完整前端解析/检查流水线，但不要求测试数据带 <!DIAG!> 标记。
        defaultDirectives {
            DiagnosticsDirectives.DIAGNOSTICS with "-errors"
            DiagnosticsDirectives.DIAGNOSTICS with "-warnings"
            DiagnosticsDirectives.DIAGNOSTICS with "-infos"
        }

        useAfterAnalysisCheckers(::CfirCjcLlTDiagnosticsChecker)
    }
}
