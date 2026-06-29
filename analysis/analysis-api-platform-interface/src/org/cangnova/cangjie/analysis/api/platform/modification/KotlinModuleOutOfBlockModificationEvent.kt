package org.cangnova.cangjie.analysis.api.platform.modification

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * This event signals that an [out-of-block modification][CaSourceModificationLocality.OutOfBlock] is occurring in the sources of [module].
 */
@CaPlatformInterface
class KotlinModuleOutOfBlockModificationEvent(val module: CaModule) : KotlinModificationEvent {
    /**
     * 按模块比较事件相等性。
     */
    override fun equals(other: Any?): Boolean =
        this === other || other is KotlinModuleOutOfBlockModificationEvent && module == other.module

    /**
     * 基于模块计算哈希值。
     */
    override fun hashCode(): Int = module.hashCode()
}
