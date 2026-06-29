package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * 提供 `FrontendDirectives` 单例，集中承载CFIR 前端测试的共享状态、常量或默认行为。
 */
object FrontendDirectives : SimpleDirectivesContainer() {
    /**
     * 保存 `CHECK_COMPILER_OUTPUT`，供CFIR 前端测试在测试执行期间读取或传递。
     */
    val CHECK_COMPILER_OUTPUT by directive(
        description = "Check compiler textual output against golden files when available."
    )
}
