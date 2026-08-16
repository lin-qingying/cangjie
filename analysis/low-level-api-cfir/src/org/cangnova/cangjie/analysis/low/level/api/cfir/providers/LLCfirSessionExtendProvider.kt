package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.materializeTopLevelExtendFiles
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * Low-level source session 的 extend provider。
 *
 * 主编译器在 `CfirExtensionsResolveProcessor.beforePhase` 中用 `CfirProviderImpl.getAllFiles()`
 * 刷新 extend 索引；LL session 没有全量文件 provider，只能以当前 session 已构建的
 * `ModuleFileCache` 作为 lazy resolve 可见文件集合。这里在 provider 查询入口刷新同一份
 * [CfirExtendIndexStore]，然后复用主干 [CfirSessionExtendProvider] 的语义查询。
 */
internal class LLCfirSessionExtendProvider(
    /**
     * 当前源码可解析模块 session。
     */
    private val session: LLCfirResolvableModuleSession,

    /**
     * 保存 extend 索引的 session 级存储。
     */
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendProvider {
    /**
     * 复用主干 extend provider 的查询语义。
     */
    private val delegate = CfirSessionExtendProvider(session, indexStore)

    @Volatile
    /**
     * 已经写入 [indexStore] 的 CFIR 文件集合。
     */
    private var indexedFiles: Set<CfirFile> = emptySet()

    /**
     * 查询目标 key 对应的扩展声明。
     */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsForTarget(targetKey)
    }

    /**
     * 查询作用于指定 class id 的扩展声明。
     */
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsForClass(classId)
    }

    /**
     * 查询指定包中的扩展声明。
     */
    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsInPackage(packageFqName)
    }

    /**
     * 查询作用于内建基本类型的扩展声明。
     */
    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsForBuiltinType(kind)
    }

    /**
     * 查询 [symbol] 所属的扩展声明。
     */
    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? {
        ensureIndexIsFresh()
        return delegate.getContainingExtend(symbol)
    }

    /**
     * 查询 [extend] 所在包名。
     */
    override fun getPackageFqName(extend: CfirExtend): FqName? {
        ensureIndexIsFresh()
        return delegate.getPackageFqName(extend)
    }

    /**
     * 判断 [extend] 在当前 session 中是否可访问。
     */
    override fun isExtendAccessible(extend: CfirExtend): Boolean {
        ensureIndexIsFresh()
        return delegate.isExtendAccessible(extend)
    }

    /**
     * 确保当前 session 已缓存的顶层 extend 文件全部进入 [indexStore]。
     *
     * 方法先物化顶层 extend 文件，再比较缓存文件集合；集合发生变化时在同步块内解析 extend 头部类型并重建索引。
     */
    private fun ensureIndexIsFresh() {
        session.symbolProvider.materializeTopLevelExtendFiles()
        val files = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
        if (files.isEmpty()) return

        val fileSet = files.toSet()
        if (fileSet == indexedFiles) return

        synchronized(this) {
            val latestFiles = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
            val latestFileSet = latestFiles.toSet()
            if (latestFiles.isEmpty() || latestFileSet == indexedFiles) return

            // LL 索引消费与主编译器 EXTENSIONS 阶段保持一致：extend 头部必须先完成 TYPES。
            latestFiles.forEach(::resolveTopLevelExtendsToTypes)
            indexStore.rebuild(latestFiles, session.typeResolver)
            indexedFiles = latestFileSet
        }
    }

/**
     * 将 [file] 中的顶层 extend 声明推进到 [CfirResolvePhase.TYPES]。
     */
    private fun resolveTopLevelExtendsToTypes(file: CfirFile) {
        for (declaration in file.declarations) {
            if (declaration is CfirExtend) {
                declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
            }
        }
    }
}
