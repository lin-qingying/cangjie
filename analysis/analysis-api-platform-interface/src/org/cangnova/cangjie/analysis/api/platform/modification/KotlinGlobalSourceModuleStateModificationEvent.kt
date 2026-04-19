package org.cangnova.cangjie.analysis.api.platform.modification

import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * [KotlinGlobalSourceModuleStateModificationEvent] signals that source module settings or structure are changing possibly globally.
 */
@CaPlatformInterface
data object KotlinGlobalSourceModuleStateModificationEvent : KotlinModificationEvent
