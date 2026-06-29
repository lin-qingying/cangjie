

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
    /**
     * 当前线程的 lazy resolve 嵌套计数器。
     *
     * 解析活动状态只描述当前线程是否处于解析调用栈中，因此不能使用工程级共享计数。
     */
    private val blockCounter = ThreadLocal.withInitial { BlockCounter() }

    /**
     * 标记当前线程进入一次 lazy resolve。
     */
    fun beforeLazyResolve() {
        blockCounter.get().enter()
    }

    /**
     * 标记当前线程离开一次 lazy resolve。
     */
    fun afterLazyResolve() {
        blockCounter.get().exit()
    }

    /**
     * 当前线程是否处于仓颉 lazy resolve 活动中。
     */
    override val isCangJieResolutionActive: Boolean
        get() = blockCounter.get().isInside

    /**
     * 记录单个线程内 lazy resolve 嵌套深度的轻量计数器。
     */
    private class BlockCounter {
        /**
         * 当前线程尚未退出的 lazy resolve 调用数量。
         */
        private var count = 0

        /**
         * 进入一层 lazy resolve。
         */
        fun enter() {
            ++count
        }

        /**
         * 离开一层 lazy resolve。
         */
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
        /**
         * 从应用级服务中取得 low-level CFIR 的解析活动 tracker。
         */
        fun getInstance(): LLCfirResolutionActivityTracker {
            return ApplicationManager.getApplication().service<CaResolutionActivityTracker>() as LLCfirResolutionActivityTracker
        }
    }
}
