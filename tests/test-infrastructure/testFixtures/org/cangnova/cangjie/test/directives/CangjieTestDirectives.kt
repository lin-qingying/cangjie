package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * Minimal test directives for Cangjie test infrastructure.
 */
object CangjieTestDirectives : SimpleDirectivesContainer() {
    /**
     * 保存 `MODULE`，供测试指令在测试执行期间读取或传递。
     */
    val MODULE by stringDirective(
        description = "Start a new module section. Optional deps, e.g. `main(dep1, dep2)`.",
    )

    /**
     * 保存 `FILE`，供测试指令在测试执行期间读取或传递。
     */
    val FILE by stringDirective(
        description = "Start a new file section inside current module.",
    )

    /**
     * 保存 `SNIPPET`，供测试指令在测试执行期间读取或传递。
     */
    val SNIPPET by directive(
        description = "Start a snippet module and auto-generate snippet file.",
    )

    /**
     * 保存 `DEPENDS_ON`，供测试指令在测试执行期间读取或传递。
     */
    val DEPENDS_ON by stringDirective(
        description = "Add dependencies for current module (comma/space separated).",
    )

    /**
     * 保存 `IMPORT_PATH`，供测试指令在测试执行期间读取或传递。
     */
    val IMPORT_PATH by stringDirective(
        description = "Add an import/classpath root for the current test module.",
    )

    /**
     * 保存 `WITH_STDLIB`，供测试指令在测试执行期间读取或传递。
     */
    val WITH_STDLIB by directive(
        description = "Include stdlib in compilation.",
    )

    /**
     * 保存 `NO_PRELUDE`，供测试指令在测试执行期间读取或传递。
     */
    val NO_PRELUDE by directive(
        description = "Compile without prelude support.",
    )

    /**
     * 保存 `IGNORE_ERRORS`，供测试指令在测试执行期间读取或传递。
     */
    val IGNORE_ERRORS by directive(
        description = "Allow test data with compile errors.",
    )

    /**
     * 保存 `LANGUAGE_VERSION`，供测试指令在测试执行期间读取或传递。
     */
    val LANGUAGE_VERSION by stringDirective(
        description = "Pin language version, e.g. `// LANGUAGE_VERSION: 1.0`.",
    )

    /**
     * 保存 `API_LEVEL`，供测试指令在测试执行期间读取或传递。
     */
    val API_LEVEL by stringDirective(
        description = "Configure project API level for APILevel diagnostics.",
    )

    /**
     * 保存 `API_LEVEL_SYSCAP`，供测试指令在测试执行期间读取或传递。
     */
    val API_LEVEL_SYSCAP by stringDirective(
        description = "Configure syscap json/config path for APILevel diagnostics.",
    )

    /**
     * 保存 `EXPECT_COMPLETION_ITEM`，供测试指令在测试执行期间读取或传递。
     */
    val EXPECT_COMPLETION_ITEM by stringDirective(
        description = "Declare expected completion item.",
    )

    /**
     * 保存 `FIX_NAME`，供测试指令在测试执行期间读取或传递。
     */
    val FIX_NAME by stringDirective(
        description = "Declare expected quick-fix name.",
    )

    /**
     * 保存 `DUMP_CFIR`，供测试指令在测试执行期间读取或传递。
     */
    val DUMP_CFIR by directive(
        description = "Enable CFIR dump and compare with expected file.",
    )

    /**
     * 保存 `DUMP_CHIR`，供测试指令在测试执行期间读取或传递。
     */
    val DUMP_CHIR by directive(
        description = "Enable CHIR dump and compare with expected file.",
    )
}
