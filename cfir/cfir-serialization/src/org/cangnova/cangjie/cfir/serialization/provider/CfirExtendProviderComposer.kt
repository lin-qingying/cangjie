package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirEmptyExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 仓颉 extend provider 装配的单一共享入口。
 *
 * 主编译器（`CfirAbstractSessionFactory`）以"own 源码 extend 索引 + 依赖会话 extend provider"
 * 构造 session 级视图；low-level/IDE 侧（`LLCfirAbstractSessionFactory`）用同一个 composer
 * 组合 own 索引、builtins 与依赖会话的 `extendProviderOrNull`。该装配语义必须只有一个实现，
 * 否则两处行为漂移会导致库（.cjo）中的 extend 对某一侧不可见。
 *
 * 语义对齐官方编译器 `BuildImportedExtendMap`（`TypeCheckExtend.cpp`）：注册阶段对所有可见
 * 来源做全量合并、不做可见性预判；可见性判定留到消费端的会话级 accessibility checker，
 * 与官方 `IsExtendAccessible`（`ImportManager.cpp`）"注册全量、消费过滤"的设计一致。
 */
object CfirExtendProviderComposer {

    /**
     * 从库符号 provider 列表中构造 extend provider。
     *
     * 只有反序列化 provider 能提供二进制依赖中的 extend 元数据；当依赖中没有这类 provider 时返回
     * 空实现，避免库会话暴露不存在的 extend 查询能力。
     */
    fun fromSymbolProviders(providers: List<CfirSymbolProvider>): CfirExtendProvider {
        val deserializedProviders = providers
            .flatMap(CfirSymbolProvider::flattenDeserializedProviders)
            .distinct()
        return if (deserializedProviders.isEmpty()) {
            CfirEmptyExtendProvider()
        } else {
            CfirDeserializedExtendProvider(deserializedProviders)
        }
    }

    /**
     * 合并当前会话与依赖会话的 extend provider。
     *
     * 当前会话 provider 始终位于第一位，依赖 provider 去重后追加，保证源码 extend 查询优先使用
     * 当前模块索引，再回退到依赖模块。
     */
    fun combine(
        ownProvider: CfirExtendProvider,
        dependencyProviders: List<CfirExtendProvider>,
    ): CfirExtendProvider {
        val providers = buildList {
            add(ownProvider)
            addAll(dependencyProviders.filter { it !== ownProvider })
        }.distinct()
        return if (providers.size == 1) providers.single() else CfirCompositeExtendProvider(providers)
    }

    /**
     * 惰性合并当前源码索引与 depends-on/库依赖 extend provider。
     *
     * LL session 的 refinement modules 在注册自身 extend provider 时尚未必完成 session 构造，
     * 因而必须在首次查询时读取依赖 provider，不能在注册阶段提前创建依赖 session。
     */
    fun lazyCombine(
        ownProvider: CfirExtendProvider,
        dependencyProvidersRef: () -> List<CfirExtendProvider>,
    ): CfirExtendProvider = lazyProvider {
        combine(ownProvider, dependencyProvidersRef())
    }

    /**
     * 惰性版本的 [fromSymbolProviders]。
     *
     * [providersRef] 延迟捕获依赖符号 provider 集合：extend provider 的注册早于依赖 provider 创建，
     * 只能在首次 extend 查询时求值，避免 eager session 创建。提取结果缓存，与
     * `LLDependenciesSymbolProvider.providers` 的 `by lazy` 语义一致——依赖 provider 集合在 session
     * 生命周期内稳定，IDE 依赖变更会重建 session。
     */
    fun lazyFromSymbolProviders(providersRef: () -> List<CfirSymbolProvider>): CfirExtendProvider {
        return lazyProvider {
            fromSymbolProviders(providersRef())
        }
    }

    /**
     * 当前线程正在求值的 lazily composed provider（identity 集合）。
     *
     * 循环模块依赖会让 A→B→A 的 session extend provider 组合递归（`CfirCompositeExtendProvider`
     * 对子 provider 递归 flatMap）。重入时直接返回空结果，保证查询终止。
     *
     * 取舍：防护键是 **provider 实例 identity**，不是 `(实例, 查询种类+参数)`。因此同一线程对同一
     * session provider 的合法重入（例如 `LLCfirSessionExtendProvider` 索引构建期间的递归查询）
     * 也会被短路为空结果，而不是返回已发布的部分索引。这只会影响同线程递归重入，正常跨 session
     * 查询不受影响；如需更精细语义，可把键收窄为 `(实例, 查询参数)`。
     */
    private val evaluatingProviders: ThreadLocal<java.util.IdentityHashMap<CfirExtendProvider, Boolean>> =
        ThreadLocal.withInitial { java.util.IdentityHashMap() }

    private fun enter(provider: CfirExtendProvider): Boolean =
        evaluatingProviders.get().put(provider, true) != null

    private fun exit(provider: CfirExtendProvider) {
        evaluatingProviders.get().remove(provider)
    }

    /** 创建使用线程安全 lazy 初始化的 provider 代理（带循环重入防护）。 */
    private fun lazyProvider(providerRef: () -> CfirExtendProvider): CfirExtendProvider = object : CfirExtendProvider {
        private val delegate: CfirExtendProvider by lazy(LazyThreadSafetyMode.PUBLICATION, providerRef)

        override fun getAllExtends(): List<CfirExtend> {
            if (enter(this)) return emptyList()
            try {
                return delegate.getAllExtends()
            } finally {
                exit(this)
            }
        }

        override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> {
            if (enter(this)) return emptyList()
            try {
                return delegate.getExtendsForTarget(targetKey)
            } finally {
                exit(this)
            }
        }

        override fun getExtendsForClass(classId: ClassId): List<CfirExtend> {
            if (enter(this)) return emptyList()
            try {
                return delegate.getExtendsForClass(classId)
            } finally {
                exit(this)
            }
        }

        override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> {
            if (enter(this)) return emptyList()
            try {
                return delegate.getExtendsInPackage(packageFqName)
            } finally {
                exit(this)
            }
        }

        override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> {
            if (enter(this)) return emptyList()
            try {
                return delegate.getExtendsForBuiltinType(kind)
            } finally {
                exit(this)
            }
        }

        override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? {
            if (enter(this)) return null
            try {
                return delegate.getContainingExtend(symbol)
            } finally {
                exit(this)
            }
        }

        override fun getPackageFqName(extend: CfirExtend): FqName? {
            if (enter(this)) return null
            try {
                return delegate.getPackageFqName(extend)
            } finally {
                exit(this)
            }
        }

        override fun getContainingFile(extend: CfirExtend): CfirFile? {
            if (enter(this)) return null
            try {
                return delegate.getContainingFile(extend)
            } finally {
                exit(this)
            }
        }

        override fun getExtendByStructuralKey(key: String): CfirExtend? {
            if (enter(this)) return null
            try {
                return delegate.getExtendByStructuralKey(key)
            } finally {
                exit(this)
            }
        }
    }
}
