package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirSessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

@RequiresOptIn(message = "Should be used just only in resolve processor")
annotation class AdapterForResolveProcessor

sealed class CfirResolveProcessor(
    override val session: CfirSession,
    override val scopeSession: CfirScopeSession,
    val phase: CfirResolvePhase?,
) : CfirSessionAndScopeSessionHolder {
    open fun beforePhase() {
        if (phase != null) {
            session.lazyDeclarationResolver.startResolvingPhase(phase)
        }
    }

    open fun afterPhase() {
        if (phase != null) {
            session.lazyDeclarationResolver.finishResolvingPhase(phase)
        }
    }
}

abstract class CfirGlobalResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
    phase: CfirResolvePhase,
) : CfirResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = phase,
) {
    abstract fun process(files: Collection<CfirFile>)
}

abstract class CfirTransformerBasedResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
    phase: CfirResolvePhase?,
) : CfirResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = phase,
) {
    abstract val transformer: CfirTransformer<Nothing?>

    open fun processFile(file: CfirFile) {
        file.accept(transformer, null)
    }
}
