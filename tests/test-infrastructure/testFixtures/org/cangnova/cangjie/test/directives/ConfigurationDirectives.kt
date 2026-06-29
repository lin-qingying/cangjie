package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * Common test configuration directives.
 *
 * Aligned with Kotlin test-infrastructure `ConfigurationDirectives`.
 */
object ConfigurationDirectives : SimpleDirectivesContainer() {
    /**
     * 保存 `DISABLE_TYPEALIAS_EXPANSION`，供测试指令在测试执行期间读取或传递。
     */
    val DISABLE_TYPEALIAS_EXPANSION by directive(
        description = "Disables automatic expansion of aliased types in type resolution."
    )
}
