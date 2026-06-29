

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getAllClassLikeSymbolsByClassIdOrSingle
import org.cangnova.cangjie.cfir.CfirNameConflictsTracker
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * Analysis API 模式下的 classifier 名称冲突查询器。
 *
 * 与主编译器注册式 tracker 不同，low-level API 按需从当前 session 的 symbol provider 查询同名 class-like symbol。
 */
internal class LLNameConflictsTracker(
    /**
     * 当前查询器所属的低阶 CFIR session。
     */
    private val session: LLCfirSession
) : CfirNameConflictsTracker() {
    /**
     * low-level API 返回的 classifier 重声明记录。
     */
    private data class LLClassifierRedeclaration(
        /**
         * 参与重声明冲突的 classifier symbol。
         */
        override val classifierSymbol: CfirClassLikeSymbol<*>
    ) : ClassifierRedeclaration() {
        // Grabbing the containing file via the symbol provider is non-trivial. Specifying it is optional, and it can later be retrieved
        // separately.
        /**
         * low-level 查询路径不在重声明记录中保存容器文件。
         */
        override val containingFile: CfirFile? get() = null
    }

    /**
     * 返回 [classId] 对应的 classifier 重声明集合。
     *
     * 只有同一 class id 下找到至少两个 CFIR classifier symbol 时才产生重声明记录。
     */
    override fun getClassifierRedeclarations(classId: ClassId): Collection<ClassifierRedeclaration> {
        // As noted in the KDoc of `getClassifierRedeclarations`, Java redeclarations should not be returned by this component. As such,
        // we limit the scope to Kotlin symbols by taking the CFIR provider's symbol provider.
        val symbolProvider = session.cfirProvider.symbolProvider

        // While redeclarations are rare, we don't know whether a given class is redeclared somewhere else in the whole module. So this
        // function will be called once for each top-level classifier. Still, we don't cache the result since checkers are only run on
        // specific files, not all files visited by lazy resolution.
        return symbolProvider
            .getAllClassLikeSymbolsByClassIdOrSingle(classId)
            .takeIf { it.size >= 2 }
            .orEmpty()
            .map { LLClassifierRedeclaration(it) }
    }

    /**
     * Analysis API 不使用注册式重声明收集。
     *
     * 注册式 tracker 需要提前分析整个模块；这里保持空实现，所有查询都走 [getClassifierRedeclarations] 的按需 provider 路径。
     */
    override fun registerClassifierRedeclaration(
        classId: ClassId,
        newSymbol: CfirClassLikeSymbol<*>,
        newSymbolFile: CfirFile,
        prevSymbol: CfirClassLikeSymbol<*>,
        prevSymbolFile: CfirFile?,
    ) {
        // In the Analysis API, classifier redeclarations are resolved from symbol providers, so we don't need to register them here.
        // Registration-based conflict trackers require the whole module to be analyzed, which is not performant in the Analysis API.
    }
}
