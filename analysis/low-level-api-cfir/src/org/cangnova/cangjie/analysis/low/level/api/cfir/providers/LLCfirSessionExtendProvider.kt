package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.materializeTopLevelExtendFiles
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
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
    private val session: LLCfirResolvableModuleSession,
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendProvider {
    private val delegate = CfirSessionExtendProvider(session, indexStore)

    @Volatile
    private var indexedFiles: Set<CfirFile> = emptySet()

    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsForClass(classId)
    }

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsInPackage(packageFqName)
    }

    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsForBuiltinType(kind)
    }

    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? {
        ensureIndexIsFresh()
        return delegate.getContainingExtend(symbol)
    }

    override fun isExtendAccessible(extend: CfirExtend): Boolean {
        ensureIndexIsFresh()
        return delegate.isExtendAccessible(extend)
    }

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

    private fun resolveTopLevelExtendsToTypes(file: CfirFile) {
        for (declaration in file.declarations) {
            if (declaration is CfirExtend) {
                declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
            }
        }
    }
}
