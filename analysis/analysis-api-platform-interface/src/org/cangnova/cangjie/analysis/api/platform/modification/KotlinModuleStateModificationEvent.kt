package org.cangnova.cangjie.analysis.api.platform.modification

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import java.util.Objects

/**
 * This event signals that [module]'s settings or structure are changing.
 */
@CaPlatformInterface
class KotlinModuleStateModificationEvent(
    val module: CaModule,
    val modificationKind: KotlinModuleStateModificationKind,
) : KotlinModificationEvent {
    override fun equals(other: Any?): Boolean =
        this === other ||
                other is KotlinModuleStateModificationEvent && module == other.module && modificationKind == other.modificationKind

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
