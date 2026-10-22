

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ModificationTracker
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

    /** 已发布的完整宏语义快照；尚未产生可记录的完整 construction 批次时为空。 */
    val macroSemanticSnapshot: LLCfirMacroSemanticSnapshot?
        get() = moduleComponents.macroSemanticSnapshotStore.snapshot

    /**
     * 宏快照发布版本，用于使依赖当前模块语义视图的 Analysis API 缓存失效。
     */
    val macroSemanticSnapshotModificationTracker: ModificationTracker
        get() = moduleComponents.macroSemanticSnapshotStore

    /**
     * 基于同一 pre-macro 输入批次，在不发布的情况下预构建语义快照索引。
     *
     * 输入是本次构造独占的 CFIR 树，索引构建不依赖实时 PSI 查询；调用方应在后台、读写访问之外执行，
     * 再把返回快照交给 [publishMacroSemanticSnapshot]。完整批次可以带有 typed error placeholder，
     * 但仍必须保留每个输入源文件的顺序与身份。
     */
    @OptIn(CaExperimentalApi::class)
    fun prepareMacroSemanticSnapshot(
        pre: PreMacroRawBuildResult,
        result: MacroConstructionResult.Recordable,
    ): LLCfirMacroSemanticSnapshot? {
        val sourceFiles = sourceFilesFor(pre, result) ?: return null
        if (moduleComponents.macroSemanticSnapshotStore.snapshot != null) return null
        return moduleComponents.macroSemanticSnapshotStore.prepareSnapshot(
            sourceFiles = sourceFiles,
            files = result.recordableFiles.files,
            registry = result.registry,
        )
    }

    /** 保留已有 Success 调用方的 JVM 方法签名，同时复用通用的完整批次路径。 */
    @OptIn(CaExperimentalApi::class)
    fun prepareMacroSemanticSnapshot(
        pre: PreMacroRawBuildResult,
        result: MacroConstructionResult.Success,
    ): LLCfirMacroSemanticSnapshot? {
        val recordableResult: MacroConstructionResult.Recordable = result
        return prepareMacroSemanticSnapshot(pre, recordableResult)
    }

    /**
     * 在 IDE 写访问屏障内切换宏语义视图，并使旧 raw CFIR 与 PSI 结构映射同时退役。
     *
     * 写访问保证已经开始的 Analysis API 读操作先完成，后续读操作则只能观察新快照和新 raw 文件缓存，
     * 不会在同一解析过程中混用两个 CFIR 视图。快照索引应提前由 [prepareMacroSemanticSnapshot] 构造。
     */
    @OptIn(CaExperimentalApi::class)
    fun publishMacroSemanticSnapshot(
        pre: PreMacroRawBuildResult,
        result: MacroConstructionResult.Recordable,
        preparedSnapshot: LLCfirMacroSemanticSnapshot,
    ): Boolean {
        check(ApplicationManager.getApplication().isWriteAccessAllowed) {
            "A macro semantic snapshot must be published under an IntelliJ write action."
        }
        val sourceFiles = sourceFilesFor(pre, result) ?: return false
        if (moduleComponents.macroSemanticSnapshotStore.snapshot != null) return false
        if (preparedSnapshot.registry !== result.registry) return false
        if (preparedSnapshot.files.size != result.recordableFiles.files.size) return false
        if (sourceFiles.indices.any { index ->
                preparedSnapshot.fileForSource(sourceFiles[index]) !== result.recordableFiles.files[index]
            }
        ) return false

        // 宏 fragment PSI 保存在非物理 CjMacroCallFile 中。为它们设置源模块上下文，
        // 使后续 reference resolve 与已发布快照使用同一模块语义，而不是落入无上下文 dangling session。
        result.registry.generatedSources
            .mapNotNull { generatedSource -> generatedSource.psiOrNull?.containingFile as? CjFile }
            .distinct()
            .forEach { generatedFile ->
                check(generatedFile.contextModule == null || generatedFile.contextModule == caModule) {
                    "A macro-generated PSI file cannot belong to multiple Analysis API modules."
                }
                generatedFile.contextModule = caModule
            }

        val published = moduleComponents.macroSemanticSnapshotStore.publish(preparedSnapshot)
        if (published) {
            // 写访问屏障阻止分析读者跨过此视图切换；释放屏障前同时淘汰所有旧语义缓存。
            moduleComponents.cache.invalidateRawCfirFiles()
            moduleComponents.fileStructureCache.clear()
        }
        return published
    }

    /** 保留已有 Success 调用方的 JVM 方法签名，同时复用通用的完整批次路径。 */
    @OptIn(CaExperimentalApi::class)
    fun publishMacroSemanticSnapshot(
        pre: PreMacroRawBuildResult,
        result: MacroConstructionResult.Success,
        preparedSnapshot: LLCfirMacroSemanticSnapshot,
    ): Boolean {
        val recordableResult: MacroConstructionResult.Recordable = result
        return publishMacroSemanticSnapshot(pre, recordableResult, preparedSnapshot)
    }

    /** 校验构造批次和源文件身份，避免发布另一个会话或另一次展开的结果。 */
    private fun sourceFilesFor(
        pre: PreMacroRawBuildResult,
        result: MacroConstructionResult.Recordable,
    ): List<CjFile>? {
        if (pre.session !== this || result.recordableFiles.session !== this) return null
        if (pre.constructionIdentity !== result.recordableFiles.constructionIdentity) return null
        if (pre.files.size != result.recordableFiles.files.size) return null

        val sourceFiles = result.recordableFiles.sourceFileIdentities.map { identity ->
            identity as? CjFile ?: return null
        }
        if (sourceFiles.size != result.recordableFiles.files.size) return null
        if (pre.files.indices.any { index -> pre.files[index].sourceFileIdentity !== sourceFiles[index] }) return null
        return sourceFiles
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
