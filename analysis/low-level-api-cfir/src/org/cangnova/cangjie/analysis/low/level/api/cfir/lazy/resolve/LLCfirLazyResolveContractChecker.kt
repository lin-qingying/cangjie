/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.isItAllowedToCallLazyResolveTo

/**
 * 检查 lazy resolve 调用是否满足 [CfirResolvePhase] 的阶段契约。
 *
 * 对齐 Kotlin `LLFirLazyResolveContractChecker`：
 * 当前线程进入某个 lazy resolve 阶段后，后续嵌套请求只能落在该阶段允许访问的范围内。
 */
internal class LLCfirLazyResolveContractChecker {
    private val currentTransformerPhase = ThreadLocal.withInitial<CfirResolvePhase?> { null }

    inline fun lazyResolveToPhaseInside(phase: CfirResolvePhase, resolve: () -> Unit) {
        checkIfCanLazyResolveToPhase(phase)

        val previousPhase = currentTransformerPhase.get()
        currentTransformerPhase.set(phase)
        try {
            resolve()
        } finally {
            currentTransformerPhase.set(previousPhase)
        }
    }

    private fun checkIfCanLazyResolveToPhase(requestedPhase: CfirResolvePhase) {
        val currentPhase = currentTransformerPhase.get() ?: return

        if (!currentPhase.isItAllowedToCallLazyResolveTo(requestedPhase)) {
            val exception = CfirLazyResolveContractViolationException(
                currentPhase = currentPhase,
                requestedPhase = requestedPhase,
            )
            if (System.getProperty("cangjie.suppress.lazy.resolve.contract.violation") != null) {
                LoggerHolder.LOG.warn(exception)
            } else {
                throw exception
            }
        }
    }

    private object LoggerHolder {
        val LOG = Logger.getInstance(LLCfirLazyResolveContractChecker::class.java)
    }
}

internal class CfirLazyResolveContractViolationException(
    val currentPhase: CfirResolvePhase,
    val requestedPhase: CfirResolvePhase,
) : IllegalStateException("Lazy resolve contract violated: current=$currentPhase requested=$requestedPhase")
