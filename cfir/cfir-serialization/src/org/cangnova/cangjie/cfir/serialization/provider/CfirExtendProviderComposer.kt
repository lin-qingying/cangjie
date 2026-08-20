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
 * 主编译器（`CfirAbstractSessionFactory`）与 low-level/IDE 侧（`LLCfirAbstractSessionFactory`）
 * 都以"own 源码 extend 索引 + 依赖反序列化注册表"构造 session 级 extend 视图。该装配语义
 * 必须只有一个实现，否则两处行为漂移会导致库（.cjo）中的 extend 对某一侧不可见。
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
     * 惰性版本的 [fromSymbolProviders]。
     *
     * [providersRef] 延迟捕获依赖符号 provider 集合：extend provider 的注册早于依赖 provider 创建，
     * 只能在首次 extend 查询时求值，避免 eager session 创建。提取结果缓存，与
     * `LLDependenciesSymbolProvider.providers` 的 `by lazy` 语义一致——依赖 provider 集合在 session
     * 生命周期内稳定，IDE 依赖变更会重建 session。
     */
    fun lazyFromSymbolProviders(providersRef: () -> List<CfirSymbolProvider>): CfirExtendProvider {
        return object : CfirExtendProvider {
            private val delegate: CfirExtendProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
                fromSymbolProviders(providersRef())
            }

            override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> =
                delegate.getExtendsForTarget(targetKey)

            override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
                delegate.getExtendsForClass(classId)

            override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
                delegate.getExtendsInPackage(packageFqName)

            override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> =
                delegate.getExtendsForBuiltinType(kind)

            override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? =
                delegate.getContainingExtend(symbol)

            override fun getPackageFqName(extend: CfirExtend): FqName? =
                delegate.getPackageFqName(extend)

            override fun getContainingFile(extend: CfirExtend): CfirFile? =
                delegate.getContainingFile(extend)

        }
    }
}
