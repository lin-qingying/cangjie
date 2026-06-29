package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * 提供 `ModuleStructureDirectives` 单例，集中承载测试指令的共享状态、常量或默认行为。
 */
object ModuleStructureDirectives : SimpleDirectivesContainer() {
    /**
     * 保存 `MODULE`，供测试指令在测试执行期间读取或传递。
     */
    val MODULE by stringDirective(
        """
            Usage: // MODULE: {name}[(dependencies)]
            Describes one module. If no targets are specified then <TODO>
        """.trimIndent()
    )

    /**
     * 保存 `FILE`，供测试指令在测试执行期间读取或传递。
     */
    val FILE by stringDirective(
        """
            Usage: // FILE: name.{kt|java}
            Declares file with specified name in current module
        """.trimIndent()
    )

    /**
     * 保存 `SNIPPET`，供测试指令在测试执行期间读取或传递。
     */
    val SNIPPET by directive(
        """
            Usage: // SNIPPET
            Declares (next) snippet with auto-incremented number
        """.trimIndent()
    )

    /**
     * 保存 `ESCAPE_MODULE_NAME`，供测试指令在测试执行期间读取或传递。
     */
    val ESCAPE_MODULE_NAME by directive("Add a unique prefix to the module name based on the test coordinates")
}
