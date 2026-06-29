/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.messages

/**
 * 面向消息收集器的通用上报工具。
 */
object MessageCollectorUtil {
    /**
     * 将异常堆栈作为编译器异常消息上报给收集器。
     */
    @JvmStatic
    fun reportException(messageCollector: MessageCollector, throwable: Throwable) {
        messageCollector.report(CompilerMessageSeverity.EXCEPTION, throwable.stackTraceToString())
    }
}
