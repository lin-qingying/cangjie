/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirExceptionHandler
import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * low-level CFIR 分析使用的异常处理器。
 */
internal object LLCfirExceptionHandler : CfirExceptionHandler() {
    /**
     * 元素分析异常在 low-level 层不吞掉，直接重新抛出。
     */
    override fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing {
        throw throwable
    }

    /**
     * 文件分析异常在 low-level 层不吞掉，直接重新抛出。
     */
    override fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing {
        throw throwable
    }
}
