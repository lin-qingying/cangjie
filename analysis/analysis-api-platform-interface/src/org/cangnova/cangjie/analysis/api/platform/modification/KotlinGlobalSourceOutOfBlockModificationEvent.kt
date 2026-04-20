package org.cangnova.cangjie.analysis.api.platform.modification

import org.cangnova.cangjie.analysis.api.CaPlatformInterface

/**
 * [KotlinGlobalSourceOutOfBlockModificationEvent] signals that [out-of-block modification][CaSourceModificationLocality.OutOfBlock] is
 * occurring possibly globally.
 */
@CaPlatformInterface
data object KotlinGlobalSourceOutOfBlockModificationEvent : KotlinModificationEvent
