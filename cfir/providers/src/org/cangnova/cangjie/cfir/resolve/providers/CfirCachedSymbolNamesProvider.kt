package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.caches.cfirCachesFactory
import org.cangnova.cangjie.cfir.caches.getValue
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.flatMapToNullableSet

/**
 * 对齐 Kotlin FIR `FirCachedSymbolNamesProvider` 的缓存包装。
 */
abstract class CfirCachedSymbolNamesProvider(protected val session: CfirSession) : CfirSymbolNamesProvider() {
    /**
     * 计算当前 provider 可见的包名集合。
     */
    abstract fun computePackageNames(): Set<String>?

    /**
     * 计算包含顶层 classifier 的包名集合。
     */
    abstract fun computePackageNamesWithTopLevelClassifiers(): Set<String>?

    /**
     * 计算指定包内可能存在的顶层 classifier 名称集合。
     */
    abstract fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>?

    /**
     * 计算包含顶层 callable 的包名集合。
     */
    abstract fun computePackageNamesWithTopLevelCallables(): Set<String>?

    /**
     * 计算指定包内可能存在的顶层 callable 名称集合。
     */
    abstract fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>?

    /**
     * 懒加载的包名缓存。
     */
    private val cachedPackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computePackageNames()
    }

    /**
     * 顶层 classifier 包名缓存。
     *
     * 若子类没有专门计算，则复用通用包名缓存。
     */
    private val topLevelClassifierPackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (hasSpecificClassifierPackageNamesComputation) {
            computePackageNamesWithTopLevelClassifiers()?.let { return@lazy it }
        }
        cachedPackageNames
    }

    /**
     * 按包名缓存顶层 classifier 名称集合。
     */
    private val topLevelClassifierNamesByPackage = session.cfirCachesFactory.createPossiblySoftLazyValue {
        session.cfirCachesFactory.createCache<FqName, Set<Name>?, Nothing?> { packageFqName, _ ->
            computeTopLevelClassifierNames(packageFqName)
        }
    }

    /**
     * 顶层 callable 包名缓存。
     *
     * 若子类没有专门计算，则复用通用包名缓存。
     */
    private val topLevelCallablePackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (hasSpecificCallablePackageNamesComputation) {
            computePackageNamesWithTopLevelCallables()?.let { return@lazy it }
        }
        cachedPackageNames
    }

    /**
     * 按包名缓存顶层 callable 名称集合。
     */
    private val topLevelCallableNamesByPackage = session.cfirCachesFactory.createPossiblySoftLazyValue {
        session.cfirCachesFactory.createCache<FqName, Set<Name>?, Nothing?> { packageFqName, _ ->
            computeTopLevelCallableNames(packageFqName)
        }
    }

    /**
     * 返回缓存后的包名集合。
     */
    override fun getPackageNames(): Set<String>? = cachedPackageNames

    /**
     * 返回缓存后的顶层 classifier 包名集合。
     */
    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? = topLevelClassifierPackageNames

    /**
     * 返回缓存后的顶层 classifier 名称集合。
     *
     * 当包名集合可知且 [packageFqName] 不在集合内时，直接返回空集合。
     */
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? {
        val packageNames = getPackageNamesWithTopLevelClassifiers()
        if (packageNames != null && packageFqName.asString() !in packageNames) return emptySet()
        return getTopLevelClassifierNamesInPackageSkippingPackageCheck(packageFqName)
    }

    /**
     * 跳过包存在性预检，直接读取指定包的 classifier 名称缓存。
     */
    protected fun getTopLevelClassifierNamesInPackageSkippingPackageCheck(packageFqName: FqName): Set<Name>? =
        topLevelClassifierNamesByPackage.getValue().getValue(packageFqName)

    /**
     * 返回缓存后的顶层 callable 包名集合。
     */
    override fun getPackageNamesWithTopLevelCallables(): Set<String>? = topLevelCallablePackageNames

    /**
     * 返回缓存后的顶层 callable 名称集合。
     *
     * 当包名集合可知且 [packageFqName] 不在集合内时，直接返回空集合。
     */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? {
        val packageNames = getPackageNamesWithTopLevelCallables()
        if (packageNames != null && packageFqName.asString() !in packageNames) return emptySet()
        return topLevelCallableNamesByPackage.getValue().getValue(packageFqName)
    }
}

/**
 * 将普通 [CfirSymbolNamesProvider] 包装为缓存 provider。
 */
