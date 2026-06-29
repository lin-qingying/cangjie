package org.cangnova.cangjie.analysis.api.platform.modification

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import java.util.Objects

/**
 * This event signals that [module]'s settings or structure are changing.
 */
@CaPlatformInterface
class KotlinModuleStateModificationEvent(
    /**
     * 状态发生变化的模块。
     */
    val module: CaModule,
    /**
     * 模块状态变化的具体类型。
     */
    val modificationKind: KotlinModuleStateModificationKind,
) : KotlinModificationEvent {
    /**
     * 按模块和变化类型比较事件相等性。
     */
    override fun equals(other: Any?): Boolean =
        this === other ||
                other is KotlinModuleStateModificationEvent && module == other.module && modificationKind == other.modificationKind

    /**
     * 基于模块和变化类型计算哈希值。
     */
    override fun hashCode(): Int = Objects.hash(module, modificationKind)
}

/**
 * Describes the kind of module state modification affecting a [CaModule] in more detail.
 */
@CaPlatformInterface
enum class KotlinModuleStateModificationKind {
    UPDATE,
    REMOVAL,
}
