package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.utils.flatMapToNullableSet

/**
 * 对齐 Kotlin FIR `FirSymbolNamesProvider` 的名称快速过滤接口。
 *
 * 该接口只表达“某个 provider 可能提供哪些名字”，允许 false positive，
 * 但不允许 false negative。
 */
abstract class CfirSymbolNamesProvider {
    /**
     * 返回该 provider 作用域内，包含任意顶层声明的包名集合。
     *
     * `null` 表示无法有效计算。
     */
    open fun getPackageNames(): Set<String>? = null

    /**
     * 当前 provider 是否对 classifier 包集合做了专门计算，而不是直接退回 [getPackageNames]。
     */
    abstract val hasSpecificClassifierPackageNamesComputation: Boolean

    /**
     * 返回包含顶层 classifier 的包名集合。
     */
    open fun getPackageNamesWithTopLevelClassifiers(): Set<String>? = getPackageNames()

    /**
     * 返回指定包内可能存在的顶层 classifier 名称集合。
     */
    abstract fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>?

    /**
     * 当前 provider 是否对 callable 包集合做了专门计算，而不是直接退回 [getPackageNames]。
     */
    abstract val hasSpecificCallablePackageNamesComputation: Boolean

    /**
     * 返回包含顶层 callable 的包名集合。
     */
    open fun getPackageNamesWithTopLevelCallables(): Set<String>? = getPackageNames()

    /**
     * 返回指定包内可能存在的顶层 callable 名称集合。
     */
    abstract fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>?

    /**
     * 当前 provider 是否可能生成函数类型对应的合成 classifier。
     */
    open val mayHaveSyntheticFunctionTypes: Boolean
        get() = false

    /**
     * 判断指定 [classId] 是否可能是该 provider 合成出的函数类型。
     */
    open fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean = mayHaveSyntheticFunctionTypes

    /**
     * 判断指定 class-like 顶层声明是否可能由该 provider 提供。
     *
     * 名称索引不可计算时返回 `true`，保持快速过滤的保守性。
     */
    open fun mayHaveTopLevelClassifier(classId: ClassId): Boolean {
        val names = getTopLevelClassifierNamesInPackage(classId.packageFqName) ?: return true
        return names.mayContainTopLevelClassifier(classId.shortClassName)
    }

    /**
     * 判断指定顶层 callable 是否可能由该 provider 提供。
     *
     * 特殊名字和不可计算的名称索引都会返回 `true`，避免过滤掉真实声明。
     */
    open fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean {
        if (name.isSpecial) return true
        val names = getTopLevelCallableNamesInPackage(packageFqName) ?: return true
        return name in names
    }
}

/**
 * 在短名集合中保守判断顶层 classifier 是否可能存在。
 */
private fun Set<Name>.mayContainTopLevelClassifier(shortClassName: Name): Boolean {
    return shortClassName.isSpecial || shortClassName in this
}

/**
 * 无名称信息 provider。
 *
 * 所有 `mayHave` 查询都返回 `true`，表示调用方必须继续进入真实 symbol lookup。
 */
object CfirNullSymbolNamesProvider : CfirSymbolNamesProvider() {
    /**
     * 无法枚举包名，返回 `null`。
     */
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false

    /**
     * 无法枚举 classifier 名称，返回 `null`。
     */
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? = null

    /**
     * 无法枚举 callable 包名，返回 `false` 让调用方退回通用包名语义。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false

    /**
     * 无法枚举 callable 名称，返回 `null`。
     */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null

    /**
     * 未知 provider 可能拥有合成函数类型。
     */
    override val mayHaveSyntheticFunctionTypes: Boolean
        get() = true

    /**
     * 未知 provider 对任意 class id 均保持可能命中。
     */
    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean = true

    /**
     * 未知 provider 对任意顶层 classifier 均保持可能命中。
     */
    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean = true

    /**
     * 未知 provider 对任意顶层 callable 均保持可能命中。
     */
    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = true
}

/**
 * 空名称 provider。
 *
 * 该对象精确表达“没有任何包、classifier、callable 或合成函数类型”。
 */
object CfirEmptySymbolNamesProvider : CfirSymbolNamesProvider() {
    /**
     * 空 provider 没有任何包。
     */
    override fun getPackageNames(): Set<String> = emptySet()

    /**
     * 空 provider 不需要额外 classifier 包计算。
     */
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false

    /**
     * 空 provider 没有顶层 classifier 名称。
     */
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()

    /**
     * 空 provider 不需要额外 callable 包计算。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false

    /**
     * 空 provider 没有顶层 callable 名称。
     */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()

    /**
     * 空 provider 永远不提供顶层 classifier。
     */
    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean = false

    /**
     * 空 provider 永远不提供顶层 callable。
     */
    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = false
}

/**
 * 不包含顶层 callable 的名称 provider 基类。
 */
abstract class CfirSymbolNamesProviderWithoutCallables : CfirSymbolNamesProvider() {
    /**
     * 明确声明 callable 包集合可精确计算为空集合。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = true

    /**
     * 返回空 callable 包集合。
     */
    override fun getPackageNamesWithTopLevelCallables(): Set<String> = emptySet()

    /**
     * 返回空 callable 名称集合。
     */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()

    /**
     * 该 provider 永远不提供顶层 callable。
     */
    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = false
}

/**
 * 组合多个名称 provider，按 Kotlin FIR 的 nullable set 语义聚合快速过滤信息。
 *
 * 任何一个子 provider 返回 `null` 时，对应聚合结果也保守返回 `null`。
 */
open class CfirCompositeSymbolNamesProvider(
    /**
     * 按查询优先级排列的子名称 provider。
     */
    val providers: List<CfirSymbolNamesProvider>,
) : CfirSymbolNamesProvider() {
    /**
     * 合并所有子 provider 的包名集合。
     */
    override fun getPackageNames(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNames() }

    /**
     * 任意子 provider 拥有专门 classifier 包计算时为 `true`。
     */
    override val hasSpecificClassifierPackageNamesComputation: Boolean =
        providers.any { it.hasSpecificClassifierPackageNamesComputation }

    /**
     * 合并所有包含顶层 classifier 的包名集合。
     */
    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelClassifiers() }

    /**
     * 合并指定包内可能存在的顶层 classifier 名称。
     */
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.getTopLevelClassifierNamesInPackage(packageFqName) }

    /**
     * 任意子 provider 拥有专门 callable 包计算时为 `true`。
     */
    override val hasSpecificCallablePackageNamesComputation: Boolean =
        providers.any { it.hasSpecificCallablePackageNamesComputation }

    /**
     * 合并所有包含顶层 callable 的包名集合。
     */
    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelCallables() }

    /**
     * 合并指定包内可能存在的顶层 callable 名称。
     */
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
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
     * 名称 provider 组合工厂。
     */
    companion object {
        /**
         * 根据 provider 数量折叠为 empty、single 或 composite provider。
         */
        fun create(providers: List<CfirSymbolNamesProvider>): CfirSymbolNamesProvider = when (providers.size) {
            0 -> CfirEmptySymbolNamesProvider
            1 -> providers.single()
            else -> CfirCompositeSymbolNamesProvider(providers)
        }

        /**
         * 从 symbol provider 列表提取并组合名称 provider。
         */
        fun fromSymbolProviders(providers: List<CfirSymbolProvider>): CfirSymbolNamesProvider =
            create(providers.map { it.symbolNamesProvider })
    }
}
