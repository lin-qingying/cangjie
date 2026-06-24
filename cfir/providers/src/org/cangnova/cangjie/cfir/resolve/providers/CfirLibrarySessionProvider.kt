package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 仅承载 library symbol provider 的 provider 壳。
 *
 * 库端没有源码 container file 真相，因此 file 相关查询默认不可用。
 */
class CfirLibrarySessionProvider(
    /**
     * 库 session 暴露的符号 provider。
     */
    override val symbolProvider: CfirSymbolProvider,
) : CfirProvider() {
    /**
     * 通过 library symbol provider 反取 class-like 声明。
     */
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? =
        symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir

    /**
     * library provider 不维护源码容器文件，调用该方法表示上层职责边界错误。
     */
    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile = shouldNotBeCalled()

    /**
     * library provider 没有源码容器文件。
     */
    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = null

    /**
     * library callable 没有源码容器文件。
     */
    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = null

    /**
     * library provider 不暴露源码文件列表。
     */
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()

    /**
     * library provider 的 class 名称查询应通过 [symbolProvider] 的名称索引完成。
     */
    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = shouldNotBeCalled()

    /**
     * 标记 source-only provider API 被错误用于 library provider。
     */
    private fun shouldNotBeCalled(): Nothing = error("Should not be called for CfirLibrarySessionProvider")
}
