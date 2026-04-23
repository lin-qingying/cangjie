package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner

public sealed interface CaCallResolutionAttempt : CaLifetimeOwner

public sealed interface CaSymbolResolutionAttempt : CaLifetimeOwner
