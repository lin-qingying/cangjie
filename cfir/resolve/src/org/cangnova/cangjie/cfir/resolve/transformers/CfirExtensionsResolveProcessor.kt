package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendIndexStoreOrNull
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/** EXTENSIONS 阶段处理器，负责在阶段开始前重建 extend 索引。 */
internal class CfirExtensionsResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.EXTENSIONS,
) {
    /** EXTENSIONS 阶段使用的 transformer。 */
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
    /** 当前 CFIR session。 */
    override val session: CfirSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.EXTENSIONS) {
    /** 对已完成 STATUS 且尚未进入 EXTENSIONS 的声明推进阶段。 */
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.STATUS || declaration.resolvePhase >= CfirResolvePhase.EXTENSIONS) {
            return declaration
        }

        declaration.replaceResolvePhase(CfirResolvePhase.EXTENSIONS)
        if (declaration is CfirExtend) {
            propagateExtendedReceiverType(declaration)
        }
        return declaration
    }

    /**
     * 把被扩展类型写进 extend 成员的 dispatch receiver。
     *
     * 到 EXTENSIONS 阶段时 `extendedTypeRef` 已完成 TYPES 解析，可以把 `extend Foo` 的 `Foo`
     * 作为成员接收者。源码侧与库侧（反序列化）因此共享同一套成员语义，
     * 符号层才能区分不同 extend 上的同名成员。
     */
    private fun propagateExtendedReceiverType(extend: CfirExtend) {
        val receiverType = extend.extendedTypeRef.coneTypeOrNull as? ConeSimpleCangJieType ?: return
        for (member in extend.declarations) {
            val callable = member as? CfirCallableDeclaration ?: continue
            if (callable.status.isStatic) continue
            callable.replaceDispatchReceiverType(receiverType)
        }
    }
}
