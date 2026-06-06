

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import org.cangnova.cangjie.analysis.api.CaIdeApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.resolution.CaResolutionActivityTracker

/**
 * The service use site guarantees that all [beforeLazyResolve] and [afterLazyResolve] calls are paired,
 * so for each [beforeLazyResolve] call where will be the following [afterLazyResolve]. **Nested calls are allowed**.
 */
@OptIn(CaIdeApi::class, CaPlatformInterface::class)
internal class LLCfirResolutionActivityTracker : CaResolutionActivityTracker {
    private val blockCounter = ThreadLocal.withInitial { BlockCounter() }

    fun beforeLazyResolve() {
        blockCounter.get().enter()
    }

    fun afterLazyResolve() {
        blockCounter.get().exit()
    }

    override val isCangJieResolutionActive: Boolean
        get() = blockCounter.get().isInside

    private class BlockCounter {
        private var count = 0

        fun enter() {
            ++count
        }

        fun exit() {
            --count
        }

        /**
         * The service guarantees that all [beforeLazyResolve] and [afterLazyResolve]
         * are paired, so 0 means there is no resolver on the stack, and more than one means ongoing resolution.
         */
        val isInside: Boolean get() = count > 0
    }

    companion object {
        fun getInstance(): LLCfirResolutionActivityTracker {
            return ApplicationManager.getApplication().service<CaResolutionActivityTracker>() as LLCfirResolutionActivityTracker
        }
    }
}
