package org.cangnova.cangjie.analysis.api.stubs

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 包维度的 stub 索引视图。
 *
 * 与 [CaStubFileProvider] 配套,把跨文件的"包内有哪些顶级声明"问题从 stub 索引提走,
 * 供补全、引用解析等场景以包为单位浏览候选。
 */
interface CaStubPackageIndex {
    /** 索引中已知的全部包 FqName 集合。 */
    fun getAvailablePackages(): Set<FqName>

    /** 指定包下顶级 classifier 的名称集合。 */
    fun getTopLevelClassifierNames(packageFqName: FqName): Set<Name>

    /** 指定包下顶级 callable 的名称集合。 */
    fun getTopLevelCallableNames(packageFqName: FqName): Set<Name>
}
