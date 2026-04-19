package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.transformers.CfirExtensionsResolveTransformer
import org.cangnova.cangjie.cfir.session.cfirProvider
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
    private val transformer = CfirExtensionsResolveTransformer(resolveTargetSession)

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        val files = (runCatching { resolveTargetSession.cfirProvider }.getOrNull() as? CfirProviderImpl)?.getAllFiles().orEmpty()
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
