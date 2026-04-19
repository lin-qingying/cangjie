package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 名称快速过滤接口。
 *
 * 提供包级别的"可能存在"快速过滤，避免对每个名称都做完整符号查找。
 * 对齐 Kotlin K2 的 FirSymbolNamesProvider。
 */
abstract class CfirSymbolNamesProvider {

    /** 该 provider 可以提供符号的包名集合（null = 不确定，不过滤） */
   open fun getPackageNames(): Set<FqName>?  = null

    /** 指定包下可能存在的顶级分类器名称（null = 不确定） */
    abstract  fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>?
    /**
     * Checks if the provider's scope may contain a top-level callable (function or property) called [name] inside the [packageFqName]
     * package.
     */
    open fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean {
        // Symbol providers can potentially provide symbols for special names. Hence, special names have to be allowed.
        if (name.isSpecial) return true

        // `packageNamesWithTopLevelCallables` is checked in `FirCachedSymbolNamesProvider.getTopLevelCallableNamesInPackage`. It is not
        // worth checking it in uncached situations, since building the package set is as or more expensive as just building the "names in
        // package" set.
        val names = getTopLevelCallableNamesInPackage(packageFqName) ?: return true
        return name in names
    }
    /** 指定包下可能存在的顶级可调用名称（null = 不确定） */
  abstract  fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>?

    companion object {
        /** 不过滤任何名称的默认实现 */
        val NO_FILTERING: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider() {
            override fun getPackageNames(): Set<FqName>? = null
            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? = null
            override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null
        }
    }
}
