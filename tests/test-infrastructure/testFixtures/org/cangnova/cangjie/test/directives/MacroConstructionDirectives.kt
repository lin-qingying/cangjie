package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * Macro construction step 测试 directive 容器（baseline 第 11 节）。
 *
 * 这些 directive 控制 `.cj` testdata 在 macro construction step 阶段的行为：
 *
 * - [MACRO_EXECUTOR] 指定 macro executor 实现：
 *     * `none`  —— 不注入 executor；CLI strict 模式应当产 `MACRO_EXECUTOR_UNAVAILABLE`。
 *     * `stub`  —— 注入 `:macro:macro-stub` 的桩 executor；多数 IDE / analysis 测试默认值。
 *     * `real`  —— 调用 `:macro:macro-process` 的真实外部进程；CI 上才使用。
 *
 * - [EXPECT_DEGRADED] 标记本 testdata 期望进入 `MacroConstructionResult.Degraded`：
 *     * `true`  —— 应产 typed error placeholder + `MACRO_NOT_EXPANDED` /
 *                  `MACRO_EXPANSION_FAILED` 诊断，且 ordinary resolve 仍运行。
 *     * `false` —— 默认值；STRICT 模式，未展开即失败。
 *
 * Baseline 第 11 节中提到的 cache key 与 ABI 入口由
 * `CompilerConfiguration` 控制，不在此处暴露 directive。
 */
object MacroConstructionDirectives : SimpleDirectivesContainer() {

    val MACRO_EXECUTOR by enumDirective<MacroExecutorMode>(
        description = """
            Macro executor 实现选择：none / stub / real
            参考 baseline 第 11 节 "测试 directive"。
        """.trimIndent()
    )

    val EXPECT_DEGRADED by directive(
        description = """
            标记本 testdata 期望进入 MacroConstructionResult.Degraded
            （typed error placeholder + MACRO_NOT_EXPANDED 等诊断）。
            参考 baseline 第 11 节 "测试 directive"。
        """.trimIndent()
    )

    enum class MacroExecutorMode { none, stub, real }
}
