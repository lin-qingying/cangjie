/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.diagnostics


import org.cangnova.cangjie.messages.CompilerMessageSeverity

/**
 * 仓颉诊断严重级别。
 */
enum class Severity {
    INFO,
    ERROR,
    WARNING,

    /**
     * see [CompilerMessageSeverity.FIXED_WARNING]
     */
    FIXED_WARNING,
    STRONG_WARNING;

    /**
     * 转换为编译器消息严重级别。
     */
    fun toCompilerMessageSeverity(): CompilerMessageSeverity = when (this) {
        INFO -> CompilerMessageSeverity.INFO
        ERROR -> CompilerMessageSeverity.ERROR
        WARNING -> CompilerMessageSeverity.WARNING
        STRONG_WARNING -> CompilerMessageSeverity.STRONG_WARNING
        FIXED_WARNING -> CompilerMessageSeverity.FIXED_WARNING
    }

    /**
     * 当前级别是否会在 Werror 下被视作错误。
     */
    val isErrorWhenWError: Boolean
        get() = when (this) {
            INFO, ERROR -> false
            FIXED_WARNING -> false
            WARNING,
            STRONG_WARNING -> true
        }

    /**
     * 当前级别是否为错误。
     */
    val isError: Boolean
        get() = when (this) {
            ERROR -> true
            INFO,
            WARNING,
            FIXED_WARNING,
            STRONG_WARNING -> false
        }
}

