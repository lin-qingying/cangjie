package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

@RequiresOptIn(message = "Should be used just only in resolve processor")
annotation class AdapterForResolveProcessor

sealed class CfirResolveProcessor(
    override val session: CfirSession,
    override val scopeSession: ScopeSession,
    val phase: CfirResolvePhase?,
) : SessionAndScopeSessionHolder {
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
    scopeSession: ScopeSession,
    phase: CfirResolvePhase,
) : CfirResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = phase,
) {
    abstract fun process(files: Collection<CfirFile>)
}

/**
 * 文件替换型解析处理器。
 *
 * 与就地转换型处理器不同，该处理器可以替换整个 [CfirFile] 列表。
 * 典型场景：宏展开阶段需要重建包含宏调用的文件。
 */
abstract class CfirFileReplacingResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
    phase: CfirResolvePhase,
) : CfirResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = phase,
) {
    /**
     * 处理文件列表，返回可能被替换的新文件列表。
     *
     * 无需替换时应返回原列表。
     */
    abstract fun processAndReplace(files: List<CfirFile>): List<CfirFile>
}

abstract class CfirTransformerBasedResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
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

