

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.BuiltinTypes
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

abstract class LLCfirResolvableModuleSession(
    ktModule: CaModule,
    builtinTypes: BuiltinTypes
) : LLCfirModuleSession(ktModule, builtinTypes, Kind.Source) {
    internal abstract val moduleComponents: LLCfirModuleResolveComponents

    final override fun getScopeSession(): ScopeSession {
        return moduleComponents.scopeSessionProvider.getScopeSession()
    }
}

internal val CfirElementWithResolveState.llCfirResolvableSession: LLCfirResolvableModuleSession?
    get() = llCfirSession as? LLCfirResolvableModuleSession

internal val CfirBasedSymbol<*>.llCfirResolvableSession: LLCfirResolvableModuleSession?
    get() = fir.llCfirResolvableSession