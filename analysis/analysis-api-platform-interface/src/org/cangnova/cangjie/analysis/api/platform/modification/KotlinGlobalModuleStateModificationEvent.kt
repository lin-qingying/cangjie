package org.cangnova.cangjie.analysis.api.platform.modification

import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * This event signals that project settings or project structure are changing possibly globally.
 */
@CaPlatformInterface
data object KotlinGlobalModuleStateModificationEvent : KotlinModificationEvent
