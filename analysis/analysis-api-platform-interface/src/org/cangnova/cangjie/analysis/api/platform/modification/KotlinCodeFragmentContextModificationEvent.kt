package org.cangnova.cangjie.analysis.api.platform.modification

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * This event signals that the context of any code fragment depending on [module] is changing.
 */
@CaPlatformInterface
class KotlinCodeFragmentContextModificationEvent(val module: CaModule) : KotlinModificationEvent {
    override fun equals(other: Any?): Boolean =
        this === other || other is KotlinCodeFragmentContextModificationEvent && module == other.module

    override fun hashCode(): Int = module.hashCode()
}
