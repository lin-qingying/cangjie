package org.cangnova.cangjie.test.directives

import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * 提供 `LanguageSettingsDirectives` 单例，集中承载测试指令的共享状态、常量或默认行为。
 */
object LanguageSettingsDirectives : SimpleDirectivesContainer() {
    /**
     * 保存 `LANGUAGE_VERSION`，供测试指令在测试执行期间读取或传递。
     */
    val LANGUAGE_VERSION by stringDirective("Pin test language version.")
    /**
     * 保存 `LANGUAGE`，供测试指令在测试执行期间读取或传递。
     */
    val LANGUAGE by stringDirective("Enable/disable features, e.g. +Feature / -Feature.")
    /**
     * 保存 `SUPPRESS_WARNINGS`，供测试指令在测试执行期间读取或传递。
     */
    val SUPPRESS_WARNINGS by stringDirective("Suppress warnings by diagnostic name.")
    /**
     * 保存 `ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING`，供测试指令在测试执行期间读取或传递。
     */
    val ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING by directive("Allow fixed language version in tests.")
}
