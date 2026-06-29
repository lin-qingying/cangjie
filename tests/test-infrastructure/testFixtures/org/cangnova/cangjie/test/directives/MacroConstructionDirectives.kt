package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * Macro construction 测试 directive。
 *
 * 测试数据不能直接声明宏定义或注入 executor 语义；宏定义必须来自真实
 * `macro package` 源码、已编译 artifact、SDK std 宏包或 production executor。
 */
object MacroConstructionDirectives : SimpleDirectivesContainer() {
    /**
     * 保存 `DISABLE_BACKGROUND_AUTO_COMPILE_MACRO_PACKAGES`，供测试指令在测试执行期间读取或传递。
     */
    val DISABLE_BACKGROUND_AUTO_COMPILE_MACRO_PACKAGES by directive(
        description = """
            关闭后台宏包自动编译。当前仅映射到 CompilerConfiguration 占位开关。
        """.trimIndent(),
    )

    /**
     * 保存 `DISABLE_EXPANSION_DEMAND_AUTO_COMPILE_MACRO_PACKAGES`，供测试指令在测试执行期间读取或传递。
     */
    val DISABLE_EXPANSION_DEMAND_AUTO_COMPILE_MACRO_PACKAGES by directive(
        description = """
            关闭宏展开 demand 触发的即时 `cjc -p <root> --compile-macro` 编译。
        """.trimIndent(),
    )

    /**
     * 保存 `DUMP_MACRO_EXPANDED_CFIR`，供测试指令在测试执行期间读取或传递。
     */
    val DUMP_MACRO_EXPANDED_CFIR by directive(
        description = """
            将 macro construction、artifact 准备与 executor 展开后的 CFIR 输出到 `<test>.macro.cfir.txt`。
            该 directive 仅用于 `testData/macro` 的真实宏端到端链路观测。
        """.trimIndent(),
    )
}