class CfirDelegatingCachedSymbolNamesProvider(
    session: CfirSession,
    /**
     * 被缓存包装的原始名称 provider。
     */
    private val delegate: CfirSymbolNamesProvider,
) : CfirCachedSymbolNamesProvider(session) {
    /**
     * 委托计算包名集合。
     */
    override fun computePackageNames(): Set<String>? = delegate.getPackageNames()

    /**
     * classifier 包集合计算能力来自委托 provider。
     */
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = delegate.hasSpecificClassifierPackageNamesComputation

    /**
     * 委托计算 classifier 包集合。
     */
    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? =
        delegate.getPackageNamesWithTopLevelClassifiers()

    /**
     * 委托计算指定包内的 classifier 名称。
     */
    override fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>? =
        delegate.getTopLevelClassifierNamesInPackage(packageFqName)

    /**
     * callable 包集合计算能力来自委托 provider。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = delegate.hasSpecificCallablePackageNamesComputation

    /**
     * 委托计算 callable 包集合。
     */
    override fun computePackageNamesWithTopLevelCallables(): Set<String>? =
        delegate.getPackageNamesWithTopLevelCallables()

    /**
     * 委托计算指定包内的 callable 名称。
     */
    override fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>? =
        delegate.getTopLevelCallableNamesInPackage(packageFqName)

    /**
     * 合成函数类型能力来自委托 provider。
     */
    override val mayHaveSyntheticFunctionTypes: Boolean
        get() = delegate.mayHaveSyntheticFunctionTypes

    /**
     * 委托判断指定合成函数类型是否可能存在。
     */
    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean =
        delegate.mayHaveSyntheticFunctionType(classId)
}

/**
 * 缓存化的 composite 名称 provider。
 */
open class CfirCompositeCachedSymbolNamesProvider(
    session: CfirSession,
    /**
     * 被聚合的子名称 provider。
     */
    val providers: List<CfirSymbolNamesProvider>,
) : CfirCachedSymbolNamesProvider(session) {
    /**
     * 合并所有子 provider 的包名集合。
     */
    override fun computePackageNames(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNames() }

    /**
     * 任意子 provider 有专门 classifier 包计算时为 `true`。
     */
    override val hasSpecificClassifierPackageNamesComputation: Boolean =
        providers.any { it.hasSpecificClassifierPackageNamesComputation }

    /**
     * 合并所有子 provider 的 classifier 包集合。
     */
    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelClassifiers() }

    /**
     * 合并指定包内所有子 provider 的 classifier 名称。
     */
    override fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.getTopLevelClassifierNamesInPackage(packageFqName) }

    /**
     * 任意子 provider 有专门 callable 包计算时为 `true`。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean =
        providers.any { it.hasSpecificCallablePackageNamesComputation }

    /**
     * 合并所有子 provider 的 callable 包集合。
     */
    override fun computePackageNamesWithTopLevelCallables(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelCallables() }

    /**
     * 合并指定包内所有子 provider 的 callable 名称。
     */
    override fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.getTopLevelCallableNamesInPackage(packageFqName) }

    /**
     * 任意子 provider 可能拥有合成函数类型时为 `true`。
     */
    override val mayHaveSyntheticFunctionTypes: Boolean =
        providers.any { it.mayHaveSyntheticFunctionTypes }

    /**
     * 任意子 provider 可能提供指定合成函数类型时为 `true`。
     */
    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean =
        providers.any { it.mayHaveSyntheticFunctionType(classId) }

    /**
     * 缓存名称 provider 组合工厂。
     */
    companion object {
        /**
         * 根据 provider 数量折叠为 empty、single cached wrapper 或 composite cached provider。
         */
        fun create(session: CfirSession, providers: List<CfirSymbolNamesProvider>): CfirSymbolNamesProvider = when (providers.size) {
            0 -> CfirEmptySymbolNamesProvider
            1 -> when (val provider = providers.single()) {
                is CfirCachedSymbolNamesProvider -> provider
                else -> CfirDelegatingCachedSymbolNamesProvider(session, provider)
            }
            else -> CfirCompositeCachedSymbolNamesProvider(session, providers)
        }

        /**
         * 从 symbol provider 提取并组合缓存化名称 provider。
         */
        fun fromSymbolProviders(session: CfirSession, providers: List<CfirSymbolProvider>): CfirSymbolNamesProvider =
            create(session, providers.map { it.symbolNamesProvider })
    }
}
