package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.lazyDeclarationResolver
import org.cangnova.cangjie.cfir.visitors.CfirTransformer

/**
 * 标记仅允许解析处理器内部使用的适配层 API。
 *
 * 这类 API 通常绕过普通访问路径，直接驱动阶段转换或文件替换，外部代码不应依赖它形成稳定契约。
 */
@RequiresOptIn(message = "Should be used just only in resolve processor")
annotation class AdapterForResolveProcessor

/**
 * CFIR 解析流水线中单个阶段处理器的共同基类。
 *
 * 处理器持有会话、作用域会话和可选解析阶段，并负责在阶段前后通知懒声明解析器维护阶段边界。
 */
sealed class CfirResolveProcessor(
    /**
     * 当前解析流水线使用的 CFIR 会话。
     */
    override val session: CfirSession,
    /**
     * 当前解析阶段共享的作用域会话。
     */
    override val scopeSession: ScopeSession,
    /**
     * 当前处理器负责推进的解析阶段；为空表示该处理器只做辅助转换。
     */
    val phase: CfirResolvePhase?,
) : SessionAndScopeSessionHolder {
    /**
     * 进入阶段前通知懒声明解析器开始解析。
     */
    open fun beforePhase() {
        if (phase != null) {
            session.lazyDeclarationResolver.startResolvingPhase(phase)
        }
    }

    /**
     * 阶段结束后通知懒声明解析器完成解析。
     */
    open fun afterPhase() {
        if (phase != null) {
            session.lazyDeclarationResolver.finishResolvingPhase(phase)
        }
    }
}

/**
 * 以整个文件集合为输入的全局解析处理器。
 *
 * 适用于需要跨文件收集信息或一次性建立全局索引的阶段。
 */
abstract class CfirGlobalResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
    phase: CfirResolvePhase,
) : CfirResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = phase,
) {
    /**
     * 处理当前模块参与解析的文件集合。
     */
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

/**
 * 基于 [CfirTransformer] 的逐文件解析处理器。
 *
 * 适用于阶段转换只需要遍历并原地更新单个文件树的解析步骤。
 */
abstract class CfirTransformerBasedResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
    phase: CfirResolvePhase?,
) : CfirResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = phase,
) {
    /**
     * 实际执行 CFIR 树遍历与转换的 transformer。
     */
    abstract val transformer: CfirTransformer<Nothing?>

    /**
     * 使用 [transformer] 处理单个文件。
     */
    open fun processFile(file: CfirFile) {
        file.accept(transformer, null)
    }
}
