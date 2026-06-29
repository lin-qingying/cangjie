/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.cangnova.cangjie.messages

import java.util.*

/**
 * 编译器消息严重级别。
 */
enum class CompilerMessageSeverity {
    EXCEPTION,
    ERROR,

    /**
     * Unlike a normal warning, a strong warning is not discarded when there are compilation errors.
     * Use it for problems related to configuration, not the diagnostics.
     */
    STRONG_WARNING,

    /**
     * Warning severity set by `-Xwarning-level=NAME:warning`.
     * Unlike a normal warning it isn't suppressed by `-nowarn` flag
     * and does not cause the compilation to fail with `-Werror` flag
     */
    FIXED_WARNING,
    WARNING,
    INFO,
    LOGGING,

    /**
     * Source to output files mapping messages (e.g A.kt->A.class).
     * It is needed for incremental compilation.
     */
    OUTPUT;

    /**
     * 是否表示错误级消息。
     */
    val isError: Boolean
        get() = this == EXCEPTION || this == ERROR

    /**
     * 是否表示警告级消息。
     */
    val isWarning: Boolean
        get() = this == STRONG_WARNING || this == WARNING || this == FIXED_WARNING

    /**
     * 是否表示会受普通 warning 策略影响的警告。
     */
    val isRegularWarning: Boolean
        get() = this == STRONG_WARNING || this == WARNING

    /**
     * 面向用户展示的严重级别名称。
     */
    val presentableName: String
        get() = when (this) {
            EXCEPTION -> "exception"
            ERROR -> "error"
            STRONG_WARNING, WARNING, FIXED_WARNING -> "warning"
            INFO -> "info"
            LOGGING -> "logging"
            OUTPUT -> "output"
        }

    companion object {
        /**
         * verbose 输出使用的严重级别集合。
         */
        @JvmField
        val VERBOSE: EnumSet<CompilerMessageSeverity> = EnumSet.of(LOGGING)
    }
}
