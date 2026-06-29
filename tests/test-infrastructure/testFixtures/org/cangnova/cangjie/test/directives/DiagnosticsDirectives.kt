package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * 提供 `DiagnosticsDirectives` 单例，集中承载测试指令的共享状态、常量或默认行为。
 */
object DiagnosticsDirectives : SimpleDirectivesContainer() {
    /**
     * 保存 `DIAGNOSTICS`，供测试指令在测试执行期间读取或传递。
     */
    val DIAGNOSTICS by stringDirective(
        description = "Enables/disables diagnostics by name or severity (e.g. +errors, -UNUSED_VARIABLE).",
    )

    /**
     * 保存 `RENDER_DIAGNOSTICS_FULL_TEXT`，供测试指令在测试执行期间读取或传递。
     */
    val RENDER_DIAGNOSTICS_FULL_TEXT by directive(
        description = "Dumps rendered diagnostics text to an expected side file.",
    )

    /**
     * 保存 `RENDER_ALL_DIAGNOSTICS_FULL_TEXT`，供测试指令在测试执行期间读取或传递。
     */
    val RENDER_ALL_DIAGNOSTICS_FULL_TEXT by directive(
        description = "Keeps rendered diagnostics side file even if per-module render directive is absent.",
    )

    /**
     * 保存 `MARK_DYNAMIC_CALLS`，供测试指令在测试执行期间读取或传递。
     */
    val MARK_DYNAMIC_CALLS by directive(
        description = "Enables debug markers for dynamic calls.",
    )
}
