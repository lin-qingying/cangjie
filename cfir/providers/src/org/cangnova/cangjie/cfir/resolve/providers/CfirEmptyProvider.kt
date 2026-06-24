package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 空 provider，对齐 Kotlin FIR 的 empty provider。
 */
class CfirEmptyProvider(
    /**
     * 空声明 provider 对应的符号 provider。
     */
    override val symbolProvider: CfirSymbolProvider,
) : CfirProvider() {
    /**
     * 空 provider 不包含任何 classifier 声明。
     */
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? = null

    /**
     * 空 provider 不存在容器文件，调用该方法代表上层错误地跳过了可空查询。
     */
    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile =
        error("No containing file in CfirEmptyProvider")

    /**
     * 空 provider 不包含任何 classifier 容器文件。
     */
    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = null

    /**
     * 空 provider 不包含任何 callable 容器文件。
     */
    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = null

    /**
     * 空 provider 不包含任何 CFIR 文件。
     */
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = emptyList()

    /**
     * 空 provider 不包含任何 class-like 短名。
     */
    override fun getClassNamesInPackage(fqName: FqName): Set<Name> = emptySet()
}
