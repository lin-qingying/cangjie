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
    /**
     * 为 [target] 创建 EXTENSIONS 阶段目标解析器。
     */
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirExtensionsTargetResolver(target)

    /**
     * EXTENSIONS 阶段只需要确认相位推进，不需要额外结构校验。
     */
    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) = Unit
}

/**
 * EXTENSIONS 阶段的目标解析器。
 *
 * 解析前会在可解析模块会话中基于缓存 CFIR 文件重建 extend 索引，然后在目标声明或文件上执行本地 transformer。
 */
private class LLCfirExtensionsTargetResolver(
    target: LLCfirResolveTarget,
) : LLCfirTargetResolver(target, CfirResolvePhase.EXTENSIONS) {
    /**
     * 推进 EXTENSIONS 阶段的本地 transformer。
     */
    private val transformer = LLCfirExtensionsResolveTransformer(resolveTargetSession)

    /**
     * 在加目标锁前刷新当前会话的 extend 索引。
     */
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

    /**
     * 在目标锁内把文件或声明推进到 EXTENSIONS。
     */
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
    /**
     * 当前 EXTENSIONS 阶段使用的 CFIR 会话。
     */
    override val session: org.cangnova.cangjie.cfir.session.CfirSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.EXTENSIONS) {
    /**
     * 如果 [declaration] 已完成 STATUS 且尚未进入 EXTENSIONS，则推进其解析阶段。
     */
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.STATUS || declaration.resolvePhase >= CfirResolvePhase.EXTENSIONS) {
            return declaration
        }

        declaration.replaceResolvePhase(CfirResolvePhase.EXTENSIONS)
        return declaration
    }
}
