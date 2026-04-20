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

    open val mayHaveSyntheticFunctionTypes: Boolean
        get() = false

    open fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean = mayHaveSyntheticFunctionTypes

    open fun mayHaveTopLevelClassifier(classId: ClassId): Boolean {
        val names = getTopLevelClassifierNamesInPackage(classId.packageFqName) ?: return true
        return names.mayContainTopLevelClassifier(classId.shortClassName)
    }

    open fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean {
        if (name.isSpecial) return true
        val names = getTopLevelCallableNamesInPackage(packageFqName) ?: return true
        return name in names
    }
}

private fun Set<Name>.mayContainTopLevelClassifier(shortClassName: Name): Boolean {
    return shortClassName.isSpecial || shortClassName in this
}

object CfirNullSymbolNamesProvider : CfirSymbolNamesProvider() {
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? = null

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null

    override val mayHaveSyntheticFunctionTypes: Boolean
        get() = true

    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean = true

    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean = true

    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = true
}

object CfirEmptySymbolNamesProvider : CfirSymbolNamesProvider() {
    override fun getPackageNames(): Set<String> = emptySet()

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = false

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = false

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()

    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean = false

    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = false
}

abstract class CfirSymbolNamesProviderWithoutCallables : CfirSymbolNamesProvider() {
    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = true

    override fun getPackageNamesWithTopLevelCallables(): Set<String> = emptySet()

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()

    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = false
}

open class CfirCompositeSymbolNamesProvider(
    val providers: List<CfirSymbolNamesProvider>,
) : CfirSymbolNamesProvider() {
    override fun getPackageNames(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNames() }

    override val hasSpecificClassifierPackageNamesComputation: Boolean =
        providers.any { it.hasSpecificClassifierPackageNamesComputation }

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelClassifiers() }

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.getTopLevelClassifierNamesInPackage(packageFqName) }

    override val hasSpecificCallablePackageNamesComputation: Boolean =
        providers.any { it.hasSpecificCallablePackageNamesComputation }

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelCallables() }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.getTopLevelCallableNamesInPackage(packageFqName) }

    override val mayHaveSyntheticFunctionTypes: Boolean =
        providers.any { it.mayHaveSyntheticFunctionTypes }

    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean =
        providers.any { it.mayHaveSyntheticFunctionType(classId) }

    companion object {
        fun create(providers: List<CfirSymbolNamesProvider>): CfirSymbolNamesProvider = when (providers.size) {
            0 -> CfirEmptySymbolNamesProvider
            1 -> providers.single()
            else -> CfirCompositeSymbolNamesProvider(providers)
        }

        fun fromSymbolProviders(providers: List<CfirSymbolProvider>): CfirSymbolNamesProvider =
            create(providers.map { it.symbolNamesProvider })
    }
}
