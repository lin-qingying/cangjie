package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 首次查询时才创建真实名称索引的代理。
 *
 * 组合符号提供器在构造期就会读取每个子 provider 的 [CfirSymbolProvider.symbolNamesProvider]
 * （`CfirCompositeSymbolNamesProvider.fromSymbolProviders`），而库符号提供器的名称索引依赖
 * 声明提供器：IDE 宿主创建声明提供器会遍历搜索作用域并触碰 stub 索引。若 session 构造期求值，
 * builtins session 构造就会重入 `.cjo` stub 构建。代理把求值推迟到第一次真实查询，并保证只创建一次。
 *
 * 组合器在构造期读取的三个能力标志（`hasSpecific*PackageNamesComputation`、`mayHaveSyntheticFunctionTypes`）
 * 在真实索引尚未创建时按“可能具备”回答：这三项只决定调用方选择哪条包名查询路径，
 * 取保守值不会漏掉任何符号。
 */
class CfirLazySymbolNamesProvider(
    /** 真实名称索引的创建入口，只会被调用一次。 */
    private val createDelegate: () -> CfirSymbolNamesProvider,
) : CfirSymbolNamesProvider() {
    private val delegateRef = lazy(LazyThreadSafetyMode.PUBLICATION, createDelegate)

    /** 真实索引是否已经创建。 */
    private fun resolved(): CfirSymbolNamesProvider? = if (delegateRef.isInitialized()) delegateRef.value else null

    override fun getPackageNames(): Set<String>? = resolved()?.getPackageNames()

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? =
        resolved()?.getPackageNamesWithTopLevelClassifiers()

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = resolved()?.hasSpecificClassifierPackageNamesComputation ?: true

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
        resolved()?.getTopLevelClassifierNamesInPackage(packageFqName)

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        resolved()?.getPackageNamesWithTopLevelCallables()

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = resolved()?.hasSpecificCallablePackageNamesComputation ?: true

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        resolved()?.getTopLevelCallableNamesInPackage(packageFqName)

    override val mayHaveSyntheticFunctionTypes: Boolean
        get() = resolved()?.mayHaveSyntheticFunctionTypes ?: true

    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean =
        resolved()?.mayHaveSyntheticFunctionType(classId) ?: true

    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean =
        resolved()?.mayHaveTopLevelClassifier(classId) ?: true

    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean =
        resolved()?.mayHaveTopLevelCallable(packageFqName, name) ?: true
}
