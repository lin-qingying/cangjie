package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.materializeTopLevelExtendFiles
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedExtendProvider
import org.cangnova.cangjie.cfir.serialization.provider.flattenDeserializedProviders
import org.cangnova.cangjie.cfir.session.extendIndexStore
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

/**
 * 为低阶源码类 session 创建组合 extend provider。
 *
 * 对齐主编译器 `CfirAbstractSessionFactory.combineExtendProviders`：源码 `CfirExtendIndexStore`
 * 只覆盖当前 session 缓存文件中的 extend，二进制依赖（std.core 等 .cjo 包）的 extend 声明
 * 必须来自反序列化 provider。LL 侧原先只注册单独的 [LLCfirSessionExtendProvider]，导致依赖库
 * 中的 extend 完全不可见：例如 `extend Int64 <: Hashable` 不成立，公共超类型交集退化为 `Any`。
 *
 * [dependencyProvidersRef] 惰性捕获依赖符号 provider 集合：extend provider 注册早于依赖
 * provider 创建，因此只能在首次 extend 查询时求值，避免 eager session 创建。
 */
internal fun LLCfirResolvableModuleSession.createLLCfirExtendProvider(
    dependencyProvidersRef: () -> List<CfirSymbolProvider>,
): CfirExtendProvider {
    val ownProvider = LLCfirSessionExtendProvider(this, extendIndexStore)
    val deserializedProvider = LLDeserializedExtendProviderProxy(dependencyProvidersRef)
    return CfirCompositeExtendProvider(listOf(ownProvider, deserializedProvider))
}

/**
 * 惰性代理反序列化 extend provider 的包装器。
 *
 * 首次查询时才从依赖符号 provider 中提取反序列化 provider 并构造
 * [CfirDeserializedExtendProvider]；依赖中没有此类 provider 时所有查询返回空结果。
 */
private class LLDeserializedExtendProviderProxy(
    private val dependencyProvidersRef: () -> List<CfirSymbolProvider>,
) : CfirExtendProvider {
    /**
     * 延迟构造的反序列化 extend provider，仅在依赖中存在反序列化 provider 时创建。
     */
    private val delegate: CfirDeserializedExtendProvider? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        dependencyProvidersRef()
            .flatMap(CfirSymbolProvider::flattenDeserializedProviders)
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.let(::CfirDeserializedExtendProvider)
    }

    /** 查询目标 key 对应的库扩展声明。 */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> =
        delegate?.getExtendsForTarget(targetKey).orEmpty()

    /** 查询作用于指定 class id 的库扩展声明。 */
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
        delegate?.getExtendsForClass(classId).orEmpty()

    /** 查询库中指定包内的扩展声明。 */
    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
        delegate?.getExtendsInPackage(packageFqName).orEmpty()

    /** 查询作用于内建基本类型的库扩展声明。 */
    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> =
        delegate?.getExtendsForBuiltinType(kind).orEmpty()

    /** 查询库中 [symbol] 所属的扩展声明。 */
    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? =
        delegate?.getContainingExtend(symbol)

    /** 查询库中 [extend] 所在包名。 */
    override fun getPackageFqName(extend: CfirExtend): FqName? =
        delegate?.getPackageFqName(extend)

    /** 库 extend 默认视为可访问（导出面由反序列化 provider 自身维护）。 */
    override fun isExtendAccessible(extend: CfirExtend): Boolean =
        delegate?.isExtendAccessible(extend) ?: true
}
