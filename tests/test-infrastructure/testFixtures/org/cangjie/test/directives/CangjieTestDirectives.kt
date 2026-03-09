package org.cangjie.test.directives

import org.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * Cangjie 测试的最小指令集（参考 Kotlin 编译器测试）。
 */
object CangjieTestDirectives : SimpleDirectivesContainer() {
    val MODULE by stringDirective(
        description = "开始一个新的模块段。可选参数支持依赖，例如 `main(dep1, dep2)`。",
    )

    val FILE by stringDirective(
        description = "在当前模块内开始一个新的文件段。",
    )

    val DEPENDS_ON by stringDirective(
        description = "为当前模块追加依赖（逗号/空格分隔）。",
    )

    val WITH_STDLIB by directive(
        description = "编译测试时包含标准库。",
    )

    val IGNORE_ERRORS by directive(
        description = "允许测试数据存在编译错误，用于错误恢复测试。",
    )

    val LANGUAGE_VERSION by stringDirective(
        description = "指定语言版本，例如 `// LANGUAGE_VERSION: 1.0`。",
    )

    val EXPECT_COMPLETION_ITEM by stringDirective(
        description = "声明期望出现的补全项。",
    )

    val FIX_NAME by stringDirective(
        description = "声明期望快速修复名称。",
    )

    val DUMP_CFIR by directive(
        description = "启用 CFIR dump 并与期望文件对比。",
    )

    val DUMP_CHIR by directive(
        description = "启用 CHIR dump 并与期望文件对比。",
    )
}

