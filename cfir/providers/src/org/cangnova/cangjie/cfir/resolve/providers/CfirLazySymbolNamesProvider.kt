package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 首次查询时才创建真实名称索引的代理。
 *
 * 组合符号提供器原本在构造期就读取每个子 provider 的 [CfirSymbolProvider.symbolNamesProvider] 及其能力标志；
 * 库符号提供器的名称索引依赖声明提供器，IDE 宿主创建声明提供器会遍历搜索作用域并触碰 stub 索引，
 * 在 session 构造期求值会让 builtins session 构造同线程重入 `.cjo` stub 构建。代理把求值推迟到第一次真实查询。
 *
 * 代理在解析前不返回任何“保守值”：`CfirCompositeSymbolNamesProvider` 用 `flatMapToNullableSet` 聚合，
 * 任一子 provider 返回 `null` 会让整个聚合变成 `null`，`CfirCachedSymbolNamesProvider` 随后按“无顶层
 * classifier 的包”过滤，符号凭空消失。因此这里让任何一次访问（含能力标志）都先创建真实索引，
 * 并由组合器改为按需读取标志，保证组合器构造本身不触发求值。 */
class CfirLazySymbolNamesProvider(
    /** 真实名称索引的创建入口，只会被调用一次。 */
    private val createDelegate: () -> CfirSymbolNamesProvider,
) : CfirSymbolNamesProvider() {
    private val delegateRef = lazy(LazyThreadSafetyMode.PUBLICATION, createDelegate)

    /** 真实索引；任何一次查询（含能力标志）都先创建它。 */
    private fun delegate(): CfirSymbolNamesProvider = delegateRef.value

    override fun getPackageNames(): Set<String>? = delegate().getPackageNames()

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? =
        delegate().getPackageNamesWithTopLevelClassifiers()

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = delegate().hasSpecificClassifierPackageNamesComputation

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
        delegate().getTopLevelClassifierNamesInPackage(packageFqName)

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        delegate().getPackageNamesWithTopLevelCallables()

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = delegate().hasSpecificCallablePackageNamesComputation

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        delegate().getTopLevelCallableNamesInPackage(packageFqName)

    override val mayHaveSyntheticFunctionTypes: Boolean
        get() = delegate().mayHaveSyntheticFunctionTypes

    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean =
        delegate().mayHaveSyntheticFunctionType(classId)

    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean =
        delegate().mayHaveTopLevelClassifier(classId)

    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean =
        delegate().mayHaveTopLevelCallable(packageFqName, name)
}
