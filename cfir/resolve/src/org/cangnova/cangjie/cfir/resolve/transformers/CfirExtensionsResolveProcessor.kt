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

/** EXTENSIONS 阶段处理器，负责在阶段开始前重建 extend 索引。 */
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

    /** EXTENSIONS 阶段前基于当前 provider 文件集合重建 session 级 extend 索引。 */
    override fun beforePhase() {
        super.beforePhase()

        val files = (runCatching { session.cfirProvider }.getOrNull() as? CfirProviderImpl)?.getAllFiles().orEmpty()
        if (files.isEmpty()) return

        session.extendIndexStoreOrNull?.rebuild(files, session.typeResolver)
    }
}

/** EXTENSIONS 阶段 transformer，负责推进声明 resolve phase。 */
internal class CfirExtensionsResolveTransformer(
    override val session: CfirSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.EXTENSIONS) {
    /** 对已完成 STATUS 且尚未进入 EXTENSIONS 的声明推进阶段。 */
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.STATUS || declaration.resolvePhase >= CfirResolvePhase.EXTENSIONS) {
            return declaration
        }

        declaration.replaceResolvePhase(CfirResolvePhase.EXTENSIONS)
        return declaration
    }
}
