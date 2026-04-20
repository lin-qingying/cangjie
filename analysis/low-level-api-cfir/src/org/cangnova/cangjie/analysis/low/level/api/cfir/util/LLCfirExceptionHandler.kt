/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirExceptionHandler
import org.cangnova.cangjie.cfir.declarations.CfirFile

internal object LLCfirExceptionHandler : CfirExceptionHandler() {
    override fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing {
        throw throwable
    }

    override fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing {
        throw throwable
    }
}