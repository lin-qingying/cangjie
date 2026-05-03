package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractTreeTransformer
import org.cangnova.cangjie.cfir.session.extendIndexStoreOrNull
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.visitors.transformSingle

/**
 * 对齐主干 `CfirExtensionsResolveProcessor`：
 * 先刷新 extend 索引，再把目标推进到 EXTENSIONS。
 */
internal object LLCfirExtensionsLazyResolver : LLCfirLazyResolver(CfirResolvePhase.EXTENSIONS) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirExtensionsTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) = Unit
}

private class LLCfirExtensionsTargetResolver(
    target: LLCfirResolveTarget,
) : LLCfirTargetResolver(target, CfirResolvePhase.EXTENSIONS) {
    private val transformer = LLCfirExtensionsResolveTransformer(resolveTargetSession)

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        val files = (resolveTargetSession as? LLCfirResolvableModuleSession)
            ?.moduleComponents
            ?.cache
            ?.getAllCachedCfirFilesForResolution()
            ?.toList()
            .orEmpty()
        if (files.isNotEmpty()) {
            resolveTargetSession.extendIndexStoreOrNull?.rebuild(files, resolveTargetSession.typeResolver)
        }
        return false
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        when (target) {
            is CfirFile -> target.transformSingle(transformer, null)
            is CfirDeclaration -> target.transformSingle(transformer, null)
        }
    }
}

/**
 * 与主干 `CfirExtensionsResolveTransformer` 同构的 low-level 本地实现。
 * 不跨模块引用主干 internal 类型，保持阶段推进语义一致。
 */
private class LLCfirExtensionsResolveTransformer(
    override val session: org.cangnova.cangjie.cfir.session.CfirSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.EXTENSIONS) {
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.STATUS || declaration.resolvePhase >= CfirResolvePhase.EXTENSIONS) {
            return declaration
        }

        declaration.replaceResolvePhase(CfirResolvePhase.EXTENSIONS)
        return declaration
    }
}
