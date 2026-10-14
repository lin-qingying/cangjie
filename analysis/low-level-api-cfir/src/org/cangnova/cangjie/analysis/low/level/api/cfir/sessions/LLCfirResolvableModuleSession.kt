

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.projectStructure.contextModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.macro.LLCfirMacroSemanticSnapshot
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.source.psiOrNull

/**
 * 具备源码或库源码 lazy resolve 能力的模块 session 基类。
 *
 * 可解析 session 持有 [LLCfirModuleResolveComponents]，并通过其中的 scope session provider 获取作用域会话。
 */
abstract class LLCfirResolvableModuleSession(
    caModule: CaModule,
    builtinTypes: CfirBuiltinTypes
) : LLCfirModuleSession(caModule, builtinTypes, Kind.Source) {
    /**
     * 当前可解析模块 session 的解析组件集合。
     */
    internal abstract val moduleComponents: LLCfirModuleResolveComponents

    /**
     * 返回模块解析组件维护的 scope session。
     */
    final override fun getScopeSession(): ScopeSession {
        return moduleComponents.scopeSessionProvider.getScopeSession()
    }

    /** 已发布的完整宏语义快照；构造尚未成功时为空。 */
    val macroSemanticSnapshot: LLCfirMacroSemanticSnapshot?
        get() = moduleComponents.macroSemanticSnapshotStore.snapshot

    /**
     * 只接受由同一 pre-macro 输入批次产生的成功构造结果，并把所有文件作为一个语义快照发布。
     *
     * degraded/failed 结果不会替换正常 LL-CFIR 视图，保证构造失败时查询继续使用展开前语义。
     */
    @OptIn(CaExperimentalApi::class)
    fun publishMacroSemanticSnapshot(
        pre: PreMacroRawBuildResult,
        result: MacroConstructionResult.Success,
    ): Boolean {
        if (pre.session !== this || result.recordableFiles.session !== this) return false
        if (pre.constructionIdentity !== result.recordableFiles.constructionIdentity) return false
        if (pre.files.size != result.recordableFiles.files.size) return false

        val sourceFiles = result.recordableFiles.sourceFileIdentities.map { identity ->
            identity as? CjFile ?: return false
        }
        if (sourceFiles.size != result.recordableFiles.files.size) return false
        if (pre.files.indices.any { index -> pre.files[index].sourceFileIdentity !== sourceFiles[index] }) return false

        // 宏 fragment PSI 保存在非物理 CjMacroCallFile 中。为它们设置源模块上下文，
        // 使后续 reference resolve 与已发布快照使用同一模块语义，而不是落入无上下文 dangling session。
        result.registry.generatedSourceOriginById.keys
            .mapNotNull { generatedSource -> generatedSource.psiOrNull?.containingFile as? CjFile }
            .distinct()
            .forEach { generatedFile ->
                check(generatedFile.contextModule == null || generatedFile.contextModule == caModule) {
                    "A macro-generated PSI file cannot belong to multiple Analysis API modules."
                }
                generatedFile.contextModule = caModule
            }

        val published = moduleComponents.macroSemanticSnapshotStore.publish(
            sourceFiles = sourceFiles,
            recordableFiles = result.recordableFiles,
            registry = result.registry,
        )
        if (published) moduleComponents.fileStructureCache.clear()
        return published
    }
}

/**
 * 从带解析状态的 CFIR 元素取得其所属的可解析 low-level session。
 */
internal val CfirElementWithResolveState.llCfirResolvableSession: LLCfirResolvableModuleSession?
    get() = llCfirSession as? LLCfirResolvableModuleSession

/**
 * 从 CFIR symbol 取得其声明所属的可解析 low-level session。
 */
internal val CfirBasedSymbol<*>.llCfirResolvableSession: LLCfirResolvableModuleSession?
    get() = cfir.llCfirResolvableSession
