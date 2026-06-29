/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.messages

/**
 * 编译器消息收集接口。
 */
interface MessageCollector {
    /**
     * 清空已收集消息。
     */
    fun clear()

    /**
     * 上报一条编译器消息。
     */
    fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation? = null,
    )

    /**
     * 判断当前收集器是否已收到错误消息。
     */
    fun hasErrors(): Boolean

    companion object {
        /**
         * 忽略所有消息的空收集器。
         */
        @JvmField
        val NONE: MessageCollector = object : MessageCollector {
            override fun clear() = Unit

            override fun report(
                severity: CompilerMessageSeverity,
                message: String,
                location: CompilerMessageSourceLocation?,
            ) = Unit

            override fun hasErrors(): Boolean = false
        }
    }
}
