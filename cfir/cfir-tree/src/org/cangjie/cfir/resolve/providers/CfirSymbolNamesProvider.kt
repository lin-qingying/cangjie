package org.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 名称快速过滤接口。
 *
 * 提供包级别的"可能存在"快速过滤，避免对每个名称都做完整符号查找。
 * 对齐 Kotlin K2 的 FirSymbolNamesProvider。
 */
interface CfirSymbolNamesProvider {

    /** 该 provider 可以提供符号的包名集合（null = 不确定，不过滤） */
    fun getPackageNames(): Set<FqName>?

    /** 指定包下可能存在的顶级分类器名称（null = 不确定） */
    fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>?

    /** 指定包下可能存在的顶级可调用名称（null = 不确定） */
    fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>?

    companion object {
        /** 不过滤任何名称的默认实现 */
        val NO_FILTERING: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider {
            override fun getPackageNames(): Set<FqName>? = null
            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? = null
            override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null
        }
    }
}
