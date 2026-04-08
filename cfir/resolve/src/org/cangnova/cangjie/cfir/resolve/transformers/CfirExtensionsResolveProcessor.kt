package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendIndexStoreOrNull
import org.cangnova.cangjie.cfir.session.typeResolver

internal class CfirExtensionsResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.EXTENSIONS,
) {
    override val transformer: CfirExtensionsResolveTransformer =
        CfirExtensionsResolveTransformer(session)

    override fun beforePhase() {
        super.beforePhase()

        val files = (runCatching { session.cfirProvider }.getOrNull() as? CfirProviderImpl)?.getAllFiles().orEmpty()
        if (files.isEmpty()) return

        session.extendIndexStoreOrNull?.rebuild(files, session.typeResolver)
    }
}

internal class CfirExtensionsResolveTransformer(
    override val session: CfirSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.EXTENSIONS) {
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.STATUS || declaration.resolvePhase >= CfirResolvePhase.EXTENSIONS) {
            return declaration
        }

        declaration.replaceResolvePhase(CfirResolvePhase.EXTENSIONS)
        return declaration
    }
}
